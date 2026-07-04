package com.vampirez;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Balance-telemetry aggregation: pick counts, per-perk win/loss from team
 * outcomes, and the two /vz perkstats queries. Mirrors PlayerStatsManagerTest
 * (in-memory manager, no persistence).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PerkStatsManagerTest {

    @Mock
    org.bukkit.plugin.java.JavaPlugin plugin;
    @Mock
    GameManager gameManager;
    @Mock
    PerkManager perkManager;

    private PerkStatsManager manager;

    @BeforeEach
    void setUp() {
        manager = new PerkStatsManager(plugin, gameManager, perkManager);
    }

    // ===== Recording =====

    @Test
    void picksAccumulatePerPerk() {
        manager.recordPick("blunt_force");
        manager.recordPick("blunt_force");
        manager.recordPick("backstab");

        assertEquals(2, manager.getStats("blunt_force").picks);
        assertEquals(1, manager.getStats("backstab").picks);
    }

    @Test
    void unknownPerkHasZeroStats() {
        PerkStatsManager.PerkStats s = manager.getStats("never_seen");
        assertEquals(0, s.picks);
        assertEquals(0, s.getGamesPlayed());
        assertEquals(0.0, s.getWinRate());
    }

    @Test
    void winRateComputesFromWinsAndLosses() {
        manager.recordWin("backstab");
        manager.recordWin("backstab");
        manager.recordWin("backstab");
        manager.recordLoss("backstab");

        PerkStatsManager.PerkStats s = manager.getStats("backstab");
        assertEquals(4, s.getGamesPlayed());
        assertEquals(0.75, s.getWinRate(), 1e-9);
    }

    // ===== /vz perkstats queries =====

    @Test
    void topByPicksOrdersDescendingAndSkipsUnpicked() {
        manager.recordPick("a");
        manager.recordPick("b");
        manager.recordPick("b");
        manager.recordPick("c");
        manager.recordPick("c");
        manager.recordPick("c");
        manager.recordWin("never_picked"); // win recorded but never chosen — not a "pick" row

        List<Map.Entry<String, PerkStatsManager.PerkStats>> top = manager.getTopByPicks(2);

        assertEquals(2, top.size());
        assertEquals("c", top.get(0).getKey());
        assertEquals("b", top.get(1).getKey());
    }

    @Test
    void topByWinRateRequiresMinimumGames() {
        // 1 win / 0 losses = 100% but only 1 game — must not top the chart at minGames=5
        manager.recordWin("lucky_once");
        for (int i = 0; i < 4; i++) manager.recordWin("consistent");
        manager.recordLoss("consistent"); // 4/5 = 80% over 5 games

        List<Map.Entry<String, PerkStatsManager.PerkStats>> top = manager.getTopByWinRate(5, 5);

        assertEquals(1, top.size(), "perks under the games threshold must be excluded");
        assertEquals("consistent", top.get(0).getKey());
        assertEquals(0.8, top.get(0).getValue().getWinRate(), 1e-9);
    }

    @Test
    void topByWinRateOrdersDescending() {
        manager.recordWin("strong");   // 1/1 = 100%
        manager.recordWin("weak");     // 1/2 = 50%
        manager.recordLoss("weak");

        List<Map.Entry<String, PerkStatsManager.PerkStats>> top = manager.getTopByWinRate(5, 1);

        assertEquals("strong", top.get(0).getKey());
        assertEquals("weak", top.get(1).getKey());
    }

    @Test
    void emptyManagerReturnsEmptyLists() {
        assertTrue(manager.getTopByPicks(10).isEmpty());
        assertTrue(manager.getTopByWinRate(10, 1).isEmpty());
    }
}
