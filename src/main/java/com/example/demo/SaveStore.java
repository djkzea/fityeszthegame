package com.example.demo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

/**
 * The runs on this machine, one per slot.
 *
 * The local file is the real save: it is written synchronously, so a run
 * survives a crash and a plane trip. The cloud half is best effort on top of
 * this.
 *
 * Writing goes through a temporary file and an atomic move, because a save
 * truncated half way through is worse than no save at all.
 */
public class SaveStore {

    /** Three, because that is what the saves table's own check constraint allows. */
    public static final int SLOTS = 3;

    private final Path dir;
    private final Supabase sb;

    public SaveStore(Path dir) {
        this(dir, null);
    }

    public SaveStore(Path dir, Supabase sb) {
        this.dir = dir;
        this.sb = sb;
    }

    public Path fileFor(int slot) {
        return dir.resolve("save-slot" + slot + ".json");
    }

    public void saveLocal(int slot, SaveData data) {

        Path file = fileFor(slot);

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

    public Optional<SaveData> loadLocal(int slot) {

        Path file = fileFor(slot);

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
        return d == null ? null : moment(d.updatedAt());
    }

    /**
     * A saved moment, however it was spelled.
     *
     * Java writes "…711238Z"; Postgres hands the same instant back as
     * "…711238+00:00". Comparing those as strings makes one run look like two,
     * which is how a run that was already in the account ended up copied into
     * a second slot.
     */
    private static Instant moment(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return java.time.OffsetDateTime.parse(text).toInstant();
        } catch (RuntimeException notAnOffset) {
            try {
                return Instant.parse(text);
            } catch (RuntimeException unreadable) {
                return null;
            }
        }
    }

    /** What the saves table held before migration 0003. */
    private static final String WEBSITE_COLUMNS =
            "player_name,display_name,chapter,xp,lebukas,szint,items,history,updated_at";

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
    public void pushCloud(int slot, SaveData data, String userId, String token) {

        if (sb == null || token == null || userId == null) {
            return;
        }

        if (data.history() == null || data.history().isEmpty()) {
            return;
        }

        try {
            Transport.Res res = upsert(rowOf(data, userId, slot, false), token);

            // 42703 is Postgres for "no such column": the project has not had the
            // desktop migration run on it yet. The stage, the log and the fight
            // are what it does not know about, and a run without them is still
            // worth keeping - the website's own columns go up on their own.
            if (!res.ok() && res.body() != null && res.body().contains("42703")) {
                res = upsert(rowOf(data, userId, slot, true), token);
            }

            if (!res.ok()) {
                System.err.println("save: felho " + res.status() + " " + res.body());
            }

        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
        }
    }

    private Transport.Res upsert(String body, String token) throws IOException {
        return sb.call("POST", "/rest/v1/saves?on_conflict=user_id,slot", body, token,
                java.util.Map.of("Prefer", "resolution=merge-duplicates,return=minimal"));
    }

    /**
     * Every run the account holds, by slot.
     *
     * One request for all three: the title screen needs them together, and row
     * level security already limits the answer to this player's own rows.
     */
    public java.util.Map<Integer, SaveData> fetchCloud(String token) {

        java.util.Map<Integer, SaveData> runs = new java.util.LinkedHashMap<>();

        if (sb == null || token == null) {
            return runs;
        }

        try {
            Transport.Res res = sb.call("GET", "/rest/v1/saves?select=slot," + COLUMNS, null, token);

            if (!res.ok() && res.body() != null && res.body().contains("42703")) {
                res = sb.call("GET", "/rest/v1/saves?select=slot," + WEBSITE_COLUMNS, null, token);
            }

            if (!res.ok()) {
                System.err.println("save: felho " + res.status());
                return runs;
            }

            for (com.fasterxml.jackson.databind.JsonNode row : Json.mapper().readTree(res.body())) {

                int slot = row.path("slot").asInt(1);

                if (slot >= 1 && slot <= SLOTS) {
                    runs.put(slot, Json.mapper().treeToValue(row, SaveData.class));
                }
            }

        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
        }

        return runs;
    }

