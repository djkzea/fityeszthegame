import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Ketnyelvu szovegtar / Two-language text store.
 *
 * Minden szoveg egyetlen p(kulcs, magyar, angol) hivasban szerepel, igy a ket
 * nyelv kulcsai nem tudnak szetcsuszni. A t(kulcs) a kivalasztott nyelven adja
 * vissza a szoveget; a t(kulcs, args...) String.format-tal helyettesit be.
 */
public final class Lang {

    public static final String HU = "hu";
    public static final String EN = "en";

    private static final Map<String, String> huSzoveg = new LinkedHashMap<>();
    private static final Map<String, String> enSzoveg = new LinkedHashMap<>();

    private static Map<String, String> aktiv = huSzoveg;
    private static String kod = HU;

    private Lang() { }

    /** Egy szoveg felvetele mindket nyelven. */
    private static void p(String kulcs, String magyar, String angol) {
        if (huSzoveg.containsKey(kulcs)) {
            throw new IllegalStateException("Lang: duplikalt kulcs / duplicate key: " + kulcs);
        }
        huSzoveg.put(kulcs, magyar);
        enSzoveg.put(kulcs, angol);
    }

    /** A kivalasztott nyelv szovege. Argumentumok eseten String.format fut le rajta. */
    public static String t(String kulcs, Object... args) {
        String s = aktiv.get(kulcs);
        if (s == null) {
            throw new IllegalArgumentException("Lang: hianyzo kulcs / missing key: " + kulcs);
        }
        return args.length == 0 ? s : String.format(s, args);
    }

    public static String kod() {
        return kod;
    }

    public static boolean magyar() {
        return HU.equals(kod);
    }

    public static void set(String nyelvKod) {
        if (EN.equals(nyelvKod)) {
            aktiv = enSzoveg;
            kod = EN;
        } else {
            aktiv = huSzoveg;
            kod = HU;
        }
    }

    /** Nyelvvalaszto a jatek indulasakor. Ketnyelvu, mert meg nincs kivalasztott nyelv. */
    public static void select(Scanner sc) {
        final String RESET = "\033[0m";
        final String BOLD  = "\033[1m";
        final String GOLD  = "\033[33m";
        final String GRAY  = "\033[90m";
        final String YELLOW = "\033[93m";

        System.out.println();
        System.out.println(GOLD + "  ╔══════════════════════════════════╗" + RESET);
        System.out.println(GOLD + "  ║   " + BOLD + "Nyelv / Language" + RESET + GOLD + "               ║" + RESET);
        System.out.println(GOLD + "  ╚══════════════════════════════════╝" + RESET);
        System.out.println("  " + BOLD + YELLOW + "1." + RESET + YELLOW + " Magyar" + RESET);
        System.out.println("  " + BOLD + YELLOW + "2." + RESET + YELLOW + " English" + RESET);
        System.out.println();

        while (true) {
            System.out.print(GOLD + "> " + RESET);
            if (!sc.hasNextLine()) {
                set(HU);
                return;
            }
            String v = sc.nextLine().trim().toLowerCase();

            if (v.equals("1") || v.equals("hu") || v.equals("magyar")) {
                set(HU);
                System.out.println(GRAY + "  Nyelv: Magyar" + RESET);
                return;
            }
            if (v.equals("2") || v.equals("en") || v.equals("english") || v.equals("angol")) {
                set(EN);
                System.out.println(GRAY + "  Language: English" + RESET);
                return;
            }
            System.out.println(GRAY + "  Ervenytelen valasztas / Invalid choice - 1 vagy/or 2" + RESET);
        }
    }

