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

        // and they were both audible in the middle of it, which is the overlap
        float[] middle = curve.get(curve.size() / 2);
        assertTrue(middle[0] > -40 && middle[1] > -40,
                "nem szoltak egyszerre: " + middle[0] + " / " + middle[1]);

        Thread.sleep(600);
        assertTrue(sound.sounding(Sound.Track.MENU), "a menu jelzes nem maradt talpon");
    }
}
