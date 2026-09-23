import java.util.Scanner;
import java.util.Random;
import java.io.RandomAccessFile;
import java.io.IOException;

public class fityesz1_0 {
    public static boolean lebukasEllenorzes(int lebukas){
        if(lebukas >= 100){
            UI.lebukas();
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        Scanner sc = new Scanner(System.in);
        Lang.select(sc);
        jatek(sc);
    }

    public static void jatek(Scanner sc) throws IOException {

        Random rnd = new Random();

        RandomAccessFile raf = new RandomAccessFile("valasztasok.txt", "rw");

        UI.cim();
        System.out.println(Lang.t("intro.welcome"));
        System.out.println(Lang.t("intro.1"));
        System.out.println(Lang.t("intro.2"));
        System.out.println(Lang.t("intro.3"));
        UI.elvalaszto();
        UI.enter();
        sc.nextLine();


        int xp = 0;
        int lebukas = 0;
        int szint = 1;

        boolean elsoBoritek = false;
        boolean kisBoritek = false;
        boolean lakatosAktaja = false;
        boolean offshoreKod = false;
        boolean peteriDosszie = false;
        boolean fonokBizalma = false;
        boolean parlamentiKulcs = false;


        System.out.println(Lang.t("pro.title"));
        System.out.println(Lang.t("pro.quote"));
        UI.enter();
        sc.nextLine();

        System.out.println(Lang.t("pro.namePrompt"));
        String nev = sc.nextLine();
        System.out.println(Lang.t("pro.youAre", nev));
        System.out.println(Lang.t("pro.teacher"));
        System.out.println(Lang.t("pro.salary"));
        System.out.println(Lang.t("pro.assets"));
        System.out.println(Lang.t("pro.bank"));
        System.out.println(Lang.t("pro.politician"));
        UI.enter();
        sc.nextLine();

        UI.narracio(Lang.t("pro.phone"));
        UI.parbeszed(Lang.t("npc.unknown"), Lang.t("pro.call", nev));
        UI.enter();
        sc.nextLine();

        UI.fejezet(1, Lang.t("ch1.title"), Lang.t("ch1.quote"));
        System.out.println(Lang.t("ch1.place"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.lipoti1", nev));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.lipoti2"));

        UI.menu(Lang.t("ch1.q1"),
                Lang.t("ch1.q1.opt1"),
                Lang.t("ch1.q1.opt2"),
                Lang.t("ch1.q1.opt3"));

        int valasztas1 = sc.nextInt();
        sc.nextLine();

        if(valasztas1 == 1){
            xp += 15;
            lebukas += 10;
            UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.q1.ans1"));
        }
        else if(valasztas1 == 2){
            xp += 10;
            lebukas += 5;
            UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.q1.ans2"));
        }
        else{
            xp += 5;
            UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.q1.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("1. fejezet valasztas: " + valasztas1 + "\n");

        UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.lipoti3", nev));
        UI.enter();
        sc.nextLine();

        UI.narracio(Lang.t("ch1.envelope"));
        UI.enter();
        sc.nextLine();

        UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.lipoti4"));

        UI.menu(Lang.t("ch1.q2"),
                Lang.t("ch1.q2.opt1"),
                Lang.t("ch1.q2.opt2"),
                Lang.t("ch1.q2.opt3"));

        int valasztas2 = sc.nextInt();
        sc.nextLine();
        if(valasztas2 == 1){
            xp += 20;
            lebukas += 15;
            elsoBoritek = true;
            UI.targy(Lang.t("item.envelope1"));
            UI.narracio(Lang.t("ch1.q2.ans1"));
        }
        else if(valasztas2 == 2){
            xp += 10;
            lebukas += 5;
            UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.q2.ans2"));
        }
        else{
            lebukas -= 10;
            UI.parbeszed(Lang.t("npc.lipoti"), Lang.t("ch1.q2.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("2. valasztas: " + valasztas2 + "\n");

        UI.fejezet(2, Lang.t("ch2.title"), Lang.t("ch2.quote"));
        System.out.println(Lang.t("ch2.place"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch2.narr1"));
        UI.enter();
        sc.nextLine();

        UI.parbeszed(Lang.t("npc.lakatos"), Lang.t("ch2.lakatos1", nev));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch2.narr2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.lakatos"), Lang.t("ch2.lakatos2"));
        UI.enter();
        sc.nextLine();

        UI.menu(Lang.t("ch2.q"),
                Lang.t("ch2.q.opt1"),
                Lang.t("ch2.q.opt2"),
                Lang.t("ch2.q.opt3"));

        int valasztas3 = sc.nextInt();
        sc.nextLine();
        if(valasztas3 == 1){
            xp += 25;
            lebukas += 20;
            kisBoritek = true;
            UI.targy(Lang.t("item.envelopeSmall"));
            UI.parbeszed(Lang.t("npc.lakatos"), Lang.t("ch2.q.ans1"));
        }
        else if(valasztas3 == 2){
            xp += 10;
            lebukas += 5;
            UI.parbeszed(Lang.t("npc.lakatos"), Lang.t("ch2.q.ans2"));
        }
        else{
            xp += 5;
            UI.parbeszed(Lang.t("npc.lakatos"), Lang.t("ch2.q.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("3. valasztas: " + valasztas3 + "\n");


        UI.fejezet(3, Lang.t("ch3.title"), Lang.t("ch3.quote"));
        System.out.println(Lang.t("ch3.place"));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("ch3.t1"));
        System.out.println(Lang.t("ch3.t2"));
        UI.enter();
        sc.nextLine();

        UI.parbeszed(Lang.t("npc.speaker"), Lang.t("ch3.speaker1"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch3.narr1"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch3.kapzs1"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch3.narr2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch3.kapzs2"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch3.narr3"));

        UI.narracio(Lang.t("ch3.narr4"));

        if(xp >= 50){
            szint = 2;
            UI.szintlepes(Lang.t("rank.localMember"));
        }

        boolean boss1Verve = false;

        while(!boss1Verve){

            int playerHp = 100;
            int bossHp = 80;

            int elozo = 0;
            int ugyanaz = 0;

            UI.boss(Lang.t("npc.lakatos"));
            System.out.println(Lang.t("boss1.rule1"));
            System.out.println(Lang.t("boss1.rule2"));
            System.out.println();

            while(playerHp > 0 && bossHp > 0){

                UI.hpBar(Lang.t("ui.you"), playerHp, 100, true);
                UI.hpBar(Lang.t("npc.lakatos.short"), bossHp, 80, false);
                UI.menu(Lang.t("fight.tactic"), Lang.t("fight.attack"), Lang.t("fight.defend"));

                int player = sc.nextInt();
                int boss = rnd.nextInt(2) + 1;

                if(player == elozo){
                    ugyanaz++;
                } else {
                    ugyanaz = 1;
                }
                elozo = player;

                if(ugyanaz == 3){
                    UI.kepesseg(Lang.t("boss1.skill"), 30);
                    playerHp -= 30;
                }

                if(player == 1){
                    if(boss == 1){
                        UI.harciEsemeny(Lang.t("fight.bothAttacked"), false);
                        playerHp -= 10;
                        bossHp -= 20;
                    } else {
                        UI.harciEsemeny(Lang.t("boss1.hit"), true);
                        bossHp -= 20;
                    }
                } else {
                    if(boss == 1){
                        UI.harciEsemeny(Lang.t("boss1.blocked"), true);
                    } else {
                        UI.harciEsemeny(Lang.t("fight.bothDefended"), false);
                    }
                }
            }

            if(bossHp <= 0){
                boss1Verve = true;
                xp += 50;
                lakatosAktaja = true;
                UI.harciEsemeny(Lang.t("boss1.win"), true);
                UI.targy(Lang.t("item.lakatosFile"));
            } else {
                UI.vereseg();
                UI.menu(Lang.t("fight.continue"), Lang.t("fight.retry"), Lang.t("fight.quit"));
                int ujra = sc.nextInt();
                if(ujra == 2){
                    System.out.println(Lang.t("fight.gameover"));
                    raf.close(); sc.close();
                    return;
                }
            }
        }

        UI.statusz(xp, lebukas, szint);

        UI.fejezet(4, Lang.t("ch4.title"), Lang.t("ch4.quote"));
        System.out.println(Lang.t("ch4.place"));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("ch4.t1"));
        System.out.println(Lang.t("ch4.t2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.peteri"), Lang.t("ch4.peteri1", nev));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.peteri"), Lang.t("ch4.peteri2"));
        UI.enter();
        sc.nextLine();

        UI.menu(Lang.t("ch4.q"),
                Lang.t("ch4.q.opt1"),
                Lang.t("ch4.q.opt2"),
                Lang.t("ch4.q.opt3"));

        int valasztas4 = sc.nextInt();
        sc.nextLine();
        if(valasztas4 == 1){
            xp += 30;
            lebukas += 30;
            offshoreKod = true;
            UI.targy(Lang.t("item.offshore"));
            UI.parbeszed(Lang.t("npc.peteri"), Lang.t("ch4.q.ans1"));
        }
        else if(valasztas4 == 2){
            xp += 15;
            lebukas += 10;
            UI.parbeszed(Lang.t("npc.peteri"), Lang.t("ch4.q.ans2"));
        }
        else{
            xp += 5;
            lebukas += 5;
            UI.parbeszed(Lang.t("npc.peteri"), Lang.t("ch4.q.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("4. valasztas: " + valasztas4 + "\n");


        UI.fejezet(5, Lang.t("ch5.title"), Lang.t("ch5.quote"));
        System.out.println(Lang.t("ch5.place"));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("ch5.t1"));
        System.out.println(Lang.t("ch5.t2"));
        UI.enter();
        sc.nextLine();

        UI.parbeszed(Lang.t("npc.molnar"), Lang.t("ch5.molnar1", nev));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch5.narr1"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.molnar"), Lang.t("ch5.molnar2"));
        UI.enter();
        sc.nextLine();

        UI.menu(Lang.t("ch5.q"),
                Lang.t("ch5.q.opt1"),
                Lang.t("ch5.q.opt2"),
                Lang.t("ch5.q.opt3"));

        int valasztas5 = sc.nextInt();
        sc.nextLine();
        if(valasztas5 == 1){
            xp += 20;
            lebukas += 15;
            UI.parbeszed(Lang.t("npc.molnar"), Lang.t("ch5.q.ans1"));
        }
        else if(valasztas5 == 2){
            xp += 10;
            lebukas += 20;
            UI.parbeszed(Lang.t("npc.molnar"), Lang.t("ch5.q.ans2", nev));
        }
        else{
            xp += 5;
            UI.parbeszed(Lang.t("npc.molnar"), Lang.t("ch5.q.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("5. valasztas: " + valasztas5 + "\n");

        System.out.println(Lang.t("ch5.pre1"));
        System.out.println(Lang.t("ch5.pre2"));
        System.out.println();

        UI.boss(Lang.t("npc.peteri"));
        System.out.println(Lang.t("boss2.rule1"));
        System.out.println(Lang.t("boss2.rule2"));
        System.out.println();

        boolean boss2Verve = false;

        while(!boss2Verve){

            int playerHp = 100;
            int bossHp = 80;
            int kor = 0;

            while(playerHp > 0 && bossHp > 0){

                kor++;

                UI.hpBar(Lang.t("ui.you"), playerHp, 100, true);
                UI.hpBar(Lang.t("npc.peteri.short"), bossHp, 80, false);
                UI.menu(Lang.t("fight.tactic"), Lang.t("fight.attack"), Lang.t("fight.defend"));

                int player = sc.nextInt();
                int boss = rnd.nextInt(2) + 1;

                if(kor % 2 == 0 && player != 1){
                    UI.kepesseg(Lang.t("boss2.skill"), 20);
                    playerHp -= 20;
                }

                if(player == 1 && boss == 2){
                    UI.harciEsemeny(Lang.t("boss2.blocked"), false);
                } else if(player == 1 && boss == 1){
                    UI.harciEsemeny(Lang.t("fight.bothAttacked"), false);
                    playerHp -= 10;
                    bossHp -= 10;
                } else if(player == 2 && boss == 1){
                    UI.harciEsemeny(Lang.t("boss2.hit"), false);
                    playerHp -= 20;
                } else {
                    UI.harciEsemeny(Lang.t("fight.bothDefended"), false);
                }

                if(player == 1 && boss != 2){
                    bossHp -= 25;
                }
            }

            if(playerHp <= 0){
                UI.vereseg();
                UI.menu(Lang.t("fight.continue"), Lang.t("fight.again"), Lang.t("fight.quit"));
                int ujra = sc.nextInt();
                if(ujra == 2){
                    raf.close(); sc.close();
                    return;
                }
            } else {
                boss2Verve = true;
                xp += 80;
                peteriDosszie = true;
                UI.harciEsemeny(Lang.t("boss2.win"), true);
                UI.targy(Lang.t("item.peteriDossier"));
            }
        }

        UI.statusz(xp, lebukas, szint);

        UI.fejezet(6, Lang.t("ch6.title"), Lang.t("ch6.quote"));
        System.out.println(Lang.t("ch6.place"));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("ch6.t1"));
        System.out.println(Lang.t("ch6.t2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.kapzs1", nev));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch6.narr1"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.kapzs2"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch6.narr2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.kapzs3", nev));
        UI.enter();
        sc.nextLine();

        UI.menu(Lang.t("ch6.q"),
                Lang.t("ch6.q.opt1"),
                Lang.t("ch6.q.opt2"),
                Lang.t("ch6.q.opt3"));

        int valasztas6 = sc.nextInt();
        sc.nextLine();
        if(valasztas6 == 1){
            xp += 50;
            lebukas += 40;
            fonokBizalma = true;
            UI.targy(Lang.t("item.bossTrust"));
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.q.ans1"));
        }
        else if(valasztas6 == 2){
            xp += 25;
            lebukas += 15;
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.q.ans2"));
        }
        else{
            xp += 10;
            lebukas += 5;
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.q.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("6. valasztas: " + valasztas6 + "\n");

        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch6.kapzs4"));
        UI.enter();
        sc.nextLine();

        UI.fejezet(7, Lang.t("ch7.title"), Lang.t("ch7.quote"));
        System.out.println(Lang.t("ch7.place"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch7.narr1"));
        UI.enter();
        sc.nextLine();

        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.kapzs1", nev));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("ch7.t1"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.kapzs2"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("ch7.narr2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.kapzs3", nev));
        UI.enter();
        sc.nextLine();

        UI.menu(Lang.t("ch7.q"),
                Lang.t("ch7.q.opt1"),
                Lang.t("ch7.q.opt2"),
                Lang.t("ch7.q.opt3"));

        int valasztas7 = sc.nextInt();
        sc.nextLine();
        if(valasztas7 == 1){
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.q.ans1"));
        }
        else if(valasztas7 == 2){
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.q.ans2"));
        }
        else{
            UI.parbeszed(Lang.t("npc.kapzs"), Lang.t("ch7.q.ans3"));
        }
        UI.enter();
        sc.nextLine();

        if(lebukasEllenorzes(lebukas)) return;

        raf.seek(raf.length());
        raf.writeBytes("7. valasztas: " + valasztas7 + "\n");

        System.out.println(Lang.t("boss3.place"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("boss3.narr"));

        UI.boss(Lang.t("npc.kapzs"));
        System.out.println(Lang.t("boss3.header"));
        System.out.println(Lang.t("boss3.rule1"));
        System.out.println(Lang.t("boss3.rule2"));
        System.out.println(Lang.t("boss3.rule3"));
        System.out.println();
        if(fonokBizalma){
            System.out.println(Lang.t("boss3.trust"));
        }
        System.out.println();

        boolean vegsoBoss = false;

        while(!vegsoBoss){

            int playerHp = 150;
            int bossHp = 170;
            int kor = 0;

            while(playerHp > 0 && bossHp > 0){

                kor++;

                UI.hpBar(Lang.t("ui.you"), playerHp, 150, true);
                UI.hpBar(Lang.t("npc.kapzs"), bossHp, 170, false);
                UI.menu(Lang.t("fight.tactic"), Lang.t("fight.attack"), Lang.t("fight.defend"));

                int player = sc.nextInt();
                int boss = rnd.nextInt(2) + 1;

                if(kor % 2 == 0){
                    int sebzes = fonokBizalma ? 10 : 15;
                    UI.kepesseg(Lang.t("boss3.skill"), sebzes);
                    playerHp -= sebzes;
                }

                if(kor % 3 == 0){
                    bossHp = Math.min(bossHp + 50, 300);
                    UI.harciEsemeny(Lang.t("boss3.heal"), false);
                }

                if(player == 1 && boss == 2){
                    UI.harciEsemeny(Lang.t("boss3.blocked"), false);
                    int sebzes = (bossHp <= 50) ? 20 : 35;
                    bossHp -= sebzes;
                } else if(player == 2 && boss == 1){
                    int sebzes = (bossHp <= 50) ? 60 : 30;
                    UI.harciEsemeny(Lang.t("boss3.hit", sebzes), false);
                    playerHp -= sebzes;
                } else if(player == 1 && boss == 1){
                    UI.harciEsemeny(Lang.t("fight.bothAttacked"), false);
                    playerHp -= 10;
                    bossHp -= 10;
                } else {
                    UI.harciEsemeny(Lang.t("fight.bothDefended"), false);
                }
            }

            if(playerHp <= 0){
                UI.vereseg();
                UI.menu(Lang.t("fight.continue"), Lang.t("fight.again"), Lang.t("fight.quit"));
                int ujra = sc.nextInt();
                if(ujra == 2){
                    raf.close(); sc.close();
                    return;
                }
            } else {
                vegsoBoss = true;
                xp += 120;
                parlamentiKulcs = true;
            }
        }

        UI.narracio(Lang.t("end.narr1", nev));
        UI.enter();
        sc.nextLine();
        System.out.println(Lang.t("end.t1"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.you"), Lang.t("end.you1"));
        UI.enter();
        sc.nextLine();
        UI.narracio(Lang.t("end.narr2"));
        UI.enter();
        sc.nextLine();
        UI.parbeszed(Lang.t("npc.unknown"), Lang.t("end.unknown"));
        UI.enter();
        sc.nextLine();

        if(lebukas < 100){
            UI.gyozelem(nev, xp, lebukas,
                    elsoBoritek, kisBoritek, lakatosAktaja,
                    offshoreKod, peteriDosszie, fonokBizalma, parlamentiKulcs);
        }

        raf.close();
        sc.close();
    }
}
