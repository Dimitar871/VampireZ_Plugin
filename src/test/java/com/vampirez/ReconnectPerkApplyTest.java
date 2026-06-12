package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Regression guard for the reconnect perk-stacking bug: the reconnect flow
 * auto-assigned pending perks (which ran apply()) and then called
 * reapplyPerks() — so item-granting perks handed out their items twice.
 * Auto-assign now defers apply (applyNow=false) and the single bulk
 * reapplyPerks() is the only apply.
 */
class ReconnectPerkApplyTest {

    private PerkManager perkManager;
    private MockedStatic<Bukkit> bukkit;
    private Player player;
    private final UUID uuid = UUID.randomUUID();

    /** Counts apply() calls — each one would re-grant items in a real item perk. */
    private static class CountingPerk extends Perk {
        int applyCalls = 0;
        CountingPerk(String id) {
            super(id, "Counting " + id, PerkTier.SILVER, PerkTeam.VAMPIRE, Material.STONE, "desc");
        }
        @Override public void apply(Player p) { applyCalls++; }
        @Override public void remove(Player p) {}
    }

    @BeforeEach
    void setUp() {
        perkManager = new PerkManager();
        player = mock(Player.class);
        when(player.isDead()).thenReturn(false);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
        bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(player);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void defaultAddAppliesExactlyOnce() {
        CountingPerk perk = new CountingPerk("a");
        assertTrue(perkManager.addPerkToPlayer(uuid, perk));
        assertEquals(1, perk.applyCalls);
    }

    @Test
    void deferredAddDoesNotApply() {
        CountingPerk perk = new CountingPerk("a");
        assertTrue(perkManager.addPerkToPlayer(uuid, perk,
                com.vampirez.api.event.PlayerPerkGainedEvent.Source.INTERNAL, false));
        assertEquals(0, perk.applyCalls, "applyNow=false must not run apply() even for online players");
    }

    @Test
    void reconnectSequenceAppliesEachPerkExactlyOnce() {
        // Simulates the reconnect flow: N pending auto-assigns + one bulk reapply.
        CountingPerk a = new CountingPerk("a");
        CountingPerk b = new CountingPerk("b");
        perkManager.addPerkToPlayer(uuid, a, com.vampirez.api.event.PlayerPerkGainedEvent.Source.INTERNAL, false);
        perkManager.addPerkToPlayer(uuid, b, com.vampirez.api.event.PlayerPerkGainedEvent.Source.INTERNAL, false);

        perkManager.reapplyPerks(uuid);

        assertEquals(1, a.applyCalls, "the old flow applied twice here — items duplicated");
        assertEquals(1, b.applyCalls);
    }
}
