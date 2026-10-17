# Bejelentkezés, chatlog, játék betöltése — implementációs terv

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A desktop játék fiókkal beléphető a weboldal Supabase projektjébe, a futás helyben és a felhőben mentődik, pontos jelenetre tölthető vissza, és a párbeszédek visszanézhetők egy naplóban.

**Architecture:** Négy új, UI nélküli osztály (`AppDirs`, `SaveData`, `SaveStore`, `Chatlog`) viszi az állapotot és a mentést; a hálózat egy egymetódusos `Transport` mögött van, így teszthez nem kell szerver. A `GameController` jelenetmetódusai egy `step()` hívásra cserélődnek, ami `StackWalker`-rel megjegyzi, melyik jelenetben vagyunk — ez adja a pontos visszatöltést regiszter nélkül. A felületek (`LoginScreen`, napló overlay, pause menüpontok) a meglévő `OpeningScreen`/`GameUI` mintáit követik.

**Tech Stack:** Java 24 forrásszint (Liberica JDK 25), JavaFX 21.0.6, Jackson databind 2.14.2 (**már a classpathon, tranzitívan — új dependency nincs**), `java.net.http.HttpClient` (JDK), JUnit 5.12.1, Supabase (GoTrue + PostgREST + Edge Function).

**Spec:** `docs/superpowers/specs/2026-10-17-login-chatlog-loadgame-design.md`

## Global Constraints

