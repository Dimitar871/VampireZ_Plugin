package com.vampirez;

import com.vampirez.config.PluginConfig;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Pure-logic tests for the gold ledger and kill/assist reward distribution.
 * Bukkit.getPlayer is static-mocked to return null (offline players) so the
 * chat-message side effects are skipped; the gold math is what's under test.
 */
class EconomyManagerTest {

    private EconomyManager economy;
    private MockedStatic<Bukkit> bukkit;

    private final UUID killer = UUID.randomUUID();
    private final UUID victim = UUID.randomUUID();
    private final UUID assister = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        VampireZPlugin plugin = mock(VampireZPlugin.class);
        when(plugin.getPluginConfig()).thenReturn(new PluginConfig()); // defaults: kill 15, assist 5
        economy = new EconomyManager(plugin);
        bukkit = mockStatic(Bukkit.class); // getPlayer(uuid) → null, skips messages
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    // ===== gold ledger =====

    @Test
    void goldStartsAtZero() {
        assertEquals(0, economy.getGold(killer));
    }

    @Test
    void addGoldAccumulates() {
        economy.addGold(killer, 10);
        economy.addGold(killer, 5);
        assertEquals(15, economy.getGold(killer));
    }

    @Test
    void removeGoldFailsWithoutEnoughAndLeavesBalanceUntouched() {
        economy.addGold(killer, 10);
        assertFalse(economy.removeGold(killer, 11));
        assertEquals(10, economy.getGold(killer));
    }

    @Test
    void removeGoldDeductsWhenAffordable() {
        economy.addGold(killer, 50);
        assertTrue(economy.removeGold(killer, 50));
        assertEquals(0, economy.getGold(killer));
    }

    @Test
    void setGoldClampsNegativeToZero() {
        economy.setGold(killer, -100);
        assertEquals(0, economy.getGold(killer));
    }

    @Test
    void hasEnoughIsInclusive() {
        economy.addGold(killer, 50);
        assertTrue(economy.hasEnough(killer, 50));
        assertFalse(economy.hasEnough(killer, 51));
    }

    // ===== kill / assist rewards (config defaults: kill 15, assist 5) =====

    @Test
    void killerReceivesKillReward() {
        economy.awardKillRewards(killer, victim);
        assertEquals(15, economy.getGold(killer));
    }

    @Test
    void recentDamagerReceivesAssistReward() {
        economy.recordDamage(victim, assister);
        economy.awardKillRewards(killer, victim);
        assertEquals(15, economy.getGold(killer));
        assertEquals(5, economy.getGold(assister));
    }

    @Test
    void killerIsNotAlsoPaidAsAssister() {
        economy.recordDamage(victim, killer); // killer damaged the victim earlier too
        economy.awardKillRewards(killer, victim);
        assertEquals(15, economy.getGold(killer), "killer must get the kill reward only, not kill + assist");
    }

    @Test
    void assistWindowIsConsumedByTheKill() {
        economy.recordDamage(victim, assister);
        economy.awardKillRewards(killer, victim);
        // Second kill of the same victim without new damage — no double assist
        economy.awardKillRewards(killer, victim);
        assertEquals(5, economy.getGold(assister));
        assertEquals(30, economy.getGold(killer));
    }

    // ===== resets =====

    @Test
    void resetPlayerClearsOnlyThatPlayer() {
        economy.addGold(killer, 10);
        economy.addGold(assister, 20);
        economy.resetPlayer(killer);
        assertEquals(0, economy.getGold(killer));
        assertEquals(20, economy.getGold(assister));
    }

    @Test
    void resetAllClearsEverythingIncludingAssistTracking() {
        economy.addGold(killer, 10);
        economy.recordDamage(victim, assister);
        economy.resetAll();
        assertEquals(0, economy.getGold(killer));
        economy.awardKillRewards(killer, victim);
        assertEquals(0, economy.getGold(assister), "assist tracking must not survive a game reset");
    }
}
