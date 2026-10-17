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

    /**
     * How much of the run the log carries. Three hundred lines is the whole
     * demo twice over, and it keeps the save from growing without end.
     */
    public static final int MAX = 300;

    private final java.util.ArrayDeque<Entry> entries = new java.util.ArrayDeque<>();

    /** Something that was said. An empty speaker is narration. */
    public void line(int chapter, String speaker, String text) {
        add(new Entry("line", speaker == null ? "" : speaker, text == null ? "" : text,
                null, null, chapter));
    }

    /** A decision, with the question it answered and what it cost. */
    public void choice(int chapter, String question, String answer, int xp, int lebukas) {
        add(new Entry("choice", question, answer, xp, lebukas, chapter));
    }

    private void add(Entry entry) {

        entries.addLast(entry);

        while (entries.size() > MAX) {
            entries.removeFirst();
        }
    }

    /** The log as it stands, oldest first. */
    public java.util.List<Entry> entries() {
        return java.util.List.copyOf(entries);
    }

    /** Takes the log out of a save. */
    public void load(java.util.List<Entry> saved) {

        entries.clear();

        if (saved == null) {
            return;
        }

        for (Entry e : saved) {
            add(e);
        }
    }

    public void clear() {
        entries.clear();
    }
}
