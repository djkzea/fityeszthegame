package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

/**
 * The run on this machine.
 *
 * The local file is the real save: it is written synchronously, so a run
 * survives a crash and a plane trip. The cloud half is best effort on top of
 * this.
 *
 * Writing goes through a temporary file and an atomic move, because a save
 * truncated half way through is worse than no save at all.
 */
public class SaveStore {

    private final Path file;
    private final Supabase sb;

    public SaveStore(Path file) {
        this(file, null);
    }

    public SaveStore(Path file, Supabase sb) {
        this.file = file;
        this.sb = sb;
    }

    public void saveLocal(SaveData data) {

        try {
            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Path temp = file.resolveSibling(file.getFileName() + ".tmp");

            Files.writeString(temp, Json.mapper().writeValueAsString(data));

            Files.move(temp, file,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (IOException e) {
            // A save that cannot be written must not take the game down with it.
            System.err.println("save: " + e.getMessage());
        }
    }

    public Optional<SaveData> loadLocal() {

        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Json.mapper().readValue(Files.readString(file), SaveData.class));
        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** The fresher of two runs. A run whose timestamp cannot be read loses. */
    public static SaveData newer(SaveData a, SaveData b) {

        if (a == null) {
            return b;
        }

        if (b == null) {
            return a;
        }

        Instant ia = instantOf(a);
        Instant ib = instantOf(b);

        if (ia == null) {
            return ib == null ? a : b;
        }

        if (ib == null) {
            return a;
        }

        return ib.isAfter(ia) ? b : a;
    }

    private static Instant instantOf(SaveData d) {
        try {
            return d.updatedAt() == null ? null : Instant.parse(d.updatedAt());
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static final String COLUMNS =
            "player_name,display_name,chapter,scene,xp,lebukas,szint,items,history,log,fight,scenery,updated_at";

    /**
     * Writes the run into the account.
     *
     * Best effort: a failure is logged and the local file remains the save. The
     * website's rule travels with the row - a run with no decisions behind it
     * never overwrites the account, because that is what a failed restore looks
     * like, and the first click would otherwise wipe a finished run.
     */
    public void pushCloud(SaveData data, String userId, String token) {

        if (sb == null || token == null || userId == null) {
            return;
        }

        if (data.history() == null || data.history().isEmpty()) {
            return;
        }

        try {
            Transport.Res res = sb.call("POST", "/rest/v1/saves?on_conflict=user_id,slot",
                    rowOf(data, userId), token,
                    java.util.Map.of("Prefer", "resolution=merge-duplicates,return=minimal"));

            if (!res.ok()) {
                System.err.println("save: felho " + res.status() + " " + res.body());
            }

        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
        }
    }

    /** The account's run, if there is one. */
    public Optional<SaveData> fetchCloud(String token) {

        if (sb == null || token == null) {
            return Optional.empty();
        }

        try {
            Transport.Res res = sb.call("GET",
                    "/rest/v1/saves?slot=eq.1&select=" + COLUMNS, null, token);

            if (!res.ok()) {
                System.err.println("save: felho " + res.status());
                return Optional.empty();
            }

            SaveData[] rows = Json.mapper().readValue(res.body(), SaveData[].class);

            return rows.length == 0 ? Optional.empty() : Optional.of(rows[0]);

        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
            return Optional.empty();
        }
    }

    /** The upsert's body: the run plus the two columns that belong to the row. */
    String rowOf(SaveData data, String userId) throws IOException {

        com.fasterxml.jackson.databind.node.ObjectNode row = Json.mapper().valueToTree(data);

        row.put("user_id", userId);
        row.put("slot", 1);

        return Json.mapper().writeValueAsString(Json.mapper().createArrayNode().add(row));
    }
}