- **Nincs új Maven dependency.** A JSON-t a már jelen lévő `jackson-databind` 2.14.2 viszi, a HTTP-t a JDK `java.net.http`.
- **Forrásszint 24** (`maven-compiler-plugin` `<source>24</source>`), modulos projekt: minden új `requires`/`opens` a `src/main/java/module-info.java`-ba kerül.
- **Minden felhasználónak látható szöveg `Lang`-on keresztül**, a `p(kulcs, magyar, angol)` alakban, mindkét nyelven. Nyers string a UI-ban nincs.
- **Commit üzenet a repó stílusában**: felszólító mód, prefix nélkül (pl. „Keep the opponent on screen through a fight"). Nincs `feat:`/`fix:` prefix, és **nincs `Co-Authored-By` sor**.
- **Egyetlen hibaszöveg az authra.** A `sign-in` Edge Function szándékosan mindenre `invalid_credentials`-t ad, hogy a felhasználónevek ne legyenek kitalálhatók; a desktop sem mond többet (`ui.authFailed`).
- **A hálózat soha nem blokkolja a játékot.** Minden HTTP hívás a JavaFX szálon kívül fut, hibája naplóba megy, a játék vendég módban megy tovább. UI-t csak `Platform.runLater` frissít.
- **A mentés egy alak.** A helyi JSON fájl és a Supabase sor ugyanaz a szerializáció: snake_case kulcsok (`player_name`, `updated_at`), mert a `saves` tábla oszlopai így hívódnak.
- **A közös mentés a slot 1**, ahogy a weboldal használja.
- Supabase URL és anon key: `FITYESZ_SUPABASE_URL` / `FITYESZ_SUPABASE_ANON_KEY` env-változó, különben a `supabase.properties` erőforrás. **A properties fájl gitignore-ba megy**, nem kerül commitba.

---

## Fájlszerkezet

**Új fájlok (`src/main/java/com/example/demo/`)**

| Fájl | Felelősség |
|---|---|
| `AppDirs.java` | Hol van a játék adatkönyvtára ezen a gépen. Egy metódus, platformonként. |
| `SaveData.java` | A mentés alakja: `record`, snake_case JSON kulcsokkal. Benne `Decision` és `Fight`. |
| `Item.java` | A hét tárgy enumja: `GameState` boolean ↔ web `ItemId` string. |
| `Chatlog.java` | A napló bejegyzései, 300-as korláttal. |
| `Json.java` | Egyetlen közös `ObjectMapper`, hogy ne szórjunk szét konfigurációt. |
| `Transport.java` | `send(method, url, headers, body) -> Res`. Ez a hálózat teljes felülete. |
| `HttpTransport.java` | A `Transport` éles megvalósítása `HttpClient`-tel, 8 másodperces időkorláttal. |
| `Supabase.java` | URL + anon key + `Transport`; `postJson`, `getJson`, `upsert`. |
| `Account.java` | Regisztráció, belépés, token-megújítás, kilépés, session fájl. |
| `SaveStore.java` | Helyi fájl írás/olvasás, felhős upsert/olvasás, „a frissebb nyer". |
| `LoginScreen.java` | A belépés/regisztráció felülete. |

**Új tesztek (`src/test/java/com/example/demo/`)**: `AppDirsTest`, `SaveDataTest`, `ItemTest`, `ChatlogTest`, `SaveStoreTest`, `AccountTest`, `SceneNamesTest`.

**Módosított fájlok**

| Fájl | Mit |
|---|---|
| `pom.xml` | `maven-surefire-plugin` 3.2.5, `useModulePath=false` |
| `module-info.java` | `requires com.fasterxml.jackson.databind;`, `requires java.net.http;`, `opens … to com.fasterxml.jackson.databind;` |
| `.gitignore` | `src/main/resources/com/example/demo/supabase.properties` |
| `GameController.java` | `step()`, `say()`, `recordDecision` bővítés, `saveNow()`, `resume()`, chapter-belépők |
| `GameUI.java` | Napló overlay a `stagePane`-ben, `NAPLÓ` gomb a headerben, `L` billentyű |
| `OpeningScreen.java` | `BELÉPÉS` menüsor, aktív `LOAD GAME`, folytatás-kártya |
| `HelloApplication.java` | Pause menü `MENTÉS` és `NAPLÓ`, ESC-sorrend a nyitott napló mellett |
| `Lang.java` | Új kulcsok |

**Weboldal repó (`~/WebstormProjects/fityeszTheGameWebsite`)**: `supabase/migrations/0003_crossplay.sql`, és a `chapter >= 2` szövegei.

---

### Task 1: Tesztek futnak, és megvan az adatkönyvtár

A projekt modulos, és nincs benne `src/test`. A Maven 3.8.5 alapértelmezett surefire-je 2.12.4, ami **nem ismeri a JUnit 5-öt** — enélkül a többi task tesztjei csendben ki sem futnak. A `useModulePath=false` azért kell, mert a `module-info.java` mellett a surefire modulként indítaná a teszteket, és minden teszt-csomagot külön `opens`-szal kellene engedni.

**Files:**
- Modify: `pom.xml` (a `<plugins>` blokk, a `javafx-maven-plugin` után)
- Create: `src/main/java/com/example/demo/AppDirs.java`
- Test: `src/test/java/com/example/demo/AppDirsTest.java`

**Interfaces:**
- Consumes: semmi
- Produces: `AppDirs.data()` → `java.nio.file.Path` (a játék adatkönyvtára), `AppDirs.data(String osName, String home, java.util.function.Function<String,String> env)` → `Path` (ugyanaz, tesztelhető bemenettel)

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/demo/AppDirsTest.java`:

```java
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
```

- [ ] **Step 2: Add surefire so the test can run at all**

A `pom.xml` `<plugins>` blokkjába, a `javafx-maven-plugin` `</plugin>` zárása után:

```xml
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.5</version>
                <configuration>
                    <!-- The project is modular, the tests are not. Running them on the
                         classpath keeps every test package from needing its own opens. -->
                    <useModulePath>false</useModulePath>
                </configuration>
            </plugin>
```

- [ ] **Step 3: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=AppDirsTest`
Expected: FAIL — fordítási hiba, `cannot find symbol: class AppDirs`

- [ ] **Step 4: Write minimal implementation**

`src/main/java/com/example/demo/AppDirs.java`:

```java
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
```

- [ ] **Step 5: Run the test to verify it passes**

Run: `./mvnw -o test -Dtest=AppDirsTest`
Expected: PASS — `Tests run: 5, Failures: 0`

- [ ] **Step 6: Commit**

```bash
git add pom.xml src/main/java/com/example/demo/AppDirs.java src/test/java/com/example/demo/AppDirsTest.java
git commit -m "Run tests with JUnit 5, and find the game's own directory"
```

---

### Task 2: A mentés alakja és a tárgyak leképezése

A helyi fájl és a Supabase sor ugyanaz a JSON, ezért a `record` komponensei `@JsonProperty`-vel a tábla oszlopneveit viselik. A hét `GameState` boolean és a web hét `ItemId` stringje között egy enum tart kapcsolatot — ez a cross-play egyetlen fordítási pontja.

**Files:**
- Create: `src/main/java/com/example/demo/Json.java`
- Create: `src/main/java/com/example/demo/SaveData.java`
- Create: `src/main/java/com/example/demo/Item.java`
- Modify: `src/main/java/module-info.java`
- Test: `src/test/java/com/example/demo/SaveDataTest.java`
- Test: `src/test/java/com/example/demo/ItemTest.java`

**Interfaces:**
- Consumes: `GameState` (meglévő: `getXp()`, `addXp(int)`, `getExposure()`, `addExposure(int)`, `getLevel()`, `getName()`, `setName(String)`, `getPlayerHp()`, `setPlayerHp(int)`, `getBossHp()`, `setBossHp(int)`, és a hét `hasX()`/`setX(boolean)` pár)
- Produces:
  - `Json.mapper()` → `com.fasterxml.jackson.databind.ObjectMapper`
  - `SaveData` record: `playerName`, `displayName`, `chapter`, `scene`, `xp`, `lebukas`, `szint`, `items` (`List<String>`), `history` (`List<SaveData.Decision>`), `log` (`List<Chatlog.Entry>` — a Task 4 vezeti be, itt `List<Object>`-ként még nem elég, ezért a Task 4 **előtt** ez a task a `log` mezőt `List<Chatlog.Entry>`-ként írja le, és a `Chatlog.Entry` rekord ebben a taskban készül el a `Chatlog` osztály nélkül), `fight` (`SaveData.Fight`), `updatedAt`
  - `SaveData.Decision(String choicePointId, int optionIndex)`
  - `SaveData.Fight(int playerHp, int bossHp, int previousMove, int sameMove, int peteriPassiveRounds)`
  - `Chatlog.Entry(String kind, String speaker, String text, Integer xp, Integer lebukas, Integer chapter)` — a `Chatlog` osztály üres kerete ebben a taskban, a logika a Task 4-ben
  - `Item` enum: `Item.idsOf(GameState)` → `List<String>`, `Item.apply(List<String>, GameState)` → `void`

- [ ] **Step 1: Write the failing tests**

`src/test/java/com/example/demo/ItemTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    @Test
    void everyItemSurvivesTheRoundTrip() {

        GameState before = new GameState();
        before.setFirstEnvelope(true);
        before.setSmallEnvelope(true);
        before.setLakatosFile(true);
        before.setOffshoreCode(true);
        before.setPeteriDossier(true);
        before.setBossTrust(true);
        before.setParliamentKey(true);

        List<String> ids = Item.idsOf(before);
        assertEquals(7, ids.size());

        GameState after = new GameState();
        Item.apply(ids, after);

        assertTrue(after.hasFirstEnvelope());
        assertTrue(after.hasSmallEnvelope());
        assertTrue(after.hasLakatosFile());
        assertTrue(after.hasOffshoreCode());
        assertTrue(after.hasPeteriDossier());
        assertTrue(after.hasBossTrust());
        assertTrue(after.hasParliamentKey());
    }

    @Test
    void theIdsAreTheOnesTheWebsiteWrites() {
        GameState s = new GameState();
        s.setFirstEnvelope(true);
        s.setOffshoreCode(true);
        assertEquals(List.of("envelope1", "offshore"), Item.idsOf(s));
    }

    @Test
    void anEmptyStateCarriesNoItems() {
        assertEquals(List.of(), Item.idsOf(new GameState()));
    }

    @Test
    void anUnknownIdIsIgnoredRatherThanThrowing() {
        GameState s = new GameState();
        Item.apply(List.of("envelope1", "valamiUjAWebrol"), s);
        assertTrue(s.hasFirstEnvelope());
    }
}
```

`src/test/java/com/example/demo/SaveDataTest.java`:

```java
package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SaveDataTest {

    private static SaveData sample() {
        return new SaveData(
                "Zsombi", "Zsombi", 3, "lipotiOffer",
                120, 40, 2,
                List.of("envelope1", "lakatosFile"),
                List.of(new SaveData.Decision("ch1.q1", 0), new SaveData.Decision("ch2.q", 2)),
                List.of(new Chatlog.Entry("line", "LIPÓTI", "Jó napot.", null, null, 1)),
                new SaveData.Fight(90, 55, 2, 1, 0),
                "2026-10-17T18:20:00Z");
    }

    @Test
    void theJsonKeysAreTheTableColumns() throws Exception {
        JsonNode n = Json.mapper().valueToTree(sample());
        assertEquals("Zsombi", n.get("player_name").asText());
        assertEquals("Zsombi", n.get("display_name").asText());
        assertEquals(3, n.get("chapter").asInt());
        assertEquals("lipotiOffer", n.get("scene").asText());
        assertEquals(40, n.get("lebukas").asInt());
        assertEquals(2, n.get("szint").asInt());
        assertEquals("2026-10-17T18:20:00Z", n.get("updated_at").asText());
        assertEquals("ch1.q1", n.get("history").get(0).get("choicePointId").asText());
        assertEquals(90, n.get("fight").get("playerHp").asInt());
    }

    @Test
    void everyFieldComesBack() throws Exception {
        String json = Json.mapper().writeValueAsString(sample());
        SaveData back = Json.mapper().readValue(json, SaveData.class);
        assertEquals(sample(), back);
    }

    @Test
    void aMissingFightIsNullRatherThanAnError() throws Exception {
        SaveData d = Json.mapper().readValue(
                "{\"player_name\":\"A\",\"chapter\":1,\"xp\":0,\"lebukas\":0,\"szint\":1}",
                SaveData.class);
        assertNull(d.fight());
        assertEquals("A", d.playerName());
    }

    @Test
    void columnsTheWebsiteAddsLaterDoNotBreakTheRead() throws Exception {
        SaveData d = Json.mapper().readValue(
                "{\"id\":\"abc\",\"user_id\":\"u1\",\"slot\":1,\"player_name\":\"A\"}",
                SaveData.class);
        assertEquals("A", d.playerName());
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./mvnw -o test -Dtest='ItemTest+SaveDataTest'`
Expected: FAIL — `cannot find symbol: class Item`, `class SaveData`, `class Json`

- [ ] **Step 3: Write the implementation**

`src/main/java/com/example/demo/Json.java`:

```java
package com.example.demo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

/** One mapper for the whole game, so the save is written the same way everywhere. */
public final class Json {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .setSerializationInclusion(JsonInclude.Include.ALWAYS);

    private Json() { }

    public static ObjectMapper mapper() {
        return MAPPER;
    }
}
```

`src/main/java/com/example/demo/SaveData.java`:

```java
package com.example.demo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One run, in the shape the saves table keeps it.
 *
 * The local file and the Supabase row are the same JSON, so the component
 * names carry the column names rather than Java's camel case. Unknown
 * properties are ignored on the way in: the row also holds id, user_id and
 * slot, which belong to the row and not to the run.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SaveData(
        @JsonProperty("player_name") String playerName,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("chapter") int chapter,
        @JsonProperty("scene") String scene,
        @JsonProperty("xp") int xp,
        @JsonProperty("lebukas") int lebukas,
        @JsonProperty("szint") int szint,
        @JsonProperty("items") List<String> items,
        @JsonProperty("history") List<Decision> history,
        @JsonProperty("log") List<Chatlog.Entry> log,
        @JsonProperty("fight") Fight fight,
        @JsonProperty("updated_at") String updatedAt) {

    /** A choice the player made, keyed the way the website keys it. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Decision(
            @JsonProperty("choicePointId") String choicePointId,
            @JsonProperty("optionIndex") int optionIndex) { }

    /** What a boss fight is holding that the stats do not: the move counters. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fight(
            @JsonProperty("playerHp") int playerHp,
            @JsonProperty("bossHp") int bossHp,
            @JsonProperty("previousMove") int previousMove,
            @JsonProperty("sameMove") int sameMove,
            @JsonProperty("peteriPassiveRounds") int peteriPassiveRounds) { }
}
```

`src/main/java/com/example/demo/Chatlog.java` — ebben a taskban csak a rekord, a logika a Task 4-ben:

```java
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
```

`src/main/java/com/example/demo/Item.java`:

```java
package com.example.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * The seven items, as the game holds them and as the website names them.
 *
 * This enum is the only place the two vocabularies meet, so a save written on
 * one side is read correctly on the other. The ids are from ItemId in
 * lib/story/types.ts and must not be renamed on a whim.
 */
public enum Item {

    FIRST_ENVELOPE("envelope1", GameState::hasFirstEnvelope, GameState::setFirstEnvelope),
    SMALL_ENVELOPE("envelopeSmall", GameState::hasSmallEnvelope, GameState::setSmallEnvelope),
    LAKATOS_FILE("lakatosFile", GameState::hasLakatosFile, GameState::setLakatosFile),
    OFFSHORE("offshore", GameState::hasOffshoreCode, GameState::setOffshoreCode),
    PETERI_DOSSIER("peteriDossier", GameState::hasPeteriDossier, GameState::setPeteriDossier),
    BOSS_TRUST("bossTrust", GameState::hasBossTrust, GameState::setBossTrust),
    PARLIAMENT_KEY("parliamentKey", GameState::hasParliamentKey, GameState::setParliamentKey);

    private final String id;
    private final Predicate<GameState> has;
    private final BiConsumer<GameState, Boolean> set;

    Item(String id, Predicate<GameState> has, BiConsumer<GameState, Boolean> set) {
        this.id = id;
        this.has = has;
        this.set = set;
    }

    public String id() {
        return id;
    }

    /** The ids the state is carrying, in this enum's order. */
    public static List<String> idsOf(GameState state) {

        List<String> ids = new ArrayList<>();

        for (Item item : values()) {
            if (item.has.test(state)) {
                ids.add(item.id);
            }
        }

        return ids;
    }

    /** Puts the ids back on the state. An id this build does not know is skipped. */
    public static void apply(List<String> ids, GameState state) {

        if (ids == null) {
            return;
        }

        for (Item item : values()) {
            item.set.accept(state, ids.contains(item.id));
        }
    }
}
```

- [ ] **Step 4: Teach the module about Jackson**

`src/main/java/module-info.java` — a `requires` blokkba és az `opens` mellé:

```java
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens com.example.demo to javafx.fxml, com.fasterxml.jackson.databind;
```

(A meglévő `opens com.example.demo to javafx.fxml;` sort kell kiegészíteni, nem újat írni mellé — a Jackson reflexióval olvassa a rekordokat, enélkül `InaccessibleObjectException` jön futásidőben.)

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./mvnw -o test -Dtest='ItemTest+SaveDataTest'`
Expected: PASS — `Tests run: 8, Failures: 0`

- [ ] **Step 6: Check that the game itself still starts**

Run: `./mvnw -o javafx:run`
Expected: a címképernyő feljön, nincs `InaccessibleObjectException` a konzolon. Zárd be.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/demo/Json.java src/main/java/com/example/demo/SaveData.java \
        src/main/java/com/example/demo/Chatlog.java src/main/java/com/example/demo/Item.java \
        src/main/java/module-info.java \
        src/test/java/com/example/demo/SaveDataTest.java src/test/java/com/example/demo/ItemTest.java
git commit -m "Describe a saved run in the shape the saves table keeps it"
```

---

### Task 3: A mentés helyi fele

A helyi fájl az igazi mentés: azonnal, szinkronban íródik, és hálózat nélkül is teljes. Az írás ideiglenes fájlba megy, majd `ATOMIC_MOVE` — félbeírt mentés így nem keletkezik.

**Files:**
- Create: `src/main/java/com/example/demo/SaveStore.java`
- Test: `src/test/java/com/example/demo/SaveStoreTest.java`

**Interfaces:**
- Consumes: `SaveData`, `Json.mapper()`
- Produces:
  - `new SaveStore(Path file)` — a felhős fél a Task 7-ben jön egy második konstruktorral
  - `SaveStore.saveLocal(SaveData)` → `void`
  - `SaveStore.loadLocal()` → `java.util.Optional<SaveData>`
  - `SaveStore.newer(SaveData a, SaveData b)` → `SaveData` (statikus; `null` bemenet megengedett)

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/demo/SaveStoreTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaveStoreTest {

    private static SaveData run(int chapter, String scene, String updatedAt) {
        return new SaveData("Zsombi", "Zsombi", chapter, scene, 10, 5, 1,
                List.of("envelope1"), List.of(new SaveData.Decision("ch1.q1", 0)),
                List.of(), null, updatedAt);
    }

    @Test
    void whatWasSavedIsWhatComesBack(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"));
        SaveData d = run(2, "lakatosRules", "2026-10-17T18:00:00Z");

        store.saveLocal(d);

        assertEquals(Optional.of(d), store.loadLocal());
    }

    @Test
    void noFileMeansNoSaveRatherThanAnError(@TempDir Path dir) {
        assertTrue(new SaveStore(dir.resolve("nincs.json")).loadLocal().isEmpty());
    }

    @Test
    void aDamagedFileReadsAsNoSave(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("save-slot1.json");
        Files.writeString(file, "{ ez nem json");
        assertTrue(new SaveStore(file).loadLocal().isEmpty());
    }

    @Test
    void savingTwiceLeavesNoTemporaryFileBehind(@TempDir Path dir) throws IOException {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"));
        store.saveLocal(run(1, "chapter1", "2026-10-17T18:00:00Z"));
        store.saveLocal(run(2, "chapter2", "2026-10-17T18:05:00Z"));

        try (var files = Files.list(dir)) {
            assertEquals(List.of("save-slot1.json"), files.map(p -> p.getFileName().toString()).toList());
        }
        assertEquals(2, store.loadLocal().orElseThrow().chapter());
    }

    @Test
    void theDirectoryIsCreatedIfItIsNotThereYet(@TempDir Path dir) {
        Path file = dir.resolve("meg").resolve("nincs").resolve("save-slot1.json");
        new SaveStore(file).saveLocal(run(1, "chapter1", "2026-10-17T18:00:00Z"));
        assertTrue(Files.exists(file));
    }

    @Test
    void theFresherRunWins() {
        SaveData older = run(1, "chapter1", "2026-10-17T18:00:00Z");
        SaveData newer = run(4, "chapter4", "2026-10-17T19:00:00Z");

        assertEquals(newer, SaveStore.newer(older, newer));
        assertEquals(newer, SaveStore.newer(newer, older));
    }

    @Test
    void aMissingSideIsNotAWinner() {
        SaveData only = run(1, "chapter1", "2026-10-17T18:00:00Z");

        assertEquals(only, SaveStore.newer(only, null));
        assertEquals(only, SaveStore.newer(null, only));
        assertFalse(SaveStore.newer(null, null) != null);
    }

    @Test
    void anUnreadableTimestampLosesToAReadableOne() {
        SaveData broken = run(1, "chapter1", "nem datum");
        SaveData good = run(2, "chapter2", "2026-10-17T18:00:00Z");

        assertEquals(good, SaveStore.newer(broken, good));
        assertEquals(good, SaveStore.newer(good, broken));
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=SaveStoreTest`
Expected: FAIL — `cannot find symbol: class SaveStore`

- [ ] **Step 3: Write the implementation**

`src/main/java/com/example/demo/SaveStore.java`:

```java
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
 * survives a crash and a plane trip. The cloud half arrives later and is best
 * effort on top of this.
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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -o test -Dtest=SaveStoreTest`
Expected: PASS — `Tests run: 8, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/demo/SaveStore.java src/test/java/com/example/demo/SaveStoreTest.java
git commit -m "Keep the run in a file on this machine"
```

---

### Task 4: A napló

**Files:**
- Modify: `src/main/java/com/example/demo/Chatlog.java` (a Task 2-ben készült rekord mellé a logika)
- Test: `src/test/java/com/example/demo/ChatlogTest.java`

**Interfaces:**
- Consumes: `Chatlog.Entry` (Task 2)
- Produces:
  - `new Chatlog()`
  - `Chatlog.line(int chapter, String speaker, String text)` → `void`
  - `Chatlog.choice(int chapter, String question, String answer, int xp, int lebukas)` → `void`
  - `Chatlog.entries()` → `List<Chatlog.Entry>` (módosíthatatlan másolat, a legrégebbi az első)
  - `Chatlog.load(List<Chatlog.Entry>)` → `void`
  - `Chatlog.clear()` → `void`
  - `Chatlog.MAX` = 300

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/demo/ChatlogTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChatlogTest {

    @Test
    void aLineKeepsItsSpeakerAndChapter() {
        Chatlog log = new Chatlog();
        log.line(2, "LAKATOS", "Üdv a klubban.");

        Chatlog.Entry e = log.entries().get(0);
        assertEquals("line", e.kind());
        assertEquals("LAKATOS", e.speaker());
        assertEquals("Üdv a klubban.", e.text());
        assertEquals(2, e.chapter());
        assertNull(e.xp());
    }

    @Test
    void aChoiceKeepsWhatItCost() {
        Chatlog log = new Chatlog();
        log.choice(1, "Mit válaszolsz?", "Elfogadom", 15, 10);

        Chatlog.Entry e = log.entries().get(0);
        assertEquals("choice", e.kind());
        assertEquals("Mit válaszolsz?", e.speaker());
        assertEquals("Elfogadom", e.text());
        assertEquals(15, e.xp());
        assertEquals(10, e.lebukas());
    }

    @Test
    void theLogStopsAtItsLimitAndKeepsTheLatest() {
        Chatlog log = new Chatlog();

        for (int i = 0; i < Chatlog.MAX + 50; i++) {
            log.line(1, "", "sor " + i);
        }

        List<Chatlog.Entry> entries = log.entries();
        assertEquals(Chatlog.MAX, entries.size());
        assertEquals("sor 50", entries.get(0).text());
        assertEquals("sor " + (Chatlog.MAX + 49), entries.get(entries.size() - 1).text());
    }

    @Test
    void aLoadedLogReplacesWhatWasThere() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");

        log.load(List.of(new Chatlog.Entry("line", "", "mentett", null, null, 3)));

        assertEquals(1, log.entries().size());
        assertEquals("mentett", log.entries().get(0).text());
    }

    @Test
    void loadingNullLeavesAnEmptyLog() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");
        log.load(null);
        assertEquals(List.of(), log.entries());
    }

    @Test
    void aNewGameClearsTheLog() {
        Chatlog log = new Chatlog();
        log.line(1, "", "régi");
        log.clear();
        assertEquals(List.of(), log.entries());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=ChatlogTest`
Expected: FAIL — `cannot find symbol: method line(int,String,String)`

- [ ] **Step 3: Write the implementation**

A `Chatlog.java`-ban a `record Entry` **alá**, az osztályon belül:

```java
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
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -o test -Dtest=ChatlogTest`
Expected: PASS — `Tests run: 6, Failures: 0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/example/demo/Chatlog.java src/test/java/com/example/demo/ChatlogTest.java
git commit -m "Remember every line the run has heard"
```

---

### Task 5: A játék tudja, hol van, és oda vissza tud térni

Ez a terv szíve. Minden jelenetmetódus első sora ma `ui.clearChoices();` — ez cserélődik `step();`-re, ami ugyanazt teszi, plusz `StackWalker`-rel megjegyzi a hívó metódus nevét. A paraméteres jelenetek kiesnek a szűrőn (`parameterCount() == 0`), így a visszatérési pont az előző paraméter nélküli jelenet.

**Files:**
- Modify: `src/main/java/com/example/demo/GameController.java`
- Test: `src/test/java/com/example/demo/SceneNamesTest.java`

**Interfaces:**
- Consumes: `SaveData`, `SaveStore`, `Chatlog`, `Item`, `AppDirs`
- Produces:
  - `GameController.CHAPTER_ENTRY` → `java.util.Map<Integer, String>` (package-private statikus: fejezetszám → jelenetmetódus neve)
  - `GameController.snapshot()` → `SaveData` (package-private, a mostani futás)
  - `GameController.resume(SaveData)` → `void` (publikus: a címképernyő `LOAD GAME`-je hívja)
  - `GameController.chatlog()` → `Chatlog` (a `GameUI` overlay-e ebből olvas)
  - `GameController.saveNow(boolean toCloud)` → `void` (a pause menü `MENTÉS`-e hívja)

- [ ] **Step 1: Write the failing test**

A reflexiós visszatöltés akkor törik el, ha valaki átnevez egy jelenetmetódust. Ez a teszt fogja el, és nem kell hozzá JavaFX:

`src/test/java/com/example/demo/SceneNamesTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * resume() calls a scene by name, through reflection. A renamed scene method
 * would turn every save pointing at it into a silent fall back to the start of
 * the chapter - so the names the game relies on are checked here.
 */
class SceneNamesTest {

    @Test
    void everyChapterHasAnEntryScene() {
        assertEquals(8, GameController.CHAPTER_ENTRY.size());

        for (Map.Entry<Integer, String> e : GameController.CHAPTER_ENTRY.entrySet()) {
            Method m = assertDoesNotThrow(
                    () -> GameController.class.getDeclaredMethod(e.getValue()),
                    "nincs ilyen jelenetmetódus: " + e.getValue() + " (" + e.getKey() + ". fejezet)");
            assertEquals(0, m.getParameterCount(), e.getValue() + " nem lehet paraméteres");
        }
    }

    @Test
    void theBossFightsCanBeReturnedTo() {
        for (String name : new String[] {"startLakatosBoss", "startPeteriBoss", "startBoss3"}) {
            Method m = assertDoesNotThrow(() -> GameController.class.getDeclaredMethod(name), name);
            assertEquals(0, m.getParameterCount(), name);
        }
    }

    @Test
    void theChoiceScenesAreTheOnesLeftOutOfTheSave() {
        for (String name : new String[] {"chapter1Choice", "bossMove", "peteriBossMove", "boss3Move"}) {
            boolean found = false;
            for (Method m : GameController.class.getDeclaredMethods()) {
                if (m.getName().equals(name)) {
                    found = true;
                    assertTrue(m.getParameterCount() > 0,
                            name + " paraméter nélkülivé vált - így bekerülne a mentésbe, "
                                    + "pedig egy választás közepe nem visszatérési pont");
                }
            }
            assertTrue(found, "nincs ilyen metódus: " + name);
        }
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=SceneNamesTest`
Expected: FAIL — `cannot find symbol: variable CHAPTER_ENTRY`

- [ ] **Step 3: Add the state the save needs to GameController**

A `GameController` mezői közé (a `peteriPassiveRounds` alá, a 37. sor után):

```java
    // ==================================================
    // SAVE
    // ==================================================

    /** Where each chapter starts, for a save that has no scene of its own - a run from the website. */
    static final java.util.Map<Integer, String> CHAPTER_ENTRY = java.util.Map.of(
            0, "showPrologue",
            1, "chapter1",
            2, "chapter2",
            3, "chapter3",
            4, "chapter4",
            5, "chapter5",
            6, "chapter6",
            7, "chapter7");

    private final Chatlog chatlog = new Chatlog();

    private final SaveStore saves = new SaveStore(AppDirs.data().resolve("save-slot1.json"));

    private final java.util.List<SaveData.Decision> history = new java.util.ArrayList<>();

    /** The scene we are in, as its method name. Written by step(). */
    private String currentScene = "showStartScreen";

    private int currentChapter = 0;

    /** Off while resume() is putting a save back, so replaying does not overwrite it. */
    private boolean saving = true;
```

- [ ] **Step 4: Add step(), say(), snapshot(), resume() and saveNow()**

A `GameController`-be, a `restartGame()` metódus **elé**:

```java
    /**
     * Clears the choices and remembers which scene we are in.
     *
     * The scene is this method's caller, read off the stack: a registry of
     * eighty-five names would be one more thing to keep in step with the code,
     * and a typo in it would only show up as a broken save.
     *
     * A scene that takes parameters is skipped. chapter1Choice(int) is the
     * middle of a decision, not a place to come back to, and its argument is
     * not in the save - so the last parameterless scene stays the return point.
     */
    private void step() {

        ui.clearChoices();

        StackWalker.getInstance()
                .walk(frames -> frames.skip(1).findFirst())
                .filter(f -> f.getMethodType().parameterCount() == 0)
                .ifPresent(f -> currentScene = f.getMethodName());

        if (saving) {
            saves.saveLocal(snapshot());
        }
    }

    /** Shows a line and writes it into the log. */
    private void say(String speaker, String text) {
        chatlog.line(currentChapter, speaker, text);
        ui.showDialogue(speaker, text);
    }

    /** The run as it stands. */
    SaveData snapshot() {

        return new SaveData(
                state.getName(),
                state.getName(),
                currentChapter,
                currentScene,
                state.getXp(),
                state.getExposure(),
                state.getLevel(),
                Item.idsOf(state),
                java.util.List.copyOf(history),
                chatlog.entries(),
                new SaveData.Fight(
                        state.getPlayerHp(), state.getBossHp(),
                        previousMove, sameMove, peteriPassiveRounds),
                java.time.Instant.now().toString());
    }

    /**
     * Puts a saved run back and reopens its scene.
     *
     * A save from the website has no scene of its own, so the chapter's first
     * scene is where it picks up. An unknown scene name - a method renamed
     * since the save was written - does the same rather than failing.
     */
    public void resume(SaveData data) {

        ui.updateLanguage();

        state = new GameState();
        state.setName(data.playerName() == null ? "" : data.playerName());
        state.addXp(data.xp());
        state.addExposure(data.lebukas());
        Item.apply(data.items(), state);

        history.clear();

        if (data.history() != null) {
            history.addAll(data.history());
        }

        chatlog.load(data.log());

        chapterDecisions.clear();

        if (data.fight() != null) {
            state.setPlayerHp(data.fight().playerHp());
            state.setBossHp(data.fight().bossHp());
            previousMove = data.fight().previousMove();
            sameMove = data.fight().sameMove();
            peteriPassiveRounds = data.fight().peteriPassiveRounds();
        }

        currentChapter = data.chapter();

        ui.resetForNewGame();
        ui.updateStats(state);
        ui.hideRestartButton();

        String scene = data.scene() == null || data.scene().isBlank()
                ? CHAPTER_ENTRY.getOrDefault(data.chapter(), "showStartScreen")
                : data.scene();

        // The replayed scene writes its own save; hold that off until it is on
        // screen, so a restore cannot overwrite the save it came from.
        saving = false;

        try {
            runScene(scene);
        } catch (ReflectiveOperationException e) {
            System.err.println("resume: nincs ilyen jelenet: " + scene);

            try {
                runScene(CHAPTER_ENTRY.getOrDefault(data.chapter(), "showStartScreen"));
            } catch (ReflectiveOperationException fallback) {
                showStartScreen();
            }

        } finally {
            saving = true;
        }
    }

    private void runScene(String name) throws ReflectiveOperationException {
        GameController.class.getDeclaredMethod(name).invoke(this);
    }

    /** The pause menu's SAVE, and every chapter boundary. */
    public void saveNow(boolean toCloud) {
        saves.saveLocal(snapshot());
    }

    /** The log the napló overlay reads. */
    public Chatlog chatlog() {
        return chatlog;
    }
```

- [ ] **Step 5: Swap every scene's first line over to step()**

Egyetlen mechanikus csere a `GameController.java`-ban. Minden `ui.clearChoices();` hívás `step();`-re vált — a jelenetmetódusok első sora mind ilyen, és a `step()` maga hívja a `clearChoices()`-t:

```bash
cd /Users/zsomborkukucska/IdeaProjects/fityeszthegame
sed -i '' 's/^\( *\)ui\.clearChoices();$/\1step();/' src/main/java/com/example/demo/GameController.java
grep -c "step();" src/main/java/com/example/demo/GameController.java
grep -n "ui.clearChoices();" src/main/java/com/example/demo/GameController.java
```

Expected: a `step();` száma 60 felett, és a `grep -n "ui.clearChoices();"` **csak** a `step()` metóduson belüli egyetlen hívást adja vissza. Ha mást is talál (pl. `chapterSummary()` lambdájában), azt hagyd `ui.clearChoices()`-nak: az nem jelenetmetódus, hanem egy `Runnable` törzse, aminek a `StackWalker` a lambdát adná névnek.

- [ ] **Step 6: Point the dialogue at say(), and make the chapter number known**

```bash
sed -i '' 's/^\( *\)ui\.showDialogue($/\1say(/' src/main/java/com/example/demo/GameController.java
grep -n "ui.showDialogue(" src/main/java/com/example/demo/GameController.java
```

Expected: a `grep` üres, vagy csak olyan hívást hoz, ami nem jelenetszöveg. A `say(` hívások paraméterei változatlanok (beszélő, szöveg).

Majd a `setChapterHeader` metódus törzsének **első sora** (a `GameController.java:122` körüli metódus):

```java
        currentChapter = chapter;
```

- [ ] **Step 7: Give recordDecision the option index**

A `recordDecision` szignatúrája kap egy `int choice` paramétert (1 alapú, ahogy a hívási helyeken a választás számozva van), és a `history`-t meg a naplót is írja:

```java
    private void recordDecision(String questionKey, int choice, String answerText,
                                int xpDelta, int exposureDelta) {

        StringBuilder points = new StringBuilder();

        if (xpDelta != 0) {
            points.append(xpDelta > 0 ? "+" : "").append(xpDelta).append(" XP");
        }

        if (exposureDelta != 0) {
            if (points.length() > 0) points.append(" · ");
            points.append(exposureDelta > 0 ? "+" : "").append(exposureDelta);
        }

        chapterDecisions.add(
                new GameUI.SummaryDecision(Lang.t(questionKey), answerText, points.toString())
        );

        // The website keys a decision by the question's own key and a zero-based
        // option index (lib/story/types.ts, Decision) - the same vocabulary, so a
        // run started there can be read here and the other way round.
        history.add(new SaveData.Decision(questionKey, Math.max(0, choice - 1)));

        chatlog.choice(currentChapter, Lang.t(questionKey), answerText, xpDelta, exposureDelta);
    }
```

A tizenegy hívási hely mind megkapja a `choice`-ot. A nyolc fejezetválasztásnál ott van a `choice` változó, a három boss-győzelemnél (`sum.boss`) `1`:

```bash
cd /Users/zsomborkukucska/IdeaProjects/fityeszthegame
sed -i '' 's/recordDecision("\(ch[0-9]*\.q[0-9]*\)", Lang\.t(/recordDecision("\1", choice, Lang.t(/' src/main/java/com/example/demo/GameController.java
sed -i '' 's/recordDecision("sum\.boss", Lang\.t(/recordDecision("sum.boss", 1, Lang.t(/' src/main/java/com/example/demo/GameController.java
grep -n "recordDecision(" src/main/java/com/example/demo/GameController.java
```

Expected: mind a tizenegy hívás négy helyett öt argumentumot ad át, és a második az index.

- [ ] **Step 8: Clear the log and the history on a new game**

A `restartGame()` törzsébe, a `chapterDecisions.clear();` alá:

```java
        history.clear();
        chatlog.clear();
        currentChapter = 0;
        saving = true;
```

- [ ] **Step 9: Run the tests and compile**

Run: `./mvnw -o test`
Expected: PASS — `SceneNamesTest` 3 teszt, és az előző taskok tesztjei is futnak, összesen 30 körül. Fordítási hiba nincs.

- [ ] **Step 10: Play a round and watch the save file**

Run: `./mvnw -o javafx:run`

Játssz végig a prológust és az 1. fejezet első pár kattintását, majd:

```bash
cat "$HOME/Library/Application Support/FityeszKronika/save-slot1.json" | python3 -m json.tool | head -40
```

Expected: a `scene` a legutóbbi jelenet metódusneve (pl. `lipotiOffer`), a `chapter` 1, a `history` tartalmazza a `ch1.q1`-et a választott indexszel, a `log` pedig az elhangzott sorokat.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/example/demo/GameController.java src/test/java/com/example/demo/SceneNamesTest.java
git commit -m "Remember which scene the run is in, and go back to it"
```

---

### Task 6: Fiók a weboldal Supabase projektjéhez

A hálózat teljes felülete egy háromsoros interfész, így a kérések alakja szerver nélkül ellenőrizhető. A végpontok azok, amiket a weboldal is használ — szerveroldali változás nem kell hozzá.

**Files:**
- Create: `src/main/java/com/example/demo/Transport.java`
- Create: `src/main/java/com/example/demo/HttpTransport.java`
- Create: `src/main/java/com/example/demo/Supabase.java`
- Create: `src/main/java/com/example/demo/Account.java`
- Modify: `.gitignore`
- Test: `src/test/java/com/example/demo/AccountTest.java`

**Interfaces:**
- Consumes: `Json.mapper()`, `AppDirs.data()`
- Produces:
  - `Transport.send(String method, String url, Map<String,String> headers, String body)` → `Transport.Res(int status, String body)`
  - `Supabase.configured()` → `java.util.Optional<Supabase>` (env, majd `supabase.properties`; üres, ha egyik sincs)
  - `new Supabase(String url, String anonKey, Transport transport)`
  - `Supabase.call(String method, String path, String body, String token)` → `Transport.Res`
  - `Account.Session(String accessToken, String refreshToken, String userId, String displayName)`
  - `new Account(Supabase sb, Path sessionFile)`
  - `Account.signIn(String identifier, String password)` → `Session` (dob `Account.AuthException`)
  - `Account.signUp(String email, String username, String password)` → `java.util.Optional<Session>` (üres = email-megerősítésre vár)
  - `Account.restore()` → `Optional<Session>`
  - `Account.session()` → `Optional<Session>`
  - `Account.signOut()` → `void`

- [ ] **Step 1: Write the failing test**

`src/test/java/com/example/demo/AccountTest.java`:

```java
package com.example.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountTest {

    /** What was asked, and what we answer - no network in sight. */
    private static final class FakeTransport implements Transport {

        record Call(String method, String url, Map<String, String> headers, String body) { }

        final List<Call> calls = new ArrayList<>();
        final List<Res> answers = new ArrayList<>();

        @Override
        public Res send(String method, String url, Map<String, String> headers, String body) {
            calls.add(new Call(method, url, headers, body));
            return answers.isEmpty() ? new Res(500, "") : answers.remove(0);
        }
    }

    private static final String TOKENS =
            "{\"access_token\":\"acc1\",\"refresh_token\":\"ref1\"}";

    private static final String USER =
            "{\"id\":\"u-1\",\"user_metadata\":{\"display_name\":\"Zsombi\"}}";

    @Test
    void signingInGoesThroughTheEdgeFunction(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.Session s = account.signIn("zsombi", "titkos123");

        assertEquals("acc1", s.accessToken());
        assertEquals("u-1", s.userId());
        assertEquals("Zsombi", s.displayName());

        FakeTransport.Call signIn = t.calls.get(0);
        assertEquals("POST", signIn.method());
        assertEquals("https://p.supabase.co/functions/v1/sign-in", signIn.url());
        assertEquals("anon-key", signIn.headers().get("apikey"));
        assertTrue(signIn.body().contains("\"identifier\":\"zsombi\""));
        assertTrue(signIn.body().contains("\"password\":\"titkos123\""));

        FakeTransport.Call who = t.calls.get(1);
        assertEquals("GET", who.method());
        assertEquals("https://p.supabase.co/auth/v1/user", who.url());
        assertEquals("Bearer acc1", who.headers().get("Authorization"));
    }

    @Test
    void theSessionOutlivesTheProcess(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Path file = dir.resolve("session.json");
        new Account(new Supabase("https://p.supabase.co", "anon-key", t), file)
                .signIn("zsombi", "titkos123");

        assertTrue(Files.isRegularFile(file));

        Account again = new Account(new Supabase("https://p.supabase.co", "anon-key", new FakeTransport()), file);
        assertEquals("Zsombi", again.restore().orElseThrow().displayName());
    }

    @Test
    void aRefusedSignInSaysNothingAboutWhy(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(400, "{\"error\":\"invalid_credentials\"}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        Account.AuthException e = assertThrows(Account.AuthException.class,
                () -> account.signIn("zsombi", "rossz"));

        assertEquals("ui.authFailed", e.messageKey());
    }

    @Test
    void registeringCarriesTheUsernameAsMetadata(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "{\"id\":\"u-2\",\"email\":\"a@b.hu\"}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertTrue(account.signUp("a@b.hu", "zsombi", "titkos123").isEmpty());

        FakeTransport.Call signUp = t.calls.get(0);
        assertEquals("https://p.supabase.co/auth/v1/signup", signUp.url());
        assertTrue(signUp.body().contains("\"display_name\":\"zsombi\""),
                "a display_name nélkül a profiles trigger nem ír sort");
    }

    @Test
    void registeringThatReturnsASessionSignsStraightIn(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200,
                "{\"access_token\":\"acc1\",\"refresh_token\":\"ref1\","
                        + "\"user\":{\"id\":\"u-2\",\"user_metadata\":{\"display_name\":\"zsombi\"}}}"));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertEquals("zsombi", account.signUp("a@b.hu", "zsombi", "titkos123").orElseThrow().displayName());
    }

    @Test
    void signingOutForgetsTheSession(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(200, USER));

        Path file = dir.resolve("session.json");
        Account account = new Account(new Supabase("https://p.supabase.co", "anon-key", t), file);
        account.signIn("zsombi", "titkos123");

        account.signOut();

        assertFalse(Files.exists(file));
        assertTrue(account.session().isEmpty());
    }

    @Test
    void anExpiredTokenIsRefreshedOnce(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, TOKENS));
        t.answers.add(new Transport.Res(401, "{\"error\":\"expired\"}"));
        t.answers.add(new Transport.Res(200, "{\"access_token\":\"acc2\",\"refresh_token\":\"ref2\"}"));
        t.answers.add(new Transport.Res(200, USER));

        Account account = new Account(
                new Supabase("https://p.supabase.co", "anon-key", t), dir.resolve("session.json"));

        assertEquals("acc2", account.signIn("zsombi", "titkos123").accessToken());

        assertEquals("https://p.supabase.co/auth/v1/token?grant_type=refresh_token",
                t.calls.get(2).url());
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=AccountTest`
Expected: FAIL — `cannot find symbol: class Transport`

- [ ] **Step 3: Write Transport and HttpTransport**

`src/main/java/com/example/demo/Transport.java`:

```java
package com.example.demo;

