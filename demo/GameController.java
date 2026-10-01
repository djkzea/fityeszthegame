package com.example.demo;

import java.util.Random;
import javafx.stage.Stage;

public class GameController {

    private final GameUI ui;
    private GameState state;
    private final Stage stage;

    private final Random random = new Random();

    private final java.util.List<GameUI.SummaryDecision> chapterDecisions = new java.util.ArrayList<>();

    private void recordDecision(String questionKey, String answerText, int xpDelta, int exposureDelta) {

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
    }

    // Boss fight state
    private int previousMove = 0;
    private int sameMove = 0;
    private int peteriPassiveRounds = 0;

    public GameController(Stage stage) {
        this.stage = stage;
        this.state = new GameState();
        this.ui = new GameUI();

        ui.setOnGameRestart(this::restartGame);

        showLanguageSelection();
    }

    // ==================================================
    // RESTART (early ending button / header RESTART button)
    // ==================================================

    /**
     * Returns a step that shows the end-of-chapter summary card and, when the
     * player presses Continue, runs "next". The decisions recorded with
     * recordDecision() since the last summary are listed on the card.
     */
    private Runnable chapterSummary(int chapter, Runnable next) {

        return () -> {

            ui.clearChoices();

            String nextUp = chapter < 7
                    ? Lang.t("ui.nextUp") + " " + Lang.t("ui.chapter") + " " + (chapter + 1)
                    + " · " + Lang.t("ch" + (chapter + 1) + ".title")
                    : Lang.t("ui.nextUp") + " " + Lang.t("ui.theEnding");

            ui.showChapterSummary(
                    chapter,
                    Lang.t("ch" + chapter + ".title"),
                    chapterDecisions,
                    nextUp
            );

            chapterDecisions.clear();

            ui.addChoice(1, Lang.t("ui.enter"), next);
        };
    }

    /** Wraps a "next step" so the "Acquired" card is shown first. */
    private Runnable withItem(String itemKey, Runnable next) {
        return () -> ui.showItemAcquired(Lang.t(itemKey), next);
    }

    private void restartGame() {

        state = new GameState();

        chapterDecisions.clear();

        previousMove = 0;
        sameMove = 0;
        peteriPassiveRounds = 0;

        ui.resetForNewGame();
        ui.updateStats(state);

        showStartScreen();
    }

    // ==================================================
    // START SCREEN
    // ==================================================


    public void showLanguageSelection() {

        ui.showDialogue(
                "LANGUAGE / NYELV",
                "Please select your language / Válaszd ki a nyelvet:"
        );

        ui.clearChoices();

        ui.addChoice(
                1,
                "Magyar",
                () -> {
                    Lang.set("hu");
                    ui.updateLanguage();
                    showStartScreen();
                }
        );

        ui.addChoice(
                2,
                "English",
                () -> {
                    Lang.set("en");
                    ui.updateLanguage();
                    showStartScreen();
                }
        );
    }

    private void showStartScreen() {
        ui.clearChoices();

        ui.hideRestartButton();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("intro.welcome") + "\n\n"
                        + Lang.t("intro.1") + "\n\n"
                        + Lang.t("intro.2") + "\n\n"
                        + Lang.t("intro.3")
        );

        // the button itself is the name field - no popup window
        ui.addTextChoice(
                1,
                Lang.t("pro.namePrompt"),
                24,
                name -> {
                    state.setName(name);
                    ui.updateStats(state);

                    showPrologue();
                }
        );

