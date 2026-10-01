package com.example.demo;

public class GameState {

    // ==========================================
    // PLAYER INFORMATION
    // ==========================================

    private String name;

    private int xp = 0;

    private int exposure = 0;

    private int level = 1;


    // ==========================================
    // INVENTORY
    // ==========================================

    private boolean firstEnvelope = false;

    private boolean smallEnvelope = false;

    private boolean lakatosFile = false;

    private boolean offshoreCode = false;

    private boolean peteriDossier = false;

    private boolean bossTrust = false;

    private boolean parliamentKey = false;

    // ==========================================
    // BOSS FIGHT
    // ==========================================

    private int playerHp = 100;

    private int bossHp = 80;


    // ==========================================
    // NAME
    // ==========================================

    public String getName() {

        return name;
    }


    public void setName(String name) {

        this.name = name;
    }


    // ==========================================
    // XP
    // ==========================================

    public int getXp() {

        return xp;
    }


    public void addXp(int amount) {

        xp += amount;


        if (xp >= 50 && level == 1) {

            level = 2;
        }
    }


    // ==========================================
    // EXPOSURE
    // ==========================================

    public int getExposure() {

        return exposure;
    }


    public void addExposure(int amount) {

        exposure += amount;


        if (exposure < 0) {

            exposure = 0;
        }


        if (exposure > 100) {

            exposure = 100;
        }
    }


    // ==========================================
    // LEVEL
    // ==========================================

    public int getLevel() {

        return level;
    }


    // ==========================================
    // INVENTORY
    // ==========================================

    public boolean hasFirstEnvelope() {

        return firstEnvelope;
    }


    public void setFirstEnvelope(boolean value) {

        firstEnvelope = value;
    }


    public boolean hasSmallEnvelope() {

        return smallEnvelope;
    }


    public void setSmallEnvelope(boolean value) {

        smallEnvelope = value;
    }


    public boolean hasLakatosFile() {

        return lakatosFile;
    }


    public void setLakatosFile(boolean value) {

        lakatosFile = value;
    }

    public boolean hasParliamentKey() {

        return parliamentKey;
    }


    public void setParliamentKey(boolean value) {

        parliamentKey = value;
    }

    public boolean hasOffshoreCode() {

        return offshoreCode;
    }


    public void setOffshoreCode(boolean value) {

        offshoreCode = value;
    }


    public boolean hasPeteriDossier() {

        return peteriDossier;
    }


    public void setPeteriDossier(boolean value) {

        peteriDossier = value;
    }


    public boolean hasBossTrust() {

        return bossTrust;
    }


    public void setBossTrust(boolean value) {

        bossTrust = value;
    }


    // ==========================================
    // PLAYER HP
    // ==========================================

    public int getPlayerHp() {

        return playerHp;
    }


    public void setPlayerHp(int hp) {

        playerHp = hp;


        if (playerHp < 0) {

            playerHp = 0;
        }


        if (playerHp > 100) {

            playerHp = 100;
        }
    }


    // ==========================================
    // BOSS HP
    // ==========================================

    public int getBossHp() {

        return bossHp;
    }


    public void setBossHp(int hp) {

        bossHp = hp;


        if (bossHp < 0) {

            bossHp = 0;
        }


        if (bossHp > 80) {

            bossHp = 80;
        }
    }


    // ==========================================
    // RESET BOSS FIGHT
    // ==========================================

    public void resetBossFight() {

        playerHp = 100;

        bossHp = 80;
    }
}