import java.io.IOException;
import java.util.Map;

/**
 * The whole of the network, in one method.
 *
 * Everything above this talks in requests and answers, so a test can hand the
 * account layer canned replies instead of standing up a server.
 */
public interface Transport {

    record Res(int status, String body) {

        public boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    Res send(String method, String url, Map<String, String> headers, String body) throws IOException;
}
```

`src/main/java/com/example/demo/HttpTransport.java`:

```java
package com.example.demo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** The real network: one client, short timeouts, no retries of its own. */
public final class HttpTransport implements Transport {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    @Override
    public Res send(String method, String url, Map<String, String> headers, String body) throws IOException {

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT);

        headers.forEach(b::header);

        b.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body));

        try {
            HttpResponse<String> res = client.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return new Res(res.statusCode(), res.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }
}
```

- [ ] **Step 4: Write Supabase**

`src/main/java/com/example/demo/Supabase.java`:

```java
package com.example.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

/**
 * The website's Supabase project, as this game talks to it.
 *
 * The anon key is public by design - the website hands it to every visitor,
 * and row-level security is what protects the data. It still does not belong
 * in git history, so it is read from the environment or from a properties
 * resource that .gitignore keeps out.
 */
public final class Supabase {

    private static final String RESOURCE = "/com/example/demo/supabase.properties";

