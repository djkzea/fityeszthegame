package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** The run's own transcript. */
public final class Chatlog {

    /**
     * One line of the log. "line" is something that was said, "choice" is a
     * decision with what it cost; the fields the other kind does not use are
     * null, which is why the numbers are boxed.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(
            @JsonProperty("kind") String kind,
            @JsonProperty("speaker") String speaker,
            @JsonProperty("text") String text,
            @JsonProperty("xp") Integer xp,
            @JsonProperty("lebukas") Integer lebukas,
            @JsonProperty("chapter") Integer chapter) { }
}
