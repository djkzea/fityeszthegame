package com.example.demo;

import javafx.application.Platform;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Throwing a run away.
 *
 * Two things have to hold: one press may not delete anything - it only asks -
 * and the second one has to actually take the file off the disk, or the list
 * would show a run that is no longer there.
 */
class DeleteRunTest {

    private static <T> T onFxThread(java.util.function.Supplier<T> work) throws Exception {

        AtomicReference<T> out = new AtomicReference<>();
        AtomicReference<Throwable> failed = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                out.set(work.get());
            } catch (Throwable t) {
                failed.set(t);
            } finally {
                done.countDown();
            }
        });

        assertTrue(done.await(20, TimeUnit.SECONDS), "a JavaFX szal nem valaszolt");

        if (failed.get() != null) {
            throw new AssertionError(failed.get());
        }

        return out.get();
    }

    @Test
    void theFirstPressAsksAndTheSecondDeletes() throws Exception {

        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());

        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }

        Path dir = Files.createTempDirectory("fityesz-saves");
        SaveStore store = new SaveStore(dir);

        store.saveLocal(2, new SaveData("Zsombi", "Zsombi", 3, "chapter3",
                120, 25, 2, List.of(), List.of(new SaveData.Decision("ch1.q1", 1)),
                List.of(), null, null, "2026-10-20T10:00:00Z"));

        assertTrue(store.loadLocal(2).isPresent(), "a mentes nem jott letre");

        OpeningScreen screen = onFxThread(() ->
                new OpeningScreen(i -> { }, () -> { }, () -> { }, r -> { }, null, store));

        onFxThread(() -> {
            screen.askDelete(2);
            return null;
        });

        assertTrue(store.loadLocal(2).isPresent(),
                "az elso nyomas meg csak kerdez, torolnie nem szabad");

        onFxThread(() -> {
            screen.askDelete(2);
            return null;
        });

        assertFalse(store.loadLocal(2).isPresent(),
                "a masodik nyomasra el kellett volna tunnie");
        assertFalse(Files.exists(store.fileFor(2)), "a fajl a helyen maradt");
    }
}