    private final String url;
    private final String anonKey;
    private final Transport transport;

    public Supabase(String url, String anonKey, Transport transport) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.anonKey = anonKey;
        this.transport = transport;
    }

    /** The configured project, or nothing - in which case the game stays guest only. */
    public static Optional<Supabase> configured() {

        String url = System.getenv("FITYESZ_SUPABASE_URL");
        String key = System.getenv("FITYESZ_SUPABASE_ANON_KEY");

        if (url == null || url.isBlank() || key == null || key.isBlank()) {

            Properties p = new Properties();

            try (InputStream in = Supabase.class.getResourceAsStream(RESOURCE)) {
                if (in == null) {
                    return Optional.empty();
                }
                p.load(in);
            } catch (IOException e) {
                return Optional.empty();
            }

            url = p.getProperty("url", "");
            key = p.getProperty("anonKey", "");
        }

        return url.isBlank() || key.isBlank()
                ? Optional.empty()
                : Optional.of(new Supabase(url, key, new HttpTransport()));
    }

    /**
     * One call against the project. The path starts with a slash; a token, when
     * given, is the signed-in player's.
     */
    public Transport.Res call(String method, String path, String body, String token) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Content-Type", "application/json");

        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }

        return transport.send(method, url + path, headers, body);
    }
}
```

- [ ] **Step 5: Write Account**

`src/main/java/com/example/demo/Account.java`:

```java
package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;

