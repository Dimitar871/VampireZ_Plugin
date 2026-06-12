package com.vampirez;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the v2.3.1 bug (BUG_PERK_PERSISTENCE.md): perk-selection
 * GUIs left open across a game end could add perks AFTER perkManager.resetAll(),
 * leaking them into the lobby and the next game.
 *
 * Layer 1 of the fix gates every perk-adding UI path on
 * GameState.allowsPerkSelection(); Layer 2 force-closes open GUIs from the
 * game-end paths via PerkSelectionGUI.cancelAllOpen().
 */
class PerkPersistenceRegressionTest {

    @Test
    void perkSelectionIsOnlyAllowedMidGame() {
        assertTrue(GameState.STARTING.allowsPerkSelection(),
                "the free starting pick opens during the STARTING countdown");
        assertTrue(GameState.ACTIVE.allowsPerkSelection());
    }

    @Test
    void perkSelectionIsBlockedOutsideTheGame() {
        assertFalse(GameState.LOBBY.allowsPerkSelection(),
                "perks added in LOBBY survive into the next game — this is the v2.3.1 leak");
        assertFalse(GameState.ENDING.allowsPerkSelection(),
                "ENDING runs between endGame() and resetToLobby(); perks added here leak too");
    }

    /**
     * Layer 2 contract: GameManager's end-of-game paths call cancelAllOpen() to
     * force-close selection GUIs. This pins the method's existence and shape so a
     * refactor can't silently drop it (the call sites are plain method calls the
     * compiler already checks — this guards against removing/renaming the hook
     * and its GameManager callers together without a replacement).
     */
    @Test
    void perkSelectionGuiExposesCancelAllOpen() throws Exception {
        Method m = PerkSelectionGUI.class.getMethod("cancelAllOpen");
        assertTrue(Modifier.isPublic(m.getModifiers()));
        assertTrue(m.getParameterCount() == 0);
    }
}
