package com.example.demo;

import javafx.application.Platform;
import javafx.event.Event;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two things a window owes the player.
 *
 * A letter typed into a name field is a letter, not a shortcut - L used to open
 * the log out from under the name prompt.
 *
 * And a page may never put a floor under the window. Half the title screen is
 * sized off its own root, and an ImageView is not resizable, so its fitted size
 * came straight back up as the root's minimum; the scene then grew the root to
 * that minimum, which raised it again. The window could only get bigger, so
 * leaving full screen left the page at full-screen size.
 */
class WindowAndKeysTest {

    /**
     * The sound is one object for the whole run, and showing the poster puts the
     * menu cue on it. Handing it back the way it was found keeps the next test
     * from waiting on a cue this one swapped out.
     */
    @org.junit.jupiter.api.AfterAll
    static void handBackTheSound() {
        Sound.get().play(Sound.Track.LOGO);
    }

    private static void toolkit() throws Exception {
        Path home = Files.createTempDirectory("fityesz-home");
        System.setProperty("user.home", home.toString());
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // another test started the toolkit first
        }
    }

    /** Lets the toolkit run a layout pass and settle. */
    private static void pulse() throws Exception {
        for (int i = 0; i < 3; i++) {
            onFxThread(() -> null);
            Thread.sleep(120);
        }
    }

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
    void theLofAnameIsNotTheLogShortcut() throws Exception {

        toolkit();

        AtomicBoolean logAsked = new AtomicBoolean(false);

        TextField field = onFxThread(() -> {

            GameUI ui = new GameUI();
            ui.setOnLog(() -> logAsked.set(true));

            Stage stage = new Stage();
            Scene scene = new Scene(ui.getRoot(), 1200, 800);
            ui.installKeys(scene);
            stage.setScene(scene);
            stage.show();

            ui.addTextChoice(1, "Add meg a neved", 20, name -> { });

            TextField tf = (TextField) scene.lookup("#nameRow").getUserData();
            tf.requestFocus();
            return tf;
        });

        // isFocused() also asks whether the window has the focus, which a test
        // window does not get; the filter reads the scene's focus owner, so does this
        pulse();
        assertEquals(field, onFxThread(() -> field.getScene().getFocusOwner()),
                "a nevmezo nem kapta meg a fokuszt");

        onFxThread(() -> {
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_PRESSED, "l", "l",
                    KeyCode.L, false, false, false, false));
            return null;
        });

        assertFalse(logAsked.get(), "az L a nev gepelese kozben megnyitotta a naplot");
    }

    /**
     * The menu column is rebuilt whenever the saved runs land from the network. If
     * that lands while the poster is still fading in - which it does as soon as the
     * player presses past the splash - the rebuild used to copy the opacity the
     * fade was passing through, and the menu never came up at all.
     */
    @Test
    void aRebuildMidFadeStillLeavesTheMenuOnScreen() throws Exception {

        toolkit();

        StackPane root = onFxThread(() -> {

            GameController controller = new GameController(new Stage());

            OpeningScreen title = new OpeningScreen(
                    slot -> { }, () -> { }, () -> { }, run -> { },
                    controller.account(), controller.saves());

            Stage stage = new Stage();
            stage.setScene(new Scene(title.getRoot(), 1200, 800));
            stage.show();
            title.showPosterNow();
            return title.getRoot();
        });

        pulse();

        // the column, caught halfway through the reveal
        Node column = onFxThread(() -> root.getChildren().stream()
                .filter(n -> n instanceof Group).findFirst().orElseThrow());

        onFxThread(() -> {
            column.setOpacity(0);
            return null;
        });

        // the language toggle is a rebuild with a scene behind it
        onFxThread(() -> {
            clickLanguage(column);
            return null;
        });

        pulse();

        Node rebuilt = onFxThread(() -> root.getChildren().stream()
                .filter(n -> n instanceof Group).findFirst().orElseThrow());

        assertEquals(1.0, onFxThread(rebuilt::getOpacity), 0.001,
                "az ujraepitett menu lathatatlan maradt");
    }

    /** Clicks whichever language label is the inactive one. */
    private static void clickLanguage(Node column) {

        for (Node n : column.lookupAll(".label")) {
            if (n.getOnMouseClicked() != null) {
                n.getOnMouseClicked().handle(new javafx.scene.input.MouseEvent(
                        javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                        0, 0, 0, 0, javafx.scene.input.MouseButton.PRIMARY, 1,
                        false, false, false, false, true, false, false,
                        true, false, false, null));
                return;
            }
        }

        throw new AssertionError("nincs nyelvvalto a hasabban");
    }

    @Test
    void thePageComesBackDownWithTheWindow() throws Exception {

        toolkit();

        StackPane page = onFxThread(() -> {

            GameController controller = new GameController(new Stage());

            OpeningScreen title = new OpeningScreen(
                    slot -> { }, () -> { }, () -> { }, run -> { },
                    controller.account(), controller.saves());

            // the shell the app puts its pages in - it takes the window's size, and
            // what is measured here is whether the page follows it back down
            StackPane box = new StackPane(title.getRoot());
            box.setMinSize(0, 0);

            Stage stage = new Stage();
            stage.setScene(new Scene(box, 1200, 800));
            stage.show();
            return title.getRoot();
        });

        onFxThread(() -> {
            page.getScene().getWindow().setWidth(1710);
            page.getScene().getWindow().setHeight(1046);
            return null;
        });

        pulse();
        double tall = onFxThread(page::getHeight);
        assertTrue(tall > 900, "nem nott meg az ablakkal: " + tall);

        onFxThread(() -> {
            page.getScene().getWindow().setWidth(1200);
            page.getScene().getWindow().setHeight(800);
            return null;
        });

        pulse();
        assertEquals(800, onFxThread(page::getHeight), 40,
                "a lap nem ment vissza a kisebb ablakba");
    }
}
