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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A boss has to be worth the fight.
 *
 * All three used to be a race: an attack took the full twenty off whether the
 * boss had covered up or not, so what the boss did never mattered and four
 * presses of ATTACK ended any of them. Both numbers moved - the boss carries
 * more, and a hit into a guard barely lands - and this holds the floor that
 * follows from them, without depending on what the dice do.
 */
class BossFightTest {

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

    /** Lets the line finish writing itself, so the choices are there to press. */
    private static void readIt(GameUI ui) throws Exception {

        int full = onFxThread(() -> ui.fullText()).length();

        for (int i = 0; i < 400 && onFxThread(ui::writtenSoFar) < full; i++) {
            Thread.sleep(25);
        }

        Thread.sleep(60);
    }

    @Test
    void fourSwingsNoLongerEndIt() throws Exception {

        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());

        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }

        SaveData saved = new SaveData("Zsombi", "Zsombi", 7, "startBoss3",
                300, 60, 3, List.of(), List.of(new SaveData.Decision("ch1.q1", 1)),
                List.of(), null, null, "2026-10-20T10:00:00Z");

        GameController controller = onFxThread(() -> {
            GameController c = new GameController(new Stage());
            c.resume(saved);
            return c;
        });

        GameUI ui = controller.getUI();

        // four attacks: what used to be a dead boss
        for (int swing = 0; swing < 4; swing++) {
            readIt(ui);
            assertTrue(onFxThread(() -> ui.pressChoice(1)), "a " + (swing + 1) + ". utes nem ment el");
        }

        readIt(ui);

        SaveData.Fight fight = onFxThread(controller::snapshot).fight();
        assertNotNull(fight, "a harcnak mennie kellene meg");

        // The floor and the ceiling of four hits: twenty each if every one
        // caught him open, eight each if he covered every time. Both sides
        // matter - the first says the fight is longer, the second says a
        // guard is worth something, and neither depends on the dice.
        assertTrue(fight.bossHp() >= GameState.BOSS_MAX_HP - 4 * 20,
                "negy utes tobbet vitt a lehetsegesnel: " + fight.bossHp());
        assertTrue(fight.bossHp() <= GameState.BOSS_MAX_HP - 4 * 8,
                "negy utes szinte semmit nem vitt: " + fight.bossHp());
        assertTrue(fight.bossHp() > 0, "negy utesre megint halott a boss: " + fight.bossHp());

        // and the player is not dead either, whatever he rolled
        assertTrue(fight.playerHp() > 0, "a jatekos halt bele negy korbe: " + fight.playerHp());
    }
}
