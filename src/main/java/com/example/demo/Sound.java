package com.example.demo;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * What the game sounds like: a cue under each screen, and the typewriter.
 *
 * Both are off switches as much as they are sounds - a text game is read, and
 * a player who wants silence has to get it in one press and keep it next time,
 * so the two settings live in a file beside the save.
 *
 * The sound comes from the JDK's own mixer rather than JavaFX's. JavaFX can
 * either loop a clip without a seam or change its volume, never both: an
 * AudioClip's volume is fixed once it sounds, and a MediaPlayer leaves a hole
 * at the loop. A javax.sound Clip loops a buffer gaplessly and has a gain
 * control that moves while it plays, which is what one cue turning into
 * another is made of.
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
     * legal text are not the same room, so each has its own cue: the ones under
     * a screen the player sits on loop, the ones that play over a moment do not.
     */
    public enum Track {

        LOGO("/com/example/demo/audio/logo-sting.wav", 0.55, false),
        MENU("/com/example/demo/audio/menu-loop.wav", 0.38, true),
        DISCLAIMER("/com/example/demo/audio/disclaimer.wav", 0.42, false),
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

    /** How long one cue takes to become another. */
    private static final int FADE_MS = 900;
    private static final int STEP_MS = 20;

    /** Quiet enough to be gone, loud enough that the mixer does not round it away. */
    private static final float SILENT_DB = -60f;

    private static final AtomicLong TOKENS = new AtomicLong();

    private static final Sound INSTANCE = new Sound();

    /** One loaded sound: the buffer, the knob that moves its level, and where it sits. */
    private static final class Cue {

        private final Clip clip;
        private final FloatControl gain;
        private final float levelDb;

        /** The only fade allowed to touch this cue; a newer one takes over. */
        private volatile long fade;

        Cue(Clip clip, FloatControl gain, float levelDb) {
            this.clip = clip;
            this.gain = gain;
            this.levelDb = levelDb;
        }

        void setDb(float db) {
            if (gain != null) {
                gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
            }
        }

        float db() {
            return gain == null ? levelDb : gain.getValue();
        }

        void from(boolean loops) {
            clip.setFramePosition(0);

            if (loops) {
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                clip.start();
            }
        }
    }

    private final Path settings = AppDirs.data().resolve("sound.properties");

    private volatile Map<Track, Cue> cues = Map.of();
    private volatile Cue typing;

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

        EnumMap<Track, Cue> loaded = new EnumMap<>(Track.class);

        for (Track track : Track.values()) {
            Cue cue = open(track.resource, track.volume);

            if (cue != null) {
                loaded.put(track, cue);
            }
        }

        Cue keys = open(TYPING, TYPING_VOLUME);

        synchronized (this) {
            cues = loaded;
            typing = keys;

            Cue now = loaded.get(current);

            if (musicOn && now != null) {
                rise(now, current.loops);
            }
        }
    }

    /**
     * Puts on the cue that belongs to the screen the player is looking at.
     *
     * The two overlap while one gives way to the other, so a switch is a change
     * of room rather than a cut.
     */
    public synchronized void play(Track track) {

        if (track == current) {
            return;
        }

        Track leaving = current;
        current = track;

        if (!musicOn) {
            return;         // remembered; it starts if the music is switched on
        }

        Cue arriving = cues.get(track);
        Cue going = cues.get(leaving);

        if (arriving != null) {
            rise(arriving, track.loops);
        }

        if (going != null) {
            fade(going, going.db(), SILENT_DB, true);
        }
    }

    public synchronized void setMusic(boolean on) {

        musicOn = on;
        write();

        Cue now = cues.get(current);

        if (now == null) {
            return;         // still loading; load() will see the new setting
        }

        if (on) {
            rise(now, current.loops);
        } else {
            cues.values().forEach(cue -> fade(cue, cue.db(), SILENT_DB, true));
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

        Cue keys = typing;

        if (keys == null || !effectsOn) {
            return;
        }

        if (!keys.clip.isRunning()) {
            keys.from(true);
        }
    }

    public void typingStopped() {

        Cue keys = typing;

        if (keys != null && keys.clip.isRunning()) {
            keys.clip.stop();
        }
    }

    // =========================================================
    // WHAT THE MIXER IS DOING - read back by the fade's test
    // =========================================================

    /** True once the files are in the mixer; false on a machine with no audio. */
    boolean ready() {
        return !cues.isEmpty();
    }

    /** The level this cue is at right now, in dB. */
    float levelOf(Track track) {
        Cue cue = cues.get(track);
        return cue == null ? SILENT_DB : cue.db();
    }

    boolean sounding(Track track) {
        Cue cue = cues.get(track);
        return cue != null && cue.clip.isRunning();
    }

    // =========================================================
    // ONE CUE BECOMING ANOTHER
    // =========================================================

    /** Starts a cue from silence and brings it up to its own level. */
    private void rise(Cue cue, boolean loops) {

        float from = cue.clip.isRunning() ? cue.db() : SILENT_DB;

        if (!cue.clip.isRunning()) {
            cue.setDb(SILENT_DB);
            cue.from(loops);
        }

        fade(cue, from, cue.levelDb, false);
    }

    private void fade(Cue cue, float fromDb, float toDb, boolean stopAtEnd) {

        long token = TOKENS.incrementAndGet();
        cue.fade = token;

        Thread fader = new Thread(() -> {

            int steps = FADE_MS / STEP_MS;

            for (int i = 0; i <= steps; i++) {

                if (cue.fade != token) {
                    return;         // a newer fade owns this cue now
                }

                cue.setDb(fromDb + (toDb - fromDb) * i / steps);

                try {
                    Thread.sleep(STEP_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            if (stopAtEnd && cue.fade == token) {
                cue.clip.stop();
            }

        }, "sound-fade");

        fader.setDaemon(true);
        fader.start();
    }

    // =========================================================
    // LOADING - every failure here is silence, never a crash
    // =========================================================

    /**
     * One WAV read into the mixer.
     *
     * The whole file is held as sound rather than streamed: a Clip loops its
     * buffer without a seam, which is the point of it, and the largest of these
     * is a few megabytes. Compressed formats are not read here - the JDK mixer
     * takes PCM - so every cue ships as a WAV.
     */
    private static Cue open(String resource, double volume) {

        try (InputStream raw = Sound.class.getResourceAsStream(resource)) {

            if (raw == null) {
                return null;
            }

            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {

                Clip clip = AudioSystem.getClip();
                clip.open(in);

                FloatControl gain = clip.isControlSupported(FloatControl.Type.MASTER_GAIN)
                        ? (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN)
                        : null;

                Cue cue = new Cue(clip, gain, (float) (20 * Math.log10(volume)));
                cue.setDb(cue.levelDb);

                return cue;
            }

        } catch (Exception noSound) {
            System.err.println("sound: " + noSound.getMessage());
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
