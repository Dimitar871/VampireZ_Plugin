package com.vampirez.engine;

import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.engine.action.Action;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Verifies DataDrivenPerk dispatches to the correct hook's TriggerEntry list and ignores
 * entries registered under other hooks.
 */
class DataDrivenPerkTest {

    @Test
    void onDamageDealt_runsOnlyDamageDealtTriggers() {
        AtomicInteger dealtCalls = new AtomicInteger();
        AtomicInteger takenCalls = new AtomicInteger();

        Action incDealt = ctx -> dealtCalls.incrementAndGet();
        Action incTaken = ctx -> takenCalls.incrementAndGet();

        Map<HookType, List<TriggerEntry>> triggers = new EnumMap<>(HookType.class);
        triggers.put(HookType.ON_DAMAGE_DEALT, List.of(new TriggerEntry(List.of(), List.of(incDealt))));
        triggers.put(HookType.ON_DAMAGE_TAKEN, List.of(new TriggerEntry(List.of(), List.of(incTaken))));

        PerkConfig config = new PerkConfig("test", "Test", PerkTier.SILVER, PerkTeam.BOTH,
                Material.STONE, new String[]{"desc"}, triggers);
        DataDrivenPerk perk = new DataDrivenPerk(config);

        Player attacker = mock(Player.class);
        Player victim = mock(Player.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);

        perk.onDamageDealt(attacker, victim, event);

        assertEquals(1, dealtCalls.get(), "damage-dealt action should fire");
        assertEquals(0, takenCalls.get(), "damage-taken action should NOT fire");
    }

    @Test
    void multipleEntriesForSameHook_allFire() {
        AtomicInteger callCount = new AtomicInteger();
        Action inc = ctx -> callCount.incrementAndGet();

        Map<HookType, List<TriggerEntry>> triggers = new EnumMap<>(HookType.class);
        triggers.put(HookType.ON_DAMAGE_DEALT, List.of(
                new TriggerEntry(List.of(), List.of(inc)),
                new TriggerEntry(List.of(), List.of(inc)),
                new TriggerEntry(List.of(), List.of(inc))
        ));

        PerkConfig config = new PerkConfig("test", "Test", PerkTier.SILVER, PerkTeam.BOTH,
                Material.STONE, new String[0], triggers);
        DataDrivenPerk perk = new DataDrivenPerk(config);

        perk.onDamageDealt(mock(Player.class), mock(Player.class), mock(EntityDamageByEntityEvent.class));

        assertEquals(3, callCount.get());
    }

    @Test
    void hookWithNoTriggers_doesNothing() {
        PerkConfig config = new PerkConfig("empty", "Empty", PerkTier.SILVER, PerkTeam.BOTH,
                Material.STONE, new String[0], new EnumMap<>(HookType.class));
        DataDrivenPerk perk = new DataDrivenPerk(config);

        // Should not throw
        perk.apply(mock(Player.class));
        perk.onDamageDealt(mock(Player.class), mock(Player.class), mock(EntityDamageByEntityEvent.class));
        perk.onTick(mock(Player.class));
    }

    @Test
    void perkExposesIdAndDisplayName() {
        PerkConfig config = new PerkConfig("blunt_force", "Blunt Force", PerkTier.SILVER, PerkTeam.BOTH,
                Material.IRON_INGOT, new String[]{"+20% melee damage"}, new EnumMap<>(HookType.class));
        DataDrivenPerk perk = new DataDrivenPerk(config);

        assertEquals("blunt_force", perk.getId());
        assertEquals("Blunt Force", perk.getDisplayName());
        assertEquals(PerkTier.SILVER, perk.getTier());
        assertEquals(PerkTeam.BOTH, perk.getTeam());
        assertEquals(Material.IRON_INGOT, perk.getIcon());
    }
}
