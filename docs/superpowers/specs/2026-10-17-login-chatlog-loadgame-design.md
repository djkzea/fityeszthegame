# Bejelentkezés, chatlog, játék betöltése — design

Dátum: 2026-10-17 · Állapot: jóváhagyásra vár · Repók: `fityeszthegame` (desktop), `fityeszTheGameWebsite` (web)

## 1. Miért

A `bugok, kijavítandó cuclik.txt` három nyitott pontja: bejelentkezési felület,
chatlog, load game. A `LOAD GAME` menüpont már ott van a címképernyőn, de
`HAMAROSAN` felirattal, letiltva (`OpeningScreen.java:272`).

A három feature egy alrendszer: a bejelentkezés adja a személyt, a mentés hozzá
tartozik, a chatlog pedig a mentés része. Ezért egy specben szerepelnek.

## 2. Eldöntött kérdések

| Kérdés | Döntés |
|---|---|
| Mi a „bejelentkezés"? | Online fiók a `fityeszthegame.com` mögötti **Supabase** projekthez — ugyanaz a felhasználónév/jelszó, mint a weben |
| Kötelező-e? | **Nem.** Vendégként is játszható, internet nélkül is |
| Hol lakik a mentés? | **Lokális fájl + felhő szinkron**: helyben azonnal, Supabase-be háttérben, best effort |
| Web és desktop ugyanaz a mentés? | **Igen, közös mentés** (igazi cross-play), a 7. szakasz korlátaival |
| Chatlog tartalma | **Napló + döntések**: elhangzott sorok, plusz a választott válaszok XP/lebukás-deltával |
| Visszatöltés pontossága | **Pontos jelenet-visszatöltés** (nem csak fejezet elejéről) |

## 3. Amit a meglévő kód megad

- A web `saves` táblája már létezik: `user_id`, `slot` (1–3), `display_name`,
  `player_name`, `chapter`, `xp`, `lebukas`, `szint`, `items text[]`,
  `history jsonb`, `updated_at`, `unique (user_id, slot)`, RLS `auth.uid() = user_id`
  (`supabase/migrations/0001_saves.sql`).
- Felhasználónevas belépés a `sign-in` Edge Functionön megy, ami csak tokeneket
  ad vissza, és minden hibára ugyanazt a választ (`supabase/functions/sign-in/index.ts`).
- A web `choicePointId`-jai **már a desktop Lang-kulcsai**: `ch1.q1`, `ch1.q2`
  (`lib/story/content/fejezet-01.json`). A desktop `recordDecision(questionKey, …)`
  ugyanezeket gyűjti (`GameController.java:16`).
- **Jackson databind 2.14.2 már a projekt classpathján van** (tranzitívan). Nem kell
  új dependency, csak `requires com.fasterxml.jackson.databind;` a `module-info`-ba.
- A `GameState` lapos primitívek halmaza — közvetlenül szerializálható.

## 4. Nem része ennek a specnek

- A sztori weboldalra költöztetése (a web csak a prológust és az 1. fejezetet ismeri adatként).
- Jelszó-visszaállítás, email-csere, fiók törlése a desktopról (a weben megvan).
- Több mentési slot. A közös mentés a **slot 1**, ahogy a web használja.
- Offline regisztráció.

## 5. Adatmodell — `0003_crossplay.sql` migráció

Három új, nullable oszlop a `saves`-re. A web kódja nem olvassa őket, tehát a
weboldal változatlanul működik:

```sql
alter table public.saves add column if not exists scene text;
alter table public.saves add column if not exists log   jsonb not null default '[]';
alter table public.saves add column if not exists fight jsonb;
```

- `scene` — a desktop pontos pozíciója: a jelenetmetódus neve (lásd 8. szakasz).
  A webnek nincs értelme, ezért hagyja. `null` = a futás nem desktopról jön.
- `log` — a chatlog bejegyzései (lásd 9. szakasz).
- `fight` — boss fight közbeni állapot: `playerHp`, `bossHp`, `previousMove`,
  `sameMove`, `peteriPassiveRounds`. `null`, ha a futás nem harcban van.

