package com.vampirez;

import com.vampirez.engine.DataDrivenPerk;
import com.vampirez.engine.HookType;
import com.vampirez.engine.PerkConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.EnumMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

/**
 * Tests the registry swap behind perks.yml hot reload
 * ({@link PerkManager#reloadDataDrivenPerks}).
 */
class PerkManagerReloadTest {

    private PerkManager perkManager;
    private MockedStatic<Bukkit> bukkit;

    /** Minimal hand-written perk standing in for the ~123 Java perks. */
    private static class JavaPerk extends Perk {
        JavaPerk(String id) {
            super(id, "Java " + id, PerkTier.SILVER, PerkTeam.BOTH, Material.STONE, "desc");
        }
        @Override public void apply(Player player) {}
        @Override public void remove(Player player) {}
    }

    private static DataDrivenPerk yamlPerk(String id) {
        return new DataDrivenPerk(new PerkConfig(id, "Yaml " + id, PerkTier.SILVER, PerkTeam.BOTH,
                Material.PAPER, new String[]{"desc"}, new EnumMap<>(HookType.class)));
    }

    @BeforeEach
    void setUp() {
        perkManager = new PerkManager();
        bukkit = mockStatic(Bukkit.class);
        // addPerkToPlayer / removePerk fire Bukkit events; a mock PluginManager absorbs them
        bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void reloadSwapsDataDrivenPerksAndKeepsJavaPerks() {
        Perk javaPerk = new JavaPerk("blood_scent");
        perkManager.registerPerk(yamlPerk("blunt_force"));
        perkManager.registerPerk(yamlPerk("old_only"));
        perkManager.registerPerk(javaPerk);

        List<String> skipped = perkManager.reloadDataDrivenPerks(List.of(
                yamlPerk("blunt_force"),   // same id, fresh instance (e.g. new factor)
                yamlPerk("brand_new")));   // added to perks.yml since boot

        assertTrue(skipped.isEmpty());
        assertNotNull(perkManager.getPerkById("blunt_force"));
        assertNotNull(perkManager.getPerkById("brand_new"));
        assertNull(perkManager.getPerkById("old_only"), "perks removed from perks.yml must unregister");
        assertSame(javaPerk, perkManager.getPerkById("blood_scent"), "Java perks must survive untouched");
        assertEquals(3, perkManager.getAllPerks().size());
    }

    @Test
    void reloadReplacesTheInstanceNotJustTheId() {
        DataDrivenPerk stale = yamlPerk("blunt_force");
        perkManager.registerPerk(stale);

        DataDrivenPerk fresh = yamlPerk("blunt_force");
        perkManager.reloadDataDrivenPerks(List.of(fresh));

        assertSame(fresh, perkManager.getPerkById("blunt_force"),
                "the freshly parsed instance must replace the stale one, or YAML edits have no effect");
    }

    @Test
    void yamlIdCollidingWithJavaPerkIsSkipped() {
        // Boot order registers YAML first, Java after — Java wins. Reload must not invert that.
        Perk javaPerk = new JavaPerk("backstab");
        perkManager.registerPerk(javaPerk);

        List<String> skipped = perkManager.reloadDataDrivenPerks(List.of(yamlPerk("backstab")));

        assertEquals(List.of("backstab"), skipped);
        assertSame(javaPerk, perkManager.getPerkById("backstab"));
    }

    @Test
    void staleDataDrivenPerksAreDetachedFromPlayers() {
        // Admin test perks can be held in LOBBY; reload must not leave players
        // holding instances the registry no longer knows.
        DataDrivenPerk stale = yamlPerk("blunt_force");
        Perk javaPerk = new JavaPerk("blood_scent");
        perkManager.registerPerk(stale);
        perkManager.registerPerk(javaPerk);

        UUID admin = UUID.randomUUID();
        assertTrue(perkManager.addPerkToPlayer(admin, stale));
        assertTrue(perkManager.addPerkToPlayer(admin, javaPerk));

        perkManager.reloadDataDrivenPerks(List.of(yamlPerk("blunt_force")));

        assertEquals(List.of(javaPerk), perkManager.getPlayerPerks(admin),
                "stale data-driven instances must be removed from player lists; Java perks stay");
    }
}
