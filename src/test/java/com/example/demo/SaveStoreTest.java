package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveStoreTest {

    private static SaveData run(int chapter, String scene, String updatedAt) {
        return new SaveData("Zsombi", "Zsombi", chapter, scene, 10, 5, 1,
                List.of("envelope1"), List.of(new SaveData.Decision("ch1.q1", 0)),
                List.of(), null, updatedAt);
    }

    @Test
    void whatWasSavedIsWhatComesBack(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"));
        SaveData d = run(2, "lakatosRules", "2026-10-17T18:00:00Z");

        store.saveLocal(d);

        assertEquals(Optional.of(d), store.loadLocal());
    }

    @Test
    void noFileMeansNoSaveRatherThanAnError(@TempDir Path dir) {
        assertTrue(new SaveStore(dir.resolve("nincs.json")).loadLocal().isEmpty());
    }

    @Test
    void aDamagedFileReadsAsNoSave(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("save-slot1.json");
        Files.writeString(file, "{ ez nem json");
        assertTrue(new SaveStore(file).loadLocal().isEmpty());
    }

    @Test
    void savingTwiceLeavesNoTemporaryFileBehind(@TempDir Path dir) throws IOException {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"));
        store.saveLocal(run(1, "chapter1", "2026-10-17T18:00:00Z"));
        store.saveLocal(run(2, "chapter2", "2026-10-17T18:05:00Z"));

        try (var files = Files.list(dir)) {
            assertEquals(List.of("save-slot1.json"), files.map(p -> p.getFileName().toString()).toList());
        }
        assertEquals(2, store.loadLocal().orElseThrow().chapter());
    }

    @Test
    void theDirectoryIsCreatedIfItIsNotThereYet(@TempDir Path dir) {
        Path file = dir.resolve("meg").resolve("nincs").resolve("save-slot1.json");
        new SaveStore(file).saveLocal(run(1, "chapter1", "2026-10-17T18:00:00Z"));
        assertTrue(Files.exists(file));
    }

    @Test
    void theFresherRunWins() {
        SaveData older = run(1, "chapter1", "2026-10-17T18:00:00Z");
        SaveData newer = run(4, "chapter4", "2026-10-17T19:00:00Z");

        assertEquals(newer, SaveStore.newer(older, newer));
        assertEquals(newer, SaveStore.newer(newer, older));
    }

    @Test
    void aMissingSideIsNotAWinner() {
        SaveData only = run(1, "chapter1", "2026-10-17T18:00:00Z");

        assertEquals(only, SaveStore.newer(only, null));
        assertEquals(only, SaveStore.newer(null, only));
        assertFalse(SaveStore.newer(null, null) != null);
    }

    @Test
    void anUnreadableTimestampLosesToAReadableOne() {
        SaveData broken = run(1, "chapter1", "nem datum");
        SaveData good = run(2, "chapter2", "2026-10-17T18:00:00Z");

        assertEquals(good, SaveStore.newer(broken, good));
        assertEquals(good, SaveStore.newer(good, broken));
    }

    /** A transport that records what was asked and answers from a list. */
    private static final class FakeTransport implements Transport {

        record Call(String method, String url, java.util.Map<String, String> headers, String body) { }

        final List<Call> calls = new java.util.ArrayList<>();
        final List<Res> answers = new java.util.ArrayList<>();

        @Override
        public Res send(String method, String url, java.util.Map<String, String> headers, String body) {
            calls.add(new Call(method, url, headers, body));
            return answers.isEmpty() ? new Res(500, "") : answers.remove(0);
        }
    }

    @Test
    void theUpsertCarriesTheRowsOwnColumns(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(201, ""));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        store.pushCloud(run(3, "lakatosRules", "2026-10-17T18:00:00Z"), "u-1", "acc1");

        FakeTransport.Call call = t.calls.get(0);
        assertEquals("POST", call.method());
        assertEquals("https://p.supabase.co/rest/v1/saves?on_conflict=user_id,slot", call.url());
        assertEquals("Bearer acc1", call.headers().get("Authorization"));
        assertEquals("resolution=merge-duplicates,return=minimal", call.headers().get("Prefer"));
        assertTrue(call.body().startsWith("["), "a PostgREST upsert egy tombot kap");
        assertTrue(call.body().contains("\"user_id\":\"u-1\""), "user_id nelkul az RLS elutasitja");
        assertTrue(call.body().contains("\"slot\":1"));
        assertTrue(call.body().contains("\"scene\":\"lakatosRules\""));
    }

    @Test
    void theCloudRunComesBackFromTheFirstRow(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200,
                "[{\"player_name\":\"Zsombi\",\"chapter\":4,\"xp\":80,\"lebukas\":20,\"szint\":2,"
                        + "\"items\":[\"envelope1\"],\"history\":[],\"log\":[],"
                        + "\"updated_at\":\"2026-10-17T19:00:00Z\"}]"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        SaveData d = store.fetchCloud("acc1").orElseThrow();

        assertEquals(4, d.chapter());
        assertEquals("Zsombi", d.playerName());
        assertTrue(t.calls.get(0).url().contains("slot=eq.1"));
        assertEquals("GET", t.calls.get(0).method());
    }

    @Test
    void anEmptyCloudAnswerIsNoSave(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "[]"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aFailingCloudDoesNotTakeTheGameDown(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(503, "down"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
        store.pushCloud(run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
    }

    @Test
    void withoutAProjectNothingIsPushed(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"), null);
        store.pushCloud(run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aRunWithNoDecisionsNeverOverwritesTheAccount(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        SaveData blank = new SaveData("Zsombi", "Zsombi", 1, "chapter1", 0, 0, 1,
                List.of(), List.of(), List.of(), null, "2026-10-17T18:00:00Z");

        store.pushCloud(blank, "u-1", "acc1");

        assertEquals(List.of(), t.calls);
    }
}