A `chapter` ezentúl az **igazi fejezetszám (0–7)**, a 0 a prológus. A web mostani
`statusOf()`-ja `chapter >= 2`-t `demoComplete`-ként kezeli, ami a desktop
futásokra pont azt jelenti, hogy „túl van a web demón" — kódot nem kell hozzá
írni, csak szöveget (12. szakasz).

Item-leképezés (desktop boolean ↔ web `ItemId`, `lib/story/types.ts`):

| `GameState` | `ItemId` |
|---|---|
| `firstEnvelope` | `envelope1` |
| `smallEnvelope` | `envelopeSmall` |
| `lakatosFile` | `lakatosFile` |
| `offshoreCode` | `offshore` |
| `peteriDossier` | `peteriDossier` |
| `bossTrust` | `bossTrust` |
| `parliamentKey` | `parliamentKey` |

`history` elemei a webbel egyező alakban: `{"choicePointId": "ch1.q1", "optionIndex": 0}`.
Az `optionIndex` nulla alapú; a `recordDecision` minden hívási helyén ott van a
`choice` változó (1 alapú), a boss-győzelmeknél 1 (`sum.boss`).

## 6. Új és módosított fájlok

**Új (desktop, `com.example.demo`)**

| Fájl | Dolga |
|---|---|
| `Supabase.java` | URL + anon key konstans, env-változóval felülírható. HTTP alapműveletek Jacksonnal. |
| `Account.java` | Regisztráció, belépés, token-megújítás, kilépés, „ki vagyok". Tokenek lemezre. |
| `SaveData.java` | A mentés egy `record`-ja — a JSON és a tábla sora ugyanez az alak. |
| `SaveStore.java` | Helyi fájl + felhő upsert/olvasás, a frissebb nyer. |
| `Chatlog.java` | A napló bejegyzései, méretkorláttal. |
| `LoginScreen.java` | A belépés/regisztráció felülete az `OpeningScreen` tokenjeivel. |
| `AppDirs.java` | Platformonkénti adatkönyvtár (egy metódus). |

**Módosított**

| Fájl | Mit |
|---|---|
| `module-info.java` | `requires com.fasterxml.jackson.databind;`, `requires java.net.http;` |
| `GameController.java` | `step()` hívások, `say()` hívások, `recordDecision` kiegészítés, `saveNow()`, `resume(SaveData)` |
| `GameUI.java` | Chatlog overlay, `L` gomb a headerben |
| `OpeningScreen.java` | `BELÉPÉS` menüsor, aktív `LOAD GAME`, folytatás-kártya |
| `HelloApplication.java` | Pause menü: `MENTÉS`, `NAPLÓ`; ESC-kezelés a nyitott napló mellett |
| `Lang.java` | Új kulcsok (11. szakasz) |

**Új (web repó)**

| Fájl | Mit |
|---|---|
| `supabase/migrations/0003_crossplay.sql` | Az 5. szakasz migrációja |

## 7. Fiókréteg — `Account.java`

`java.net.http.HttpClient` (JDK, Java 25) + Jackson. Pontosan azokra a
végpontokra megy, amiket a web is használ, így nem kell szerveroldali változás.

| Művelet | Kérés |
|---|---|
| Regisztráció | `POST {URL}/auth/v1/signup`, `{"email":…,"password":…,"data":{"display_name":"<felhasználónév>"}}` — a `data.display_name` kell, hogy a `handle_new_user` trigger megírja a `profiles` sort |
| Belépés | `POST {URL}/functions/v1/sign-in`, `{"identifier":"<név vagy email>","password":…}` → `{access_token, refresh_token}` |
| Token-megújítás | `POST {URL}/auth/v1/token?grant_type=refresh_token`, `{"refresh_token":…}` |
| Ki vagyok | `GET {URL}/auth/v1/user` → `id`, `user_metadata.display_name` |

Minden kérésen `apikey: <anon key>` és `Content-Type: application/json`; a
bejelentkezett kéréseken `Authorization: Bearer <access_token>`.

