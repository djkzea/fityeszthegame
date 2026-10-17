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
