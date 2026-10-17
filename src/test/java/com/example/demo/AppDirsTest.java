package com.example.demo;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppDirsTest {

    @Test
    void macOsKeepsTheSaveUnderApplicationSupport() {
        Path p = AppDirs.data("Mac OS X", "/Users/teszt", name -> null);
        assertEquals(Path.of("/Users/teszt/Library/Application Support/FityeszKronika"), p);
    }

    @Test
    void windowsFollowsAppData() {
        Path p = AppDirs.data("Windows 11", "C:\\Users\\teszt",
                name -> "APPDATA".equals(name) ? "C:\\Users\\teszt\\AppData\\Roaming" : null);
        assertEquals(Path.of("C:\\Users\\teszt\\AppData\\Roaming", "FityeszKronika"), p);
    }

    @Test
    void windowsWithoutAppDataFallsBackToTheHomeDirectory() {
        Path p = AppDirs.data("Windows 11", "C:\\Users\\teszt", name -> null);
        assertEquals(Path.of("C:\\Users\\teszt", "FityeszKronika"), p);
    }

    @Test
    void linuxPrefersXdgDataHome() {
        Path p = AppDirs.data("Linux", "/home/teszt",
                name -> "XDG_DATA_HOME".equals(name) ? "/home/teszt/.data" : null);
        assertEquals(Path.of("/home/teszt/.data/fityesz"), p);
    }

    @Test
    void linuxWithoutXdgUsesLocalShare() {
        Path p = AppDirs.data("Linux", "/home/teszt", name -> "");
        assertEquals(Path.of("/home/teszt/.local/share/fityesz"), p);
    }
}