- **Hibaüzenet**: a `sign-in` szándékosan ugyanazt a `invalid_credentials`-t adja
  minden hibára, hogy a felhasználónevek ne legyenek kitalálhatók. A desktop
  ugyanígy egyetlen szöveget mutat, és nem árul el többet.
- **Token tárolás**: `<adatkönyvtár>/session.json`, `600` jogosultsággal
  (`Files.setPosixFilePermissions`, Windows-on kihagyva). Így a belépés túléli az
  újraindítást.
- **Lejárt token**: egy 401 után egy megújítási kísérlet, majd újra a kérés. Ha az
  is elesik, a session törlődik, és a játék vendég módba esik vissza — a futás
  megy tovább, csak nem szinkronizál.
- **Anon key a binárisban**: publikus kulcs, a böngészőben is az; az adatot az RLS
  védi. A `Supabase.java` konstansai a `FITYESZ_SUPABASE_URL` és
  `FITYESZ_SUPABASE_ANON_KEY` env-változókkal felülírhatók.
- **Tesztelhetőség**: az `Account` és a `SaveStore` konstruktorban kapja a
  `HttpClient`-et, így a kérések alakja hálózat nélkül ellenőrizhető.
- **Időkorlát**: 8 másodperc kapcsolódás és válasz. Minden hálózati hívás a
  JavaFX szálon kívül fut; a UI csak `Platform.runLater`-ben frissül.

## 8. Mentés és pontos visszatöltés

### 8.1 A mentés alakja

```java
record SaveData(
    String playerName, String displayName,
    int chapter, String scene,
    int xp, int lebukas, int szint,
    List<String> items,
    List<Decision> history,
    List<Chatlog.Entry> log,
    Fight fight,
    String updatedAt) { }
```

`Decision(String choicePointId, int optionIndex)`, `Fight(int playerHp, int bossHp,
int previousMove, int sameMove, int peteriPassiveRounds)`.

### 8.2 Hol lakik

- Helyben: `<adatkönyvtár>/save-slot1.json`, Jacksonnal, **szinkronban** írva
  (ideiglenes fájl + `ATOMIC_MOVE`, hogy a félbeírás ne tegye tönkre a mentést).
- Felhőben: `POST {URL}/rest/v1/saves`, `Prefer: resolution=merge-duplicates,return=minimal`,
  `onConflict` a `user_id,slot` páron — ugyanaz az upsert, amit a web `persist()`-je csinál.
  Egyetlen szálas végrehajtó, best effort: a hiba naplóba megy, a játék nem áll meg.
- Adatkönyvtár: macOS `~/Library/Application Support/FityeszKronika`, Windows
  `%APPDATA%/FityeszKronika`, egyébként `$XDG_DATA_HOME/fityesz` vagy `~/.local/share/fityesz`.

### 8.3 Mikor mentünk

- Minden jelenetváltásnál (a `step()`-ben, lásd 8.5) — helyben.
- Felhőbe fejezethatáron, a chapter summary után, és a pause menü `MENTÉS`-ére.
  Így nem lesz percenként ötven HTTP-kérés.
- A web `persist()` szabályát átvesszük: **döntés nélküli futás nem írja felül a
  felhős sort**, mert pont az a sikertelen visszatöltés képe.

### 8.4 Melyik mentés nyer

Betöltéskor a helyi és a felhős sor közül a frissebb `updated_at` nyer. Ha a
felhős nyer, és a `scene` üres (vagyis weben játszották), akkor a statok és a
`history` jönnek át, a pozíció pedig a `chapter` első jelenete.

### 8.5 A jelenet-azonosító — regiszter nélkül

Minden jelenetmetódus első sora ma `ui.clearChoices();`. Ezt egy mechanikus
cserével `step();`-re váltjuk:

```java
/** Clears the choices and remembers which scene we are in, for the save. */
private void step() {
    ui.clearChoices();
    StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(frames -> frames.skip(1).findFirst())
        .filter(f -> f.getMethodType().parameterCount() == 0)
        .ifPresent(f -> currentScene = f.getMethodName());
    autosaveLocal();
}
```

