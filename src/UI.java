public class UI {

    private static final String RESET  = "\033[0m";
    private static final String BOLD   = "\033[1m";
    private static final String ITALIC = "\033[3m";
    private static final String GOLD   = "\033[33m";
    private static final String YELLOW = "\033[93m";
    private static final String RED    = "\033[31m";
    private static final String GREEN  = "\033[32m";
    private static final String CYAN   = "\033[36m";
    private static final String GRAY   = "\033[90m";
    private static final String WHITE  = "\033[97m";
    private static final String CREAM  = "\033[37m";

    /** Kozepre igazitas adott szelessegben / centre within a given width. */
    private static String kozep(String szoveg, int szelesseg) {
        int hiany = Math.max(0, szelesseg - szoveg.length());
        int bal = hiany / 2;
        return " ".repeat(bal) + szoveg + " ".repeat(hiany - bal);
    }

    /** Balra igazitas adott szelessegben / pad right to a given width. */
    private static String balra(String szoveg, int szelesseg) {
        return szoveg + " ".repeat(Math.max(0, szelesseg - szoveg.length()));
    }

    public static void cim() {
        String szoveg = "✦  " + Lang.t("ui.title") + "  ✦";
        System.out.println();
        System.out.println(GOLD + "════════════════════════════════════════" + RESET);
        System.out.println(BOLD + GOLD + kozep(szoveg, 40) + RESET);
        System.out.println(GOLD + "════════════════════════════════════════" + RESET);
        System.out.println();
    }

    public static void fejezet(int szam, String cim, String idezet) {
        System.out.println();
        System.out.println(GRAY + "────────────────────────────────────────" + RESET);
        System.out.println(BOLD + GOLD + "  ▸ " + szam + ". " + Lang.t("ui.chapter") + ": " + cim.toUpperCase() + " ◂" + RESET);
        if (idezet != null && !idezet.isEmpty()) {
            System.out.println(GRAY + ITALIC + "  \"" + idezet + "\"" + RESET);
        }
        System.out.println(GRAY + "────────────────────────────────────────" + RESET);
        System.out.println();
    }

    public static void parbeszed(String karakter, String szoveg) {
        System.out.println(BOLD + GOLD + "[ " + karakter.toUpperCase() + " ]" + RESET);
        for (String sor : szoveg.split("\n")) {
            System.out.println("  " + CREAM + sor + RESET);
        }
        System.out.println();
    }

    public static void narracio(String szoveg) {
        System.out.println(GRAY + ITALIC + "  * " + szoveg + " *" + RESET);
        System.out.println();
    }

    public static void menu(String kerdes, String... opciok) {
        System.out.println();
        int szelesseg = 38;
        System.out.println(CREAM + "┌─ " + kerdes + " " + "─".repeat(Math.max(1, szelesseg - kerdes.length() - 2)) + "┐" + RESET);
        for (int i = 0; i < opciok.length; i++) {
            String sor = "  " + BOLD + YELLOW + (i + 1) + ". " + RESET + YELLOW + opciok[i];
            System.out.println(CREAM + "│" + RESET + sor + RESET);
        }
        String also = "└─── " + Lang.t("ui.choose") + " (1-" + opciok.length + "): ";
        System.out.println(CREAM + also + "─".repeat(Math.max(1, szelesseg + 2 - also.length())) + "┘" + RESET);
        System.out.print(GOLD + "> " + RESET);
    }

    public static void hpBar(String nev, int jelenlegi, int max, boolean jatekos) {
        int barSzelesseg = 12;
        int teli = (int) Math.round((double) jelenlegi / max * barSzelesseg);
        teli = Math.max(0, Math.min(teli, barSzelesseg));
        int ures = barSzelesseg - teli;

        String szinTeli = jatekos ? GREEN : RED;
        String nevSzin  = jatekos ? CYAN  : "\033[91m";

        String bar = szinTeli + BOLD + "█".repeat(teli) + RESET + GRAY + "░".repeat(ures) + RESET;
        System.out.printf("  %s%-12s%s %s %s%d/%d%s%n",
                nevSzin, nev, RESET, bar, WHITE, Math.max(0, jelenlegi), max, RESET);
    }

    public static void boss(String nev) {
        System.out.println();
        System.out.println(RED + BOLD + "  ⚔  " + Lang.t("ui.bossfight") + ": " + nev.toUpperCase() + "  ⚔" + RESET);
        System.out.println(GRAY + "────────────────────────────────────────" + RESET);
    }

    public static void kepesseg(String nev, int sebzes) {
        System.out.println(RED + BOLD + "  ⚡ " + nev.toUpperCase() + "!" + RESET
                + (sebzes > 0 ? RED + "  -" + sebzes + " HP" + RESET : ""));
    }

    public static void harciEsemeny(String uzenet, boolean pozitiv) {
        String szin = pozitiv ? GREEN : RED;
        System.out.println(szin + "  " + uzenet + RESET);
    }

    public static void statusz(int xp, int lebukas, int szint) {
        System.out.println();
        System.out.println(GRAY + "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄" + RESET);
        System.out.println("  " + GOLD + "✦ " + Lang.t("ui.xp") + ": " + xp + RESET
                + "   " + RED + "⚠ " + Lang.t("ui.exposure") + ": " + lebukas + "%" + RESET
                + "   " + GRAY + "◈ " + Lang.t("ui.level") + ": " + szint + RESET);
        System.out.println(GRAY + "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄" + RESET);
        System.out.println();
    }

    public static void targy(String nev) {
        String tartalom = "  ★ " + Lang.t("ui.item") + ": " + nev;
        int belso = Math.max(34, tartalom.length() + 2);
        System.out.println();
        System.out.println(GOLD + "  ┌" + "─".repeat(belso) + "┐" + RESET);
        System.out.println(GOLD + "  │" + balra(tartalom, belso) + "│" + RESET);
        System.out.println(GOLD + "  └" + "─".repeat(belso) + "┘" + RESET);
        System.out.println();
    }

    public static void szintlepes(String rang) {
        String cim  = Lang.t("ui.levelup");
        String also = "  " + Lang.t("ui.newrank") + rang;
        int belso = Math.max(34, Math.max(cim.length(), also.length()) + 2);
        System.out.println();
        System.out.println(BOLD + YELLOW + "  ╔" + "═".repeat(belso) + "╗" + RESET);
        System.out.println(BOLD + YELLOW + "  ║" + kozep(cim, belso) + "║" + RESET);
        System.out.println(BOLD + YELLOW + "  ║" + balra(also, belso) + "║" + RESET);
        System.out.println(BOLD + YELLOW + "  ╚" + "═".repeat(belso) + "╝" + RESET);
        System.out.println();
    }

    public static void gyozelem(String nev, int xp, int lebukas,
                                boolean boritek1, boolean boritekKis, boolean lakatosAktaja,
                                boolean offshoreKod, boolean peteriDosszie,
                                boolean fonokBizalma, boolean parlamentiKulcs) {
        System.out.println();
        System.out.println(GOLD + "════════════════════════════════════════" + RESET);
        System.out.println(BOLD + GOLD + kozep(Lang.t("ui.congrats", nev.toUpperCase()), 40) + RESET);
        System.out.println(BOLD + GOLD + kozep(Lang.t("ui.president"), 40) + RESET);
        System.out.println(GOLD + "════════════════════════════════════════" + RESET);
        System.out.println();
        System.out.println(GOLD + "  " + balra(Lang.t("ui.finalxp"), 16) + WHITE + xp + RESET);
        System.out.println(GOLD + "  " + balra(Lang.t("ui.exposuremeter"), 16) + WHITE + lebukas + "%" + RESET);
        System.out.println();
        System.out.println(GOLD + "  " + Lang.t("ui.itemsheader") + RESET);
        if (boritek1)        System.out.println(YELLOW + "    - " + Lang.t("item.envelope1") + RESET);
        if (boritekKis)      System.out.println(YELLOW + "    - " + Lang.t("item.envelopeSmall") + RESET);
        if (lakatosAktaja)   System.out.println(YELLOW + "    - " + Lang.t("item.lakatosFile") + RESET);
        if (offshoreKod)     System.out.println(YELLOW + "    - " + Lang.t("item.offshore") + RESET);
        if (peteriDosszie)   System.out.println(YELLOW + "    - " + Lang.t("item.peteriDossier") + RESET);
        if (fonokBizalma)    System.out.println(YELLOW + "    - " + Lang.t("item.bossTrust") + RESET);
        if (parlamentiKulcs) System.out.println(YELLOW + "    - " + Lang.t("item.parliamentKey") + RESET);
        System.out.println();
        System.out.println(GOLD + "════════════════════════════════════════" + RESET);
    }

    public static void vereseg() {
        String szoveg = Lang.t("ui.defeat");
        int belso = Math.max(34, szoveg.length() + 2);
        System.out.println();
        System.out.println(RED + BOLD + "  ╔" + "═".repeat(belso) + "╗" + RESET);
        System.out.println(RED + BOLD + "  ║" + kozep(szoveg, belso) + "║" + RESET);
        System.out.println(RED + BOLD + "  ╚" + "═".repeat(belso) + "╝" + RESET);
        System.out.println();
    }

    public static void lebukas() {
        System.out.println();
        System.out.println(RED + BOLD + "════════════════════════════════════════" + RESET);
        System.out.println(RED + BOLD + "   " + Lang.t("ui.exposed") + RESET);
        System.out.println(RED + "   " + Lang.t("ui.pressfound") + RESET);
        System.out.println(RED + BOLD + "════════════════════════════════════════" + RESET);
        System.out.println();
    }

    public static void enter() {
        System.out.println(GRAY + "  " + Lang.t("ui.enter") + RESET);
    }

    public static void elvalaszto() {
        System.out.println(GRAY + "────────────────────────────────────────" + RESET);
    }
}
