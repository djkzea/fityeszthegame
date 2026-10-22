package com.example.demo;

import javafx.scene.media.AudioClip;

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

    private static final String MUSIC = "/com/example/demo/audio/music-loop.wav";
    private static final String TYPING = "/com/example/demo/audio/typing-loop.wav";

    /** Quiet enough to read over: the bed sits under the text, not beside it. */
    private static final double MUSIC_VOLUME = 0.38;
    private static final double TYPING_VOLUME = 0.30;

    private static final Sound INSTANCE = new Sound();

    private final Path settings = AppDirs.data().resolve("sound.properties");

    private volatile AudioClip music;
    private volatile AudioClip typing;

    private volatile boolean musicOn = true;
    private volatile boolean effectsOn = true;

    private boolean started = false;

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

    /**
     * Starts the bed, if the player has not turned it off. Safe to call twice.
     *
     * Both sounds are read in on a thread of their own: decoding a track takes
     * long enough that doing it on the UI thread stutters whatever is on screen,
     * and the typewriter was skipping lines because of it.
     */
    public void start() {

        if (started) {
            return;
        }

        started = true;

        Thread loader = new Thread(this::load, "sound-load");
        loader.setDaemon(true);
        loader.start();
    }

    private void load() {

        AudioClip bed = clip(MUSIC, MUSIC_VOLUME);
        AudioClip keys = clip(TYPING, TYPING_VOLUME);

        synchronized (this) {
            music = bed;
            typing = keys;

            if (bed != null && musicOn) {
                bed.play();
            }
        }
    }

    public synchronized void setMusic(boolean on) {

        musicOn = on;
        write();

        if (music == null) {
            return;         // still loading; load() will see the new setting
        }

        if (on) {
            music.play();
        } else {
            music.stop();
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

        AudioClip keys = typing;

        if (keys == null || !effectsOn) {
            return;
        }

        if (!keys.isPlaying()) {
            keys.play();
        }
    }

    public void typingStopped() {

        AudioClip keys = typing;

        if (keys != null && keys.isPlaying()) {
            keys.stop();
        }
    }

    // =========================================================
    // LOADING - every failure here is silence, never a crash
    // =========================================================

    /**
     * One decoded buffer, looped by the mixer.
     *
     * The bed is a plain WAV played by AudioClip rather than an mp3 through
     * MediaPlayer on purpose: an mp3 carries encoder padding at both ends and
     * the player seeks back to restart, which put an audible hitch in the loop
     * every time round. AudioClip reads straight out of the jar, so nothing has
     * to be unpacked.
     */
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
