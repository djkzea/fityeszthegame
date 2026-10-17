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
                List.of(), null, null, updatedAt);
    }

    @Test
    void whatWasSavedIsWhatComesBack(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir);
        SaveData d = run(2, "lakatosRules", "2026-10-17T18:00:00Z");

        store.saveLocal(1, d);

        assertEquals(Optional.of(d), store.loadLocal(1));
    }

    @Test
    void noFileMeansNoSaveRatherThanAnError(@TempDir Path dir) {
        assertTrue(new SaveStore(dir.resolve("ures")).loadLocal(1).isEmpty());
    }

    @Test
    void aDamagedFileReadsAsNoSave(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("save-slot1.json"), "{ ez nem json");
        assertTrue(new SaveStore(dir).loadLocal(1).isEmpty());
    }

    @Test
    void savingTwiceLeavesNoTemporaryFileBehind(@TempDir Path dir) throws IOException {
        SaveStore store = new SaveStore(dir);
        store.saveLocal(1, run(1, "chapter1", "2026-10-17T18:00:00Z"));
        store.saveLocal(1, run(2, "chapter2", "2026-10-17T18:05:00Z"));

        try (var files = Files.list(dir)) {
            assertEquals(List.of("save-slot1.json"), files.map(p -> p.getFileName().toString()).toList());
        }
        assertEquals(2, store.loadLocal(1).orElseThrow().chapter());
    }

    @Test
    void theDirectoryIsCreatedIfItIsNotThereYet(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir.resolve("meg").resolve("nincs"));
        store.saveLocal(1, run(1, "chapter1", "2026-10-17T18:00:00Z"));
        assertTrue(Files.exists(store.fileFor(1)));
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

    @Test
    void eachSlotKeepsItsOwnRun(@TempDir Path dir) {

        SaveStore store = new SaveStore(dir);

        store.saveLocal(1, run(1, "chapter1", "2026-10-17T18:00:00Z"));
        store.saveLocal(3, run(5, "chapter5", "2026-10-17T19:00:00Z"));

        assertEquals(1, store.loadLocal(1).orElseThrow().chapter());
        assertTrue(store.loadLocal(2).isEmpty());
        assertEquals(5, store.loadLocal(3).orElseThrow().chapter());

        assertEquals(java.util.Set.of(1, 3), store.runs(null).keySet());
    }

    @Test
    void theCloudRunsComeBackUnderTheirOwnSlots(@TempDir Path dir) {

        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200,
                "[{\"slot\":2,\"player_name\":\"Zsombi\",\"chapter\":4,\"updated_at\":\"2026-10-17T19:00:00Z\"},"
                        + "{\"slot\":3,\"player_name\":\"Anna\",\"chapter\":1,\"updated_at\":\"2026-10-17T18:00:00Z\"}]"));

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        java.util.Map<Integer, SaveData> runs = store.fetchCloud("acc1");

        assertEquals(java.util.Set.of(2, 3), runs.keySet());
        assertEquals("Anna", runs.get(3).playerName());
        assertTrue(t.calls.get(0).url().contains("select=slot,"), t.calls.get(0).url());
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

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        store.pushCloud(1, run(3, "lakatosRules", "2026-10-17T18:00:00Z"), "u-1", "acc1");

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
    void aProjectWithoutTheMigrationStillGetsTheRun(@TempDir Path dir) {

        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(400,
                "{\"code\":\"42703\",\"message\":\"column saves.scene does not exist\"}"));
        t.answers.add(new Transport.Res(201, ""));

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        store.pushCloud(1, run(3, "lakatosRules", "2026-10-17T18:00:00Z"), "u-1", "acc1");

        assertEquals(2, t.calls.size(), "a masodik probalkozas elmaradt");

        String retry = t.calls.get(1).body();
        assertFalse(retry.contains("\"scene\""), retry);
        assertFalse(retry.contains("\"scenery\""), retry);
        assertFalse(retry.contains("\"log\""), retry);
        assertTrue(retry.contains("\"chapter\":3"), retry);
        assertTrue(retry.contains("\"user_id\":\"u-1\""), retry);
    }

    @Test
    void anEmptyCloudAnswerIsNoSave(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "[]"));

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aFailingCloudDoesNotTakeTheGameDown(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(503, "down"));

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
        store.pushCloud(1, run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
    }

    @Test
    void withoutAProjectNothingIsPushed(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir, null);
        store.pushCloud(1, run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aRunWithNoDecisionsNeverOverwritesTheAccount(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();

        SaveStore store = new SaveStore(dir,
                new Supabase("https://p.supabase.co", "anon-key", t));

        SaveData blank = new SaveData("Zsombi", "Zsombi", 1, "chapter1", 0, 0, 1,
                List.of(), List.of(), List.of(), null, null, "2026-10-17T18:00:00Z");

        store.pushCloud(1, blank, "u-1", "acc1");

        assertEquals(List.of(), t.calls);
    }
}
