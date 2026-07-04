package com.vampirez;

import org.junit.jupiter.api.Test;

import static com.vampirez.GameManager.SpectateCheck.ALLOWED;
import static com.vampirez.GameManager.SpectateCheck.ALREADY_IN_GAME;
import static com.vampirez.GameManager.SpectateCheck.NO_ACTIVE_GAME;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the /vz spectate entry rules: watching is for non-participants during an
 * ACTIVE game only. Joined players must /vz leave first (spectating would dodge
 * the disconnect-conversion rule), and there's nothing to watch outside ACTIVE.
 */
class SpectateCheckTest {

    @Test
    void nonParticipantCanSpectateAnActiveGame() {
        assertEquals(ALLOWED, GameManager.spectateCheck(GameState.ACTIVE, false));
    }

    @Test
    void joinedPlayersAreRejectedInEveryState() {
        for (GameState state : GameState.values()) {
            assertEquals(ALREADY_IN_GAME, GameManager.spectateCheck(state, true),
                    "a participant spectating would bypass conversion rules (state=" + state + ")");
        }
    }

    @Test
    void noSpectatingOutsideActive() {
        assertEquals(NO_ACTIVE_GAME, GameManager.spectateCheck(GameState.LOBBY, false));
        assertEquals(NO_ACTIVE_GAME, GameManager.spectateCheck(GameState.STARTING, false));
        assertEquals(NO_ACTIVE_GAME, GameManager.spectateCheck(GameState.ENDING, false));
    }
}
