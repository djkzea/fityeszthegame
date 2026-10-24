package com.example.demo;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * What the game sounds like: a cue under each screen, and the typewriter.
 *
 * Both are off switches as much as they are sounds - a text game is read, and
 * a player who wants silence has to get it in one press and keep it next time,
 * so the two settings live in a file beside the save.
 *
 * The sound is mixed here rather than handed to a player. Nothing on the shelf
 * does the two things this needs at once. JavaFX will loop a clip without a
 * seam but freezes its volume the moment it sounds; its MediaPlayer leaves a
 * hole at the loop. The JDK's own Clip loops properly and has a volume control,
 * but that control moves in steps of five eighths of a decibel and sits behind
 * a one second buffer, so a fade through it arrives as three or four jumps -
 * which is what a player hears as the music catching.
 *
 * So the cues are read into memory as plain numbers and written to one output
 * line a few hundred samples at a time, every sample multiplied by a level of
 * its own. A fade is then exactly a hand on a fader: no steps, no lag, and a
 * loop that comes round without a gap because the reading simply starts over.
 *
 * Nothing here is allowed to stop the game. A missing file, a machine with no
 * audio device, a format the mixer will not take: each is caught and the game
 * plays on in silence.
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
        private final double level;
        private final boolean loops;

        Track(String resource, double level, boolean loops) {
            this.resource = resource;
            this.level = level;
            this.loops = loops;
        }
    }

    private static final String TYPING = "/com/example/demo/audio/typing-loop.wav";
    private static final double TYPING_LEVEL = 0.30;

    private static final int RATE = 44100;

    /** Frames written at a time: short enough to answer quickly, long enough to be safe. */
    private static final int BLOCK = 512;

    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 2, true, false);

    /** How long one cue takes to become another, and how fast the typewriter arrives. */
    private static final double FADE_SECONDS = 0.9;
    private static final double TYPING_FADE_SECONDS = 0.05;

    private static final Sound INSTANCE = new Sound();

    /**
     * One cue in the mixer: the sound itself, where the reading is, and how far
     * up its fader stands.
     *
     * The fader is held as a quarter turn rather than as a level, so a pair of
     * cues handing over - one on the way up, one on the way down - keeps a
     * steady power between them instead of dipping in the middle.
     */
    private static final class Voice {

        private final short[] pcm;
        private final int channels;
        private final int frames;
        private final boolean loops;
        private final double level;

        private int at;
        private double turn;            // 0 silent .. 1 full
        private double per;             // how much of the turn one sample moves
        private int going;              // +1 coming up, -1 going down, 0 holding
        private boolean sounding;

        Voice(short[] pcm, int channels, boolean loops, double level) {
            this.pcm = pcm;
            this.channels = channels;
            this.frames = pcm.length / channels;
            this.loops = loops;
            this.level = level;
        }

        double gain() {
            return level * Math.sin(turn * Math.PI / 2);
        }

        void rise(double seconds) {

            per = 1.0 / (seconds * RATE);
            going = 1;

            if (!sounding) {
                at = 0;
                turn = 0;
                sounding = true;
            }
        }

        void fall(double seconds) {
            per = 1.0 / (seconds * RATE);
            going = -1;
        }
    }

    private final Path settings = AppDirs.data().resolve("sound.properties");

    private final Map<Track, Voice> cues = new EnumMap<>(Track.class);
    private final List<Voice> voices = new ArrayList<>();

    private Voice typing;

    private volatile Track current = Track.LOGO;

    private volatile boolean musicOn = true;
    private volatile boolean effectsOn = true;

    private volatile boolean ready = false;
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
     * Reads the cues in and opens the output. Safe to call twice.
     *
     * All of it happens on a thread of its own: reading a few megabytes of
     * sound on the UI thread stutters whatever is on screen, and the typewriter
     * was skipping lines because of it.
     */
    public void start() {

        if (started) {
            return;
        }

        started = true;

        Thread mixer = new Thread(this::run, "sound-mixer");
        mixer.setDaemon(true);
        mixer.start();
    }

    private void run() {

        synchronized (this) {

            for (Track track : Track.values()) {
                Voice voice = open(track.resource, track.loops, track.level);

                if (voice != null) {
                    cues.put(track, voice);
                    voices.add(voice);
                }
            }

            typing = open(TYPING, true, TYPING_LEVEL);

            if (typing != null) {
                voices.add(typing);
            }

            ready = !cues.isEmpty();

            Voice first = cues.get(current);

            if (musicOn && first != null) {
                first.rise(FADE_SECONDS);
            }
        }

        // The output is opened again if it is lost. A machine's sound device comes
        // and goes - headphones are plugged in, the system changes its mind about
        // which one is in use - and when it does, the line under us dies. Without
        // this the game is silent from then on, which is what a player met after
        // a while in the menu.
        while (true) {

            try (SourceDataLine line = AudioSystem.getSourceDataLine(FORMAT)) {

                line.open(FORMAT, BLOCK * 4 * 4);
                line.start();

                pump(line);

            } catch (Exception lost) {
                System.err.println("sound: " + lost.getMessage());
            }

            try {
                Thread.sleep(1200);     // give the device a moment to come back
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /** Writes sound for as long as the game runs - silence when nothing is up. */
    private void pump(SourceDataLine line) {

        float[] mix = new float[BLOCK * 2];
        byte[] out = new byte[BLOCK * 4];
        int refused = 0;

        while (true) {

            Arrays.fill(mix, 0f);

            synchronized (this) {
                for (Voice voice : voices) {
                    if (voice.sounding) {
                        render(voice, mix);
                    }
                }
            }

            for (int i = 0; i < mix.length; i++) {
                int s = Math.round(Math.max(-1f, Math.min(1f, mix[i])) * 32767);
                out[i * 2] = (byte) (s & 0xFF);
                out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }

            int written = line.write(out, 0, out.length);

            // a line that stops taking what it is given has gone; let it be reopened
            refused = written < out.length ? refused + 1 : 0;

            if (refused > 20) {
                System.err.println("sound: a kimenet nem fogadja a hangot, ujranyitas");
                return;
            }
        }
    }

    /**
     * One cue's share of the next block.
     *
     * The fader moves across the block rather than at its edge: the level is
     * worked out at both ends and every sample in between gets its own, which
     * is what makes a fade a slope instead of a staircase.
     */
    private void render(Voice voice, float[] mix) {

        double from = voice.gain();

        if (voice.going != 0) {
            voice.turn = Math.max(0, Math.min(1, voice.turn + voice.going * voice.per * BLOCK));

            if (voice.turn <= 0 || voice.turn >= 1) {
                voice.going = 0;
            }
        }

        double to = voice.gain();

        for (int i = 0; i < BLOCK; i++) {

            double gain = from + (to - from) * i / BLOCK;
            int frame = voice.at * voice.channels;

            float left = voice.pcm[frame] / 32768f;
            float right = voice.channels > 1 ? voice.pcm[frame + 1] / 32768f : left;

            mix[i * 2] += (float) (left * gain);
            mix[i * 2 + 1] += (float) (right * gain);

            voice.at++;

            if (voice.at >= voice.frames) {

                if (!voice.loops) {
                    voice.sounding = false;
                    return;
                }

                voice.at = 0;       // round again, with nothing in between
            }
        }

        if (voice.turn <= 0 && voice.going == 0) {
            voice.sounding = false;
        }
    }

    /**
     * Puts on the cue that belongs to the screen the player is looking at.
     *
     * The two cross over: one comes up as the other goes down, so a switch is a
     * change of room rather than a cut.
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

        Voice arriving = cues.get(track);
        Voice going = cues.get(leaving);

        if (arriving != null) {
            arriving.rise(FADE_SECONDS);
        }

        if (going != null) {
            going.fall(FADE_SECONDS);
        }
    }

    public synchronized void setMusic(boolean on) {

        musicOn = on;
        write();

        if (on) {
            Voice now = cues.get(current);

            if (now != null) {
                now.rise(FADE_SECONDS);
            }

        } else {
            cues.values().forEach(voice -> voice.fall(0.35));
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
    public synchronized void typingStarted() {

        if (typing != null && effectsOn) {
            typing.rise(TYPING_FADE_SECONDS);
        }
    }

    public synchronized void typingStopped() {

        if (typing != null) {
            typing.fall(TYPING_FADE_SECONDS);
        }
    }

    // =========================================================
    // WHAT THE MIXER IS DOING - read back by the fade's test
    // =========================================================

    /** True once the cues are in memory; false on a machine with no audio. */
    boolean ready() {
        return ready;
    }

    /** The level this cue stands at, in dB, as the mixer has it this instant. */
    synchronized float levelOf(Track track) {

        Voice voice = cues.get(track);

        if (voice == null || !voice.sounding || voice.gain() <= 0.0001) {
            return -60f;
        }

        return (float) (20 * Math.log10(voice.gain()));
    }

    synchronized boolean sounding(Track track) {
        Voice voice = cues.get(track);
        return voice != null && voice.sounding;
    }

    // =========================================================
    // LOADING - every failure here is silence, never a crash
    // =========================================================

    /**
     * One WAV read into memory as numbers.
     *
     * The whole file is held rather than streamed: the largest is a few
     * megabytes, and a loop that has to come round without a gap cannot wait on
     * a disk. Only PCM is read - every cue ships as a WAV for that reason.
     */
    private static Voice open(String resource, boolean loops, double level) {

        try (InputStream raw = Sound.class.getResourceAsStream(resource)) {

            if (raw == null) {
                return null;
            }

            try (AudioInputStream in = AudioSystem.getAudioInputStream(new BufferedInputStream(raw))) {

                AudioFormat format = in.getFormat();

                if (format.getSampleSizeInBits() != 16 || (int) format.getSampleRate() != RATE) {
                    System.err.println("sound: " + resource + " nem 16 bites 44.1 kHz");
                    return null;
                }

                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] chunk = new byte[16384];

                for (int n = in.read(chunk); n > 0; n = in.read(chunk)) {
                    bytes.write(chunk, 0, n);
                }

                byte[] data = bytes.toByteArray();
                short[] pcm = new short[data.length / 2];

                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = format.isBigEndian()
                            ? (short) ((data[i * 2] << 8) | (data[i * 2 + 1] & 0xFF))
                            : (short) ((data[i * 2 + 1] << 8) | (data[i * 2] & 0xFF));
                }

                return new Voice(pcm, format.getChannels(), loops, level);
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
