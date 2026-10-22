package com.example.demo;

import javafx.scene.media.AudioClip;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/**
 * What the game sounds like: a cue under each screen, and the typewriter.
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

    /**
     * What is playing, and where it belongs.
     *
     * One bed used to run under everything. A wordmark, a menu and a page of
     * legal text are not the same room, so each has its own cue now: the ones
     * under a screen the player sits on loop, the ones that play over a moment
     * do not.
     */
    public enum Track {

        LOGO("/com/example/demo/audio/logo-sting.mp3", 0.55, false),
        MENU("/com/example/demo/audio/menu-loop.wav", 0.38, true),
        DISCLAIMER("/com/example/demo/audio/disclaimer.mp3", 0.42, false),
        GAME("/com/example/demo/audio/music-loop.wav", 0.38, true);

        final String resource;
        private final double volume;
        private final boolean loops;

        Track(String resource, double volume, boolean loops) {
            this.resource = resource;
            this.volume = volume;
            this.loops = loops;
        }
    }

    private static final String TYPING = "/com/example/demo/audio/typing-loop.wav";
    private static final double TYPING_VOLUME = 0.30;

    /**
     * How long the cue being left runs under the one arriving.
     *
     * An AudioClip's volume cannot be moved while it sounds, so one cannot be
     * faded into the other. They overlap for a moment instead, which reads as a
     * dissolve - and every switch happens under a curtain or a card fading
     * anyway.
     */
    private static final long HANDOVER_MS = 350;

    private static final Sound INSTANCE = new Sound();

    private final Path settings = AppDirs.data().resolve("sound.properties");

    private volatile Map<Track, AudioClip> music = Map.of();
    private volatile AudioClip typing;

    private volatile Track current = Track.LOGO;

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
     * Reads the sounds in and starts the opening cue. Safe to call twice.
     *
     * It all happens on a thread of its own: decoding a track takes long enough
     * that doing it on the UI thread stutters whatever is on screen, and the
     * typewriter was skipping lines because of it.
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

        EnumMap<Track, AudioClip> loaded = new EnumMap<>(Track.class);

        for (Track track : Track.values()) {
            AudioClip c = clip(track.resource, track.volume, track.loops);

            if (c != null) {
                loaded.put(track, c);
            }
        }

        AudioClip keys = clip(TYPING, TYPING_VOLUME, true);

        synchronized (this) {
            music = loaded;
            typing = keys;

            AudioClip now = loaded.get(current);

            if (musicOn && now != null) {
                now.play();
            }
        }
    }

    /** Puts on the cue that belongs to the screen the player is looking at. */
    public synchronized void play(Track track) {

        if (track == current) {
            return;
        }

        Track leaving = current;
        current = track;

        if (musicOn) {
            handover(leaving, track);
        }
    }

    private void handover(Track leaving, Track arriving) {

        AudioClip out = music.get(leaving);
        AudioClip in = music.get(arriving);

        if (in != null) {
            in.play();
        }

        if (out == null) {
            return;
        }

        Thread tail = new Thread(() -> {

            try {
                Thread.sleep(HANDOVER_MS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }

            // unless the player has gone back to it in the meantime
            if (current != leaving) {
                out.stop();
            }

        }, "sound-handover");

        tail.setDaemon(true);
        tail.start();
    }

    public synchronized void setMusic(boolean on) {

        musicOn = on;
        write();

        AudioClip now = music.get(current);

        if (now == null) {
            return;         // still loading; load() will see the new setting
        }

        if (on) {
            now.play();
        } else {
            music.values().forEach(AudioClip::stop);
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
     * A cue that loops is a plain WAV played by AudioClip rather than an mp3
     * through MediaPlayer on purpose: an mp3 carries encoder padding at both
     * ends and the player seeks back to restart, which put an audible hitch in
     * the loop every time round. The ones that play once are mp3s, since there
     * is no seam to protect and they are a tenth of the size. AudioClip reads
     * either straight out of the jar, so nothing has to be unpacked.
     */
    private static AudioClip clip(String resource, double volume, boolean loops) {

        try {
            URL url = Sound.class.getResource(resource);

            if (url == null) {
                return null;
            }

            AudioClip c = new AudioClip(url.toExternalForm());

            if (loops) {
                c.setCycleCount(AudioClip.INDEFINITE);
            }

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