        ui.updateStats(state);
    }

    // ==================================================
    // PROLOGUE
    // ==================================================

    private void showPrologue() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.you"),
                Lang.t("pro.youAre", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::prologueTeacher);
    }

    private void prologueTeacher() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("pro.teacher") + "\n\n"
                        + Lang.t("pro.salary") + "\n\n"
                        + Lang.t("pro.assets")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::prologueBank);
    }

    private void prologueBank() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("pro.bank") + "\n\n"
                        + Lang.t("pro.politician")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::prologuePhone);
    }

    private void prologuePhone() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("pro.phone"),
                Lang.t("pro.call", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::chapter1);
    }

    // ==================================================
    // CHAPTER 1 - RECRUITMENT
    // ==================================================

    private void chapter1() {
        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch1_cafe.png"
        );
        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/lipoti.png",
                Lang.t("npc.lipoti"),
                Lang.t("npc.lipoti.role")
        );

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch1.title") + "\n\n"
                        + Lang.t("ch1.quote") + "\n\n"
                        + Lang.t("ch1.place")
        );

        ui.addChoice(
                1, Lang.t("ui.enter"),
                this::lipotiIntroduction
        );
    }

    private void lipotiIntroduction() {
        ui.clearChoices();

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/lipoti.png"
        );

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/lipoti.png",
                Lang.t("npc.lipoti"),
                Lang.t("npc.lipoti.role")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::lipotiOffer
        );
    }

    private void lipotiOffer() {
        ui.clearChoices();

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/lipoti2.png",
                Lang.t("npc.lipoti"),
                Lang.t("npc.lipoti.role")
        );

        ui.showDialogue(
                Lang.t("npc.lipoti"),
                Lang.t("ch1.lipoti2")
        );

        ui.addChoice(
                1,
                Lang.t("ch1.q1.opt1"),
                () -> chapter1Choice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch1.q1.opt2"),
                () -> chapter1Choice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch1.q1.opt3"),
                () -> chapter1Choice(3)
        );
    }

    private void chapter1Choice(int choice) {
        ui.clearChoices();

        int xpDelta;
        int exposureDelta;

        if (choice == 1) {
            xpDelta = 15;
            exposureDelta = 10;

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans1")
            );
        } else if (choice == 2) {
            xpDelta = 10;
            exposureDelta = 5;

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans2")
            );
        } else {
            xpDelta = 5;
            exposureDelta = 0;

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans3")
            );
        }

        state.addXp(xpDelta);
        state.addExposure(exposureDelta);
        recordDecision("ch1.q1", Lang.t("ch1.q1.opt" + choice), xpDelta, exposureDelta);

        ui.updateStats(state);

        ui.addChoice(1, Lang.t("ui.enter"), this::lipotiEnvelopeIntroduction);
    }

    private void lipotiEnvelopeIntroduction() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lipoti"),
                Lang.t("ch1.lipoti3", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::showEnvelope);
    }

    private void showEnvelope() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lipoti"),
                Lang.t("ch1.envelope")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::envelopeOffer);
    }

    private void envelopeOffer() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lipoti"),
                Lang.t("ch1.lipoti4")
        );

        ui.addChoice(
                1,
                Lang.t("ch1.q2.opt1"),
                () -> envelopeChoice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch1.q2.opt2"),
                () -> envelopeChoice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch1.q2.opt3"),
                () -> envelopeChoice(3)
        );
    }

    private void envelopeChoice(int choice) {
        ui.clearChoices();

        int xpDelta;
        int exposureDelta;

        if (choice == 1) {
            xpDelta = 20;
            exposureDelta = 15;
            state.setFirstEnvelope(true);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans1")
            );
        } else if (choice == 2) {
            xpDelta = 10;
            exposureDelta = 5;

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans2")
            );
        } else {
            xpDelta = 0;
            exposureDelta = -10;

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans3")
            );
        }

        state.addXp(xpDelta);
        state.addExposure(exposureDelta);
        recordDecision("ch1.q2", Lang.t("ch1.q2.opt" + choice), xpDelta, exposureDelta);

        ui.updateStats(state);

        final boolean gotItem = (choice == 1);

        ui.addChoice(1, Lang.t("ui.enter"), () -> {

            Runnable summary = () -> {
                ui.clearChoices();

                ui.showChapterSummary(
                        1,
                        Lang.t("ch1.title"),
                        chapterDecisions,
                        Lang.t("ui.nextUp") + " " + Lang.t("ui.chapter") + " 2 · " + Lang.t("ch2.title")
                );

                chapterDecisions.clear();

                ui.addChoice(1, Lang.t("ui.enter"), this::chapter2);
            };

            if (gotItem) {
                ui.showItemAcquired(Lang.t("item.envelope1"), summary);
            } else {
                summary.run();
            }
        });
    }

    // ==================================================
    // CHAPTER 2 - FIRST MEETING
    // ==================================================

    private void chapter2() {
        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch2_hall.png"
        );
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch2.title") + "\n\n"
                        + Lang.t("ch2.quote") + "\n\n"
                        + Lang.t("ch2.place")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::chapter2Inside);
    }

    private void chapter2Inside() {
        ui.clearChoices();

        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch2.narr1") + "\n\n"
                        + Lang.t("ch2.narr2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosIntroduction);
    }

    private void lakatosIntroduction() {
        ui.clearChoices();

        ui.showCharacter("/com/example/demo/fityesz_art/characters/lakatoservin.png");

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("ch2.lakatos1", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosRules);
    }

    private void lakatosRules() {
        ui.clearChoices();
        ui.showCharacter("/com/example/demo/fityesz_art/characters/lakatoservin2.png");

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("ch2.lakatos2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosChoice);
    }

    private void lakatosChoice() {
        ui.clearChoices();
        ui.showCharacter("/com/example/demo/fityesz_art/characters/lakatoservin.png");

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("ch2.q")
        );

        ui.addChoice(
                1,
                Lang.t("ch2.q.opt1"),
                () -> lakatosChoiceResult(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch2.q.opt2"),
                () -> lakatosChoiceResult(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch2.q.opt3"),
                () -> lakatosChoiceResult(3)
        );
    }

    private void lakatosChoiceResult(int choice) {
        ui.clearChoices();

        if (choice == 1) {
            state.addXp(25);
            state.addExposure(20);
            state.setSmallEnvelope(true);

            ui.showDialogue(
                    Lang.t("npc.lakatos"),
                    Lang.t("ch2.q.ans1")
            );
        } else if (choice == 2) {
            state.addXp(10);
            state.addExposure(5);

            ui.showDialogue(
                    Lang.t("npc.lakatos"),
                    Lang.t("ch2.q.ans2")
            );
        } else {
            state.addXp(5);

            ui.showDialogue(
                    Lang.t("npc.lakatos"),
                    Lang.t("ch2.q.ans3")
            );
        }

        ui.updateStats(state);

        int[] xpByChoice = {25, 10, 5};
        int[] exposureByChoice = {20, 5, 0};
        recordDecision("ch2.q", Lang.t("ch2.q.opt" + choice), xpByChoice[choice - 1], exposureByChoice[choice - 1]);

        Runnable afterLakatosTalk = chapterSummary(2, this::chapter3);

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                choice == 1 ? withItem("item.envelopeSmall", afterLakatosTalk) : afterLakatosTalk
        );
    }

    // ==================================================
    // CHAPTER 3 - CONGRESS
    // ==================================================

    private void chapter3() {
        ui.clearChoices();

        ui.showImage("/com/example/demo/fityesz_art/bg/ch3_hall.png");
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch3.title") + "\n\n"
                        + Lang.t("ch3.quote") + "\n\n"
                        + Lang.t("ch3.place")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::congressStory);
    }

    private void congressStory() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.t1") + "\n\n"
                        + Lang.t("ch3.t2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::congressSpeech);
    }

    private void congressSpeech() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.speaker"),
                Lang.t("ch3.speaker1")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::congressCrowd);
    }

    private void congressCrowd() {
        ui.clearChoices();

        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.narr1")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech1);
    }

    private void kapzsSpeech1() {
        ui.clearChoices();

        ui.showCharacter("/com/example/demo/fityesz_art/characters/kapzsimre.png");


        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch3.kapzs1")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech2);
    }

    private void kapzsSpeech2() {
        ui.clearChoices();

        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.narr2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech3);
    }

    private void kapzsSpeech3() {
        ui.clearChoices();

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/kapzsimre.png"
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch3.kapzs2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::congressEnd);
    }

    private void congressEnd() {
        ui.clearChoices();

        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.narr3") + "\n\n"
                        + Lang.t("ch3.narr4")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::bossIntroduction);
    }

    // ==================================================
    // BOSS INTRODUCTION
    // ==================================================

    private void bossIntroduction() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("boss1.rule1") + "\n"
                        + Lang.t("boss1.rule2")
        );

        ui.addChoice(
                1,
                Lang.t("fight.continue"),
                this::startLakatosBoss
        );
    }

    // ==================================================
    // LAKATOS BOSS FIGHT
    // ==================================================

    private void startLakatosBoss() {
        state.resetBossFight();

        previousMove = 0;
        sameMove = 0;

        ui.clearChoices();

        ui.clearBossAbilities();
        ui.addBossAbility(Lang.t("boss1.skill"), Lang.t("boss1.ab.desc"));

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/lakatoservin.png"
        );

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("boss1.skill")
        );

        ui.showBossFight(
                Lang.t("npc.lakatos"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        addBossButtons();
    }

    private void addBossButtons() {
        ui.clearChoices();

        ui.showBossFight(
                Lang.t("npc.lakatos"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        ui.addChoice(
                1,
                Lang.t("fight.attack"),
                () -> bossMove(1)
        );

        ui.addChoice(
                2,
                Lang.t("fight.defend"),
                () -> bossMove(2)
        );
    }

    private void bossMove(int playerMove) {

        // Count consecutive identical player moves.
        if (playerMove == previousMove) {
            sameMove++;
        } else {
            sameMove = 1;
        }

        previousMove = playerMove;

        // 1 = attack, 2 = defend
        int bossMove = random.nextInt(2) + 1;

        String result;

        // If the player repeats the same move three times,
        // Lakatos uses FELJELENTÉS.
        if (sameMove >= 3) {

            state.setPlayerHp(
                    state.getPlayerHp() - 30
            );

            result = Lang.t("boss1.skill") + "\n\n"
                    + Lang.t("boss1.hit");
        }

        // Player attacks.
        else if (playerMove == 1) {

            if (bossMove == 1) {
                // Both attack.
                state.setPlayerHp(
                        state.getPlayerHp() - 10
                );

                state.setBossHp(
                        state.getBossHp() - 20
                );

                result = Lang.t("fight.bothAttacked");
            } else {
                // Boss defends, player still deals damage.
                state.setBossHp(
                        state.getBossHp() - 20
                );

                result = Lang.t("boss1.hit");
            }
        }

        // Player defends.
        else {

            if (bossMove == 1) {
                // Player blocks the boss attack.
                result = Lang.t("boss1.blocked");
            } else {
                // Both defend.
                result = Lang.t("fight.bothDefended");
            }
        }

        // angry pose when FELJELENTÉS lands
        ui.showCharacter(sameMove >= 3
                ? "/com/example/demo/fityesz_art/characters/lakatoservin2.png"
                : "/com/example/demo/fityesz_art/characters/lakatoservin.png");

        ui.updateBossHp(
                Lang.t("npc.lakatos"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                result
        );

        if (state.getBossHp() <= 0) {
            lakatosDefeated();
            return;
        }

        if (state.getPlayerHp() <= 0) {
            playerDefeated();
            return;
        }

        addBossButtons();

    }

    // ==================================================
    // LAKATOS DEFEATED
    // ==================================================

    private void lakatosDefeated() {
        // Remove boss HP screen
        ui.hideBossFight();

        state.addXp(50);
        state.setLakatosFile(true);
        recordDecision("sum.boss", Lang.t("boss1.win"), 50, 0);

        ui.updateStats(state);
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("boss1.win")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                withItem("item.lakatosFile", chapterSummary(3, this::chapter4))
        );
    }

    // ==================================================
    // PLAYER DEFEATED
    // ==================================================

    private void playerDefeated() {
        ui.hideBossFight();
        ui.clearChoices();
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ui.defeat")
        );

        ui.addChoice(
                1,
                Lang.t("fight.retry"),
                this::startLakatosBoss
        );

        ui.addChoice(
                2,
                Lang.t("fight.quit"),
                this::restartGame
        );
    }

    // ==================================================
    // AFTER LAKATOS
    // ==================================================

    // ==================================================
// CHAPTER 4 - ORSZÁGOS VÁLASZTMÁNY ÁRNYAI
// ==================================================

    private void chapter4() {
        ui.clearChoices();
        ui.showImage("/com/example/demo/fityesz_art/bg/ch4_office.png");
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch4.title") + "\n\n"
                        + Lang.t("ch4.quote") + "\n\n"
                        + Lang.t("ch4.place")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter4Meeting
        );
    }

    // ==================================================
// CHAPTER 4 - STORY
// ==================================================

    private void chapter4Meeting() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch4.t1") + "\n\n"
                        + Lang.t("ch4.t2")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter4Peteri
        );
    }

    private void chapter4Peteri() {
        ui.clearChoices();
        ui.showCharacter("/com/example/demo/fityesz_art/characters/drpeterikatalin.png");

        ui.showDialogue(
                Lang.t("npc.peteri"),
                Lang.t("ch4.t1") + "\n\n"
                        + Lang.t("ch4.t2")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter4Conversation
        );
    }

    private void chapter4Conversation() {
        ui.clearChoices();
        ui.showCharacter("/com/example/demo/fityesz_art/characters/drpeterikatalin2.png");

        ui.showDialogue(
                Lang.t("npc.peteri"),
                Lang.t("ch4.peteri1", state.getName()) + "\n\n"
                        + Lang.t("ch4.peteri2") + "\n\n"
                        + Lang.t("ch4.q")
        );

        ui.addChoice(
                1,
                Lang.t("ch4.q.opt1"),
                () -> chapter4Choice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch4.q.opt2"),
                () -> chapter4Choice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch4.q.opt3"),
                () -> chapter4Choice(3)
        );
    }


    private void chapter4Choice(int choice) {

        ui.clearChoices();

        if (choice == 1) {

            state.addXp(25);
            state.addExposure(20);
            state.setOffshoreCode(true);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch4.q.ans1")
            );

            ui.updateStats(state);

        } else if (choice == 2) {

            state.addXp(15);
            state.addExposure(10);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch4.q.ans2")
            );

            ui.updateStats(state);

        } else if (choice == 3) {

            state.addXp(5);
            state.addExposure(-5);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch4.q.ans3")
            );

            ui.updateStats(state);
        }

        int[] xpByChoice = {25, 15, 5};
        int[] exposureByChoice = {20, 10, -5};
        recordDecision("ch4.q", Lang.t("ch4.q.opt" + choice), xpByChoice[choice - 1], exposureByChoice[choice - 1]);

        Runnable toChapter5 = chapterSummary(4, this::chapter5);

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                choice == 1 ? withItem("item.offshore", toChapter5) : toChapter5
        );
    }

    private void chapter5() {

        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch5_corridor.png"
        );
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch5.t1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter5Story
        );
    }

    private void chapter5Story() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch5.t2")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter5Molnar
        );
    }

    private void chapter5Molnar() {

        ui.clearChoices();

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/molnar.png"
        );


        ui.showDialogue(
                Lang.t("npc.molnar"),
                Lang.t("ch5.molnar1", state.getName()) + "\n\n" + Lang.t("ch5.narr1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter5Conversation
        );
    }

    private void chapter5Conversation() {

        ui.clearChoices();
        ui.showCharacter("/com/example/demo/fityesz_art/characters/molnar2.png");

        ui.showDialogue(
                Lang.t("npc.molnar"),
                Lang.t("ch5.molnar2") + "\n\n"
                        + Lang.t("ch5.q")
        );

        ui.addChoice(
                1,
                Lang.t("ch5.q.opt1"),
                () -> chapter5Choice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch5.q.opt2"),
                () -> chapter5Choice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch5.q.opt3"),
                () -> chapter5Choice(3)
        );
    }

    private void chapter5Choice(int choice) {

        ui.clearChoices();
        ui.hideCharacter();

        if (choice == 1) {

            state.addXp(25);
            state.addExposure(20);
            state.setParliamentKey(true);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch5.q.ans1")
            );

        } else if (choice == 2) {

            state.addXp(10);
            state.addExposure(10);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch5.q.ans2", state.getName())
            );

        } else if (choice == 3) {

            state.addXp(15);
            state.addExposure(-5);

            ui.showDialogue(
                    Lang.t("npc.unknown"),
                    Lang.t("ch5.q.ans3")
            );
        }

        ui.updateStats(state);

        int[] xpByChoice = {25, 10, 15};
        int[] exposureByChoice = {20, 10, -5};
        recordDecision("ch5.q", Lang.t("ch5.q.opt" + choice), xpByChoice[choice - 1], exposureByChoice[choice - 1]);

        Runnable toPreparation = this::chapter5Preparation;

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                choice == 1 ? withItem("item.parliamentKey", toPreparation) : toPreparation
        );
    }

    private void chapter5Preparation() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch5.pre1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::boss2Introduction
        );
    }

    private void boss2Introduction() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch5.pre2")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::startPeteriBoss
        );
    }

    private void startPeteriBoss() {

        state.resetBossFight();

        previousMove = 0;
        sameMove = 0;
        peteriPassiveRounds = 0;

        ui.clearChoices();

        ui.clearBossAbilities();
        ui.addBossAbility(Lang.t("boss2.skill"), Lang.t("boss2.ab.desc"));

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/drpeterikatalin.png"
        );

        ui.showDialogue(
                Lang.t("npc.peteri"),
                Lang.t("boss2.skill") + "\n\n"
                        + Lang.t("boss2.ab.desc")
        );

        ui.showBossFight(
                Lang.t("npc.peteri"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        addPeteriBossButtons();
    }

    private void addPeteriBossButtons() {

        ui.clearChoices();

        ui.showBossFight(
                Lang.t("npc.peteri"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        ui.addChoice(
                1,
                Lang.t("fight.attack"),
                () -> peteriBossMove(1)
        );

        ui.addChoice(
                2,
                Lang.t("fight.defend"),
                () -> peteriBossMove(2)
        );
    }

    private void peteriBossMove(int playerMove) {

        // 1 = attack, 2 = defend
        int bossMove = random.nextInt(2) + 1;

        // MEDIA SCANDAL: counts rounds in which the player did not attack.
        if (playerMove == 2) {
            peteriPassiveRounds++;
        } else {
            peteriPassiveRounds = 0;
        }

        String result;

        // Every 2nd round without an attack, Péteri hits the player.
        if (peteriPassiveRounds >= 2) {

            peteriPassiveRounds = 0;

            state.setPlayerHp(
                    state.getPlayerHp() - 20
            );

            result = Lang.t("boss2.skill") + "\n\n"
                    + Lang.t("boss2.hit");
        }

        // Player attacks.
        else if (playerMove == 1) {

            if (bossMove == 1) {
                // Both attack.
                state.setPlayerHp(
                        state.getPlayerHp() - 10
                );

                state.setBossHp(
                        state.getBossHp() - 20
                );

                result = Lang.t("fight.bothAttacked");

            } else {
                // Péteri defends, the player still deals damage.
                state.setBossHp(
                        state.getBossHp() - 20
                );

                result = Lang.t("boss2.struck");
            }
        }

        // Player defends.
        else {

            if (bossMove == 1) {
                // The player blocks Péteri's attack.
                result = Lang.t("boss2.blocked");
            } else {
                // Both defend.
                result = Lang.t("fight.bothDefended");
            }
        }

        // pointing pose when MEDIA SCANDAL lands
        ui.showCharacter(result.startsWith(Lang.t("boss2.skill"))
                ? "/com/example/demo/fityesz_art/characters/drpeterikatalin2.png"
                : "/com/example/demo/fityesz_art/characters/drpeterikatalin.png");

        ui.updateBossHp(
                Lang.t("npc.peteri"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        ui.showDialogue(
                Lang.t("npc.peteri"),
                result
        );

        if (state.getBossHp() <= 0) {

            peteriDefeated();
            return;
        }

        if (state.getPlayerHp() <= 0) {

            playerDefeatedPeteri();
            return;
        }

        addPeteriBossButtons();
    }

    private void playerDefeatedPeteri() {

        ui.hideBossFight();
        ui.clearChoices();
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ui.defeat")
        );

        ui.addChoice(
                1,
                Lang.t("fight.retry"),
                this::startPeteriBoss
        );

        ui.addChoice(
                2,
                Lang.t("fight.quit"),
                this::restartGame
        );
    }

    private void peteriDefeated() {

        ui.hideBossFight();

        state.addXp(80);
        state.setPeteriDossier(true);
        recordDecision("sum.boss", Lang.t("boss2.win"), 80, 0);

        ui.updateStats(state);
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.peteri"),
                Lang.t("boss2.win")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                withItem("item.peteriDossier", chapterSummary(5, this::chapter6))
        );
    }

    private void chapter6() {

        ui.hideBossFight();
        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch6_lodge.png"
        );
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch6.title") + "\n\n"
                        + Lang.t("ch6.quote")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter6Story
        );
    }

    private void chapter6Story() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("ch6.place"),
                Lang.t("ch6.t1") + "\n\n"
                        + Lang.t("ch6.t2")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::kapzsIntroduction
        );
    }

    private void kapzsIntroduction() {

        ui.clearChoices();

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/kapzsimre.png"
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch6.kapzs1", state.getName())
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::kapzsConversation
        );
    }

    private void kapzsConversation() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch6.kapzs2")
        );

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ch6.narr2")
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch6.kapzs3", state.getName())
        );

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ch6.q")
        );

        ui.addChoice(
                1,
                Lang.t("ch6.q.opt1"),
                () -> chapter6Choice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch6.q.opt2"),
                () -> chapter6Choice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch6.q.opt3"),
                () -> chapter6Choice(3)
        );
    }

    private void chapter6Choice(int choice) {

        ui.clearChoices();
        ui.hideCharacter();

        if (choice == 1) {

            state.addXp(30);
            state.addExposure(15);
            state.setBossTrust(true);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch6.q.ans1")
            );

        } else if (choice == 2) {

            state.addXp(20);
            state.addExposure(5);
            state.setBossTrust(true);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch6.q.ans2")
            );

        } else {

            state.addXp(10);
            state.addExposure(-5);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch6.q.ans3")
            );
        }

        ui.updateStats(state);

        int[] xpByChoice = {30, 20, 10};
        int[] exposureByChoice = {15, 5, -5};
        recordDecision("ch6.q", Lang.t("ch6.q.opt" + choice), xpByChoice[choice - 1], exposureByChoice[choice - 1]);

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                chapterSummary(6, this::chapter7)
        );
    }

    private void chapter7() {

        ui.hideBossFight();
        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch7-1_office.png"
        );
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch7.title") + "\n\n"
                        + Lang.t("ch7.quote")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter7Story
        );
    }

    private void chapter7Story() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("ch7.place"),
                Lang.t("ch7.narr1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::kapzsChapter7
        );
    }

    private void kapzsChapter7() {

        ui.clearChoices();
        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/kapzsimre2.png"
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch7.kapzs1", state.getName())
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::chapter7Conversation
        );
    }

    private void chapter7Conversation() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch7.kapzs2")
        );

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ch7.narr2")
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch7.kapzs3", state.getName())
        );

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ch7.q")
        );

        ui.addChoice(
                1,
                Lang.t("ch7.q.opt1"),
                () -> chapter7Choice(1)
        );

        ui.addChoice(
                2,
                Lang.t("ch7.q.opt2"),
                () -> chapter7Choice(2)
        );

        ui.addChoice(
                3,
                Lang.t("ch7.q.opt3"),
                () -> chapter7Choice(3)
        );
    }

    private void chapter7Choice(int choice) {

        ui.clearChoices();
        ui.hideCharacter();


        if (choice == 1) {

            state.addXp(30);
            state.addExposure(20);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch7.q.ans1")
            );

        } else if (choice == 2) {

            state.addXp(20);
            state.addExposure(10);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch7.q.ans2")
            );

        } else {

            state.addXp(10);
            state.addExposure(-5);

            ui.showDialogue(
                    Lang.t("ui.title"),
                    Lang.t("ch7.q.ans3")
            );
        }

        ui.updateStats(state);

        int[] xpByChoice = {30, 20, 10};
        int[] exposureByChoice = {20, 10, -5};
        recordDecision("ch7.q", Lang.t("ch7.q.opt" + choice), xpByChoice[choice - 1], exposureByChoice[choice - 1]);

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::boss3Introduction
        );
    }



    private void boss3Introduction() {

        ui.clearChoices();
        ui.showImage("/com/example/demo/fityesz_art/bg/ch7-2_chamber.png");

        ui.showDialogue(
                Lang.t("boss3.place"),
                Lang.t("boss3.narr") + "\n\n"
                        + Lang.t("boss3.header").trim() + "\n"
                        + Lang.t("boss3.rule1").trim() + "\n"
                        + Lang.t("boss3.rule2").trim() + "\n"
                        + Lang.t("boss3.rule3").trim()
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::startBoss3
        );
    }

    private void startBoss3() {

        state.resetBossFight();

        ui.clearChoices();

        ui.clearBossAbilities();
        ui.addBossAbility(Lang.t("boss3.ab1.name"), Lang.t("boss3.ab1.desc"));
        ui.addBossAbility(Lang.t("boss3.ab2.name"), Lang.t("boss3.ab2.desc"));
        ui.addBossAbility(Lang.t("boss3.ab3.name"), Lang.t("boss3.ab3.desc"));

        ui.showCharacter(
                "/com/example/demo/fityesz_art/characters/kapzsimre2.png"
        );

        // The long intro text (narration + rules) would stack with the boss box,
        // which already lists the rules, and push the top of the screen under the
        // header. Keep only the closing line of the narration.
        String[] introLines = Lang.t("boss3.narr").split("\n");
        ui.showDialogue(
                Lang.t("boss3.place"),
                introLines[introLines.length - 1]
        );

        ui.showBossFight(
                Lang.t("npc.kapzs"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        addBoss3Buttons();
    }

    private void addBoss3Buttons() {

        ui.clearChoices();

        ui.addChoice(
                1,
                Lang.t("fight.attack"),
                () -> boss3Move(1)
        );

        ui.addChoice(
                2,
                Lang.t("fight.defend"),
                () -> boss3Move(2)
        );
    }

    private void boss3Move(int playerMove) {

        int bossMove = random.nextInt(2) + 1;

        String result;

        if (playerMove == 1) {

            state.setBossHp(state.getBossHp() - 20);

            if (bossMove == 1) {

                int damage = 10;

                if (state.getBossHp() < 50) {
                    damage = 20;
                }

                if (state.hasBossTrust()) {
                    damage = (int) (damage * 0.7);
                }

                state.setPlayerHp(
                        state.getPlayerHp() - damage
                );

                result = Lang.t("fight.bothAttacked");

            } else {

                result = Lang.t("boss3.hit");
            }

        } else {

            if (bossMove == 1) {

                result = Lang.t("boss3.blocked");

            } else {

                result = Lang.t("fight.bothDefended");
            }
        }

        ui.showCharacter(playerMove == 1
                ? "/com/example/demo/fityesz_art/characters/kapzsimre2.png"
                : "/com/example/demo/fityesz_art/characters/kapzsimre.png");

        ui.updateBossHp(
                Lang.t("npc.kapzs"),
                state.getPlayerHp(),
                state.getBossHp()
        );

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                result
        );

        if (state.getBossHp() <= 0) {
            boss3Defeated();
            return;
        }

        if (state.getPlayerHp() <= 0) {
            playerDefeatedBoss3();
            return;
        }

        addBoss3Buttons();
    }

    private void boss3Defeated() {

        ui.hideBossFight();
        ui.clearChoices();

        state.addXp(100);
        recordDecision("sum.boss", Lang.t("boss3.win"), 100, 0);

        ui.updateStats(state);

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("boss3.win")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                chapterSummary(7, this::endDemo)
        );
    }

    private void playerDefeatedBoss3() {

        ui.hideBossFight();
        ui.clearChoices();
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("ui.defeat")
        );

        ui.addChoice(
                1,
                Lang.t("fight.retry"),
                this::startBoss3
        );

        ui.addChoice(
                2,
                Lang.t("fight.quit"),
                this::restartGame
        );
    }
    // ==================================================
    // TEMPORARY END
    // ==================================================

    private void endDemo() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("end.narr1", state.getName())
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::endingScene
        );
    }

    private void endingScene() {

        ui.clearChoices();

        ui.showImage(
                "/com/example/demo/fityesz_art/bg/ch7-3_office.png"
        );
        ui.hideCharacter();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("end.t1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::endingPlayer
        );
    }

    private void endingPlayer() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.you"),
                Lang.t("end.you1")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::endingFinal
        );
    }

    private void endingFinal() {

        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("end.narr2") + "\n\n"
                        + Lang.t("end.unknown")
        );

        ui.showRestartButton();

        ui.addChoice(
                1,
                Lang.t("fight.again"),
                this::restartGame
        );
    }

    // ==================================================
    // GET UI
    // ==================================================

    public GameUI getUI() {
        return ui;
    }
}