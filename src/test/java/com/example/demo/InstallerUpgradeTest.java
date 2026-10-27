package com.example.demo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * What makes a new installer an upgrade rather than a second copy.
 *
 * Windows decides that by a single number, and jpackage works it out from the
 * name and the vendor alone - not the version:
 *
 *     UpgradeCode = UUID.nameUUIDFromBytes("UpgradeCode/<vendor>/<name>")
 *     ProductCode = UUID.nameUUIDFromBytes("ProductCode/<vendor>/<name>/<version>")
 *
 * So the two strings in the packaging scripts are load bearing. Rename the game
 * or the vendor and every machine that already has it installed stops seeing the
 * new one as the same product: it installs beside the old one, with two entries
 * in the start menu and two in Add/Remove Programs. Nothing fails loudly - which
 * is why it is pinned here, against the code the 1.0.0 installer was built with.
 */
class InstallerUpgradeTest {

    /** Read out of the Windows build log of the 1.0.0 installer. */
    private static final String SHIPPED_UPGRADE_CODE = "788f347d-de65-33f6-ab57-0719213c6d7f";

    private static final String NAME = "Fityesz Kronika";
    private static final String VENDOR = "fityeszthegame.com";

    private static String find(String file, String pattern) throws IOException {

        String text = Files.readString(Path.of("packaging", file), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile(pattern).matcher(text);

        if (!m.find()) {
            throw new AssertionError("nincs meg a mintazat a packaging/" + file + "-ban: " + pattern);
        }

        return m.group(1).trim();
    }

    private static UUID code(String what, String... parts) {
        return UUID.nameUUIDFromBytes((what + "/" + String.join("/", parts))
                .getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void theWindowsInstallerStillUpgradesTheOneAlreadyOutThere() throws IOException {

        String name = find("package.bat", "set NAME=(.*)");
        String vendor = find("package.bat", "--vendor (\\S+)");

        assertEquals(NAME, name, "a jatek neve megvaltozott a Windows-szkriptben");
        assertEquals(VENDOR, vendor, "a kiado neve megvaltozott a Windows-szkriptben");

        assertEquals(SHIPPED_UPGRADE_CODE, code("UpgradeCode", vendor, name).toString(),
                "ez a telepito nem frissiteskent erkezne a mar telepitett jatekra, hanem melle");
    }

    @Test
    void aNewVersionIsANewProductCode() {

        assertEquals("31eb5e96-6399-3a2a-bae6-a7845ee387f2",
                code("ProductCode", VENDOR, NAME, "1.0.0").toString(),
                "a kiadott 1.0.0 termekkodja nem ez volt");

        // and the next version has to differ, or Windows refuses the upgrade
        org.junit.jupiter.api.Assertions.assertNotEquals(
                code("ProductCode", VENDOR, NAME, "1.0.0"),
                code("ProductCode", VENDOR, NAME, "1.1.0"));
    }

    @Test
    void theOtherTwoScriptsCallItTheSameThing() throws IOException {

        assertEquals(NAME, find("package.sh", "NAME=\"(.*)\""),
                "a mac es a linux szkript mas neven epitene");
        assertEquals(VENDOR, find("package.sh", "VENDOR=\"(.*)\""),
                "a mac es a linux szkript mas kiadot irna");
    }
}
