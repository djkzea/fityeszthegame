# A jelzőzenék

A `logo-sting.wav`, a `menu-loop.wav` és a `disclaimer.wav` itt készült,
nem felvételről való: `synth.py` a hangszerek (vonós, zongora, üstdob, óra,
terem), `menu.py` a menü hurkát rakja össze belőlük.

**Miért így:** egy generált vagy felvett sávnál a hurokpontot keresni kell,
és a varrat akkor is hallatszik, ha a két vége egyezik. Itt a hurok
*periodikusan épül*: két kör készül ugyanazokból a hullámformákból, és a
középső kör kerül a fájlba — a varrat lépése így kisebb, mint a zene
tipikus mintalépése.

Újragyártás:

    python3 tools/audio/menu.py     # 16 mp-es menühurok
    # a kiírt fájl a munkakönyvtárba kerül, onnan másolandó:
    # src/main/resources/com/example/demo/audio/

Csak `numpy` kell hozzá. A játék `music-loop.wav`-ja (a fő ágy) nem itt
készült, az generált felvételből van vágva.
