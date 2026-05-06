package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Geometry test for the from-behind dot product. yaw=0 in Bukkit means the entity
 * faces south (+Z). So an attacker at -Z is "behind" the victim.
 */
class FromBehindConditionTest {

    @Test
    void attackerBehindVictim_passes() {
        World world = mock(World.class);
        Player victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(new Location(world, 0, 64, 0, 0f, 0f));  // facing south (+Z)

        Player attacker = mock(Player.class);
        when(attacker.getLocation()).thenReturn(new Location(world, 0, 64, -3, 0f, 0f));  // 3 blocks north (behind)

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .attacker(attacker).victimPlayer(victim).build();

        assertTrue(new FromBehindCondition().test(ctx));
    }

    @Test
    void attackerInFrontOfVictim_fails() {
        World world = mock(World.class);
        Player victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(new Location(world, 0, 64, 0, 0f, 0f));

        Player attacker = mock(Player.class);
        when(attacker.getLocation()).thenReturn(new Location(world, 0, 64, 3, 0f, 0f));  // 3 blocks south (front)

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .attacker(attacker).victimPlayer(victim).build();

        assertFalse(new FromBehindCondition().test(ctx));
    }

    @Test
    void attackerSideOfVictim_fails() {
        World world = mock(World.class);
        Player victim = mock(Player.class);
        when(victim.getLocation()).thenReturn(new Location(world, 0, 64, 0, 0f, 0f));

        Player attacker = mock(Player.class);
        when(attacker.getLocation()).thenReturn(new Location(world, 3, 64, 0, 0f, 0f));  // east — perpendicular

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .attacker(attacker).victimPlayer(victim).build();

        // dot product is 0 — the impl uses < 0, so a side hit doesn't count as backstab.
        assertFalse(new FromBehindCondition().test(ctx));
    }

    @Test
    void noAttackerInContext_fails() {
        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).build();
        assertFalse(new FromBehindCondition().test(ctx));
    }
}
