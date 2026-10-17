package com.example.demo;

import java.io.File;

import javafx.animation.AnimationTimer;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

/** Dev-only: stages the boss fight and the exposed ending for the cut. */
public class Shot extends Application {

    private static final String OUT = System.getProperty("shot.dir", "/tmp");
    private GameUI ui;
    private Scene scene;
    private double t = 0.8;
    private String clip = null;
    private int frame = 0;
    private long lastNs = 0;

    @Override
    public void start(Stage stage) {

        ui = new GameUI();
        // the game root sits in a Group so the scene does not resize it: that lets
        // the snapshot be any size, including taller than the display
        ui.getRoot().setManaged(false);   // or the Group resizes it back to its pref size
        scene = new Scene(new javafx.scene.Group(ui.getRoot()), 600, 400);
        stage.setScene(scene);
        stage.setX(-4000);
        stage.setY(-4000);
        stage.show();

        new AnimationTimer() {
            @Override public void handle(long now) {
                if (clip == null || now - lastNs < 41_000_000L) return;
                lastNs = now;
                try {
                    double w = Double.parseDouble(System.getProperty("shot.w", "1200"));
                    double h = Double.parseDouble(System.getProperty("shot.h", "800"));
                    javafx.scene.Parent root = ui.getRoot();
                    root.resize(w, h);
                    root.applyCss();
                    root.layout();
                    WritableImage img = root.snapshot(null, null);
                    File dir = new File(OUT, clip); dir.mkdirs();
                    javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(img, null), "png",
                            new File(dir, String.format("%04d.png", frame++)));
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }.start();

        // --- the boss fight, with the real strings
        at(t += 0.2, () -> {
            ui.setChapter("3. FEJEZET · " + Lang.t("ch3.title"));
            ui.showImage("/com/example/demo/fityesz_art/bg/ch3_hall.png");
            ui.showCharacter("/com/example/demo/fityesz_art/characters/lakatoservin.png",
                    Lang.t("npc.lakatos"), Lang.t("npc.lakatos.role"));
            ui.showBossFight(Lang.t("npc.lakatos"), 80, 65);
            ui.addBossAbility(Lang.t("boss1.skill"), Lang.t("boss1.ab.desc"));
            ui.showDialogue(Lang.t("npc.lakatos"), Lang.t("boss1.hit"));
        });
        at(t += 1.0, () -> {
            ui.addChoice(1, "TÁMADÁS", () -> { });
            ui.addChoice(2, "VÉDEKEZÉS", () -> { });
            ui.addChoice(3, Lang.t("boss1.skill"), () -> { });
        });
        rec("boss", 3.0);

        // --- the exposed ending: push the meter to 100
        at(t += 0.3, () -> {
            ui.hideBossFight();
            ui.clearChoices();
            GameState st = new GameState();
            st.setName("Zsombor");
            st.addXp(240);
            st.addExposure(100);
            ui.updateStats(st);
        });
        rec("lebuktal", 3.2);

        at(t += 1.0, Platform::exit);
    }

    private void rec(String name, double seconds) {
        at(t += 0.1, () -> { clip = name; frame = 0; });
        at(t += seconds, () -> clip = null);
    }

    private void at(double s, Runnable r) {
        PauseTransition p = new PauseTransition(Duration.seconds(s));
        p.setOnFinished(e -> r.run());
        p.play();
    }

    public static void main(String[] args) { launch(); }
}
