package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SaveDataTest {

    private static SaveData sample() {
        return new SaveData(
                "Zsombi", "Zsombi", 3, "lipotiOffer",
                120, 40, 2,
                List.of("envelope1", "lakatosFile"),
                List.of(new SaveData.Decision("ch1.q1", 0), new SaveData.Decision("ch2.q", 2)),
                List.of(new Chatlog.Entry("line", "LIPÓTI", "Jó napot.", null, null, 1)),
                new SaveData.Fight(90, 55, 2, 1, 0),
                new SaveData.Scenery("/bg/ch1_cafe.png", "/ch/lipoti.png", "LIPÓTI", "A KÖZVETÍTŐ",
                        "EGY ELDUGOTT KÁVÉZÓ", "1. FEJEZET · A TOBORZÁS"),
                "2026-10-17T18:20:00Z");
    }

    @Test
    void theJsonKeysAreTheTableColumns() {
        JsonNode n = Json.mapper().valueToTree(sample());
        assertEquals("Zsombi", n.get("player_name").asText());
        assertEquals("Zsombi", n.get("display_name").asText());
        assertEquals(3, n.get("chapter").asInt());
        assertEquals("lipotiOffer", n.get("scene").asText());
        assertEquals(40, n.get("lebukas").asInt());
        assertEquals(2, n.get("szint").asInt());
        assertEquals("2026-10-17T18:20:00Z", n.get("updated_at").asText());
        assertEquals("ch1.q1", n.get("history").get(0).get("choicePointId").asText());
        assertEquals(90, n.get("fight").get("playerHp").asInt());
        assertEquals("/bg/ch1_cafe.png", n.get("scenery").get("background").asText());
    }

    @Test
    void everyFieldComesBack() throws Exception {
        String json = Json.mapper().writeValueAsString(sample());
        SaveData back = Json.mapper().readValue(json, SaveData.class);
        assertEquals(sample(), back);
    }

    @Test
    void aMissingFightIsNullRatherThanAnError() throws Exception {
        SaveData d = Json.mapper().readValue(
                "{\"player_name\":\"A\",\"chapter\":1,\"xp\":0,\"lebukas\":0,\"szint\":1}",
                SaveData.class);
        assertNull(d.fight());
        assertEquals("A", d.playerName());
    }

    @Test
    void columnsTheWebsiteAddsLaterDoNotBreakTheRead() throws Exception {
        SaveData d = Json.mapper().readValue(
                "{\"id\":\"abc\",\"user_id\":\"u1\",\"slot\":1,\"player_name\":\"A\"}",
                SaveData.class);
        assertEquals("A", d.playerName());
    }
}
