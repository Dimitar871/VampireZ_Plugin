package com.vampirez;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamManagerTest {

    private TeamManager tm;
    private UUID alice;
    private UUID bob;

    @BeforeEach
    void setUp() {
        tm = new TeamManager();
        alice = UUID.randomUUID();
        bob = UUID.randomUUID();
    }

    // ===== Membership =====

    @Test
    void addHuman_makesPlayerHumanButNotVampire() {
        tm.addHuman(alice);

        assertTrue(tm.isHuman(alice));
        assertFalse(tm.isVampire(alice));
        assertTrue(tm.isInGame(alice));
        assertEquals(1, tm.getHumanTeam().size());
        assertEquals(0, tm.getVampireTeam().size());
    }

    @Test
    void addVampire_makesPlayerVampireButNotHuman() {
        tm.addVampire(bob);

        assertTrue(tm.isVampire(bob));
        assertFalse(tm.isHuman(bob));
        assertTrue(tm.isInGame(bob));
    }

    @Test
    void notInAnyTeam_isNotInGame() {
        UUID stranger = UUID.randomUUID();
        assertFalse(tm.isHuman(stranger));
        assertFalse(tm.isVampire(stranger));
        assertFalse(tm.isInGame(stranger));
    }

    @Test
    void removeHuman_returnsTrueWhenPresent_falseOtherwise() {
        tm.addHuman(alice);
        assertTrue(tm.removeHuman(alice));
        assertFalse(tm.isHuman(alice));
        assertFalse(tm.removeHuman(alice), "Second remove should return false");
    }

    @Test
    void clearTeams_wipesEverything() {
        tm.addHuman(alice);
        tm.addVampire(bob);
        tm.tagCombat(alice);

        tm.clearTeams();

        assertEquals(0, tm.getHumanTeam().size());
        assertEquals(0, tm.getVampireTeam().size());
        assertFalse(tm.isInCombat(alice), "Combat tags must clear with teams (same lifecycle)");
    }

    // ===== Combat tag =====

    @Test
    void tagCombat_marksPlayerInCombat_within7s() {
        tm.tagCombat(alice);
        assertTrue(tm.isInCombat(alice));
    }

    @Test
    void untaggedPlayer_isNotInCombat() {
        assertFalse(tm.isInCombat(alice));
    }

    // ===== State flags =====

    @Test
    void vampiresReleased_defaultsTrue_canToggle() {
        assertTrue(tm.isVampiresReleased(), "Default state must be true so PvP isn't gated before a game starts");

        tm.setVampiresReleased(false);
        assertFalse(tm.isVampiresReleased());

        tm.setVampiresReleased(true);
        assertTrue(tm.isVampiresReleased());
    }

    @Test
    void bloodCompassGiven_defaultsFalse_canToggle() {
        assertFalse(tm.isBloodCompassGiven());

        tm.setBloodCompassGiven(true);
        assertTrue(tm.isBloodCompassGiven());

        tm.setBloodCompassGiven(false);
        assertFalse(tm.isBloodCompassGiven(), "Reset on game end must work");
    }
}
