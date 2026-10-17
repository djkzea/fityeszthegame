package com.example.demo;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * resume() calls a scene by name, through reflection. A renamed scene method
 * would turn every save pointing at it into a silent fall back to the start of
 * the chapter - so the names the game relies on are checked here.
 */
class SceneNamesTest {

    @Test
    void everyChapterHasAnEntryScene() {
        assertEquals(8, GameController.CHAPTER_ENTRY.size());

        for (Map.Entry<Integer, String> e : GameController.CHAPTER_ENTRY.entrySet()) {
            Method m = assertDoesNotThrow(
                    () -> GameController.class.getDeclaredMethod(e.getValue()),
                    "nincs ilyen jelenetmetódus: " + e.getValue() + " (" + e.getKey() + ". fejezet)");
            assertEquals(0, m.getParameterCount(), e.getValue() + " nem lehet paraméteres");
        }
    }

    @Test
    void theBossFightsCanBeReturnedTo() {
        for (String name : new String[] {"startLakatosBoss", "startPeteriBoss", "startBoss3"}) {
            Method m = assertDoesNotThrow(() -> GameController.class.getDeclaredMethod(name), name);
            assertEquals(0, m.getParameterCount(), name);
        }
    }

    @Test
    void theChoiceScenesAreTheOnesLeftOutOfTheSave() {
        for (String name : new String[] {"chapter1Choice", "bossMove", "peteriBossMove", "boss3Move"}) {
            boolean found = false;
            for (Method m : GameController.class.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    found = true;
                    assertTrue(m.getParameterCount() > 0,
                            name + " paraméter nélkülivé vált - így bekerülne a mentésbe, "
                                    + "pedig egy választás közepe nem visszatérési pont");
                }
            }
            assertTrue(found, "nincs ilyen metódus: " + name);
        }
    }
}
