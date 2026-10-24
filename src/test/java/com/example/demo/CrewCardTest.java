package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The card of names after the wordmark.
 *
 * A portrait that did not make it into the build leaves the card standing
 * there with two names and a gap, which nobody would notice until a player
 * did - so the files are checked here, where the build will say so.
 */
class CrewCardTest {

    @Test
    void bothPortraitsAreInTheBuild() {
        for (String file : new String[] {"Frezzard2.png", "djkzea.png"}) {
            assertNotNull(
                    GameUI.class.getResource("/com/example/demo/fityesz_art/crew/" + file),
                    "nincs meg az alkoto kepe: " + file);
        }
    }

    /**
     * The card is mostly the game's own art. A renamed file does not break the
     * build or throw - it just leaves a hole in the strip nobody put there.
     */
    @Test
    void theArtTheCardBorrowsIsStillWhereItLooks() {

        String[] files = {
                "bg/ch1_cafe.png", "bg/ch2_hall.png", "bg/ch4_office.png",
                "bg/ch5_corridor.png", "bg/ch6_lodge.png", "bg/ch7-2_chamber.png",
                "characters/lipoti.png", "characters/lakatoservin.png",
                "characters/drpeterikatalin.png", "characters/kapzsimre.png",
                "characters/molnar.png",
        };

        for (String file : files) {
            assertNotNull(GameUI.class.getResource("/com/example/demo/fityesz_art/" + file),
                    "az alkotok kartyaja erre a kepre hivatkozik: " + file);
        }
    }

    @Test
    void theCardSpeaksBothLanguages() {

        for (String key : new String[] {"crew.made", "crew.name1", "crew.name2",
                "crew.role1", "crew.role2", "crew.support", "ui.crew"}) {

            Lang.set("hu");
            String hu = Lang.t(key);

            Lang.set("en");
            String en = Lang.t(key);

            assertFalse(hu.isBlank(), key + " magyarul ures");
            assertFalse(en.isBlank(), key + " angolul ures");
            assertNotEquals(key, hu, "nincs ilyen kulcs: " + key);
        }

        // the name order is the one thing that must differ between the two
        Lang.set("en");
        String en = Lang.t("crew.name1");

        Lang.set("hu");
        assertNotEquals(Lang.t("crew.name1"), en, "az angol nevsorrend ugyanaz maradt");
    }
}
