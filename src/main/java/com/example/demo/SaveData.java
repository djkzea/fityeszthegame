package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One run, in the shape the saves table keeps it.
 *
 * The local file and the Supabase row are the same JSON, so the component
 * names carry the column names rather than Java's camel case. Unknown
 * properties are ignored on the way in: the row also holds id, user_id and
 * slot, which belong to the row and not to the run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveData(
        @JsonProperty("player_name") String playerName,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("chapter") int chapter,
        @JsonProperty("scene") String scene,
        @JsonProperty("xp") int xp,
        @JsonProperty("lebukas") int lebukas,
        @JsonProperty("szint") int szint,
        @JsonProperty("items") List<String> items,
        @JsonProperty("history") List<Decision> history,
        @JsonProperty("log") List<Chatlog.Entry> log,
        @JsonProperty("fight") Fight fight,
        @JsonProperty("updated_at") String updatedAt) {

    /** A choice the player made, keyed the way the website keys it. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Decision(
            @JsonProperty("choicePointId") String choicePointId,
            @JsonProperty("optionIndex") int optionIndex) { }

    /** What a boss fight is holding that the stats do not: the move counters. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fight(
            @JsonProperty("playerHp") int playerHp,
            @JsonProperty("bossHp") int bossHp,
            @JsonProperty("previousMove") int previousMove,
            @JsonProperty("sameMove") int sameMove,
            @JsonProperty("peteriPassiveRounds") int peteriPassiveRounds) { }
}
