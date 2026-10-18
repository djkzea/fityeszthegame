package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

/**
 * Who is playing, against the website's accounts.
 *
 * Signing in goes through the sign-in Edge Function, the same way the website
 * does it: Supabase authenticates on an e-mail address, and resolving a
 * username to one in the client would make every username enumerable. The
 * function answers invalid_credentials to everything that fails, and so does
 * this class - one message, nothing learned.
 */
public final class Account {

    /** A refusal the player is allowed to see, as a Lang key. */
    public static final class AuthException extends Exception {

        private final String messageKey;

        AuthException(String messageKey) {
            super(messageKey);
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public record Session(String accessToken, String refreshToken, String userId, String displayName,
                          boolean anonymous) {

        /** A guest session, or one whose account has no name on it yet. */
        public boolean named() {
            return !anonymous && displayName != null && !displayName.isBlank();
        }
    }

    private final Supabase sb;
    private final Path sessionFile;

    /**
     * The guest identity, kept beside the real one.
     *
     * Signing out of an account returns the player to the guest they were
     * before it, rather than stranding the runs that guest has in the cloud.
     */
    private final Path guestFile;

    private Session session;

    /** Set when the address put on an account still has to be confirmed by e-mail. */
    private boolean emailPending = false;

    public Account(Supabase sb, Path sessionFile) {
        this.sb = sb;
        this.sessionFile = sessionFile;
        this.guestFile = sessionFile.resolveSibling("guest.json");
    }

    public Optional<Session> session() {
        return Optional.ofNullable(session);
    }

    /**
     * Whether the last registration left an address waiting on its confirmation
     * link. The session works either way; the address does not reach another
     * machine until the link is clicked.
     */
    public boolean emailPending() {
        return emailPending;
    }

    /** The session this machine remembers from last time. */
    public Optional<Session> restore() {

        if (session != null) {
            return Optional.of(session);
        }

        if (!Files.isRegularFile(sessionFile)) {
            return Optional.empty();
        }

        try {
            session = Json.mapper().readValue(Files.readString(sessionFile), Session.class);
            return Optional.ofNullable(session);

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Signs in with an address or a username, in that order of directness.
     *
     * An address is what Supabase authenticates on, so it goes straight to the
     * token endpoint - the same shortcut the website takes. A username has to be
     * resolved to an address first, and that only happens inside the sign-in
     * Edge Function, which holds the service role key; doing the lookup in a
     * client would make every username enumerable.
     *
     * If that function has not been deployed, a username cannot be signed in at
     * all, and saying so is more use than one more "wrong details".
     */
    /**
     * Makes sure there is somebody to save as, without asking the player anything.
     *
     * The first run worth saving signs in anonymously: the save then goes to an
     * account row from that moment on, and the player never saw a login screen.
     * The identity lives in this machine's guest file until they put an address
     * on it, which is what turns it into a real account they can also reach
     * from the website.
     *
     * Called when there is something to save, not at startup - somebody who
     * opens the game and closes it again should not leave an empty account
     * behind. Synchronized, so two saves at once cannot make two guests.
     *
     * Best effort throughout - no project, no network, or anonymous sign-ins
     * turned off on the project all mean the game plays on and saves locally.
     */
    public synchronized Optional<Session> ensureSession() {

        if (restore().isPresent()) {
            return Optional.of(session);
        }

        try {
            session = Json.mapper().readValue(Files.readString(guestFile), Session.class);
            store(session, sessionFile);
            return Optional.of(session);

        } catch (IOException noGuestYet) {
            // nothing saved on this machine; make one
        }

        try {
            return Optional.of(signInAnonymously());
        } catch (AuthException e) {
            System.err.println("account: " + e.messageKey());
            return Optional.empty();
        }
    }

    /** A new guest. The project has to allow anonymous sign-ins for this to work. */
    public Session signInAnonymously() throws AuthException {

        Transport.Res res = call("POST", "/auth/v1/signup", "{\"data\":{}}", null);

        if (!res.ok()) {
            throw new AuthException("ui.authOffline");
        }

        JsonNode tokens = read(res.body());

        Session guest = finish(tokens.path("access_token").asText(),
                tokens.path("refresh_token").asText());

        store(guest, guestFile);

        return guest;
    }

    public Session signIn(String identifier, String password) throws AuthException {

        String id = identifier.trim();

        // who we were a moment ago - a guest left behind by this sign-in has no
        // further use, and is cleaned up once the new session is in hand
        Session previous = session;

        if (id.contains("@")) {

            Transport.Res direct = call("POST", "/auth/v1/token?grant_type=password",
                    write(new PasswordBody(id, password)), null);

            if (direct.ok()) {
                JsonNode tokens = read(direct.body());
                Session now = finish(tokens.path("access_token").asText(),
                        tokens.path("refresh_token").asText());
                discardGuest(previous);
                return now;
            }

            throw new AuthException("ui.authFailed");
        }

        Transport.Res res = call("POST", "/functions/v1/sign-in",
                write(new SignInBody(id, password)), null);

        if (res.status() == 404) {
            throw new AuthException("ui.authNoUsernames");
        }

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode tokens = read(res.body());

        Session now = finish(tokens.path("access_token").asText(),
                tokens.path("refresh_token").asText());

        discardGuest(previous);

        return now;
    }

    /**
     * Throws away the guest the player has just walked away from.
     *
     * Registering upgrades the guest, so that one lives on as the account. But
     * signing in to an account that already existed leaves the guest behind
     * with nobody to claim it, and Supabase cannot merge two users - so it is
     * deleted, along with the rows that hang off it. The runs are not lost with
     * it: the local save files are what the account is reconciled against.
     *
     * Best effort. A guest that survives is clutter in the project, not a
     * problem for the player in front of us.
     */
    private void discardGuest(Session previous) {

        if (previous == null || !previous.anonymous()) {
            return;
        }

        if (session != null && previous.userId().equals(session.userId())) {
            return;    // the same user, upgraded rather than left behind
        }

        try {
            Transport.Res res = sb.call("POST", "/functions/v1/delete-account",
                    null, previous.accessToken());

            if (!res.ok()) {
                System.err.println("account: a vendég törlése nem sikerült (" + res.status() + ")");
            }

        } catch (IOException e) {
            System.err.println("account: " + e.getMessage());
        }

        try {
            Files.deleteIfExists(guestFile);
        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }
    }

    /**
     * Puts an address and a name on the player.
     *
     * A guest is upgraded rather than replaced: the same user id keeps its
     * runs, which is the whole point of signing in anonymously first. Somebody
     * with no session at all gets an ordinary sign-up.
     *
     * An empty answer means Supabase wants the address confirmed before the
     * account is usable from anywhere else - the session in hand keeps working
     * either way.
     */
    public Optional<Session> signUp(String email, String username, String password) throws AuthException {

        if (session != null && session.anonymous()) {
            return Optional.of(upgrade(email.trim(), username.trim(), password));
        }

        Transport.Res res = call("POST", "/auth/v1/signup",
                write(new SignUpBody(email.trim(), password, new Meta(username.trim()))), null);

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode node = read(res.body());

        if (node.hasNonNull("access_token")) {
            return Optional.of(finish(
                    node.path("access_token").asText(), node.path("refresh_token").asText()));
        }

        return Optional.empty();
    }

    /**
     * Turns the guest into a named account, keeping the user id - and with it
     * every run already saved under it.
     *
     * The profile row is written here rather than by the sign-up trigger: that
     * trigger fires on a new user, and this user is not new.
     */
    private Session upgrade(String email, String username, String password) throws AuthException {

        Transport.Res res = call("PUT", "/auth/v1/user",
                write(new UpgradeBody(email, password, new Meta(username))), session.accessToken());

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode updated = read(res.body());

        // With confirmations on, the address sits in new_email until the link in
        // the mail is clicked - the player is signed in here, but not yet anywhere else.
        emailPending = !updated.path("new_email").asText("").isBlank()
                || updated.path("email").asText("").isBlank();

        Transport.Res profile = call("POST", "/rest/v1/profiles",
                write(new ProfileBody(session.userId(), username)), session.accessToken());

        // 409 is the username's unique index saying somebody else has it
        if (profile.status() == 409) {
            throw new AuthException("ui.nameTaken");
        }

        Session named = finish(session.accessToken(), session.refreshToken());

        // the guest identity and the account are the same user now
        try {
            Files.deleteIfExists(guestFile);
        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }

        return named;
    }

    public void signOut() {

        session = null;

        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }

        ensureSession();
    }

    /**
     * Asks who the token belongs to, refreshing it once if it has expired, and
     * stores the session.
     */
    private Session finish(String accessToken, String refreshToken) throws AuthException {

        Transport.Res who = call("GET", "/auth/v1/user", null, accessToken);

        if (who.status() == 401) {

            Transport.Res refreshed = call("POST", "/auth/v1/token?grant_type=refresh_token",
                    write(new RefreshBody(refreshToken)), null);

            if (!refreshed.ok()) {
                throw new AuthException("ui.authFailed");
            }

            JsonNode tokens = read(refreshed.body());
            accessToken = tokens.path("access_token").asText();
            refreshToken = tokens.path("refresh_token").asText();

            who = call("GET", "/auth/v1/user", null, accessToken);
        }

        if (!who.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode user = read(who.body());

        session = new Session(
                accessToken,
                refreshToken,
                user.path("id").asText(),
                user.path("user_metadata").path("display_name").asText(),
                user.path("is_anonymous").asBoolean(false));

        store(session, sessionFile);

        return session;
    }

    private void store(Session s, Path file) {

        try {
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(file, Json.mapper().writeValueAsString(s));

            try {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows has no POSIX permissions; the file stays as it is.
            }

        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }
    }

    private Transport.Res call(String method, String path, String body, String token) throws AuthException {
        try {
            return sb.call(method, path, body, token);
        } catch (IOException e) {
            throw new AuthException("ui.authOffline");
        }
    }

    private static String write(Object value) throws AuthException {
        try {
            return Json.mapper().writeValueAsString(value);
        } catch (IOException e) {
            throw new AuthException("ui.authFailed");
        }
    }

    private static JsonNode read(String body) throws AuthException {
        try {
            return Json.mapper().readTree(body);
        } catch (IOException e) {
            throw new AuthException("ui.authFailed");
        }
    }

    private record SignInBody(String identifier, String password) { }

    private record PasswordBody(String email, String password) { }

    private record SignUpBody(String email, String password, Meta data) { }

    private record UpgradeBody(String email, String password, Meta data) { }

    private record ProfileBody(String user_id, String username) { }

    private record Meta(String display_name) { }

    private record RefreshBody(String refresh_token) { }
}
