package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Turning the sound off has to still be off tomorrow.
 *
 * The sound itself needs a machine with speakers to judge; this is the part
 * that can break quietly - a player silences the game, quits, and finds it
 * singing again.
 */
class SoundSettingsTest {

    /** A cue whose file is missing is silence, and silence is hard to notice. */
    @Test
    void everyCueHasItsFile() {
        for (Sound.Track track : Sound.Track.values()) {
            assertNotNull(Sound.class.getResource(track.resource),
                    "nincs meg a hangfajl: " + track + " -> " + track.resource);
        }
    }

    @Test
    void bothAreOnBeforeAnybodySaysOtherwise(@TempDir Path dir) {
        Sound.Switches s = Sound.read(dir.resolve("nincs.properties"));
        assertTrue(s.music());
        assertTrue(s.effects());
    }

    @Test
    void whatWasSwitchedOffStaysOff(@TempDir Path dir) {
        Path file = dir.resolve("sound.properties");

        Sound.write(file, new Sound.Switches(false, true));

        Sound.Switches back = Sound.read(file);
        assertFalse(back.music(), "a zene maradjon kikapcsolva");
        assertTrue(back.effects());
    }

    @Test
    void theFileIsMadeEvenIfItsFolderIsNotThere(@TempDir Path dir) {
        Path file = dir.resolve("meg").resolve("nincs").resolve("sound.properties");

        Sound.write(file, new Sound.Switches(true, false));

        assertTrue(Files.exists(file));
        assertFalse(Sound.read(file).effects());
    }

    @Test
    void aDamagedFileLeavesTheGameAudible(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sound.properties");
        Files.writeString(file, "\u0000\u0000 ez nem beallitas");

        Sound.Switches s = Sound.read(file);

        assertEquals(new Sound.Switches(true, true), s);
    }
}
