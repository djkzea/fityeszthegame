package com.example.demo;

import java.nio.file.Path;
import java.util.function.Function;

/**
 * Where this machine keeps the game's own files - the save and the session.
 *
 * Each platform has its own answer, and the test needs to check all three on
 * one machine, so the values come in as arguments and data() only fills them
 * in from the running system.
 */
public final class AppDirs {

    private static final String DIR = "FityeszKronika";

    private AppDirs() { }

    public static Path data() {
        return data(System.getProperty("os.name"), System.getProperty("user.home"), System::getenv);
    }

    static Path data(String osName, String home, Function<String, String> env) {

        String os = osName == null ? "" : osName.toLowerCase();

        if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", DIR);
        }

        if (os.contains("win")) {
            String appData = env.apply("APPDATA");
            return appData == null || appData.isBlank()
                    ? Path.of(home, DIR)
                    : Path.of(appData, DIR);
        }

        String xdg = env.apply("XDG_DATA_HOME");
        return xdg == null || xdg.isBlank()
                ? Path.of(home, ".local", "share", "fityesz")
                : Path.of(xdg, "fityesz");
    }
}
