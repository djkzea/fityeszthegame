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
