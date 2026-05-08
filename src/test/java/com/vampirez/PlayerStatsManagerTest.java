package com.vampirez;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PlayerStatsManagerTest {

    @TempDir
    Path tempDir;

    @Mock
    org.bukkit.plugin.java.JavaPlugin plugin;
    @Mock
    GameManager gameManager;

    private PlayerStatsManager manager;

    @BeforeEach
    void setUp() {
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        manager = new PlayerStatsManager(plugin, gameManager);
    }

    // ===== Kill recording =====

    @Test
    void recordKill_incrementsCount() {
        UUID uuid = UUID.randomUUID();
        manager.recordKill(uuid, "Alice");
        assertEquals(1, manager.getStats(uuid).kills);
    }

    @Test
    void recordKill_accumulatesMultiple() {
        UUID uuid = UUID.randomUUID();
        manager.recordKill(uuid, "Alice");
        manager.recordKill(uuid, "Alice");
        manager.recordKill(uuid, "Alice");
        assertEquals(3, manager.getStats(uuid).kills);
    }

    @Test
    void recordKill_storesName() {
        UUID uuid = UUID.randomUUID();
        manager.recordKill(uuid, "Bob");
        assertEquals("Bob", manager.getStats(uuid).name);
    }

    // ===== Win / loss recording =====

    @Test
    void recordWin_incrementsWins() {
        UUID uuid = UUID.randomUUID();
        manager.recordWin(uuid, "Alice");
        assertEquals(1, manager.getStats(uuid).wins);
        assertEquals(0, manager.getStats(uuid).losses);
    }

    @Test
    void recordLoss_incrementsLosses() {
        UUID uuid = UUID.randomUUID();
        manager.recordLoss(uuid, "Alice");
        assertEquals(0, manager.getStats(uuid).wins);
        assertEquals(1, manager.getStats(uuid).losses);
    }

    @Test
    void gamesPlayed_sumWinsAndLosses() {
        UUID uuid = UUID.randomUUID();
        manager.recordWin(uuid, "Alice");
        manager.recordWin(uuid, "Alice");
        manager.recordLoss(uuid, "Alice");
        assertEquals(3, manager.getStats(uuid).getGamesPlayed());
    }

    @Test
    void winRate_calculatesCorrectly() {
        UUID uuid = UUID.randomUUID();
        manager.recordWin(uuid, "Alice");
        manager.recordWin(uuid, "Alice");
        manager.recordLoss(uuid, "Alice");
        assertEquals(2.0 / 3.0, manager.getStats(uuid).getWinRate(), 0.0001);
    }

    @Test
    void winRate_returnsZeroWithNoGames() {
        UUID uuid = UUID.randomUUID();
        assertEquals(0.0, manager.getStats(uuid).getWinRate(), 0.0001);
    }

    // ===== getStats for unknown UUID =====

    @Test
    void getStats_unknownUUID_returnsEmptyStats() {
        PlayerStatsManager.PlayerStats s = manager.getStats(UUID.randomUUID());
        assertEquals(0, s.kills);
        assertEquals(0, s.wins);
        assertEquals(0, s.losses);
    }

    // ===== Top-N queries =====

    @Test
    void getTopByKills_returnsDescendingOrder() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        manager.recordKill(a, "A"); manager.recordKill(a, "A"); manager.recordKill(a, "A"); // 3
        manager.recordKill(b, "B");                                                          // 1
        manager.recordKill(c, "C"); manager.recordKill(c, "C");                             // 2

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> top = manager.getTopByKills(3);
        assertEquals(3, top.size());
        assertEquals(a, top.get(0).getKey());
        assertEquals(c, top.get(1).getKey());
        assertEquals(b, top.get(2).getKey());
    }

    @Test
    void getTopByWins_returnsDescendingOrder() {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        manager.recordWin(a, "A"); manager.recordWin(a, "A");
        manager.recordWin(b, "B");

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> top = manager.getTopByWins(2);
        assertEquals(a, top.get(0).getKey());
        assertEquals(b, top.get(1).getKey());
    }

    @Test
    void getTopByWinRate_excludesPlayersWithNoGames() {
        UUID a = UUID.randomUUID(), killsOnly = UUID.randomUUID();
        manager.recordWin(a, "A"); manager.recordLoss(a, "A");
        manager.recordKill(killsOnly, "KO"); // has kills but no games played

        List<Map.Entry<UUID, PlayerStatsManager.PlayerStats>> top = manager.getTopByWinRate(10);
        assertTrue(top.stream().noneMatch(e -> e.getKey().equals(killsOnly)),
                "Players with no games played should not appear in win-rate board");
    }

    @Test
    void getTopByKills_respectsLimit() {
        for (int i = 0; i < 5; i++) {
            UUID uuid = UUID.randomUUID();
            manager.recordKill(uuid, "P" + i);
        }
        assertEquals(3, manager.getTopByKills(3).size());
    }

    // ===== Save / load round-trip =====

    @Test
    void saveAndLoad_roundTrips() throws IOException {
        UUID uuid = UUID.randomUUID();
        manager.recordKill(uuid, "Carol");
        manager.recordKill(uuid, "Carol");
        manager.recordWin(uuid, "Carol");
        manager.recordLoss(uuid, "Carol");
        manager.save();

        // New instance reads the same file
        PlayerStatsManager loaded = new PlayerStatsManager(plugin, gameManager);
        PlayerStatsManager.PlayerStats s = loaded.getStats(uuid);

        assertEquals(2, s.kills);
        assertEquals(1, s.wins);
        assertEquals(1, s.losses);
        assertEquals("Carol", s.name);
    }

    @Test
    void load_ignoresNonUUIDKeys() throws IOException {
        // Write a YAML file with a non-UUID key
        File statsFile = new File(tempDir.toFile(), "player-stats.yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("not-a-uuid.kills", 5);
        config.set("not-a-uuid.wins", 2);
        config.set("not-a-uuid.losses", 1);
        config.save(statsFile);

        // Should load without throwing
        PlayerStatsManager m = new PlayerStatsManager(plugin, gameManager);
        assertTrue(m.stats.isEmpty(), "Non-UUID keys must be ignored");
    }

    @Test
    void load_withNoFile_producesEmptyState() {
        // stats file doesn't exist at this point
        PlayerStatsManager m = new PlayerStatsManager(plugin, gameManager);
        assertEquals(0, m.stats.size());
    }
}
