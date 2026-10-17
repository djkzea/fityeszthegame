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

    public SaveStore(Path file) {
        this.file = file;
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
}
