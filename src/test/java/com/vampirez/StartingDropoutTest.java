package com.vampirez;

import org.junit.jupiter.api.Test;

import static com.vampirez.GameManager.StartingDropoutAction.ABORT_GAME;
import static com.vampirez.GameManager.StartingDropoutAction.CONTINUE;
import static com.vampirez.GameManager.StartingDropoutAction.PROMOTE_HUMAN_TO_VAMPIRE;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the STARTING-countdown dropout rules: a disconnect or /vz leave before
 * the game begins must never convert the player for the round or hand vampires
 * a win — the team is rebalanced or the start is aborted instead.
 */
class StartingDropoutTest {

    @Test
    void viableTeamsContinue() {
        assertEquals(CONTINUE, GameManager.afterStartingDropout(5, 2));
        assertEquals(CONTINUE, GameManager.afterStartingDropout(1, 1));
    }

    @Test
    void lastVampireDroppingPromotesAHuman() {
        assertEquals(PROMOTE_HUMAN_TO_VAMPIRE, GameManager.afterStartingDropout(7, 0),
                "a game needs at least one vampire — promote, don't start hunterless");
    }

    @Test
    void lastHumanDroppingAbortsInsteadOfVampireWin() {
        assertEquals(ABORT_GAME, GameManager.afterStartingDropout(0, 3),
                "the old behavior awarded vampires a win for a game that never began");
    }

    @Test
    void everyoneDroppingAborts() {
        assertEquals(ABORT_GAME, GameManager.afterStartingDropout(0, 0));
    }
}