Visszatöltéskor ugyanez a név reflexióval hívható: a `GameController` a saját
privát, paraméter nélküli metódusát hívja, amihez nem kell `setAccessible`.

Miért így:

- Nincs 85 soros `Map`, amit karban kell tartani, és nincs elgépelhető string.
- A paraméteres jelenetek (`chapter1Choice(int)`, `bossMove(int)`,
  `peteriBossMove(int)`, `boss3Move(int)`) **nem** mentődnek — a `parameterCount() == 0`
  szűrő kiszórja őket —, így a visszatérési pont a legutóbbi paraméter nélküli
  jelenet. Ez egy-két kattintás ismétlést jelenthet, viszont nem kell a
  köztes állapotot modellezni.
- Harc közben a `fight` visszaállítja a HP-ket és a mozdulat-számlálókat, mielőtt
  a harc belépő jelenete (`startLakatosBoss`, `startPeteriBoss`, `startBoss3`)
  újra lefut.
- **Átnevezés-védelem**: egy teszt feloldat minden mentett jelenetnevet, és
  elhasal, ha egy metódus átnevezése törött mentést hagyna (13. szakasz).

### 8.6 A fejezetszám

A `setChapterHeader(chapter, title)` minden fejezet elején lefut — ez írja a
`currentChapter` mezőt, ami a mentésbe kerül.

## 9. Chatlog

`Chatlog.Entry` két fajta bejegyzés egy rekordban:

```java
record Entry(String kind, String speaker, String text, Integer xp, Integer lebukas, Integer chapter) { }
```

- `kind = "line"` — elhangzott sor: beszélő (üres a narrációnál) és a szöveg.
- `kind = "choice"` — döntés: a kérdés, a választott válasz, a kapott XP és lebukás.

Gyűjtés ugyanazzal a módszerrel, mint a 8.5: a `ui.showDialogue(` hívások egy
helyi `say(` helperre cserélődnek, ami naplóz és továbbad. A döntések a már
létező `recordDecision`-ből jönnek, ami egy `choice` paramétert kap az
`optionIndex`-hez (a `history` is innen hízik).

- **Méret**: az utolsó 300 bejegyzés megy a mentésbe. A szöveg kirenderelve
  tárolódik (nem kulcsként), mert a `Lang.t(kulcs, név)` behelyettesítéssel készül,
  és a napló azt mutassa, ami elhangzott.
- **Felület**: görgethető overlay a `GameUI`-ban, fejezetenként tagolva, a
  legfrissebb bejegyzésre ugrik. `L` nyitja (meg egy gomb a headerben és a pause
  menü `NAPLÓ` pontja), `ESC` zárja.
- **ESC**: a `HelloApplication.onKey` az ESC-et most a pause menühöz adja. A nyitott
  napló előbb kapja meg és elnyeli, hogy ne a szünet jöjjön fel helyette.

## 10. Felületek

**Bejelentkezés (`LoginScreen.java`)** — az `OpeningScreen` design tokenjeivel
(`OpeningScreen.java:59`), ugyanaz a menü-tipográfia és a piros kijelölés.
Belépés: felhasználónév vagy email + jelszó. Regisztráció: email, felhasználónév
(3–32 karakter, ahogy a `profiles` check-je kéri), jelszó (min. 8, ahogy a web
`AuthForm`-ja). Enter küld, ESC visszalép. Egy hibaszöveg mindenre.

**Címképernyő (`OpeningScreen.java`)** — új `BELÉPÉS` menüsor a `NEW GAME` fölé;
belépés után a helyén a felhasználónév és egy `KILÉPÉS`. A `LOAD GAME`-ről lejön
a `HAMAROSAN`, és egy kártyát nyit: név, fejezet, mentés ideje, honnan jött
(gép/fiók). Ha nincs mentés, a gomb letiltva marad.

