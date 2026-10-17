package com.example.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatlogTest {

    @Test
    void aLineKeepsItsSpeakerAndChapter() {
        Chatlog log = new Chatlog();
        log.line(2, "LAKATOS", "Üdv a klubban.");

        Chatlog.Entry e = log.entries().get(0);
        assertEquals("line", e.kind());
        assertEquals("LAKATOS", e.speaker());
        assertEquals("Üdv a klubban.", e.text());
        assertEquals(2, e.chapter());
        assertNull(e.xp());
    }

    @Test
    void aChoiceKeepsWhatItCost() {
        Chatlog log = new Chatlog();
        log.choice(1, "Mit válaszolsz?", "Elfogadom", 15, 10);

        Chatlog.Entry e = log.entries().get(0);
        assertEquals("choice", e.kind());
        assertEquals("Mit válaszolsz?", e.speaker());
        assertEquals("Elfogadom", e.text());
        assertEquals(15, e.xp());
        assertEquals(10, e.lebukas());
    }

    @Test
    void theLogStopsAtItsLimitAndKeepsTheLatest() {
        Chatlog log = new Chatlog();

        for (int i = 0; i < Chatlog.MAX + 50; i++) {
            log.line(1, "", "sor " + i);
        }

        List<Chatlog.Entry> entries = log.entries();
        assertEquals(Chatlog.MAX, entries.size());
        assertEquals("sor 50", entries.get(0).text());
        assertEquals("sor " + (Chatlog.MAX + 49), entries.get(entries.size() - 1).text());
    }

    @Test
    void aLoadedLogReplacesWhatWasThere() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");

        log.load(List.of(new Chatlog.Entry("line", "", "mentett", null, null, 3)));

        assertEquals(1, log.entries().size());
        assertEquals("mentett", log.entries().get(0).text());
    }

    @Test
    void loadingNullLeavesAnEmptyLog() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");
        log.load(null);
        assertEquals(List.of(), log.entries());
    }

    @Test
    void aNewGameClearsTheLog() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");
        log.clear();
        assertEquals(List.of(), log.entries());
    }
}