    static {
        // ── Kezelofelulet / UI chrome ──────────────────────────────────────
        p("ui.title",            "FITYESZ KRÓNIKA", "THE FITYESZ CHRONICLE");
        p("ui.chapter",          "FEJEZET", "CHAPTER");
        p("ui.choose",           "Válassz", "Choose");
        p("ui.bossfight",        "BOSSFIGHT", "BOSSFIGHT");
        p("ui.item",             "MEGSZERZETT TÁRGY", "ITEM ACQUIRED");
        p("ui.levelup",          "✦  SZINTLÉPÉS!  ✦", "✦  LEVEL UP!  ✦");
        p("ui.newrank",          "Új rang: ", "New rank: ");
        p("ui.xp",               "XP", "XP");
        p("ui.exposure",         "LEBUKÁS", "EXPOSURE");
        p("ui.level",            "Szint", "Level");
        p("ui.congrats",         "✦  GRATULÁLUNK, %s!  ✦", "✦  CONGRATULATIONS, %s!  ✦");
        p("ui.president",        "TE LETTÉL A PÁRTELNÖK!", "YOU ARE THE PARTY PRESIDENT!");
        p("ui.finalxp",          "Végső XP:", "Final XP:");
        p("ui.exposuremeter",    "Lebukásmérő:", "Exposure meter:");
        p("ui.itemsheader",      "Megszerzett tárgyak:", "Items acquired:");
        p("ui.defeat",           "✖  VESZTETTÉL!  ✖", "✖  YOU LOST!  ✖");
        p("ui.exposed",          "✖  LEBUKTÁL!", "✖  YOU'VE BEEN EXPOSED!");
        p("ui.pressfound",       "A sajtó mindent kiderített.", "The press has uncovered everything.");
        p("ui.enter",            "[ Nyomj Enter-t a folytatáshoz... ]", "[ Press Enter to continue... ]");
        p("ui.you",              "Te", "You");

        // ── Szereplok / Speakers ───────────────────────────────────────────
        p("npc.unknown",         "Ismeretlen hang", "Unknown voice");
        p("npc.lipoti",          "Lipóti Dezső", "Lipóti Dezső");
        p("npc.lakatos",         "Lakatos Ervin", "Lakatos Ervin");
        p("npc.speaker",         "Hangszóró", "Loudspeaker");
        p("npc.kapzs",           "Kapzs Imre", "Kapzs Imre");
        p("npc.peteri",          "Dr. Péteri Katalin", "Dr. Péteri Katalin");
        p("npc.peteri.short",    "Dr. Péteri", "Dr. Péteri");
        p("npc.lakatos.short",   "Lakatos", "Lakatos");
        p("npc.molnar",          "Molnár Gábor", "Molnár Gábor");
        p("npc.you",             "Te", "You");

        // ── Targyak es rangok / Items and ranks ────────────────────────────
        p("item.envelope1",      "ELSŐ BORÍTÉK", "FIRST ENVELOPE");
        p("item.envelopeSmall",  "KIS BORÍTÉK", "SMALL ENVELOPE");
        p("item.lakatosFile",    "LAKATOS AKTÁJA", "LAKATOS'S FILE");
        p("item.offshore",       "OFFSHORE SZÁMLA BELÉPÉSI KÓD", "OFFSHORE ACCOUNT ACCESS CODE");
        p("item.peteriDossier",  "PÉTERI DOSSZIÉ", "PÉTERI DOSSIER");
        p("item.bossTrust",      "FŐNÖK BIZALMA", "THE BOSS'S TRUST");
        p("item.parliamentKey",  "PARLAMENTI TÖBBSÉG KULCSA", "KEY TO THE PARLIAMENTARY MAJORITY");
        p("rank.localMember",    "HELYI PÁRTTAG", "LOCAL PARTY MEMBER");

        // ── Harc / Combat ──────────────────────────────────────────────────
        p("fight.tactic",        "Taktika?", "Tactic?");
        p("fight.attack",        "Támadás", "Attack");
        p("fight.defend",        "Védekezés", "Defend");
        p("fight.bothAttacked",  "Mindketten támadtatok!", "You both attacked!");
        p("fight.bothDefended",  "Mindketten védekeztetek.", "You both defended.");
        p("fight.continue",      "Folytatod?", "Continue?");
        p("fight.retry",         "Újrapróbálás", "Try again");
        p("fight.again",         "Újra", "Again");
        p("fight.quit",          "Kilépés", "Quit");
        p("fight.gameover",      "Játék vége.", "Game over.");

        // ── Bevezeto / Intro ───────────────────────────────────────────────
        p("intro.welcome",       "Üdvözöllek a játékban!",
                                 "Welcome to the game!");
        p("intro.1",             "Ez egy szöveg alapú kaland játék, amiben egy izgalmas történeten járhatsz végig.",
                                 "This is a text-based adventure game that walks you through a gripping story.");
        p("intro.2",             "A döntéseid mind befolyásolják a cselekmény kimenetelét. Bölcsen válassz!",
                                 "Every decision you make shapes how the plot turns out. Choose wisely!");
        p("intro.3",             "A sztori során 1-3 számok közül kell választanod a folytatást.",
                                 "Throughout the story you choose how it continues by picking a number from 1 to 3.");

        // ── Prologus / Prologue ────────────────────────────────────────────
        p("pro.title",           "Prológus: A mélyPont", "Prologue: Rock Bottom");
        p("pro.quote",           "Aki a múltat nem ismeri, az a jövőt sem érti.",
                                 "He who does not know the past cannot understand the future.");
        p("pro.namePrompt",      "Add meg a neved: ", "Enter your name: ");
        p("pro.youAre",          "Te %s vagy.", "You are %s.");
        p("pro.teacher",         "Volt középiskolai tanár vagy.",
                                 "You are a former secondary school teacher.");
        p("pro.salary",          "A havi 180 ezres fizetésből nem jött ki a matek.",
                                 "On a salary of 180 thousand a month, the maths never added up.");
        p("pro.assets",          "Van 3 kiflid, 380 Forintod és 4 millió forint adósságod.",
                                 "You own 3 bread rolls, 380 forints and 4 million forints of debt.");
        p("pro.bank",            "A bank már keres.", "The bank is already looking for you.");
        p("pro.politician",      "Ezért politikusnak állsz.", "So you go into politics.");
        p("pro.phone",           "Csörög a telefonod", "Your phone rings");
        p("pro.call",            "%s? Hallottam a helyzetéről. Van egy ajánlatom, amit nem fog tudni visszautasítani.\n"
                               + "Találkozzunk a Dohány utcai kávézóban, holnap délben. Kérdezzen Lipóti úr után.",
                                 "%s? I have heard about your situation. I have an offer you will not be able to refuse.\n"
                               + "Meet me at the café on Dohány Street, tomorrow at noon. Ask for Mr. Lipóti.");

        // ── 1. fejezet / Chapter 1 ─────────────────────────────────────────
        p("ch1.title",           "A Toborzás", "The Recruitment");
        p("ch1.quote",           "Az országért mindenkinek tenni kell, nem csak beszélni róla.",
                                 "Everyone must act for the country, not merely talk about it.");
        p("ch1.place",           "Helyszín: egy eldugott kávézó. A falon egy Kossuth portrét és egy rejtélyes grafikát látsz.",
                                 "Location: a tucked-away café. On the wall you see a portrait of Kossuth and a cryptic chart.");
        p("ch1.lipoti1",         "%s? Lipóti Dezső vagyok, kérem, foglaljon helyet! Kávét? Pálinkát? Mindkettőt?\n"
                               + "Látom a szemén, hogy az utóbbi kell.",
                                 "%s? Dezső Lipóti is the name, please, take a seat! Coffee? Pálinka? Both?\n"
                               + "I can see in your eyes that it is the latter you need.");
        p("ch1.lipoti2",         "Szóval tanár volt. Nemes hivatás, nemde? De tudja, mi a nemesebb? A szolgálat. A NEMZET szolgálata.\n"
                               + "És mellékesen... nos, a nemzet szolgálata jól fizet.",
                                 "So you were a teacher. A noble calling, is it not? But do you know what is nobler? Service. Service to the NATION.\n"
                               + "And incidentally... well, serving the nation pays well.");
        p("ch1.q1",              "Mit válaszolsz?", "What do you say?");
        p("ch1.q1.opt1",         "Engem csak a pénz érdekel", "I'm only in it for the money");
        p("ch1.q1.opt2",         "A nemzet szolgálata érdekel", "I care about serving the nation");
        p("ch1.q1.opt3",         "Mi lenne a dolgom?", "What would my job be?");
        p("ch1.q1.ans1",         "Őszinte ember! Ezt szeretem. Ilyen emberekre van szükségünk.",
                                 "An honest man! I like that. These are the people we need.");
        p("ch1.q1.ans2",         "Szép szavak! Megtanulja még a többit is.",
                                 "Fine words! You will learn the rest in time.");
        p("ch1.q1.ans3",         "Óvatos! Ez jó. De a részletekbe majd később megyünk bele.",
                                 "Cautious! That is good. But we will get into the details later.");
        p("ch1.lipoti3",         "A Fityesz nem csak egy párt, %s. Család. És a családban mindenkiről gondoskodunk. Nézze csak...",
                                 "The Fityesz is not merely a party, %s. It is a family. And in a family we look after everyone. Just look...");
        p("ch1.envelope",        "Lipóti kitesz az asztalra egy borítékot. Kinyitod és 500 ezer magyar forintot látsz benne. Készpénzben.",
                                 "Lipóti lays an envelope on the table. You open it and see 500 thousand Hungarian forints inside. In cash.");
        p("ch1.lipoti4",         "Ez csak az eleje. Előleg a jövőbeni... hűségéért. Ahogy a Miniszterelnök Úr mondta egyszer...",
                                 "This is only the beginning. An advance on your future... loyalty. As the Prime Minister once said...");
        p("ch1.q2",              "Elfogadod a pénzt?", "Do you take the money?");
        p("ch1.q2.opt1",         "Elveszed és bólogatsz", "Take it and nod");
        p("ch1.q2.opt2",         "Elveszed, de kérdezel: \"Mi a feltétel?\"", "Take it, but ask: \"What's the catch?\"");
        p("ch1.q2.opt3",         "Nem nyúlsz hozzá", "Don't touch it");
        p("ch1.q2.ans1",         "Lipóti elmosolyodik", "Lipóti smiles");
        p("ch1.q2.ans2",         "Feltétel? Csak annyi, hogy holnap megjelensz ezen a címen.",
                                 "A catch? Only that tomorrow you show up at this address.");
        p("ch1.q2.ans3",         "Ó, tehát alkudni akar! Maga tényleg politikusnak született!",
                                 "Oh, so you want to haggle! You really were born to be a politician!");

        // ── 2. fejezet / Chapter 2 ─────────────────────────────────────────
        p("ch2.title",           "Az első gyűlés", "The First Meeting");
        p("ch2.quote",           "Aki nincs ellenünk, az velünk van.", "Whoever is not against us is with us.");
        p("ch2.place",           "Helyszín: Egy kerületi irodaépület, piros-fehér-zöld zászlókat látsz mindenhol.",
                                 "Location: A district office building, red-white-green flags wherever you look.");
        p("ch2.narr1",           "A teremben körülbelül 30 ember ül műanyag székeken. A falon gigantikus Kapzs Imre-portré, alatta a felirat: \"CSAK FELFELÉ!\"",
                                 "About 30 people sit on plastic chairs in the hall. On the wall a gigantic portrait of Kapzs Imre, beneath it the caption: \"ONWARDS AND UPWARDS!\"");
        p("ch2.lakatos1",        "Na, te vagy az új! %s, ugye? Gyere, gyere! Pont jókor jöttél, ma van a havi... izé... KÖZÖSSÉGI TALÁLKOZÓ.\n"
                               + "Igen, úgy hívjuk.",
                                 "So you're the new one! %s, right? Come in, come in! You've come at just the right time, today is the monthly... er... COMMUNITY GATHERING.\n"
                               + "Yes, that is what we call it.");
        p("ch2.narr2",           "A teremben az emberek borítékokat osztogatnak egymás között.",
                                 "Around the room people are passing envelopes to one another.");
        p("ch2.lakatos2",        "Figyelj, itt egyszerűek a szabályok. Egy: amit látsz, nem láttad.\n"
                               + "Kettő: amit hallasz, nem hallottad. Három: amit kapsz, az jutalom a kemény munkáért.\n"
                               + "Négy: minden a Főnökért. Világos?",
                                 "Listen, the rules here are simple. One: what you see, you didn't see.\n"
                               + "Two: what you hear, you didn't hear. Three: what you get is a reward for hard work.\n"
                               + "Four: everything for the Boss. Clear?");
        p("ch2.q",               "Hogyan reagálsz?", "How do you react?");
        p("ch2.q.opt1",          "Teljesen világos. Hol az én borítékom?", "Perfectly clear. Where's my envelope?");
        p("ch2.q.opt2",          "És ha valaki lebukik?", "And what if someone gets caught?");
        p("ch2.q.opt3",          "Csendben maradsz és figyelsz.", "Stay quiet and watch.");
        p("ch2.q.ans1",          "Na EZ a hozzáállás! Fiúk, szeretni fogjátok ezt az embert!",
                                 "Now THAT'S the attitude! Boys, you are going to love this man!");
        p("ch2.q.ans2",          "Lebukás? Haha! Mi vagyunk a hatalom, kisfiam!",
                                 "Caught? Haha! We are the power, my boy!");
        p("ch2.q.ans3",          "A néma gyereknek az anyja sem érti a szavát...",
                                 "Even his own mother can't understand a silent child...");

        // ── 3. fejezet / Chapter 3 ─────────────────────────────────────────
        p("ch3.title",           "A Kongresszusi Próba", "The Congress Trial");
        p("ch3.quote",           "Ha nincs semmi gond, csinálunk valamit, hogy legyen.",
                                 "If there is no trouble, we will make some.");
        p("ch3.place",           "Helyszín: Hatalmas rendezvénycsarnok, ezrek a nézőtéren.",
                                 "Location: A vast event hall, thousands in the audience.");
        p("ch3.t1",              "Két hónap telt el. Szorgalmasan jártál a gyűlésekre, osztottad a szórólapokat,",
                                 "Two months have passed. You diligently attended the meetings, handed out the flyers,");
        p("ch3.t2",              "és megtanultad, mikor kell tapsolni. Most előléptettek: meghívást kaptál a Kongresszusra.",
                                 "and learned when to applaud. Now you have been promoted: you are invited to the Congress.");
        p("ch3.speaker1",        "Tisztelt Kongresszus! Kérjük, álljanak fel és köszöntsék a Nemzet Megmentőjét,\n"
                               + "a Haza Pajzsát, Kapzs Imre Miniszterelnök Urat!",
                                 "Honoured Congress! Please rise and welcome the Saviour of the Nation,\n"
                               + "the Shield of the Homeland, Prime Minister Kapzs Imre!");
        p("ch3.narr1",           "A tömeg felugrik. Te is.", "The crowd leaps to its feet. So do you.");
        p("ch3.kapzs1",          "Magyarok! Testvéreim! Mi vagyunk az utolsó bástya a... a... nos, mindenki ellen, aki nem mi vagyunk!",
                                 "Hungarians! My brothers! We are the last bastion against... against... well, against everyone who is not us!");
        p("ch3.narr2",           "A tömeg nevet. Te is.", "The crowd laughs. So do you.");
        p("ch3.kapzs2",          "Tudják, mit mondott nekem ma reggel a szakácsunk? Azt mondta: 'Miniszterelnök Úr, maga a legnagyobb!'\n"
                               + "És én mit válaszoltam? Azt mondtam: 'TUDOM!'",
                                 "Do you know what our cook said to me this morning? He said: 'Prime Minister, you are the greatest!'\n"
                               + "And what did I reply? I said: 'I KNOW!'");
        p("ch3.narr3",           "Mennydörgő taps. Te is tapsolsz.", "Thunderous applause. You applaud too.");
        p("ch3.narr4",           "Lakatos megtudta, hogy te is pályázol a választmányi helyre. Nem nézi jó szemmel a versenyt.\n"
                               + "Kihív egy \"baráti vitára\" – ami valójában nyilvános megalázási kísérlet.",
                                 "Lakatos has found out that you are also running for the committee seat. He does not take kindly to competition.\n"
                               + "He challenges you to a \"friendly debate\" – which is really an attempt at public humiliation.");

        // ── 1. bossfight / Boss fight 1 ────────────────────────────────────
        p("boss1.rule1",         "  Ha 3-szor ugyanazt a képességet használod,",
                                 "  If you use the same move three times in a row,");
        p("boss1.rule2",         "  Lakatos használni fogja a FELJELENTÉS képességét!",
                                 "  Lakatos will use his DENUNCIATION move!");
        p("boss1.skill",         "FELJELENTÉS", "DENUNCIATION");
        p("boss1.hit",           "Eltaláltad Lakatos Ervint! -20 HP", "You struck Lakatos Ervin! -20 HP");
        p("boss1.blocked",       "Kivédted Lakatos támadását!", "You blocked Lakatos's attack!");
        p("boss1.win",           "Legyőzted Lakatos Ervint!", "You defeated Lakatos Ervin!");

        // ── 4. fejezet / Chapter 4 ─────────────────────────────────────────
        p("ch4.title",           "Az Országos Választmány Árnyai", "Shadows of the National Committee");
        p("ch4.quote",           "A diplomácia nem arról szól, hogy mindig igent mondunk.",
                                 "Diplomacy is not about always saying yes.");
        p("ch4.place",           "Helyszín: Elegáns irodaépület, 5. emelet, panorámás kilátás.",
                                 "Location: An elegant office building, 5th floor, panoramic view.");
        p("ch4.t1",              "Feljutottál a Választmányba. A boríték most már vastagabb, az autó most már szolgálati.",
                                 "You have made it onto the Committee. The envelope is thicker now, the car now comes with the job.");
        p("ch4.t2",              "De a játékosok is veszélyesebbek.", "But the players are more dangerous too.");
        p("ch4.peteri1",         "%s! Hallottam magáról. Gyorsan emelkedik. Talán túl gyorsan.\n"
                               + "Tudja, mit mondanak a régi rókák?",
                                 "%s! I have heard about you. You are rising fast. Perhaps too fast.\n"
                               + "Do you know what the old foxes say?");
        p("ch4.peteri2",         "De ne aggódjon. Én segíteni akarok. Van itt egy... projekt. EU-s pályázat.\n"
                               + "800 millió forint. A papírmunka már megvan, csak... kell valaki, aki aláírja. Érti?",
                                 "But do not worry. I want to help. There is a... project here. An EU grant.\n"
                               + "800 million forints. The paperwork is all done, we just... need someone to sign it. Do you understand?");
        p("ch4.q",               "Mit válaszolsz?", "What do you say?");
        p("ch4.q.opt1",          "Mennyi az én részem?", "What's my cut?");
        p("ch4.q.opt2",          "Kinek a projektje ez valójában?", "Whose project is this really?");
        p("ch4.q.opt3",          "Ezt át kell gondolnom.", "I need to think this over.");
        p("ch4.q.ans1",          "10%. De ez csak az első üzlet.", "10%. And this is only the first deal.");
        p("ch4.q.ans2",          "Kíváncsi kis ember... Ez tetszik. De a nevek nem fontosak.",
                                 "A curious little man... I like that. But names are not important.");
        p("ch4.q.ans3",          "Gondolja át. De ne túl sokáig. Az idő pénz – szó szerint.",
                                 "Think it over. But not for too long. Time is money – literally.");

        // ── 5. fejezet / Chapter 5 ─────────────────────────────────────────
        p("ch5.title",           "A Parlamenti Útvesztő", "The Parliamentary Maze");
        p("ch5.quote",           "Egyetlen forint közpénzt sem hagyunk kárba veszni.",
                                 "We will not let a single forint of public money go to waste.");
        p("ch5.place",           "Helyszín: Parlament, folyosók és titkos szobák.",
                                 "Location: Parliament, corridors and secret rooms.");
        p("ch5.t1",              "Most már parlamenti képviselő vagy. Van irodád, van asszisztensed,",
                                 "You are a Member of Parliament now. You have an office, you have an assistant,");
        p("ch5.t2",              "és van egy hosszú lista arról, hogy kinek mivel tartozol.",
                                 "and you have a long list of who you owe what.");
        p("ch5.molnar1",         "Gratulálok a mandátumhoz, %s! Most már az elit része vagy.\n"
                               + "Na, nem az igazi elité, ahhoz még messze vagy. De a kapuban állsz.",
                                 "Congratulations on the seat, %s! You are part of the elite now.\n"
                               + "Well, not the real elite, you are still far from that. But you are standing at the gate.");
        p("ch5.narr1",           "Molnár átad egy borítékot.", "Molnár hands over an envelope.");
        p("ch5.molnar2",         "Ez a holnapi szavazás. Az EU-kritikus állásfoglalás. Te MELLETTE szavazol.\n"
                               + "Nem ELLENE, nem TARTÓZKODSZ. MELLETTE. Világos?",
                                 "This is tomorrow's vote. The EU-critical resolution. You vote FOR it.\n"
                               + "Not AGAINST, not ABSTAIN. FOR. Clear?");
        p("ch5.q",               "Mit reagálsz?", "How do you respond?");
        p("ch5.q.opt1",          "Persze. Hol kell aláírni?", "Of course. Where do I sign?");
        p("ch5.q.opt2",          "És ha esetleg a saját véleményem más?", "And what if my own opinion happens to differ?");
        p("ch5.q.opt3",          "Előbb elolvasom a javaslatot.", "I'll read the proposal first.");
        p("ch5.q.ans1",          "Látom, gyorsan tanul. Ez jó.", "I see you learn fast. That is good.");
        p("ch5.q.ans2",          "Vélemény? VÉLEMÉNY?! %s, maga vicces ember.",
                                 "Opinion? OPINION?! %s, you are a funny man.");
        p("ch5.q.ans3",          "Olvasás? Hm. Furcsa szokás. De legyen.",
                                 "Reading? Hm. A strange habit. But very well.");
        p("ch5.pre1",            "Megtudtad, hogy Dr. Péteri az előléptetésedért cserébe 50%-os részesedést",
                                 "You have learned that in exchange for your promotion Dr. Péteri wants a 50% share");
        p("ch5.pre2",            "akar minden jövőbeli üzletedből. Ideje tisztázni a viszonyokat.",
                                 "of every future deal you make. Time to settle where you stand.");

        // ── 2. bossfight / Boss fight 2 ────────────────────────────────────
        p("boss2.rule1",         "  Ha nem támadsz, 2 körönként -20 HP sebzést fog okozni",
                                 "  If you do not attack, every 2nd round you take -20 HP from");
        p("boss2.rule2",         "  Péteri MÉDIABOTRÁNY képessége!",
                                 "  Péteri's MEDIA SCANDAL move!");
        p("boss2.skill",         "MÉDIABOTRÁNY", "MEDIA SCANDAL");
        p("boss2.blocked",       "Péteri kivédte.", "Péteri blocked it.");
        p("boss2.hit",           "Péteri megütött! -20 HP", "Péteri hit you! -20 HP");
        p("boss2.win",           "Legyőzted Dr. Péterit!", "You defeated Dr. Péteri!");

        // ── 6. fejezet / Chapter 6 ─────────────────────────────────────────
        p("ch6.title",           "Az Országos Elnökség Kapujában", "At the Gates of the National Presidency");
        p("ch6.quote",           "A demokratikus jogrend nem ad felmentést a felelősség alól.",
                                 "The democratic rule of law grants no exemption from responsibility.");
        p("ch6.place",           "Helyszín: Elit vadászkastély, éjszaka, szivarfüst.",
                                 "Location: An elite hunting lodge, night, cigar smoke.");
        p("ch6.t1",              "Meghívást kaptál a \"belső körbe\". Ahova nem mindenki juthat be.",
                                 "You have been invited into the \"inner circle\". Where not everyone gets in.");
        p("ch6.t2",              "A szobában a párt legbefolyásosabb emberei ülnek bőrfotelekben.",
                                 "In the room the party's most influential men sit in leather armchairs.");
        p("ch6.kapzs1",          "Szóval te vagy %s. Hallottam rólad. Gyorsan mászol. Ez jó.\n"
                               + "De tudod, mi a különbség a mászó és a csillag között?",
                                 "So you are %s. I have heard about you. You climb fast. That is good.\n"
                               + "But do you know the difference between a climber and a star?");
        p("ch6.narr1",           "Mindenki téged néz.", "Everyone is looking at you.");
        p("ch6.kapzs2",          "A mászó bármikor leeshet. A csillag... a csillag örökké ragyog.\n"
                               + "Na persze, amíg MI ragyogtatjuk.",
                                 "A climber can fall at any time. A star... a star shines forever.\n"
                               + "As long as WE keep it shining, of course.");
        p("ch6.narr2",           "Mindenki nevet.", "Everyone laughs.");
        p("ch6.kapzs3",          "Figyelj, %s. Van egy kis... probléma.\n"
                               + "Az ellenzék túl hangos lett. Szükségünk van valakire, aki... nos, aki megoldja. Kreatívan. Érted, ugye?",
                                 "Listen, %s. There is a small... problem.\n"
                               + "The opposition has grown too loud. We need someone who... well, who solves it. Creatively. You understand, don't you?");
        p("ch6.q",               "Mit válaszolsz?", "What do you say?");
        p("ch6.q.opt1",          "Bármit, Főnök. Csak mondja, mit.", "Anything, Boss. Just say the word.");
        p("ch6.q.opt2",          "Milyen jellegű 'megoldásra' gondol?", "What kind of 'solution' do you have in mind?");
        p("ch6.q.opt3",          "*Meghajolsz, de nem szólsz semmit*", "*You bow, but say nothing*");
        p("ch6.q.ans1",          "NA EZ AZ! Ilyen embereket akarok! Gyere ide, ülj mellém!",
                                 "NOW THAT'S IT! These are the men I want! Come here, sit beside me!");
        p("ch6.q.ans2",          "Óvatos! Ez bölcs. De néha a bölcsesség lassít.",
                                 "Cautious! That is wise. But sometimes wisdom slows you down.");
        p("ch6.q.ans3",          "Hm. A néma kutya sosem harap. De nem is véd.",
                                 "Hm. A silent dog never bites. But it does not guard either.");
        p("ch6.kapzs4",          "Rendben. A részletekről később beszélhetünk.",
                                 "Very well. We can talk about the details later.");

        // ── 7. fejezet / Chapter 7 ─────────────────────────────────────────
        p("ch7.title",           "Az Elnökség Trónján", "On the Throne of the Presidency");
        p("ch7.quote",           "A törvény mindenkire egyformán vonatkozik.",
                                 "The law applies equally to everyone.");
        p("ch7.place",           "Helyszín: A párt központja, az elnökségi terem.",
                                 "Location: Party headquarters, the presidential chamber.");
        p("ch7.narr1",           "Most már az Országos Elnökség tagja vagy. A hatalom szaga érezhető – és kissé rothadt.\n"
                               + "De te már hozzászoktál.",
                                 "You are a member of the National Presidency now. You can smell the power – and it is slightly rotten.\n"
                               + "But you have grown used to it.");
        p("ch7.kapzs1",          "%s! Gyere, sétáljunk. Van valami, amit csak neked mondok el.",
                                 "%s! Come, let us walk. There is something I will tell only you.");
        p("ch7.t1",              "Kimentek a kertbe. Senki más nincs ott.",
                                 "You walk out into the garden. There is no one else there.");
        p("ch7.kapzs2",          "Tudod, én nem leszek örökké. Nem, nem, ne tiltakozz.\n"
                               + "Mindenki halandó. És nekem... nos, nekem szükségem van valakire, aki folytatja.\n"
                               + "Aki érti, hogyan működik ez az egész.",
                                 "You know, I will not be here forever. No, no, do not protest.\n"
                               + "Everyone is mortal. And I... well, I need someone to carry it on.\n"
                               + "Someone who understands how all of this works.");
        p("ch7.narr2",           "Kapzs Imre megáll és rádnéz.", "Kapzs Imre stops and looks at you.");
        p("ch7.kapzs3",          "Te lehetnél az, %s. De előbb... előbb bizonyítanod kell.\nVan egy utolsó teszt.",
                                 "That could be you, %s. But first... first you must prove yourself.\nThere is one last test.");
        p("ch7.q",               "Mit válaszolsz?", "What do you say?");
        p("ch7.q.opt1",          "Bármi lesz, kész vagyok rá.", "Whatever it is, I am ready for it.");
        p("ch7.q.opt2",          "Mi lenne ez a teszt?", "What would this test be?");
        p("ch7.q.opt3",          "Talán még nem vagyok készen...", "Perhaps I am not ready yet...");
        p("ch7.q.ans1",          "Bármi? Bármi egy veszélyes szó, kisfiam. De legyen.",
                                 "Anything? Anything is a dangerous word, my boy. But so be it.");
        p("ch7.q.ans2",          "Az egyetlen teszt, ami számít: le kell győznöd engem.",
                                 "The only test that matters: you must defeat me.");
        p("ch7.q.ans3",          "Hm. Az önismeret erény. De a gyávaság nem.",
                                 "Hm. Self-knowledge is a virtue. Cowardice is not.");

        // ── Vegso bossfight / Final boss fight ─────────────────────────────
        p("boss3.place",         "Helyszín: A párt titkos tanácsterme, éjfél.",
                                 "Location: The party's secret council chamber, midnight.");
        p("boss3.narr",          "Ez az a pillanat, amire minden eddig történt felkészített.\n"
                               + "Kapzs tudja, hogy te vagy az egyetlen, aki fenyegetést jelenthet rá.\n"
                               + "És te is tudod: csak egyikőtök maradhat a csúcson.",
                                 "This is the moment everything so far has prepared you for.\n"
                               + "Kapzs knows that you are the only one who could threaten him.\n"
                               + "And you know it too: only one of you can stay at the top.");
        p("boss3.header",        "  KÜLÖNLEGES KÉPESSÉGEK:", "  SPECIAL MOVES:");
        p("boss3.rule1",         "  Média Manipuláció  — minden 2. körben -15 HP neked",
                                 "  Media Manipulation — every 2nd round: -15 HP to you");
        p("boss3.rule2",         "  Hűséges Talpnyalók — 3 körönként +50 HP magának",
                                 "  Loyal Bootlickers  — every 3rd round: +50 HP to himself");
        p("boss3.rule3",         "  Végső Szónoklat    — HP alatt 50: dupla sebzés",
                                 "  Final Oration      — below 50 HP: double damage");
        p("boss3.trust",         "  ★ FŐNÖK BIZALMA aktív — Kapzs támadásai 30%-kal gyengébbek!",
                                 "  ★ THE BOSS'S TRUST is active — Kapzs's attacks are 30% weaker!");
        p("boss3.skill",         "MÉDIA MANIPULÁCIÓ", "MEDIA MANIPULATION");
        p("boss3.heal",          "Kapzs gyógyult! +50 HP", "Kapzs healed! +50 HP");
        p("boss3.blocked",       "Kapzs kivédte.", "Kapzs blocked it.");
        p("boss3.hit",           "Kapzs megütött! -%s HP", "Kapzs hit you! -%s HP");

        // ── Befejezes / Ending ─────────────────────────────────────────────
        p("end.narr1",           "Kapzs Imre \"egészségügyi okokból\" visszavonult. A párt új elnöke: %s. Te.",
                                 "Kapzs Imre has retired \"for health reasons\". The party's new president: %s. You.");
        p("end.t1",              "A tükör előtt állsz, az elnöki irodában. Az ablakon túl Budapest fényei.",
                                 "You stand before the mirror in the president's office. Beyond the window, the lights of Budapest.");
        p("end.you1",            "4 millió forint adósság. Ezzel kezdődött. És most... most itt vagyok.",
                                 "4 million forints of debt. That is how it started. And now... now here I am.");
        p("end.narr2",           "Valaki kopog.", "Someone knocks at the door.");
        p("end.unknown",         "Elnök úr? Van itt egy fiatal srác. Kovács Péter a neve.\n"
                               + "Azt mondja, tartozik a banknak, és... nos, bármire hajlandó.",
                                 "Mr. President? There is a young man here. Kovács Péter is his name.\n"
                               + "He says he owes the bank, and... well, he is willing to do anything.");

        // Vegso ellenorzes: a ket nyelv kulcsainak egyeznie kell.
        // Final check: both languages must carry exactly the same keys.
        if (!huSzoveg.keySet().equals(enSzoveg.keySet())) {
            java.util.Set<String> hianyzo = new java.util.LinkedHashSet<>(huSzoveg.keySet());
            hianyzo.removeAll(enSzoveg.keySet());
            java.util.Set<String> tobblet = new java.util.LinkedHashSet<>(enSzoveg.keySet());
            tobblet.removeAll(huSzoveg.keySet());
            throw new IllegalStateException(
                    "Lang: a nyelvek kulcsai elternek / language key sets differ. "
                  + "Csak magyarul / HU only: " + hianyzo + " | Csak angolul / EN only: " + tobblet);
        }
    }
}
