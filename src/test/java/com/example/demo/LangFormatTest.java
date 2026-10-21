package com.example.demo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A text with a %s in it has to be given something to put there.
 *
 * Lang.t(key) without arguments returns the text as written, so a line meant to
 * read "-20 HP" reached a player as "-%s HP" during a boss fight. Nothing fails
 * when that happens: the game carries on and prints the placeholder, which is
 * why this is checked over the source rather than caught at runtime.
 */
class LangFormatTest {

    private static final Path SOURCE = Path.of("src/main/java/com/example/demo");

    private static final Pattern DEFINITION =
            Pattern.compile("p\\(\"([^\"]+)\",\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*,\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private static final Pattern USE = Pattern.compile("Lang\\.t\\(\"([^\"]+)\"\\s*([,)])");

    private static final Pattern PLACEHOLDER = Pattern.compile("%[sd]");

    /** Every key whose Hungarian or English text carries a placeholder. */
    private static Set<String> keysNeedingArguments() throws IOException {

        String lang = Files.readString(SOURCE.resolve("Lang.java"));
        Set<String> keys = new TreeSet<>();

        Matcher m = DEFINITION.matcher(lang);

        while (m.find()) {
            if (PLACEHOLDER.matcher(m.group(2)).find() || PLACEHOLDER.matcher(m.group(3)).find()) {
                keys.add(m.group(1));
            }
        }

        return keys;
    }

    @Test
    void theTextsWithPlaceholdersAreTheOnesWeThinkTheyAre() throws IOException {
        assertTrue(keysNeedingArguments().contains("boss3.hit"),
                "ez a teszt a Lang.java alakjára épül - ha a p(...) hívás megváltozott, itt kell követni");
    }

    @Test
    void noTextWithAPlaceholderIsUsedWithoutArguments() throws IOException {

        Set<String> needArguments = keysNeedingArguments();
        List<String> wrong = new ArrayList<>();

        try (Stream<Path> files = Files.walk(SOURCE)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {

                String src = Files.readString(file);
                Matcher m = USE.matcher(src);

                while (m.find()) {
                    if (needArguments.contains(m.group(1)) && ")".equals(m.group(2))) {
                        long line = src.substring(0, m.start()).chars().filter(c -> c == '\n').count() + 1;
                        wrong.add(file.getFileName() + ":" + line + "  " + m.group(1));
                    }
                }
            }
        }

        assertEquals(List.of(), wrong, "ezek a szövegek behelyettesítés nélkül jutnának a játékoshoz");
    }
}
