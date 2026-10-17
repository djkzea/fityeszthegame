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
}
