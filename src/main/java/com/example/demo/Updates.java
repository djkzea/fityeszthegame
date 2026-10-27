package com.example.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * Whether there is a newer game than the one being played.
 *
 * A player who installed the game has no way of knowing a new one exists -
 * the installers upgrade cleanly, but nothing tells anyone to go and fetch
 * one. This asks the releases page and, if what is there is newer than what
 * is running, hands back the number so the title screen can say so quietly.
 *
 * Quietly is the whole design. No network, a draft release, a rate limit, a
 * version this build cannot name: all of them end the same way, with nothing
 * on screen. An update is worth a line on the title page; it is not worth an
 * error message about a check nobody asked for.
 */
public final class Updates {

    private static final String LATEST =
            "https://api.github.com/repos/djkzea/fityeszthegame/releases/latest";

    /** Where the installers are, for the line to point at. */
    public static final String PAGE =
            "https://github.com/djkzea/fityeszthegame/releases/latest";

    private static final String VERSION_FILE = "/com/example/demo/version.properties";

    /** Asked once per run of the game, not once per title screen. */
    private static volatile String found;
    private static volatile boolean asked;

    private Updates() {
    }

    /**
     * The version this copy was built as.
     *
     * Written into the build by the packaging scripts, which take it from the
     * tag. A copy run from the source tree has no such file, and a build that
     * cannot name itself has no business telling anyone they are out of date.
     */
    public static String running() {

        try (InputStream in = Updates.class.getResourceAsStream(VERSION_FILE)) {

            if (in == null) {
                return "";
            }

            Properties p = new Properties();
            p.load(in);

            return p.getProperty("version", "").trim();

        } catch (IOException noFile) {
            return "";
        }
    }

    /** The newer version, if the answer names one. Package private for its test. */
    static Optional<String> newerIn(String body, String running) {

        if (body == null || running.isBlank()) {
            return Optional.empty();
        }

        int at = body.indexOf("\"tag_name\"");

        if (at < 0) {
            return Optional.empty();
        }

        int from = body.indexOf('"', body.indexOf(':', at)) + 1;
        int to = from <= 0 ? -1 : body.indexOf('"', from);

        if (from <= 0 || to < 0) {
            return Optional.empty();
        }

        String tag = body.substring(from, to).trim();
        String number = tag.startsWith("v") ? tag.substring(1) : tag;

        return newer(number, running) ? Optional.of(number) : Optional.empty();
    }

    /**
     * Whether one version number is above another.
     *
     * Dotted numbers, compared piece by piece, with anything that is not a
     * number counting as nothing: 1.0.10 is above 1.0.9, and a tag the scheme
     * does not fit is simply not newer, rather than an error.
     */
    static boolean newer(String candidate, String running) {

        String[] a = candidate.split("[.\\-+]");
        String[] b = running.split("[.\\-+]");

        for (int i = 0; i < Math.max(a.length, b.length); i++) {

            int one = number(a, i);
            int two = number(b, i);

            if (one != two) {
                return one > two;
            }
        }

        return false;
    }

    private static int number(String[] parts, int at) {

        if (at >= parts.length) {
            return 0;
        }

        try {
            return Integer.parseInt(parts[at].trim());
        } catch (NumberFormatException notANumber) {
            return 0;
        }
    }

    /**
     * Asks, once, and calls back on the thread it was asked on if there is
     * something to say. Never throws, never waits on the caller.
     */
    public static void check(Transport net, Consumer<String> say) {

        if (asked) {
            if (found != null) {
                say.accept(found);
            }
            return;
        }

        asked = true;

        String running = running();

        if (running.isBlank()) {
            return;         // a source build cannot know what it is
        }

        Thread worker = new Thread(() -> {

            try {
                Transport.Res res = net.send("GET", LATEST,
                        Map.of("Accept", "application/vnd.github+json"), null);

                if (res.ok()) {
                    newerIn(res.body(), running).ifPresent(version -> {
                        found = version;
                        say.accept(version);
                    });
                }

            } catch (IOException | RuntimeException offline) {
                // no release, no network, no bother
            }

        }, "fityesz-updates");

        worker.setDaemon(true);
        worker.start();
    }
}
