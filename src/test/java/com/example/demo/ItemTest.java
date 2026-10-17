package com.example.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    @Test
    void everyItemSurvivesTheRoundTrip() {

        GameState before = new GameState();
        before.setFirstEnvelope(true);
        before.setSmallEnvelope(true);
        before.setLakatosFile(true);
        before.setOffshoreCode(true);
        before.setPeteriDossier(true);
        before.setBossTrust(true);
        before.setParliamentKey(true);

        List<String> ids = Item.idsOf(before);
        assertEquals(7, ids.size());

        GameState after = new GameState();
        Item.apply(ids, after);

        assertTrue(after.hasFirstEnvelope());
        assertTrue(after.hasSmallEnvelope());
        assertTrue(after.hasLakatosFile());
        assertTrue(after.hasOffshoreCode());
        assertTrue(after.hasPeteriDossier());
        assertTrue(after.hasBossTrust());
        assertTrue(after.hasParliamentKey());
    }

    @Test
    void theIdsAreTheOnesTheWebsiteWrites() {
        GameState s = new GameState();
        s.setFirstEnvelope(true);
        s.setOffshoreCode(true);
        assertEquals(List.of("envelope1", "offshore"), Item.idsOf(s));
    }

    @Test
    void anEmptyStateCarriesNoItems() {
        assertEquals(List.of(), Item.idsOf(new GameState()));
    }

    @Test
    void anUnknownIdIsIgnoredRatherThanThrowing() {
        GameState s = new GameState();
        Item.apply(List.of("envelope1", "valamiUjAWebrol"), s);
        assertTrue(s.hasFirstEnvelope());
    }
}
