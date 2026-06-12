package com.vampirez;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the quit-compensation rule: a mid-game quitter is owed exactly one
 * auto-perk per perk they ACTUALLY held, at the same tier — plus the starting
 * Silver freebie only if it was granted and they held nothing yet.
 *
 * The old rule reconstructed freebies from elapsed time and double-counted any
 * free pick that happened to be a HUMAN-team perk, so quitting could return
 * MORE perks than staying through a conversion.
 */
class QuitCompensationTest {

    private static Perk perk(String id, PerkTier tier) {
        return new Perk(id, "P " + id, tier, PerkTeam.HUMAN, Material.STONE, "d") {
            @Override public void apply(Player p) {}
            @Override public void remove(Player p) {}
        };
    }

    @Test
    void compensatesExactlyTheHeldTiersInOrder() {
        List<Perk> held = List.of(
                perk("a", PerkTier.SILVER),
                perk("b", PerkTier.GOLD),
                perk("c", PerkTier.PRISMATIC));

        assertEquals(List.of(PerkTier.SILVER, PerkTier.GOLD, PerkTier.PRISMATIC),
                GameManager.quitCompensationTiers(held, true));
    }

    @Test
    void noDoubleCountingOfTheStartingFreebie() {
        // The starting pick was a HUMAN-team Silver perk — the old rule compensated
        // it twice (freebie + human-only replacement). Now: exactly one.
        List<Perk> held = List.of(perk("wolf_pack", PerkTier.SILVER));

        assertEquals(List.of(PerkTier.SILVER),
                GameManager.quitCompensationTiers(held, true),
                "one held perk must yield exactly one replacement, not freebie + replacement");
    }

    @Test
    void emptyHandedQuitterIsStillOwedTheGrantedFreebie() {
        // Quit in the seconds between game start and picking the free Silver perk.
        assertEquals(List.of(PerkTier.SILVER),
                GameManager.quitCompensationTiers(List.of(), true));
    }

    @Test
    void scoutingVampireWithNothingGetsNothing() {
        // Vampires receive their freebie on release — quitting before release
        // with no perks owes nothing yet (they're offered it via release flow).
        assertTrue(GameManager.quitCompensationTiers(List.of(), false).isEmpty());
    }
}