/**
 * Who is playing, against the website's accounts.
 *
 * Signing in goes through the sign-in Edge Function, the same way the website
 * does it: Supabase authenticates on an e-mail address, and resolving a
 * username to one in the client would make every username enumerable. The
 * function answers invalid_credentials to everything that fails, and so does
 * this class - one message, nothing learned.
 */
public final class Account {

    /** A refusal the player is allowed to see, as a Lang key. */
    public static final class AuthException extends Exception {

        private final String messageKey;

        AuthException(String messageKey) {
            super(messageKey);
            this.messageKey = messageKey;
        }

        public String messageKey() {
            return messageKey;
        }
    }

    public record Session(String accessToken, String refreshToken, String userId, String displayName) { }

    private final Supabase sb;
    private final Path sessionFile;

    private Session session;

    public Account(Supabase sb, Path sessionFile) {
        this.sb = sb;
        this.sessionFile = sessionFile;
    }

    public Optional<Session> session() {
        return Optional.ofNullable(session);
    }

    /** The session this machine remembers from last time. */
    public Optional<Session> restore() {

        if (session != null) {
            return Optional.of(session);
        }

        if (!Files.isRegularFile(sessionFile)) {
            return Optional.empty();
        }

        try {
            session = Json.mapper().readValue(Files.readString(sessionFile), Session.class);
            return Optional.ofNullable(session);

        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Session signIn(String identifier, String password) throws AuthException {

        String body = write(new SignInBody(identifier.trim(), password));

        Transport.Res res = post("/functions/v1/sign-in", body, null);

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode tokens = read(res.body());

        return finish(tokens.path("access_token").asText(), tokens.path("refresh_token").asText());
    }

    /**
     * Registers an account. An empty answer means Supabase wants the address
     * confirmed first - the sign-up worked, the session comes after the click.
     */
    public Optional<Session> signUp(String email, String username, String password) throws AuthException {

        String body = write(new SignUpBody(email.trim(), password, new Meta(username.trim())));

        Transport.Res res = post("/auth/v1/signup", body, null);

        if (!res.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode node = read(res.body());

        if (node.hasNonNull("access_token")) {
            return Optional.of(finish(
                    node.path("access_token").asText(), node.path("refresh_token").asText()));
        }

        return Optional.empty();
    }

    public void signOut() {

        session = null;

        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }
    }

    /**
     * Asks who the token belongs to, refreshing it once if it has expired, and
     * stores the session.
     */
    private Session finish(String accessToken, String refreshToken) throws AuthException {

        Transport.Res who = post("/auth/v1/user", null, accessToken, "GET");

        if (who.status() == 401) {

            Transport.Res refreshed = post("/auth/v1/token?grant_type=refresh_token",
                    "{\"refresh_token\":\"" + refreshToken + "\"}", null);

            if (!refreshed.ok()) {
                throw new AuthException("ui.authFailed");
            }

            JsonNode tokens = read(refreshed.body());
            accessToken = tokens.path("access_token").asText();
            refreshToken = tokens.path("refresh_token").asText();

            who = post("/auth/v1/user", null, accessToken, "GET");
        }

        if (!who.ok()) {
            throw new AuthException("ui.authFailed");
        }

        JsonNode user = read(who.body());

        session = new Session(
                accessToken,
                refreshToken,
                user.path("id").asText(),
                user.path("user_metadata").path("display_name").asText());

        store(session);

        return session;
    }

    private void store(Session s) {

        try {
            Path parent = sessionFile.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(sessionFile, Json.mapper().writeValueAsString(s));

            try {
                Files.setPosixFilePermissions(sessionFile, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows has no POSIX permissions; the file stays as it is.
            }

        } catch (IOException e) {
            System.err.println("session: " + e.getMessage());
        }
    }

    private Transport.Res post(String path, String body, String token) throws AuthException {
        return post(path, body, token, "POST");
    }

    private Transport.Res post(String path, String body, String token, String method) throws AuthException {
        try {
            return sb.call(method, path, body, token);
        } catch (IOException e) {
            throw new AuthException("ui.authOffline");
        }
    }

    private static String write(Object value) throws AuthException {
        try {
            return Json.mapper().writeValueAsString(value);
        } catch (IOException e) {
            throw new AuthException("ui.authFailed");
        }
    }

    private static JsonNode read(String body) throws AuthException {
        try {
            return Json.mapper().readTree(body);
        } catch (IOException e) {
            throw new AuthException("ui.authFailed");
        }
    }

    private record SignInBody(String identifier, String password) { }

    private record SignUpBody(String email, String password, Meta data) { }

    private record Meta(String display_name) { }
}
```

- [ ] **Step 6: Keep the key out of git**

```bash
cd /Users/zsomborkukucska/IdeaProjects/fityeszthegame
printf '\n### Supabase keys, generated from the website project\nsrc/main/resources/com/example/demo/supabase.properties\n' >> .gitignore
```

- [ ] **Step 7: Generate the properties from the website project**

```bash
cd /Users/zsomborkukucska/IdeaProjects/fityeszthegame
grep -E '^NEXT_PUBLIC_SUPABASE_(URL|ANON_KEY)=' ~/WebstormProjects/fityeszTheGameWebsite/.env.local \
  | sed -e 's/^NEXT_PUBLIC_SUPABASE_URL=/url=/' -e 's/^NEXT_PUBLIC_SUPABASE_ANON_KEY=/anonKey=/' \
  > src/main/resources/com/example/demo/supabase.properties
wc -l src/main/resources/com/example/demo/supabase.properties
git status --short src/main/resources/com/example/demo/supabase.properties
```

Expected: két sor a fájlban (`url=…`, `anonKey=…`), és a `git status` **semmit** nem mutat rá — a gitignore fogja.

- [ ] **Step 8: Run the test to verify it passes**

Run: `./mvnw -o test -Dtest=AccountTest`
Expected: PASS — `Tests run: 7, Failures: 0`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/example/demo/Transport.java src/main/java/com/example/demo/HttpTransport.java \
        src/main/java/com/example/demo/Supabase.java src/main/java/com/example/demo/Account.java \
        .gitignore src/test/java/com/example/demo/AccountTest.java
git commit -m "Sign in to the website's accounts from the desktop game"
```

---

### Task 7: A mentés felhős fele

**Files:**
- Modify: `src/main/java/com/example/demo/SaveStore.java`
- Modify: `src/test/java/com/example/demo/SaveStoreTest.java`

**Interfaces:**
- Consumes: `Supabase.call(...)`, `Account.Session`, `SaveData`
- Produces:
  - `new SaveStore(Path file, Supabase sb)` (a `sb` lehet `null`)
  - `SaveStore.pushCloud(SaveData data, String userId, String token)` → `void` (szinkron; a hívó dönt a szálról)
  - `SaveStore.fetchCloud(String token)` → `Optional<SaveData>`
  - `SaveStore.rowOf(SaveData data, String userId)` → `String` (az upsert törzse; package-private, hogy tesztelhető legyen)

- [ ] **Step 1: Write the failing test**

A `SaveStoreTest`-be, az osztály végére:

```java
    /** A transport that records what was asked and answers from a list. */
    private static final class FakeTransport implements Transport {

        record Call(String method, String url, java.util.Map<String, String> headers, String body) { }

        final List<Call> calls = new java.util.ArrayList<>();
        final List<Res> answers = new java.util.ArrayList<>();

        @Override
        public Res send(String method, String url, java.util.Map<String, String> headers, String body) {
            calls.add(new Call(method, url, headers, body));
            return answers.isEmpty() ? new Res(500, "") : answers.remove(0);
        }
    }

    @Test
    void theUpsertCarriesTheRowsOwnColumns(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(201, ""));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        store.pushCloud(run(3, "lakatosRules", "2026-10-17T18:00:00Z"), "u-1", "acc1");

        FakeTransport.Call call = t.calls.get(0);
        assertEquals("POST", call.method());
        assertEquals("https://p.supabase.co/rest/v1/saves?on_conflict=user_id,slot", call.url());
        assertEquals("Bearer acc1", call.headers().get("Authorization"));
        assertEquals("resolution=merge-duplicates,return=minimal", call.headers().get("Prefer"));
        assertTrue(call.body().startsWith("["), "a PostgREST upsert egy tömböt kap");
        assertTrue(call.body().contains("\"user_id\":\"u-1\""), "user_id nélkül az RLS elutasítja");
        assertTrue(call.body().contains("\"slot\":1"));
        assertTrue(call.body().contains("\"scene\":\"lakatosRules\""));
    }

    @Test
    void theCloudRunComesBackFromTheFirstRow(@TempDir Path dir) throws Exception {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200,
                "[{\"player_name\":\"Zsombi\",\"chapter\":4,\"xp\":80,\"lebukas\":20,\"szint\":2,"
                        + "\"items\":[\"envelope1\"],\"history\":[],\"log\":[],"
                        + "\"updated_at\":\"2026-10-17T19:00:00Z\"}]"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        SaveData d = store.fetchCloud("acc1").orElseThrow();

        assertEquals(4, d.chapter());
        assertEquals("Zsombi", d.playerName());
        assertTrue(t.calls.get(0).url().contains("slot=eq.1"));
        assertEquals("GET", t.calls.get(0).method());
    }

    @Test
    void anEmptyCloudAnswerIsNoSave(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(200, "[]"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aFailingCloudDoesNotTakeTheGameDown(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();
        t.answers.add(new Transport.Res(503, "down"));

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        assertTrue(store.fetchCloud("acc1").isEmpty());
        store.pushCloud(run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
    }

    @Test
    void withoutAProjectNothingIsPushed(@TempDir Path dir) {
        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"), null);
        store.pushCloud(run(1, "chapter1", "2026-10-17T18:00:00Z"), "u-1", "acc1");
        assertTrue(store.fetchCloud("acc1").isEmpty());
    }

    @Test
    void aRunWithNoDecisionsNeverOverwritesTheAccount(@TempDir Path dir) {
        FakeTransport t = new FakeTransport();

        SaveStore store = new SaveStore(dir.resolve("save-slot1.json"),
                new Supabase("https://p.supabase.co", "anon-key", t));

        SaveData blank = new SaveData("Zsombi", "Zsombi", 1, "chapter1", 0, 0, 1,
                List.of(), List.of(), List.of(), null, "2026-10-17T18:00:00Z");

        store.pushCloud(blank, "u-1", "acc1");

        assertEquals(List.of(), t.calls);
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./mvnw -o test -Dtest=SaveStoreTest`
Expected: FAIL — `constructor SaveStore cannot be applied to given types`

- [ ] **Step 3: Write the implementation**

A `SaveStore`-ban a mező és a konstruktor:

```java
    private final Path file;
    private final Supabase sb;

    public SaveStore(Path file) {
        this(file, null);
    }

    public SaveStore(Path file, Supabase sb) {
        this.file = file;
        this.sb = sb;
    }
```

És az osztály végére:

```java
    private static final String COLUMNS =
            "player_name,display_name,chapter,scene,xp,lebukas,szint,items,history,log,fight,updated_at";

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
            java.util.Map<String, String> extra = new java.util.HashMap<>();
            extra.put("Prefer", "resolution=merge-duplicates,return=minimal");

            Transport.Res res = sb.call("POST", "/rest/v1/saves?on_conflict=user_id,slot",
                    rowOf(data, userId), token, extra);

            if (!res.ok()) {
                System.err.println("save: felhő " + res.status() + " " + res.body());
            }

        } catch (IOException e) {
            System.err.println("save: " + e.getMessage());
        }
    }

    /** The account's run, if there is one. */
    public java.util.Optional<SaveData> fetchCloud(String token) {

        if (sb == null || token == null) {
            return Optional.empty();
        }

        try {
            Transport.Res res = sb.call("GET",
                    "/rest/v1/saves?slot=eq.1&select=" + COLUMNS, null, token);

            if (!res.ok()) {
                System.err.println("save: felhő " + res.status());
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

        com.fasterxml.jackson.databind.node.ObjectNode row =
                Json.mapper().valueToTree(data);

        row.put("user_id", userId);
        row.put("slot", 1);

        return Json.mapper().writeValueAsString(
                Json.mapper().createArrayNode().add(row));
    }
```

A `Supabase`-nek kell egy ötparaméteres `call` a `Prefer` fejléchez — a `Supabase.java`-ban a meglévő `call` mellé:

```java
    public Transport.Res call(String method, String path, String body, String token) throws IOException {
        return call(method, path, body, token, Map.of());
    }

    public Transport.Res call(String method, String path, String body, String token,
                              Map<String, String> extra) throws IOException {

        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", anonKey);
        headers.put("Content-Type", "application/json");

        if (token != null) {
            headers.put("Authorization", "Bearer " + token);
        }

        headers.putAll(extra);

        return transport.send(method, url + path, headers, body);
    }
```

(A négyparaméteres `call` törzse így a delegálás lesz — a Task 6-ban írt törzs átkerül az ötparaméteresbe.)

- [ ] **Step 4: Run the test to verify it passes**

Run: `./mvnw -o test -Dtest=SaveStoreTest`
Expected: PASS — `Tests run: 14, Failures: 0`

- [ ] **Step 5: Wire the cloud into GameController**

A `GameController`-ben a `saves` mező és a `saveNow` kiegészül. A mező:

```java
    private final Account account = Supabase.configured()
            .map(sb -> new Account(sb, AppDirs.data().resolve("session.json")))
            .orElse(null);

    private final SaveStore saves = new SaveStore(
            AppDirs.data().resolve("save-slot1.json"),
            Supabase.configured().orElse(null));

    private final java.util.concurrent.ExecutorService network =
            java.util.concurrent.Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "fityesz-save");
                t.setDaemon(true);
                return t;
            });
```

És a `saveNow`:

```java
    /** The pause menu's SAVE, and every chapter boundary. */
    public void saveNow(boolean toCloud) {

        SaveData data = snapshot();

        saves.saveLocal(data);

        if (!toCloud || account == null) {
            return;
        }

        account.session().ifPresent(s ->
                network.submit(() -> saves.pushCloud(data, s.userId(), s.accessToken())));
    }

    /** The signed-in player, if there is one. */
    public Account account() {
        return account;
    }
```

A `chapterSummary()`-ben, a `chapterDecisions.clear();` **elé**:

```java
            saveNow(true);
```

- [ ] **Step 6: Compile and run the whole suite**

Run: `./mvnw -o test`
Expected: PASS, minden teszt fut (kb. 37).

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/example/demo/SaveStore.java src/main/java/com/example/demo/Supabase.java \
        src/main/java/com/example/demo/GameController.java src/test/java/com/example/demo/SaveStoreTest.java
git commit -m "Put the run in the player's account too"
```

---

### Task 8: Belépési felület és a címképernyő

Felületi munka, tesztek helyett kézi ellenőrzéssel: a JavaFX jeleneteket ebben a projektben nem teszteljük (nincs TestFX, és nem is veszünk fel újat).

**Files:**
- Create: `src/main/java/com/example/demo/LoginScreen.java`
- Modify: `src/main/java/com/example/demo/OpeningScreen.java`
- Modify: `src/main/java/com/example/demo/HelloApplication.java`
- Modify: `src/main/java/com/example/demo/Lang.java`

**Interfaces:**
- Consumes: `Account`, `Supabase.configured()`, `SaveStore`, `GameController.resume(SaveData)`, `GameController.account()`
- Produces:
  - `new LoginScreen(Account account, Runnable onDone, Runnable onBack)`; `LoginScreen.getRoot()` → `javafx.scene.layout.StackPane`
  - `OpeningScreen` új konstruktora: `new OpeningScreen(Runnable onNewGame, Runnable onExit, Runnable onSignIn, java.util.function.Consumer<SaveData> onContinue, Account account, SaveStore saves)`

- [ ] **Step 1: Add the new Lang keys**

A `Lang.java` `ui.` blokkjába (a `ui.loadGame` köré), a `p(kulcs, magyar, angol)` alakban:

```java
        p("ui.signIn",           "Belépés", "Sign in");
        p("ui.signOut",          "Kilépés a fiókból", "Sign out");
        p("ui.register",         "Regisztráció", "Register");
        p("ui.save",             "Mentés", "Save");
        p("ui.saved",            "Mentve", "Saved");
        p("ui.log",              "Napló", "Log");
        p("ui.username",         "Felhasználónév", "Username");
        p("ui.usernameOrEmail",  "Felhasználónév vagy e-mail", "Username or e-mail");
        p("ui.email",            "E-mail", "E-mail");
        p("ui.password",         "Jelszó", "Password");
        p("ui.authFailed",       "Hibás belépési adatok.", "Those sign-in details are not right.");
        p("ui.authOffline",      "Nincs kapcsolat a kiszolgálóval.", "The server cannot be reached.");
        p("ui.authUnavailable",  "A fiókok ebben a buildben nincsenek beállítva.",
                                 "Accounts are not configured in this build.");
        p("ui.checkEmail",       "Nézd meg a postafiókod, és erősítsd meg az e-mail-címet.",
                                 "Check your inbox and confirm your e-mail address.");
        p("ui.noSave",           "Nincs mentés", "No save");
        p("ui.saveFromLocal",    "ezen a gépen", "on this machine");
        p("ui.saveFromCloud",    "a fiókodból", "from your account");
        p("ui.logEmpty",         "Még nem hangzott el semmi.", "Nothing has been said yet.");
        p("ui.decision",         "Döntés", "Decision");
        p("ui.back",             "Vissza", "Back");
```

- [ ] **Step 2: Write LoginScreen**

`src/main/java/com/example/demo/LoginScreen.java`. A felépítés az `OpeningScreen` mintáit követi: ugyanazok a színkonstansok (`PAGE #F4EFE6`, `INK #161616`, `RED #C8102E`, `GRAY #4A4640`, `RULE #CFC6B6`), az `Antonio-Bold.ttf` a címsorokhoz és a `PublicSans-Regular.ttf` a mezőkhöz (`loadFont`/`family` mintájára), és a menüsorok a `menuButton`/`menuRow` stílusát kapják (52 magas, bal oldali 4px-es piros rúd a kijelölésen).

```java
package com.example.demo;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * Signing in to the website's accounts.
 *
 * Two modes in one screen: sign in with a username or an address, or register
 * with both. Every refusal reads the same, because the Edge Function behind
 * this answers the same to everything that fails - a caller who does not know
 * the password must not learn whether the username exists.
 */
public final class LoginScreen {

    private static final String PAGE = "#F4EFE6";
    private static final String INK  = "#161616";
    private static final String RED  = "#C8102E";
    private static final String GRAY = "#4A4640";
    private static final String RULE = "#CFC6B6";

    private final StackPane root = new StackPane();

    private final Account account;
    private final Runnable onDone;

    private final TextField identifier = new TextField();
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label note = new Label("");

    private boolean registering = false;

    public LoginScreen(Account account, Runnable onDone, Runnable onBack) {

        this.account = account;
        this.onDone = onDone;

        root.setStyle("-fx-background-color: " + PAGE + ";");
        root.setAlignment(Pos.CENTER);

        Label heading = new Label(Lang.t("ui.signIn").toUpperCase());
        heading.setFont(Font.font(family(), 44));
        heading.setTextFill(Color.web(INK));

        note.setFont(Font.font(labelFamily(), 12));
        note.setTextFill(Color.web(RED));
        note.setWrapText(true);
        note.setMaxWidth(380);

        VBox form = new VBox(10,
                heading,
                field(Lang.t("ui.email"), email),
                field(Lang.t("ui.usernameOrEmail"), identifier),
                field(Lang.t("ui.password"), password),
                note,
                action(Lang.t("ui.signIn"), this::submit),
                action(Lang.t("ui.register"), this::toggle),
                action(Lang.t("ui.back"), onBack));

        form.setMaxWidth(420);
        form.setPadding(new Insets(40));

        root.getChildren().add(form);

        showMode();

        password.setOnAction(e -> submit());
        identifier.setOnAction(e -> submit());

        Platform.runLater(identifier::requestFocus);
    }

    public StackPane getRoot() {
        return root;
    }

    /** Registration needs an address as well; signing in does not. */
    private void toggle() {
        registering = !registering;
        note.setText("");
        showMode();
    }

    private void showMode() {

        email.getParent().setVisible(registering);
        email.getParent().setManaged(registering);

        ((Label) ((VBox) identifier.getParent()).getChildren().get(0))
                .setText((registering ? Lang.t("ui.username") : Lang.t("ui.usernameOrEmail")).toUpperCase());
    }

    private void submit() {

        if (account == null) {
            note.setText(Lang.t("ui.authUnavailable"));
            return;
        }

        String id = identifier.getText() == null ? "" : identifier.getText().trim();
        String pass = password.getText() == null ? "" : password.getText();

        if (id.isEmpty() || pass.isEmpty()) {
            note.setText(Lang.t("ui.authFailed"));
            return;
        }

        note.setText("…");

        // Off the JavaFX thread: a slow server must not freeze the screen.
        Thread worker = new Thread(() -> {

            String message = null;
            boolean done = false;

            try {
                if (registering) {
                    done = account.signUp(email.getText().trim(), id, pass).isPresent();
                    message = done ? null : Lang.t("ui.checkEmail");
                } else {
                    account.signIn(id, pass);
                    done = true;
                }

            } catch (Account.AuthException e) {
                message = Lang.t(e.messageKey());
            }

            final String shown = message;
            final boolean ok = done;

            Platform.runLater(() -> {
                note.setText(shown == null ? "" : shown);

                if (ok) {
                    onDone.run();
                }
            });
        }, "fityesz-auth");

        worker.setDaemon(true);
        worker.start();
    }

    private VBox field(String label, TextField input) {

        Label l = new Label(label.toUpperCase());
        l.setFont(Font.font(labelFamily(), 10));
        l.setTextFill(Color.web(GRAY));

        input.setFont(Font.font(labelFamily(), 15));
        input.setPrefHeight(38);
        input.setStyle("-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-border-color: transparent transparent " + RULE + " transparent;"
                + "-fx-border-width: 0 0 1 0;"
                + "-fx-text-fill: " + INK + ";"
                + "-fx-padding: 0;");

        return new VBox(2, l, input);
    }

    private Button action(String text, Runnable run) {

        Button b = new Button(text.toUpperCase());
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(44);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setFont(Font.font(family(), 26));
        b.setFocusTraversable(false);
        b.setOnAction(e -> run.run());

        String base = "-fx-background-color: transparent;"
                + "-fx-background-radius: 0;"
                + "-fx-padding: 0 10 0 16;"
                + "-fx-border-width: 0 0 0 4;"
                + "-fx-cursor: hand;";

        b.setStyle(base + "-fx-border-color: transparent; -fx-text-fill: " + INK + ";");
        b.setOnMouseEntered(e -> b.setStyle(base + "-fx-border-color: " + RED + "; -fx-text-fill: " + RED + ";"));
        b.setOnMouseExited(e -> b.setStyle(base + "-fx-border-color: transparent; -fx-text-fill: " + INK + ";"));

        return b;
    }

    private static String family() {
        return Font.getFamilies().contains("Antonio") ? "Antonio" : "Arial Narrow";
    }

    private static String labelFamily() {
        return Font.getFamilies().contains("Public Sans") ? "Public Sans" : "Arial";
    }
}
```

- [ ] **Step 3: Put sign-in and load game on the title screen**

Az `OpeningScreen` konstruktora két új visszahívást és a fiókot kapja. A meglévő mezők mellé:

```java
    private final Runnable onSignIn;
    private final java.util.function.Consumer<SaveData> onContinue;
    private final Account account;
    private final SaveStore saves;
```

A konstruktor szignatúrája és a két hozzárendelés:

```java
    public OpeningScreen(Runnable onNewGame, Runnable onExit, Runnable onSignIn,
                         java.util.function.Consumer<SaveData> onContinue,
                         Account account, SaveStore saves) {

        this.onNewGame = onNewGame;
        this.onExit = onExit;
        this.onSignIn = onSignIn;
        this.onContinue = onContinue;
        this.account = account;
        this.saves = saves;
```

A menü összeállítása (a mostani `newGame`/`loadGame`/`exit` hármas helyén):

```java
        SaveData save = latestSave();

        String who = account == null
                ? null
                : account.session().map(Account.Session::displayName).orElse(null);

        Button signIn = menuButton(who == null ? Lang.t("ui.signIn") : who, true,
                who == null ? onSignIn : () -> {
                    account.signOut();
                    rebuild();
                });

        Button newGame  = menuButton(Lang.t("ui.newGame"), true, this::startNewGame);
        Button loadGame = menuButton(Lang.t("ui.loadGame"), save != null,
                save == null ? null : () -> onContinue.accept(save));
        Button exit     = menuButton(Lang.t("ui.exit"), true, onExit);

        menu.getChildren().addAll(
                menuRow(signIn, who == null ? null : Lang.t("ui.signOut").toUpperCase()),
                menuRow(newGame, null),
                menuRow(loadGame, save == null
                        ? Lang.t("ui.noSave").toUpperCase()
                        : saveNote(save)),
                menuRow(exit, null));

        nav.add(signIn);
        nav.add(newGame);

        if (save != null) {
            nav.add(loadGame);
        }

        nav.add(exit);
```

És a két segítő metódus az osztály végére:

```java
    /** The run to offer: the fresher of this machine's and the account's. */
    private SaveData latestSave() {

        if (saves == null) {
            return null;
        }

        SaveData local = saves.loadLocal().orElse(null);

        SaveData cloud = account == null
                ? null
                : account.session()
                        .flatMap(s -> saves.fetchCloud(s.accessToken()))
                        .orElse(null);

        return SaveStore.newer(local, cloud);
    }

    /** "3. FEJEZET · A FIÓKODBÓL" - what the LOAD GAME row says on its right. */
    private String saveNote(SaveData save) {

        String chapter = save.chapter() == 0
                ? Lang.t("ui.prologue")
                : (Lang.magyar()
                        ? save.chapter() + ". " + Lang.t("ui.chapter")
                        : Lang.t("ui.chapter") + " " + save.chapter());

        String from = save.scene() == null || save.scene().isBlank()
                ? Lang.t("ui.saveFromCloud")
                : Lang.t("ui.saveFromLocal");

        return (chapter + " · " + from).toUpperCase();
    }
```

A `rebuild()` a nyelvváltáskor már létező újraépítést hívja — a `languageToggle()` ugyanezt teszi (`OpeningScreen.java:439` környéke); ha nincs ilyen nevű metódus, azt a kódot kell kiemelni `rebuild()` néven, és a nyelvváltás is azt hívja.

- [ ] **Step 4: Wire the screens together in HelloApplication**

A `GameController` egyetlen új gettert kap — a `getUI()` már megvan:

```java
    public SaveStore saves() {
        return saves;
    }
```

A `title()` a két új visszahívással:

```java
    /** A fresh title page wired to this shell. */
    private OpeningScreen title() {
        return new OpeningScreen(
                this::enterGame,
                () -> dip(Platform::exit, 420),
                this::showLogin,
                this::enterGame,
                controller.account(),
                controller.saves());
    }
```

(A két `this::enterGame` nem kétértelmű: a `Runnable` paraméterre a paraméter nélküli, a `Consumer<SaveData>`-ra az egyargumentumú illeszkedik.)

A meglévő `enterGame()` (`HelloApplication.java:282`) **nem másolódik** — egy paramétert kap, és a `null` jelenti az új játékot. Így a függöny, a disclaimer és a billentyűk bekötése egy helyen marad. A szignatúra és a `controller.begin()` sora változik, minden más a helyén:

```java
    private void enterGame() {
        enterGame(null);
    }

    /** The curtain, the card, and then either a fresh run or a saved one. */
    private void enterGame(SaveData save) {

        Node title = shell.getChildren().get(0);
        // ... a 286-297. sor változatlan: push, curtain, fall ...

        fall.setOnFinished(e -> showDisclaimer(() -> {

            shell.getChildren().set(0, controller.getUI().getRoot());

            // started as the curtain lifts, so the first line is already typing
            if (save == null) {
                controller.begin();
            } else {
                controller.resume(save);
            }

            // ... a 305-313. sor változatlan: keysInstalled, inGame, lift ...
        }));

        fall.play();
    }
```

És a belépőfelület, ami a címképernyő helyére ül, majd újraépített címképernyőre tér vissza:

```java
    /** The sign-in screen, and back to a rebuilt title page either way. */
    private void showLogin() {

        Runnable back = () -> dip(() -> shell.getChildren().set(0, title().getRoot()), 420);

        LoginScreen screen = new LoginScreen(controller.account(), back, back);

        dip(() -> shell.getChildren().set(0, screen.getRoot()), 420);
    }
```

A `dip(Runnable, int)` a fájlban már megvan (a `toTitle()` is ezt használja), tehát nem kell újat írni.

- [ ] **Step 5: Check it by hand**

Run: `./mvnw -o javafx:run`

Expected, sorban:
1. A címképernyőn ott a `BELÉPÉS` sor, a `JÁTÉK BETÖLTÉSE` pedig vagy aktív a mentés adataival a jobb szélen, vagy `NINCS MENTÉS` felirattal letiltott.
2. `BELÉPÉS` → a belépőfelület. Rossz jelszóval: „Hibás belépési adatok." Helyes adatokkal: visszatér a címképernyőre, és a `BELÉPÉS` helyén a felhasználónév áll.
3. `JÁTÉK BETÖLTÉSE` → a játék a mentett jelenettel és statokkal nyit.
4. A felület nem fagy be a belépés közben.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/demo/LoginScreen.java src/main/java/com/example/demo/OpeningScreen.java \
        src/main/java/com/example/demo/HelloApplication.java src/main/java/com/example/demo/GameController.java \
        src/main/java/com/example/demo/Lang.java
git commit -m "Sign in and load a run from the title screen"
```

---

### Task 9: A napló a játékban, és a pause menü

**Files:**
- Modify: `src/main/java/com/example/demo/GameUI.java`
- Modify: `src/main/java/com/example/demo/HelloApplication.java`

**Interfaces:**
- Consumes: `Chatlog.entries()`, `GameController.chatlog()`, `GameController.saveNow(boolean)`
- Produces:
  - `GameUI.showLog(java.util.List<Chatlog.Entry> entries)` → `void`
  - `GameUI.hideLog()` → `void`
  - `GameUI.logVisible()` → `boolean`
  - `GameUI.setOnLog(Runnable action)` → `void` (a header `NAPLÓ` gombja és az `L` billentyű ezt hívja)

- [ ] **Step 1: Build the overlay in GameUI**

A mezők közé:

```java
    private final StackPane logCard;
    private final VBox logList = new VBox(10);
    private Runnable onLog = null;
```

A konstruktorban, a `summaryCard` felépítése után (a `stagePane` rétegei közé kerül, ugyanúgy, ahogy a `summaryCard`):

```java
        Label logHeading = new Label(Lang.t("ui.log").toUpperCase());
        logHeading.setFont(head(28));
        logHeading.setTextFill(Color.web(INK));

        logList.setPadding(new Insets(14, 0, 0, 0));

        ScrollPane logScroll = new ScrollPane(logList);
        logScroll.setFitToWidth(true);
        logScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        logScroll.setPrefHeight(420);

        VBox logContent = new VBox(logHeading, logScroll);
        logContent.setPadding(new Insets(26, 30, 26, 30));
        logContent.setMaxWidth(760);
        logContent.setStyle("-fx-background-color: " + PAPER + ";"
                + "-fx-border-color: " + INK + ";"
                + "-fx-border-width: 2;");

        logCard = new StackPane(logContent);
        logCard.setStyle("-fx-background-color: rgba(22,22,22,0.86);");
        logCard.setVisible(false);
        logCard.setManaged(false);
```

A `stagePane.getChildren().addAll(...)` hívásba a `logCard` a **legfelső** rétegként kerül be (a `summaryCard` és az `exposedCard` után).

A metódusok a `showChapterSummary` közelébe:

```java
    /** The run's transcript, newest at the bottom. */
    public void showLog(java.util.List<Chatlog.Entry> entries) {

        logList.getChildren().clear();

        if (entries.isEmpty()) {
            Label empty = new Label(Lang.t("ui.logEmpty"));
            empty.setFont(bodyItalic(15));
            empty.setTextFill(Color.web(GRAY));
            logList.getChildren().add(empty);
        }

        int chapter = -1;

        for (Chatlog.Entry e : entries) {

            if (e.chapter() != null && e.chapter() != chapter) {
                chapter = e.chapter();

                Label mark = new Label((chapter == 0
                        ? Lang.t("ui.prologue")
                        : Lang.t("ui.chapter") + " " + chapter).toUpperCase());
                mark.setFont(label(10));
                mark.setTextFill(Color.web(RED));
                VBox.setMargin(mark, new Insets(14, 0, 0, 0));
                logList.getChildren().add(mark);
            }

            logList.getChildren().add("choice".equals(e.kind()) ? logChoice(e) : logLine(e));
        }

        logCard.setVisible(true);
        logCard.setManaged(true);
    }

    private Node logLine(Chatlog.Entry e) {

        VBox box = new VBox(2);

        if (e.speaker() != null && !e.speaker().isBlank()) {
            Label who = new Label(e.speaker().toUpperCase());
            who.setFont(label(10));
            who.setTextFill(Color.web(GRAY));
            box.getChildren().add(who);
        }

        Label text = new Label(e.text());
        text.setFont(e.speaker() == null || e.speaker().isBlank() ? bodyItalic(15) : body(15));
        text.setTextFill(Color.web(INK));
        text.setWrapText(true);
        box.getChildren().add(text);

        return box;
    }

    private Node logChoice(Chatlog.Entry e) {

        Label question = new Label(Lang.t("ui.decision").toUpperCase() + " · " + e.speaker());
        question.setFont(label(10));
        question.setTextFill(Color.web(RED));

        String points = (e.xp() == null || e.xp() == 0 ? "" : (e.xp() > 0 ? "+" : "") + e.xp() + " XP")
                + (e.lebukas() == null || e.lebukas() == 0 ? "" :
                        ((e.xp() == null || e.xp() == 0) ? "" : " · ") + (e.lebukas() > 0 ? "+" : "") + e.lebukas());

        Label answer = new Label(e.text() + (points.isEmpty() ? "" : "   " + points));
        answer.setFont(body(15));
        answer.setTextFill(Color.web(INK));
        answer.setWrapText(true);

        VBox box = new VBox(2, question, answer);
        box.setPadding(new Insets(0, 0, 0, 12));
        box.setStyle("-fx-border-color: transparent transparent transparent " + RED + ";"
                + "-fx-border-width: 0 0 0 3;");

        return box;
    }

    public void hideLog() {
        logCard.setVisible(false);
        logCard.setManaged(false);
    }

    public boolean logVisible() {
        return logCard.isVisible();
    }

    public void setOnLog(Runnable action) {
        onLog = action;
    }
```

- [ ] **Step 2: Add the header button and the L key**

A `restartButton` mellé, a `headerRow` összeállítása előtt:

```java
        logButton = new Button(Lang.t("ui.log"));
        logButton.setFont(label(11));
        logButton.setFocusTraversable(false);
        styleOutlineButton(logButton);
        logButton.setOnAction(e -> {
            if (onLog != null) {
                onLog.run();
            }
        });
```

A `headerRow` konstruktorhívásába a `restartButton` **elé** kerül a `logButton`, és a mezőlistába `private final Button logButton;`.

Az `installKeys` eseményszűrőjébe, a „korai ending kártya" blokk **után**:

```java
            // the log is a reader, not a scene: it swallows everything while it is up
            if (logCard.isVisible()) {
                if (code == KeyCode.ESCAPE || code == KeyCode.L) {
                    hideLog();
                }
                event.consume();
                return;
            }

            if (code == KeyCode.L) {
                if (onLog != null) {
                    onLog.run();
                }
                event.consume();
                return;
            }
```

- [ ] **Step 3: Let the pause menu save and open the log**

A `HelloApplication.openPause()` menüsorai:

A `MENTÉS` a helyén marad, és a feliratával jelez vissza — ehhez egy mező kell a `HelloApplication`-ben (`private Button pauseSave;`), mert a lambda a gombra hivatkozik:

```java
        Button resume = pauseItem(hu ? "Folytatás" : "Resume", this::resume);

        pauseSave = pauseItem(Lang.t("ui.save"), () -> {
            controller.saveNow(true);
            pauseSave.setText(Lang.t("ui.saved").toUpperCase());
        });

        Button log = pauseItem(Lang.t("ui.log"), () -> {
            resume();
            controller.getUI().showLog(controller.chatlog().entries());
        });
        Button menu = pauseItem(hu ? "Főmenü" : "Main menu", this::toTitle);
        Button quit = pauseItem(Lang.t("ui.exit"), () -> dip(Platform::exit, 420));

        VBox items = new VBox(2, resume, pauseSave, log, menu, quit);
```

És az ESC sorrendje az `onKey`-ben: a `paused` ág **előtt**, mert a nyitott napló kapja előbb:

```java
        if (inGame && controller.getUI().logVisible()) {
            if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                controller.getUI().hideLog();
                e.consume();
            }
            return;
        }
```

- [ ] **Step 4: Wire the header button to the log**

A `GameController` konstruktorába, az `ui.setOnGameRestart(this::restartGame);` mellé:

```java
        ui.setOnLog(() -> ui.showLog(chatlog.entries()));
```

- [ ] **Step 5: Check it by hand**

Run: `./mvnw -o javafx:run`

Expected:
1. Új játék, néhány kattintás, egy választás meghozva.
2. `L` → a napló feljön, benne az elhangzott sorok fejezetcímkével, és a döntés piros rúddal, `+15 XP · +10` ponttal.
3. `ESC` → a napló bezárul, és **nem** a szünet menü jön fel. Még egy `ESC` → szünet.
4. A szünetben `NAPLÓ` → ugyanaz a napló. `MENTÉS` → a felirata `MENTVE`-re vált, és a `save-slot1.json` `updated_at`-ja frissül.
5. A header `NAPLÓ` gombja is nyitja.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/example/demo/GameUI.java src/main/java/com/example/demo/HelloApplication.java \
        src/main/java/com/example/demo/GameController.java
git commit -m "Let the player read back everything that was said"
```

---

### Task 10: A weboldal oldala

Más repó, külön commit. A migráció nélkül a desktop `scene`/`log`/`fight` írása 400-as hibát kap a PostgREST-től — ez a task nyitja meg nekik az utat.

**Files:**
- Create: `~/WebstormProjects/fityeszTheGameWebsite/supabase/migrations/0003_crossplay.sql`
- Modify: `~/WebstormProjects/fityeszTheGameWebsite/lib/story/save.ts` (a `COLS` konstans kommentje és a `chapterOf` dokumentációja)
- Modify: a `chapter >= 2` szövegei a `messages/` alatt

- [ ] **Step 1: Write the migration**

`supabase/migrations/0003_crossplay.sql`:

```sql
-- Run this once in the Supabase SQL editor.
--
-- The desktop game writes the same row as the website, and needs three things
-- the website has no use for: where in its own script the run is, the lines it
-- has heard, and a boss fight's move counters. All three are nullable and the
-- website's queries never name them, so this changes nothing for the browser.
--
-- chapter now carries the real chapter number (0 for the prologue, 1-7 for the
-- chapters). The website only ever writes 1 or 2, and reads anything from 2 up
-- as "past the web demo", which is exactly what a desktop run beyond chapter
-- one is - so no website query changes here either.
--
-- Safe to run more than once.

alter table public.saves add column if not exists scene text;
alter table public.saves add column if not exists log   jsonb not null default '[]';
alter table public.saves add column if not exists fight jsonb;

comment on column public.saves.scene is
  'Desktop only: the scene method the run is in. Null for a run from the website.';
comment on column public.saves.log is
  'Desktop only: the run transcript, newest last, capped at 300 entries.';
comment on column public.saves.fight is
  'Desktop only: a boss fight in progress - hp and move counters.';
```

- [ ] **Step 2: Run the migration**

A Supabase projekt SQL editorában futtatva (ezt a repó tulajdonosa végzi, lásd a spec 15. szakaszát). Ellenőrzés utána, a desktop oldaláról:

```bash
cd /Users/zsomborkukucska/IdeaProjects/fityeszthegame
./mvnw -o javafx:run
```

Expected: belépés után egy fejezethatárt átlépve a konzolon **nincs** `save: felhő 400` sor, és a Supabase táblanézetében a sor `scene` oszlopa kitöltött.

- [ ] **Step 3: Say what a run past chapter one means**

A `lib/story/save.ts` `chapterOf`/`statusOf` fölötti kommentblokk kiegészítése, hogy a `chapter` szemantikája írásban is egyezzen a desktoppal:

```ts
/**
 * The row has no status column. `chapter` carries how far the run got: 1 while
 * chapter one is being played, 2 once its end screen has been reached — and,
 * for a run saved by the desktop game, the real chapter number up to 7. The
 * website has only the prologue and chapter one as data, so anything from 2 up
 * reads as `demoComplete` here: the run exists, and it continues in the
 * desktop game.
 */
```

Majd a `demoComplete` ághoz tartozó szöveg a `messages/` alatt (`hu` és `en`), amely ma a demo végét jelenti, mondja ki a folytatást is:

```bash
cd ~/WebstormProjects/fityeszTheGameWebsite
grep -rn "demoComplete" components messages app | head -20
```

A találatok szövegkulcsait úgy írd át, hogy a „letölthető játékban folytathatod" is elhangozzon, mindkét nyelven, a fájlok meglévő hangját követve.

- [ ] **Step 4: Run the website's tests**

Run: `cd ~/WebstormProjects/fityeszTheGameWebsite && npm test`
Expected: PASS — a `vitest` suite zöld, a szövegváltozás nem bontott el snapshotot.

- [ ] **Step 5: Commit, in the website repo**

```bash
cd ~/WebstormProjects/fityeszTheGameWebsite
git add supabase/migrations/0003_crossplay.sql lib/story/save.ts messages
git commit -m "Make room in a save for the desktop game's own position"
```

---

## A terv végén

- [ ] **A teljes suite**: `./mvnw -o test` — minden zöld
- [ ] **Egy teljes kör kézzel**: belépés → új játék → 2. fejezetig → `MENTÉS` → kilépés az appból → újraindítás → `JÁTÉK BETÖLTÉSE` → ugyanott, ugyanazzal a naplóval
- [ ] **Repülőgép-mód**: wifi ki, új játék, pár kattintás, app bezárás, indítás, betöltés — a helyi mentés visz mindent, és a konzolon csak egy `save:` sor van, fagyás nincs
- [ ] **Cross-play egy irányban**: a weben indított 1. fejezetes futás a desktop `JÁTÉK BETÖLTÉSE`-vel megnyílik, a statokkal és a döntésekkel
- [ ] A `bugok, kijavítandó cuclik.txt` három sora `[x]`-re váltható
