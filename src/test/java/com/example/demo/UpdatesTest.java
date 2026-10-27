package com.example.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Telling a player a newer game exists - and, more often, saying nothing.
 *
 * The quiet half is the half that matters: a draft release, a rate limit, an
 * answer in a shape nobody expected. None of them is worth a word on screen,
 * so each has to end as "nothing to say" rather than as an error.
 */
class UpdatesTest {

    private static String release(String tag) {
        return "{\"url\":\"https://api.github.com/x\",\"tag_name\":\"" + tag
                + "\",\"name\":\"Fityesz Kronika " + tag + "\",\"draft\":false}";
    }

    @Test
    void aHigherNumberIsNewer() {
        assertTrue(Updates.newer("1.1.0", "1.0.0"));
        assertTrue(Updates.newer("1.0.1", "1.0.0"));
        assertTrue(Updates.newer("2.0.0", "1.9.9"));
    }

    /** Ten is after nine, which text comparison gets wrong. */
    @Test
    void theNumbersAreNumbersNotText() {
        assertTrue(Updates.newer("1.0.10", "1.0.9"));
        assertFalse(Updates.newer("1.0.9", "1.0.10"));
    }

    @Test
    void theSameVersionIsNotNewer() {
        assertFalse(Updates.newer("1.0.0", "1.0.0"));
        assertFalse(Updates.newer("1.0", "1.0.0"));
        assertFalse(Updates.newer("0.9.9", "1.0.0"));
    }

    @Test
    void theTagIsReadOutOfTheAnswer() {
        assertEquals("1.1.0", Updates.newerIn(release("v1.1.0"), "1.0.0").orElseThrow());
        assertEquals("1.1.0", Updates.newerIn(release("1.1.0"), "1.0.0").orElseThrow());
    }

    @Test
    void anAnswerAboutTheVersionAlreadyRunningSaysNothing() {
        assertTrue(Updates.newerIn(release("v1.0.0"), "1.0.0").isEmpty());
    }

    @Test
    void nothingToSayIsNotAnError() {
        assertTrue(Updates.newerIn(null, "1.0.0").isEmpty());
        assertTrue(Updates.newerIn("", "1.0.0").isEmpty());
        assertTrue(Updates.newerIn("{\"message\":\"Not Found\"}", "1.0.0").isEmpty());
        assertTrue(Updates.newerIn(release("kiscica"), "1.0.0").isEmpty());
    }

    /** A copy built from the source tree has no number, so it offers nothing. */
    @Test
    void aBuildThatCannotNameItselfStaysQuiet() {
        assertTrue(Updates.newerIn(release("v9.9.9"), "").isEmpty());
    }
}
