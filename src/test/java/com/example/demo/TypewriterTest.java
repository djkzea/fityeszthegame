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
 * The line has to be written, not thrown on screen.
 *
 * The reported bug: the line before a choice skipped itself and the choices
 * came up on their own. It is the input, not the typewriter - when the UI
 * thread is held up, everything pressed meanwhile is delivered in one go, and
 * two Enters in the same breath mean "finish the line" and then "take the
 * choice". The second test is that pair.
 */
class TypewriterTest {

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
    void theLineIsNearlyFinishedBeforeItCountsAsFinished() throws Exception {

        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());

        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }

        SaveData saved = new SaveData("Zsombi", "Zsombi", 1, "lipotiOffer",
                30, 10, 1, List.of(), List.of(), List.of(), null, null,
                "2026-10-17T18:00:00Z");

        GameController controller = onFxThread(() -> {
            GameController c = new GameController(new Stage());
            c.resume(saved);
            return c;
        });

        GameUI ui = controller.getUI();

        int full = onFxThread(() -> ui.fullText().length());
        assertTrue(full > 60, "ez a jelenet tul rovid a mereshez: " + full);

        // the most that was on screen while the line was still being written
        int most = 0;

        for (int i = 0; i < 400; i++) {
            int now = onFxThread(ui::writtenSoFar);

            if (now >= full) {
                break;
            }

            most = Math.max(most, now);
            Thread.sleep(15);
        }

        assertTrue(most >= full * 0.9,
                "a sor veget egyszerre dobta ki: " + most + " / " + full + " karakter latszott");
    }

    @Test
    void twoEntersInOneBreathDoNotTakeTheChoice() throws Exception {

        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());

        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }

        SaveData saved = new SaveData("Zsombi", "Zsombi", 1, "lipotiOffer",
                30, 10, 1, List.of(), List.of(), List.of(), null, null,
                "2026-10-17T18:00:00Z");

        GameController controller = onFxThread(() -> {
            GameController c = new GameController(new Stage());
            c.resume(saved);
            return c;
        });

        GameUI ui = controller.getUI();

        // what a stalled thread hands over: both presses in the same pulse
        onFxThread(() -> {
            ui.pressEnterButton();
            ui.pressEnterButton();
            return null;
        });

        assertEquals(onFxThread(ui::fullText).length(), (int) onFxThread(ui::writtenSoFar),
                "az elso Enter nem irta ki a sort");
        assertEquals("lipotiOffer", onFxThread(controller::snapshot).scene(),
                "a masodik Enter elvitte a jelenetet, a jatekos olvasas nelkul maradt");

        // and the game is not jammed: a press a moment later does go through
        Thread.sleep(200);

        onFxThread(() -> {
            ui.pressEnterButton();
            return null;
        });

        assertNotEquals("lipotiOffer", onFxThread(controller::snapshot).scene(),
                "a kesobbi Enter sem lepett tovabb");
    }
}
