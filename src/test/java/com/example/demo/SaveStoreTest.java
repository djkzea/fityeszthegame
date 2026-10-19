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

    private static FakeTransport cloudWith(String rows) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, rows));
        return t;
    }

    @Test
    void signingInKeepsBothTheAccountsRunAndTheGuestsOne(@TempDir Path dir) {

        FakeTransport t = cloudWith(
                "[{\"slot\":1,\"player_name\":\"Zsombi\",\"chapter\":5,\"updated_at\":\"2026-10-16T10:00:00Z\"}]");

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));

        // what was played as a guest, in the slot the account also uses
        store.saveLocal(1, run(2, "lakatosRules", "2026-10-17T20:00:00Z"));

        store.reconcile("u-1", "acc1");

        // the account's run takes its own slot back
        assertEquals(5, store.loadLocal(1).orElseThrow().chapter());

        // and the guest's run is still there, moved out of the way
        assertEquals(2, store.loadLocal(2).orElseThrow().chapter());
    }

    @Test
    void anUntouchedSlotJustTakesTheAccountsRun(@TempDir Path dir) {

        FakeTransport t = cloudWith(
                "[{\"slot\":3,\"player_name\":\"Anna\",\"chapter\":4,\"updated_at\":\"2026-10-16T10:00:00Z\"}]");

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));

        store.reconcile("u-1", "acc1");

        assertEquals("Anna", store.loadLocal(3).orElseThrow().playerName());
        assertTrue(store.loadLocal(1).isEmpty());
    }

    @Test
    void theSameRunOnBothSidesIsNotDuplicated(@TempDir Path dir) {

        FakeTransport t = cloudWith(
                "[{\"slot\":1,\"player_name\":\"Zsombi\",\"chapter\":2,\"updated_at\":\"2026-10-17T20:00:00Z\"}]");

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));
        store.saveLocal(1, run(2, "lakatosRules", "2026-10-17T20:00:00Z"));

        store.reconcile("u-1", "acc1");

        assertTrue(store.loadLocal(2).isEmpty(), "ugyanaz a futás nem foglalhat két helyet");
        assertTrue(store.loadLocal(3).isEmpty());
    }

    @Test
    void withEverySlotTakenTheGuestsRunIsStillNotDeleted(@TempDir Path dir) throws IOException {

        FakeTransport t = cloudWith(
                "[{\"slot\":1,\"player_name\":\"Zsombi\",\"chapter\":5,\"updated_at\":\"2026-10-16T10:00:00Z\"},"
                        + "{\"slot\":2,\"player_name\":\"Zsombi\",\"chapter\":6,\"updated_at\":\"2026-10-16T11:00:00Z\"},"
                        + "{\"slot\":3,\"player_name\":\"Zsombi\",\"chapter\":7,\"updated_at\":\"2026-10-16T12:00:00Z\"}]");

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));
        store.saveLocal(1, run(2, "lakatosRules", "2026-10-17T20:00:00Z"));

        store.reconcile("u-1", "acc1");

        assertEquals(5, store.loadLocal(1).orElseThrow().chapter());

        try (var files = Files.list(dir)) {
            assertTrue(files.anyMatch(f -> f.getFileName().toString().contains("-guest-")),
                    "a vendég futása nyomtalanul eltűnt");
        }
    }

    @Test
    void theSameMomentSpelledTwoWaysIsOneRun(@TempDir Path dir) {

        // Java writes Z, Postgres hands the same instant back as +00:00
        FakeTransport t = cloudWith(
                "[{\"slot\":1,\"player_name\":\"Zsombi\",\"chapter\":4,\"scene\":\"chapter4Meeting\","
                        + "\"updated_at\":\"2026-10-19T06:42:29.711238+00:00\"}]");

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));
        store.saveLocal(1, new SaveData("Zsombi", "Zsombi", 4, "chapter4Meeting", 0, 0, 1,
                List.of(), List.of(), List.of(), null, null, "2026-10-19T06:42:29.711238Z"));

        store.reconcile("u-1", "acc1");

        assertTrue(store.loadLocal(2).isEmpty(), "ugyanaz a futás nem másolódhat másik helyre");
        assertTrue(store.loadLocal(3).isEmpty());
        assertEquals(1, t.calls.size(), "nem volt mit feltölteni");
    }

    @Test
    void aCloudTimestampIsReadableByNewer() {

        SaveData fromCloud = new SaveData("Zsombi", "Zsombi", 4, null, 0, 0, 1,
                List.of(), List.of(), List.of(), null, null, "2026-10-19T06:42:29.711238+00:00");
        SaveData older = run(2, "chapter2", "2026-10-18T10:00:00Z");

        assertEquals(fromCloud, SaveStore.newer(older, fromCloud));
        assertEquals(fromCloud, SaveStore.newer(fromCloud, older));
    }

    @Test
    void aRunTheAccountHasNeverSeenIsSentUp(@TempDir Path dir) {

        FakeTransport t = cloudWith("[]");
        t.answers.add(new Transport.Res(201, ""));

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));
        store.saveLocal(2, run(7, "boss3Defeated", "2026-10-19T06:36:28Z"));

        store.reconcile("u-1", "acc1");

        assertEquals(2, t.calls.size(), "a feltöltés elmaradt");
        assertTrue(t.calls.get(1).body().contains("\"slot\":2"), t.calls.get(1).body());
        assertTrue(t.calls.get(1).body().contains("\"chapter\":7"), t.calls.get(1).body());
    }

    @Test
    void aRunCarriedFurtherHereGoesUpInsteadOfBeingOverwritten(@TempDir Path dir) {

        FakeTransport t = cloudWith(
                "[{\"slot\":1,\"player_name\":\"Zsombi\",\"chapter\":2,\"history\":[{\"choicePointId\":\"ch1.q1\",\"optionIndex\":0}],"
                        + "\"updated_at\":\"2026-10-18T10:00:00Z\"}]");
        t.answers.add(new Transport.Res(201, ""));

        SaveStore store = new SaveStore(dir, new Supabase("https://p.supabase.co", "anon-key", t));

        // the same run, played on past that point while offline
        store.saveLocal(1, new SaveData("Zsombi", "Zsombi", 5, "chapter5", 120, 30, 2,
                List.of(), List.of(new SaveData.Decision("ch1.q1", 0), new SaveData.Decision("ch2.q", 1)),
                List.of(), null, null, "2026-10-19T06:00:00Z"));

        store.reconcile("u-1", "acc1");

        assertEquals(5, store.loadLocal(1).orElseThrow().chapter(), "a régebbi felhős sor nem írhatja felül");
        assertTrue(store.loadLocal(2).isEmpty(), "ugyanaz a futás nem foglalhat másik helyet");
        assertTrue(t.calls.get(1).body().contains("\"chapter\":5"), t.calls.get(1).body());
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
