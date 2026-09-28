package com.example.demo;

import java.util.Random;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

public class GameController {

    private final GameUI ui;
    private final GameState state;
    private final Stage stage;

    private final Random random = new Random();

    // Boss fight state
    private int previousMove = 0;
    private int sameMove = 0;

    public GameController(Stage stage) {
        this.stage = stage;
        this.state = new GameState();
        this.ui = new GameUI();

        showLanguageSelection();
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
                    showStartScreen();
                }
        );

        ui.addChoice(
                2,
                "English",
                () -> {
                    Lang.set("en");
                    showStartScreen();
                }
        );
    }

    private void showStartScreen() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("intro.welcome") + "\n\n"
                        + Lang.t("intro.1") + "\n\n"
                        + Lang.t("intro.2") + "\n\n"
                        + Lang.t("intro.3")
        );

        ui.addChoice(
                1,
                Lang.t("pro.namePrompt"),
                this::askForName
        );

        ui.updateStats(state);
    }

    // ==================================================
    // NAME
    // ==================================================

    private void askForName() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle(Lang.t("ui.title"));
        dialog.setHeaderText(Lang.t("pro.namePrompt"));
        dialog.setContentText(Lang.t("pro.namePrompt"));

        dialog.showAndWait().ifPresent(input -> {
            String name = input.trim();

            if (name.isEmpty()) {
                askForName();
                return;
            }

            state.setName(name);
            ui.updateStats(state);

            showPrologue();
        });
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
                "C:/Downloads/src/demo/main/resources/com/example/demo/fityesz_art/bg/ch1_cafe.png"
        );

        ui.showDialogue(
                Lang.t("ui.chapter"),
                Lang.t("ch1.title") + "\n\n"
                        + Lang.t("ch1.quote") + "\n\n"
                        + Lang.t("ch1.place")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lipotiIntroduction);
    }

    private void lipotiIntroduction() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lipoti"),
                Lang.t("ch1.lipoti1", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lipotiOffer);
    }

    private void lipotiOffer() {
        ui.clearChoices();

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

        if (choice == 1) {
            state.addXp(15);
            state.addExposure(10);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans1")
            );
        } else if (choice == 2) {
            state.addXp(10);
            state.addExposure(5);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans2")
            );
        } else {
            state.addXp(5);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q1.ans3")
            );
        }

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

        if (choice == 1) {
            state.addXp(20);
            state.addExposure(15);
            state.setFirstEnvelope(true);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans1")
            );
        } else if (choice == 2) {
            state.addXp(10);
            state.addExposure(5);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans2")
            );
        } else {
            state.addExposure(-10);

            ui.showDialogue(
                    Lang.t("npc.lipoti"),
                    Lang.t("ch1.q2.ans3")
            );
        }

        ui.updateStats(state);

        ui.addChoice(1, Lang.t("ui.enter"), this::chapter2);
    }

    // ==================================================
    // CHAPTER 2 - FIRST MEETING
    // ==================================================

    private void chapter2() {
        ui.clearChoices();

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

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch2.narr1") + "\n\n"
                        + Lang.t("ch2.narr2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosIntroduction);
    }

    private void lakatosIntroduction() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("ch2.lakatos1", state.getName())
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosRules);
    }

    private void lakatosRules() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("ch2.lakatos2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::lakatosChoice);
    }

    private void lakatosChoice() {
        ui.clearChoices();

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

        ui.addChoice(1, Lang.t("ui.enter"), this::chapter3);
    }

    // ==================================================
    // CHAPTER 3 - CONGRESS
    // ==================================================

    private void chapter3() {
        ui.clearChoices();

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

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.narr1")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech1);
    }

    private void kapzsSpeech1() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch3.kapzs1")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech2);
    }

    private void kapzsSpeech2() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("ch3.narr2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::kapzsSpeech3);
    }

    private void kapzsSpeech3() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.kapzs"),
                Lang.t("ch3.kapzs2")
        );

        ui.addChoice(1, Lang.t("ui.enter"), this::congressEnd);
    }

    private void congressEnd() {
        ui.clearChoices();

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
        state.resetLakatosFight();

        previousMove = 0;
        sameMove = 0;

        ui.clearChoices();

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

    }

    // ==================================================
    // LAKATOS DEFEATED
    // ==================================================

    private void lakatosDefeated() {
        // Remove boss HP screen
        ui.hideBossFight();

        state.addXp(50);
        state.setLakatosFile(true);

        ui.updateStats(state);
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.lakatos"),
                Lang.t("boss1.win") + "\n\n"
                        + Lang.t("item.lakatosFile")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::afterLakatos
        );
    }

    // ==================================================
    // PLAYER DEFEATED
    // ==================================================

    private void playerDefeated() {
        ui.clearChoices();

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
                this::showStartScreen
        );
    }

    // ==================================================
    // AFTER LAKATOS
    // ==================================================

    private void afterLakatos() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("npc.unknown"),
                Lang.t("boss1.win") + "\n\n"
                        + Lang.t("item.lakatosFile") + "\n\n"
                        + Lang.t("ch4.title")
        );

        ui.addChoice(
                1,
                Lang.t("ui.enter"),
                this::endDemo
        );
    }

    // ==================================================
    // TEMPORARY END
    // ==================================================

    private void endDemo() {
        ui.clearChoices();

        ui.showDialogue(
                Lang.t("ui.title"),
                Lang.t("fight.gameover")
        );

        ui.addChoice(
                1,
                Lang.t("fight.again"),
                this::showStartScreen
        );
    }

    // ==================================================
    // GET UI
    // ==================================================

    public GameUI getUI() {
        return ui;
    }
}