**Pause menü (`HelloApplication.java`)** — `FOLYTATÁS`, **`MENTÉS`**, **`NAPLÓ`**,
`FŐMENÜ`, `KILÉPÉS`. A `MENTÉS` azonnal kitolja a felhőbe, és visszajelez egy sorban.

## 11. Új `Lang` kulcsok

Magyar és angol minden kulcshoz, a `p(kulcs, hu, en)` alakban, a weboldal
`messages/` szövegeiből átvéve, ahol van megfelelő:

`ui.signIn`, `ui.signOut`, `ui.register`, `ui.continue`, `ui.save`, `ui.saved`,
`ui.log`, `ui.username`, `ui.email`, `ui.password`, `ui.authFailed`,
`ui.authOffline`, `ui.checkEmail`, `ui.guest`, `ui.noSave`, `ui.saveFrom.local`,
`ui.saveFrom.cloud`, `ui.logEmpty`, `ui.decision`.

## 12. Weboldal oldali teendő

Kód nem kell hozzá, szöveg igen: a `chapter >= 2` (`statusOf` → `demoComplete`)
eset ma „a demo végére értél"-t jelent. Ez a desktop futásoknál „a futásod
túlment a weboldal demóján, a letölthető játékban folytathatod". Érintett:
`components/play/StoryPlayer.tsx`, `components/dash/DashboardClient.tsx`, és a
hozzá tartozó `messages/` kulcsok.

## 13. Ellenőrzés

JUnit 5 már a pomban (`junit-jupiter` 5.12.1).

| Teszt | Mit fog el |
|---|---|
| `SaveDataTest` | JSON körbe-oda: minden mező visszajön, az üres `fight` is |
| `ItemMappingTest` | A hét boolean ↔ `ItemId` leképezés mindkét irányban, teljesen |
| `SceneRegistryTest` | Minden mentett jelenetnév feloldható paraméter nélküli metódusra — ez fogja el a jelenet-átnevezésből jövő törött mentést |
| `ChatlogTest` | A 300-as korlát a legfrissebbeket tartja meg |
| `AccountTest` | A kérések alakja (URL, fejlécek, body) egy helyi `HttpClient`-duplán, hálózat nélkül |

Kézi próba, amit nem lehet unit tesztelni: regisztráció és belépés az éles
Supabase projekt ellen; egy kör repülőgép-módban (hálózat nélkül mentés-betöltés
működik); egy weben kezdett 1. fejezetes futás átvétele desktopon.

## 14. Korlát, amit tudni kell a cross-playről

A weboldal csak a prológust és az 1. fejezetet ismeri adatként
(`lib/story/content/`), a desktop 7 fejezetet játszik.

- **Web → desktop**: működik. Az 1. fejezetes futás statjai és döntései átjönnek,
  a desktop a fejezet elejéről folytatja a mentett statokkal.
- **Desktop → web**: amíg a futás az 1. fejezeten belül van, a web is folytatni
  tudja. Azon túl a weboldal **látja** a futást (dashboard, statok), de nem tudja
  játszani — ezt mondja meg a 12. szakasz szövege.

A teljes szimmetria a sztori weboldalra költöztetése, ami külön projekt.

## 15. Nyitott kérdés

1. **Email-megerősítés**: ha be van kapcsolva a Supabase projekten, a desktop
   regisztráció után levelet kell kattintani, mielőtt a belépés menne. A
   `LoginScreen` ezt megmondja (`ui.checkEmail`), de a beállítást meg kell nézni.
2. **A `0003` migrációt ki futtatja** a Supabase SQL editorban.

## 16. Építési sorrend

1. `0003_crossplay.sql` + `AppDirs`, `SaveData`, `SaveStore` (csak helyi fájl) + tesztek
2. `step()` csere és `resume(SaveData)` — a pontos visszatöltés működik helyben
3. `Chatlog` + overlay
4. `Supabase`, `Account` + `LoginScreen`
5. `SaveStore` felhős fele (upsert, olvasás, frissebb nyer)
6. Címképernyő és pause menü felületek, `Lang` kulcsok
7. Weboldal szövegei (12. szakasz)
