package com.example.demo;

import javafx.application.Platform;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The save and the restore, driven through the real UI.
 *
 * GameController writes to AppDirs.data(), which hangs off user.home - so the
 * property is pointed at a temporary directory first and this never touches
 * the player's own save.
 */
class ResumeSmokeTest {

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
    void aRestoredRunReopensItsSceneAndKeepsSaving() throws Exception {

        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());

        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }

        SaveData saved = new SaveData("Zsombi", "Zsombi", 1, "lipotiOffer",
                30, 10, 1, List.of("envelope1"),
                List.of(new SaveData.Decision("ch1.q1", 1)),
                List.of(new Chatlog.Entry("line", "LIPÓTI", "Régi sor.", null, null, 1)),
                null,
                new SaveData.Scenery(
                        "/com/example/demo/fityesz_art/bg/ch1_cafe.png",
                        "/com/example/demo/fityesz_art/characters/lipoti.png",
                        "LIPÓTI", "A KÖZVETÍTŐ", "EGY ELDUGOTT KÁVÉZÓ",
                        "1. FEJEZET · A TOBORZÁS"),
                "2026-10-17T18:00:00Z");

        GameController controller = onFxThread(() -> {
            GameController c = new GameController(new Stage());
            c.resume(saved);
            return c;
        });

        Path file = AppDirs.data().resolve("save-slot1.json");

        // the restored scene is on screen, and it saved itself from there
        SaveData first = onFxThread(controller::snapshot);
        assertEquals("lipotiOffer", first.scene());
        assertEquals(30, first.xp());
        assertEquals(10, first.lebukas());
        assertEquals(List.of("envelope1"), first.items());
        assertEquals(List.of(new SaveData.Decision("ch1.q1", 1)), first.history());
        assertEquals("Régi sor.", first.log().get(0).text());

        // The background is what a mid-chapter scene no longer sets for itself:
        // without it a restored run opened on a bare stage with only the ghost
        // title on it, which is what the player reported.
        assertEquals("/com/example/demo/fityesz_art/bg/ch1_cafe.png", first.scenery().background());
        assertEquals("EGY ELDUGOTT KÁVÉZÓ", first.scenery().location());
        assertEquals("1. FEJEZET · A TOBORZÁS", first.scenery().chapter());

        // Two presses forward: the first finishes the typewriter, the second is
        // the click that moves the scene on - the same as playing it.
        for (int i = 0; i < 2; i++) {
            onFxThread(() -> {
                controller.getUI().pressEnterButton();
                return null;
            });
        }

        assertTrue(Files.isRegularFile(file), "a mentes nem jott letre: " + file);

        SaveData onDisk = new SaveStore(file).loadLocal().orElseThrow();
        assertNotEquals("lipotiOffer", onDisk.scene(), "a jelenet nem lepett tovabb");
        assertEquals("Zsombi", onDisk.playerName());
        assertTrue(onDisk.log().size() > 1, "a naplo nem nott a kattintassal");
    }
}
