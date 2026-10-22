package com.example.demo;

import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * What the game sounds like: a bed under the story and the typewriter.
 *
 * Both are off switches as much as they are sounds - a text game is read, and
 * a player who wants silence has to get it in one press and keep it next time,
 * so the two settings live in a file beside the save.
 *
 * Nothing here is allowed to stop the game. A missing codec, a machine with no
 * audio device, a file that will not decode: each is caught and the game plays
 * on in silence.
 */
public final class Sound {

    private static final String MUSIC = "/com/example/demo/audio/music-loop.mp3";
    private static final String TYPING = "/com/example/demo/audio/typing-loop.wav";

    /** Quiet enough to read over: the bed sits under the text, not beside it. */
    private static final double MUSIC_VOLUME = 0.38;
    private static final double TYPING_VOLUME = 0.30;

    private static final Sound INSTANCE = new Sound();

    private final Path settings = AppDirs.data().resolve("sound.properties");

    private MediaPlayer music;
    private AudioClip typing;

    private boolean musicOn = true;
    private boolean effectsOn = true;

    private Sound() {
        read();
    }

    public static Sound get() {
        return INSTANCE;
    }

    public boolean musicOn() {
        return musicOn;
    }

    public boolean effectsOn() {
        return effectsOn;
    }

    /** Starts the bed, if the player has not turned it off. Safe to call twice. */
    public void start() {

        if (music == null) {
            music = player(MUSIC, MUSIC_VOLUME);
        }

        if (music != null && musicOn) {
            music.play();
        }
    }

    public void setMusic(boolean on) {

        musicOn = on;
        write();

        if (music == null) {
            start();
            return;
        }

        if (on) {
            music.play();
        } else {
            music.pause();
        }
    }

    public void setEffects(boolean on) {

        effectsOn = on;
        write();

        if (!on) {
            typingStopped();
        }
    }

    /** The typewriter, for as long as a line is being typed. */
    public void typingStarted() {

        if (!effectsOn) {
            return;
        }

        if (typing == null) {
            typing = clip(TYPING, TYPING_VOLUME);
        }

        if (typing != null && !typing.isPlaying()) {
            typing.play();
        }
    }

    public void typingStopped() {

        if (typing != null && typing.isPlaying()) {
            typing.stop();
        }
    }

    // =========================================================
    // LOADING - every failure here is silence, never a crash
    // =========================================================

    private static MediaPlayer player(String resource, double volume) {

        try {
            URL url = Sound.class.getResource(resource);

            if (url == null) {
                return null;
            }

            MediaPlayer p = new MediaPlayer(new Media(playable(url, resource).toString()));
            p.setCycleCount(MediaPlayer.INDEFINITE);
            p.setVolume(volume);
            p.setOnError(() -> System.err.println("sound: " + p.getError()));

            return p;

        } catch (RuntimeException | IOException e) {
            System.err.println("sound: " + e.getMessage());
            return null;
        }
    }

    private static AudioClip clip(String resource, double volume) {

        try {
            URL url = Sound.class.getResource(resource);

            if (url == null) {
                return null;
            }

            AudioClip c = new AudioClip(url.toExternalForm());
            c.setCycleCount(AudioClip.INDEFINITE);
            c.setVolume(volume);

            return c;

        } catch (RuntimeException e) {
            System.err.println("sound: " + e.getMessage());
            return null;
        }
    }

    /**
     * A URL the media player will accept.
     *
     * Media reads a URI, and inside a packaged app the resource lives in a jar,
     * which it refuses to open - so the file is unpacked once beside the save
     * and played from there.
     */
    private static URL playable(URL resource, String name) throws IOException {

        if (!"jar".equals(resource.getProtocol())) {
            return resource;
        }

        Path out = AppDirs.data().resolve("audio").resolve(name.substring(name.lastIndexOf('/') + 1));

        if (!Files.isRegularFile(out)) {
            Files.createDirectories(out.getParent());

            try (InputStream in = Sound.class.getResourceAsStream(name);
                 OutputStream to = Files.newOutputStream(out)) {
                in.transferTo(to);
            }
        }

        return out.toUri().toURL();
    }

    // =========================================================
    // THE TWO SWITCHES, REMEMBERED
    // =========================================================

    /** What the two switches were set to. Both on until a player says otherwise. */
    record Switches(boolean music, boolean effects) { }

    static Switches read(Path file) {

        Properties p = new Properties();

        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        } catch (IOException noneYet) {
            return new Switches(true, true);
        }

        return new Switches(
                !"false".equals(p.getProperty("music")),
                !"false".equals(p.getProperty("effects")));
    }

    static void write(Path file, Switches switches) {

        Properties p = new Properties();
        p.setProperty("music", String.valueOf(switches.music()));
        p.setProperty("effects", String.valueOf(switches.effects()));

        try {
            Files.createDirectories(file.getParent());

            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "Fityesz Kronika - sound");
            }

        } catch (IOException e) {
            System.err.println("sound: " + e.getMessage());
        }
    }

    private void read() {

        Switches s = read(settings);

        musicOn = s.music();
        effectsOn = s.effects();
    }

    private void write() {
        write(settings, new Switches(musicOn, effectsOn));
    }
}
