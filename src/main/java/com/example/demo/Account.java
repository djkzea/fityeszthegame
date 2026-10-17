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

    public record Session(String accessToken, String refreshToken, String userId, String displayName) { }

    private final Supabase sb;
    private final Path sessionFile;

    private Session session;

    public Account(Supabase sb, Path sessionFile) {
        this.sb = sb;
        this.sessionFile = sessionFile;
    }

    public Optional<Session> session() {
        return Optional.ofNullable(session);
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

    public Session signIn(String identifier, String password) throws AuthException {

        Transport.Res res = call("POST", "/functions/v1/sign-in",
                write(new SignInBody(identifier.trim(), password)), null);

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode tokens = read(res.body());

        return finish(tokens.path("access_token").asText(), tokens.path("refresh_token").asText());
    }

    /**
     * Registers an account. An empty answer means Supabase wants the address
     * confirmed first - the sign-up worked, the session comes after the click.
     */
    public Optional<Session> signUp(String email, String username, String password) throws AuthException {

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

    public void signOut() {

        session = null;

        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }
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
                user.path("user_metadata").path("display_name").asText());

        store(session);

        return session;
    }

    private void store(Session s) {

        try {
            Path parent = sessionFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(sessionFile, Json.mapper().writeValueAsString(s));

            try {
                Files.setPosixFilePermissions(sessionFile, PosixFilePermissions.fromString("rw-------"));
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

    private record SignUpBody(String email, String password, Meta data) { }

    private record Meta(String display_name) { }

    private record RefreshBody(String refresh_token) { }
}