    /**
     * Settles this machine's runs against the account's, in both directions.
     *
     * Signing in is where two histories meet: runs played as a guest sit in the
     * local files, the account's runs sit in the cloud, and Supabase cannot
     * merge two users. So:
     *
     *  - a run only the account has comes down;
     *  - a run only this machine has goes up, which is how a guest's runs are
     *    claimed - and how a run played offline reaches the account at all;
     *  - the same run on both sides, carried further here, goes up;
     *  - two genuinely different runs in one slot: the account keeps the slot,
     *    because that is the run that follows the player to another machine,
     *    and this machine's run moves to a free slot (or, with none free, to a
     *    leftover file) rather than being written over.
     *
     * Safe to run more than once: once the two sides agree, nothing moves.
     */
    public void reconcile(String userId, String token) {

        java.util.Map<Integer, SaveData> cloud = fetchCloud(token);

        for (int slot = 1; slot <= SLOTS; slot++) {

            SaveData theirs = cloud.get(slot);
            SaveData ours = loadLocal(slot).orElse(null);

            if (theirs == null) {

                if (ours != null) {
                    pushCloud(slot, ours, userId, token);
                }

                continue;
            }

            if (ours == null) {
                saveLocal(slot, theirs);
                continue;
            }

            if (sameRun(ours, theirs)) {
                continue;
            }

            if (continues(ours, theirs)) {
                pushCloud(slot, ours, userId, token);
                continue;
            }

            int free = freeSlot(cloud);

            if (free > 0) {
                saveLocal(free, ours);
                pushCloud(free, ours, userId, token);
            } else {
                keepAside(ours, slot);
            }

            saveLocal(slot, theirs);
        }
    }

    /** The same run, taken further on this machine than the account has seen. */
    private static boolean continues(SaveData ours, SaveData theirs) {

        if (!java.util.Objects.equals(ours.playerName(), theirs.playerName())) {
            return false;
        }

        // a run does not go backwards: an earlier chapter under the same name
        // is a different run, not this one carried further
        if (ours.chapter() < theirs.chapter()) {
            return false;
        }

        int decisionsHere = ours.history() == null ? 0 : ours.history().size();
        int decisionsThere = theirs.history() == null ? 0 : theirs.history().size();

        if (decisionsHere < decisionsThere) {
            return false;
        }

        Instant here = moment(ours.updatedAt());
        Instant there = moment(theirs.updatedAt());

        return here != null && there != null && here.isAfter(there);
    }

    /** A slot neither this machine nor the account is using. 0 if there is none. */
    private int freeSlot(java.util.Map<Integer, SaveData> cloud) {

        for (int slot = 1; slot <= SLOTS; slot++) {
            if (cloud.get(slot) == null && loadLocal(slot).isEmpty()) {
                return slot;
            }
        }

        return 0;
    }

    /** Two reads of the same run - the same moment, saved by the same player. */
    private static boolean sameRun(SaveData a, SaveData b) {

        Instant here = moment(a.updatedAt());
        Instant there = moment(b.updatedAt());

        return here != null && here.equals(there)
                && java.util.Objects.equals(a.playerName(), b.playerName());
    }

    /** Somewhere a run can wait that is not a slot. Not shown anywhere; just not deleted. */
    private void keepAside(SaveData run, int slot) {

        Path aside = dir.resolve("save-slot" + slot + "-guest-"
                + java.time.Instant.now().toEpochMilli() + ".json");

        try {
            Files.createDirectories(dir);
            Files.writeString(aside, Json.mapper().writeValueAsString(run));
        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
        }
    }

    /**
     * What every slot holds, this machine and the account taken together, the
     * fresher of the two winning. A slot with no run at all is left out.
     */
    public java.util.Map<Integer, SaveData> runs(String token) {

        java.util.Map<Integer, SaveData> cloud = fetchCloud(token);
        java.util.Map<Integer, SaveData> all = new java.util.LinkedHashMap<>();

        for (int slot = 1; slot <= SLOTS; slot++) {

            SaveData run = newer(loadLocal(slot).orElse(null), cloud.get(slot));

            if (run != null) {
                all.put(slot, run);
            }
        }

        return all;
    }

    /**
     * The upsert's body: the run plus the two columns that belong to the row.
     * "websiteOnly" leaves out what migration 0003 added, for a project that has
     * not had it run yet.
     */
    String rowOf(SaveData data, String userId, int slot, boolean websiteOnly) throws IOException {

        com.fasterxml.jackson.databind.node.ObjectNode row = Json.mapper().valueToTree(data);

        row.put("user_id", userId);
        row.put("slot", slot);

        if (websiteOnly) {
            row.remove(java.util.List.of("scene", "log", "fight", "scenery"));
        }

        return Json.mapper().writeValueAsString(Json.mapper().createArrayNode().add(row));
    }
}
