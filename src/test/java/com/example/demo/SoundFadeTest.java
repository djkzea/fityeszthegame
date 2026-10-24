package com.example.demo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * One cue has to become the other, not replace it.
 *
 * The first attempt had no transition at all, because a JavaFX AudioClip's
 * volume is fixed once it sounds. The JDK's mixer has a gain control that
 * moves while it plays - and reads back, which is what this watches: the cue
 * being left has to come down while the one arriving goes up.
 */
class SoundFadeTest {

    @Test
    void oneCueComesUpAsTheOtherGoesDown() throws Exception {

        Sound sound = Sound.get();
        sound.start();

        for (int i = 0; i < 60 && !sound.ready(); i++) {
            Thread.sleep(100);
        }

        assumeTrue(sound.ready(), "ezen a gepen nincs hangeszkoz");

        // the opening cue is up
        Thread.sleep(1200);
        assertTrue(sound.sounding(Sound.Track.LOGO), "a nyito jelzes nem szol");

        List<float[]> curve = new ArrayList<>();
        sound.play(Sound.Track.MENU);

        for (int i = 0; i < 10; i++) {
            curve.add(new float[]{sound.levelOf(Sound.Track.LOGO), sound.levelOf(Sound.Track.MENU)});
            Thread.sleep(100);
        }

        float[] first = curve.get(0);
        float[] last = curve.get(curve.size() - 1);

        assertTrue(last[0] < first[0] - 10,
                "a tavozo jelzes nem halkult: " + first[0] + " -> " + last[0]);
        assertTrue(last[1] > first[1] + 10,
                "az erkezo jelzes nem erosodott: " + first[1] + " -> " + last[1]);

        // The middle of a crossfade is where both are nearly as loud as they get.
        // A straight line in decibels puts them both near silence there instead,
        // which is the hole the player heard as the music stumbling.
        float[] middle = curve.get(curve.size() / 2);

        assertTrue(middle[1] > -15,
                "az erkezo jelzes a keveres kozepen is alig szolt: " + middle[1] + " dB");
        assertTrue(middle[0] > -15,
                "a tavozo jelzes a keveres kozepere mar eltunt: " + middle[0] + " dB");

        Thread.sleep(600);
        assertTrue(sound.sounding(Sound.Track.MENU), "a menu jelzes nem maradt talpon");
    }
}
