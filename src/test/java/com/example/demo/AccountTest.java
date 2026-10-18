package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    /** What was asked, and what we answer - no network in sight. */
    private static final class FakeTransport implements Transport {

        record Call(String method, String url, Map<String, String> headers, String body) { }

        final List<Call> calls = new ArrayList<>();
        final List<Res> answers = new ArrayList<>();

        @Override
        public Res send(String method, String url, Map<String, String> headers, String body) {
            calls.add(new Call(method, url, headers, body));
            return answers.isEmpty() ? new Res(500, "") : answers.remove(0);
        }
    }

    private static final String TOKENS =
            "{\"access_token\":\"acc1\",\"refresh_token\":\"ref1\"}";

    private static final String USER =
            "{\"id\":\"u-1\",\"user_metadata\":{\"display_name\":\"Zsombi\"}}";

    @Test
    void signingInGoesThroughTheEdgeFunction(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.Session s = account.signIn("zsombi", "titkos123");

        assertEquals("acc1", s.accessToken());
        assertEquals("u-1", s.userId());
        assertEquals("Zsombi", s.displayName());

        FakeTransport.Call signIn = t.calls.get(0);
        assertEquals("POST", signIn.method());
        assertEquals("https://p.supabase.co/functions/v1/sign-in", signIn.url());
        assertEquals("anon-key", signIn.headers().get("apikey"));
        assertTrue(signIn.body().contains("\"identifier\":\"zsombi\""));
        assertTrue(signIn.body().contains("\"password\":\"titkos123\""));

        FakeTransport.Call who = t.calls.get(1);
        assertEquals("GET", who.method());
        assertEquals("https://p.supabase.co/auth/v1/user", who.url());
        assertEquals("Bearer acc1", who.headers().get("Authorization"));
    }

    @Test
    void anAddressSignsInWithoutTheEdgeFunction(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertEquals("acc1", account.signIn("zsombi@pelda.hu", "titkos123").accessToken());

        assertEquals("https://p.supabase.co/auth/v1/token?grant_type=password", t.calls.get(0).url());
        assertTrue(t.calls.get(0).body().contains("\"email\":\"zsombi@pelda.hu\""));
    }

    @Test
    void aMissingEdgeFunctionSaysSoInsteadOfBlamingThePassword(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(404, "{\"code\":\"NOT_FOUND\"}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.AuthException e = assertThrows(Account.AuthException.class,
                () -> account.signIn("zsombi", "titkos123"));

        assertEquals("ui.authNoUsernames", e.messageKey());
    }

    @Test
    void theSessionOutlivesTheProcess(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Path file = dir.resolve("session.json");
        new Account(new Supabase("https://p.supabase.co", "anon-key", t), file)
                .signIn("zsombi", "titkos123");

        assertTrue(Files.isRegularFile(file));

        Account again = new Account(
                new Supabase("https://p.supabase.co", "anon-key", new FakeTransport()), file);
        assertEquals("Zsombi", again.restore().orElseThrow().displayName());
    }

    @Test
    void aRefusedSignInSaysNothingAboutWhy(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(400, "{\"error\":\"invalid_credentials\"}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.AuthException e = assertThrows(Account.AuthException.class,
                () -> account.signIn("zsombi", "rossz"));

        assertEquals("ui.authFailed", e.messageKey());
    }

    @Test
    void registeringCarriesTheUsernameAsMetadata(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "{\"id\":\"u-2\",\"email\":\"a@b.hu\"}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertTrue(account.signUp("a@b.hu", "zsombi", "titkos123").isEmpty());

        FakeTransport.Call signUp = t.calls.get(0);
        assertEquals("https://p.supabase.co/auth/v1/signup", signUp.url());
        assertTrue(signUp.body().contains("\"display_name\":\"zsombi\""),
                "a display_name nélkül a profiles trigger nem ír sort");
    }

    @Test
    void registeringThatReturnsASessionSignsStraightIn(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200,
                "{\"access_token\":\"acc1\",\"refresh_token\":\"ref1\","
                        + "\"user\":{\"id\":\"u-2\",\"user_metadata\":{\"display_name\":\"zsombi\"}}}"));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-2\",\"user_metadata\":{\"display_name\":\"zsombi\"}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertEquals("zsombi", account.signUp("a@b.hu", "zsombi", "titkos123").orElseThrow().displayName());
    }

    @Test
    void afreshInstallSignsInAsAGuestWithoutBeingAsked(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.Session guest = account.ensureSession().orElseThrow();

        assertTrue(guest.anonymous());
        assertFalse(guest.named(), "a vendégnek nincs neve, tehát nincs mibol kilepni");
        assertEquals("https://p.supabase.co/auth/v1/signup", t.calls.get(0).url());
        assertEquals("{\"data\":{}}", t.calls.get(0).body());

        assertTrue(Files.isRegularFile(dir.resolve("guest.json")), "a vendég identitás nem maradt meg");
    }

    @Test
    void aSecondStartReusesTheGuestInsteadOfMakingAnother(@TempDir Path dir) {
        FakeTransport first = new FakeTransport();
        first.answers.add(new Transport.Res(200, TOKENS));
        first.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Path file = dir.resolve("session.json");
        new Account(new Supabase("https://p.supabase.co", "anon-key", first), file).ensureSession();

        FakeTransport again = new FakeTransport();
        Account.Session back = new Account(new Supabase("https://p.supabase.co", "anon-key", again), file)
                .ensureSession().orElseThrow();

        assertEquals("u-guest", back.userId());
        assertEquals(java.util.List.of(), again.calls, "nem lett volna szabad újra hálózathoz nyúlnia");
    }

    @Test
    void registeringAsAGuestKeepsTheSameUserAndItsRuns(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));
        account.ensureSession();

        t.answers.add(new Transport.Res(200, "{\"id\":\"u-guest\",\"email\":\"a@b.hu\"}"));
        t.answers.add(new Transport.Res(201, ""));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":false,\"user_metadata\":{\"display_name\":\"zsombi\"}}"));

        Account.Session named = account.signUp("a@b.hu", "zsombi", "titkos123").orElseThrow();

        assertEquals("u-guest", named.userId(), "a felminősítés nem tarthatja meg a futásokat új user id-vel");
        assertTrue(named.named());

        FakeTransport.Call upgrade = t.calls.get(2);
        assertEquals("PUT", upgrade.method());
        assertEquals("https://p.supabase.co/auth/v1/user", upgrade.url());
        assertTrue(upgrade.body().contains("\"display_name\":\"zsombi\""), upgrade.body());

        FakeTransport.Call profile = t.calls.get(3);
        assertEquals("https://p.supabase.co/rest/v1/profiles", profile.url());
        assertTrue(profile.body().contains("\"user_id\":\"u-guest\""), profile.body());

        assertFalse(Files.exists(dir.resolve("guest.json")), "a vendégfájl feleslegessé vált");
    }

    @Test
    void aTakenUsernameSaysSo(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));
        account.ensureSession();

        t.answers.add(new Transport.Res(200, "{\"id\":\"u-guest\"}"));
        t.answers.add(new Transport.Res(409, "{\"code\":\"23505\"}"));

        Account.AuthException e = assertThrows(Account.AuthException.class,
                () -> account.signUp("a@b.hu", "foglalt", "titkos123"));

        assertEquals("ui.nameTaken", e.messageKey());
    }

    @Test
    void signingInToAnExistingAccountTakesTheGuestAwayWithIt(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "{\"access_token\":\"guest1\",\"refresh_token\":\"gr1\"}"));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));
        account.ensureSession();

        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));
        t.answers.add(new Transport.Res(200, "{\"deleted\":true}"));

        account.signIn("zsombi@pelda.hu", "titkos123");

        FakeTransport.Call cleanup = t.calls.get(t.calls.size() - 1);
        assertEquals("https://p.supabase.co/functions/v1/delete-account", cleanup.url());
        assertEquals("Bearer guest1", cleanup.headers().get("Authorization"),
                "a vendég saját tokenjével kell törölni, nem az új fiókéval");

        assertFalse(Files.exists(dir.resolve("guest.json")));
    }

    @Test
    void anUpgradedGuestIsNotDeleted(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));
        account.ensureSession();

        t.answers.add(new Transport.Res(200, "{\"id\":\"u-guest\",\"email\":\"a@b.hu\"}"));
        t.answers.add(new Transport.Res(201, ""));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":false,\"user_metadata\":{\"display_name\":\"zsombi\"}}"));

        account.signUp("a@b.hu", "zsombi", "titkos123");

        assertTrue(t.calls.stream().noneMatch(c -> c.url().contains("delete-account")),
                "a felminősített vendég maga a fiók - nem szabad törölni");
    }

    @Test
    void signingOutLeavesTheGameSavingAsAGuestAgain(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "{\"access_token\":\"guest1\",\"refresh_token\":\"gr1\"}"));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        Path file = dir.resolve("session.json");
        Account account = new Account(new Supabase("https://p.supabase.co", "anon-key", t), file);
        account.ensureSession();

        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));
        t.answers.add(new Transport.Res(200, "{\"deleted\":true}"));
        account.signIn("zsombi@pelda.hu", "titkos123");
        assertTrue(account.session().orElseThrow().named());

        // the first guest went with the sign-in, so leaving the account makes a new one
        t.answers.add(new Transport.Res(200, "{\"access_token\":\"guest2\",\"refresh_token\":\"gr2\"}"));
        t.answers.add(new Transport.Res(200,
                "{\"id\":\"u-guest-2\",\"is_anonymous\":true,\"user_metadata\":{}}"));

        account.signOut();

        Account.Session now = account.session().orElseThrow();
        assertTrue(now.anonymous(), "kilépés után is kell lennie kinek a nevében menteni");
        assertEquals("u-guest-2", now.userId());
    }

    @Test
    void signingOutForgetsTheSession(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Path file = dir.resolve("session.json");
        Account account = new Account(new Supabase("https://p.supabase.co", "anon-key", t), file);
        account.signIn("zsombi", "titkos123");

        account.signOut();

        assertFalse(Files.exists(file));
        assertTrue(account.session().isEmpty());
    }

    @Test
    void anExpiredTokenIsRefreshedOnce(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(401, "{\"error\":\"expired\"}"));
        t.answers.add(new Transport.Res(200, "{\"access_token\":\"acc2\",\"refresh_token\":\"ref2\"}"));
        t.answers.add(new Transport.Res(200, USER));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertEquals("acc2", account.signIn("zsombi", "titkos123").accessToken());

        assertEquals("https://p.supabase.co/auth/v1/token?grant_type=refresh_token",
                t.calls.get(2).url());
    }
}
