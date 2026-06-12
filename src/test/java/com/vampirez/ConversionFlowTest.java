package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests the human → vampire conversion flow: the team-membership half
 * (TeamManager.convertToVampire, including the vampires-win extinction signal)
 * and the perk half (removal of HUMAN-only perks, replacement pick pool).
 * The physical side (gear, teleport, GUI) stays on GameManager and is
 * verified live on the dev server.
 */
class ConversionFlowTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    /** Minimal concrete perk with a configurable team, plus a clearGlobalState probe. */
    private static class StubPerk extends Perk {
        boolean globalStateCleared = false;
        StubPerk(String id, PerkTeam team) {
            this(id, team, PerkTier.SILVER);
        }
        StubPerk(String id, PerkTeam team, PerkTier tier) {
            super(id, "Stub " + id, tier, team, Material.STONE, "desc");
        }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
        @Override public void clearGlobalState() { globalStateCleared = true; }
    }

    @Nested
    class TeamMembership {

        private final TeamManager teams = new TeamManager();

        @Test
        void conversionMovesPlayerBetweenTeams() {
            teams.addHuman(alice);
            teams.addHuman(bob);

            boolean extinct = teams.convertToVampire(alice);

            assertFalse(extinct, "humans remain, vampires must not win yet");
            assertFalse(teams.isHuman(alice));
            assertTrue(teams.isVampire(alice));
            assertTrue(teams.isHuman(bob));
        }

        @Test
        void convertingTheLastHumanSignalsExtinction() {
            teams.addHuman(alice);
            teams.addVampire(bob);

            assertTrue(teams.convertToVampire(alice),
                    "last human converted → extinction signal triggers endGame(vampires win)");
        }

        @Test
        void convertingANonHumanThrows() {
            teams.addVampire(alice);
            assertThrows(IllegalStateException.class, () -> teams.convertToVampire(alice),
                    "double-converting (e.g. death + disconnect race) must fail loudly, "
                    + "not silently corrupt team counts");
        }

        @Test
        void convertingTwiceThrowsOnTheSecondCall() {
            teams.addHuman(alice);
            teams.addHuman(bob);
            teams.convertToVampire(alice);
            assertThrows(IllegalStateException.class, () -> teams.convertToVampire(alice));
            assertEquals(1, teams.getVampireTeam().size(), "no duplicate membership");
        }
    }

    @Nested
    class PerkSide {

        private PerkManager perkManager;
        private MockedStatic<Bukkit> bukkit;

        private StubPerk humanPerk;
        private StubPerk bothPerk;
        private StubPerk vampirePerk;

        @BeforeEach
        void setUp() {
            bukkit = mockStatic(Bukkit.class); // getPlayer → null, callEvent absorbed
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));

            perkManager = new PerkManager();
            humanPerk = new StubPerk("wolf_pack", PerkTeam.HUMAN);
            bothPerk = new StubPerk("blunt_force", PerkTeam.BOTH);
            vampirePerk = new StubPerk("backstab", PerkTeam.VAMPIRE);
            perkManager.registerPerk(humanPerk);
            perkManager.registerPerk(bothPerk);
            perkManager.registerPerk(vampirePerk);
        }

        @AfterEach
        void tearDown() {
            bukkit.close();
        }

        @Test
        void conversionRemovesOnlyHumanTeamPerks() {
            perkManager.addPerkToPlayer(alice, humanPerk);
            perkManager.addPerkToPlayer(alice, bothPerk);

            List<Perk> removed = perkManager.removeTeamSpecificPerks(alice, PerkTeam.HUMAN);

            assertEquals(List.of(humanPerk), removed,
                    "removed count drives how many free replacement picks the convert gets");
            assertEquals(List.of(bothPerk), perkManager.getPlayerPerks(alice),
                    "BOTH-team perks survive conversion");
        }

        @Test
        void conversionWithNoHumanPerksRemovesNothing() {
            perkManager.addPerkToPlayer(alice, bothPerk);
            assertTrue(perkManager.removeTeamSpecificPerks(alice, PerkTeam.HUMAN).isEmpty());
        }

        @Test
        void replacementPickPoolOffersOnlyVampireAndBothPerks() {
            // After conversion the replacement GUI rolls VAMPIRE-team Silver perks
            List<Perk> pool = perkManager.getRandomPerks(PerkTier.SILVER, PerkTeam.VAMPIRE, 10, alice);
            assertTrue(pool.contains(vampirePerk));
            assertTrue(pool.contains(bothPerk));
            assertFalse(pool.contains(humanPerk), "a fresh vampire must never be offered a HUMAN perk");
        }

        @Test
        void replacementPickPoolExcludesOwnedAndDisabledPerks() {
            perkManager.addPerkToPlayer(alice, bothPerk);
            perkManager.setDisabledPerks(List.of("backstab"));

            List<Perk> pool = perkManager.getRandomPerks(PerkTier.SILVER, PerkTeam.VAMPIRE, 10, alice);

            assertFalse(pool.contains(bothPerk), "already owned");
            assertFalse(pool.contains(vampirePerk), "disabled in config");
        }

        @Test
        void removedPerksKeepTheirTiersInRemovalOrder() {
            // The conversion GUI replaces each lost perk at ITS tier, in order —
            // so the removal result must preserve both.
            StubPerk goldHuman = new StubPerk("marksman", PerkTeam.HUMAN, PerkTier.GOLD);
            StubPerk prisHuman = new StubPerk("guardian_angel", PerkTeam.HUMAN, PerkTier.PRISMATIC);
            perkManager.registerPerk(goldHuman);
            perkManager.registerPerk(prisHuman);
            perkManager.addPerkToPlayer(alice, humanPerk);   // SILVER
            perkManager.addPerkToPlayer(alice, goldHuman);
            perkManager.addPerkToPlayer(alice, prisHuman);

            List<PerkTier> lostTiers = perkManager.removeTeamSpecificPerks(alice, PerkTeam.HUMAN)
                    .stream().map(Perk::getTier).toList();

            assertEquals(List.of(PerkTier.SILVER, PerkTier.GOLD, PerkTier.PRISMATIC), lostTiers);
        }

        @Test
        void replacementPoolMatchesTheLostPerkTier() {
            StubPerk goldVampire = new StubPerk("shadow_strike", PerkTeam.VAMPIRE, PerkTier.GOLD);
            StubPerk prisVampire = new StubPerk("final_form", PerkTeam.VAMPIRE, PerkTier.PRISMATIC);
            perkManager.registerPerk(goldVampire);
            perkManager.registerPerk(prisVampire);

            // Lost a GOLD perk → the pick pool must be gold-only
            List<Perk> goldPool = perkManager.getRandomPerks(PerkTier.GOLD, PerkTeam.VAMPIRE, 10, alice);
            assertEquals(List.of(goldVampire), goldPool,
                    "a lost Gold perk must roll Gold replacements — not Silver, not Prismatic");

            // Lost a PRISMATIC perk → prismatic-only
            List<Perk> prisPool = perkManager.getRandomPerks(PerkTier.PRISMATIC, PerkTeam.VAMPIRE, 10, alice);
            assertEquals(List.of(prisVampire), prisPool);
        }

        @Test
        void maxPerkCapHoldsDuringConversionPicks() {
            perkManager.setMaxPerks(1);
            assertTrue(perkManager.addPerkToPlayer(alice, bothPerk));
            assertFalse(perkManager.addPerkToPlayer(alice, vampirePerk), "cap reached");
            perkManager.forceAddPerkToPlayer(alice, vampirePerk);
            assertEquals(2, perkManager.getPlayerPerkCount(alice), "forceAdd (Lucky Roll) bypasses the cap");
        }

        @Test
        void resetAllClearsPlayersAndGlobalState() {
            perkManager.addPerkToPlayer(alice, humanPerk);

            perkManager.resetAll();

            assertTrue(perkManager.getPlayerPerks(alice).isEmpty());
            assertTrue(humanPerk.globalStateCleared, "resetAll must clear cross-player perk state");
            assertTrue(vampirePerk.globalStateCleared, "even for perks nobody held");
        }
    }
}
