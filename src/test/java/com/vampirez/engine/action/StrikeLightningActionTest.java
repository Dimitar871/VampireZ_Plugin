package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import com.vampirez.engine.condition.Target;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrikeLightningActionTest {

    private static Player victimInWorld(World world, Location loc) {
        Player victim = mock(Player.class);
        when(victim.getWorld()).thenReturn(world);
        when(victim.getLocation()).thenReturn(loc);
        return victim;
    }

    @Test
    void strikesEffectAtTargetAndDealsAttributedDamage() {
        World world = mock(World.class);
        Location loc = new Location(null, 1, 2, 3);
        Player victim = victimInWorld(world, loc);
        Player owner = mock(Player.class);

        new StrikeLightningAction(Target.VICTIM, 3.0).run(HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(owner).victim(victim).victimPlayer(victim).build());

        verify(world).strikeLightningEffect(loc);
        verify(victim).damage(3.0, owner); // attributed → counts as PvP for kill credit
    }

    @Test
    void zeroDamageIsVisualOnly() {
        World world = mock(World.class);
        Player victim = victimInWorld(world, new Location(null, 0, 0, 0));

        new StrikeLightningAction(Target.VICTIM, 0.0).run(HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(mock(Player.class)).victim(victim).victimPlayer(victim).build());

        verify(world).strikeLightningEffect(any(Location.class));
        verify(victim, never()).damage(anyDouble(), any(org.bukkit.entity.Entity.class));
        verify(victim, never()).damage(anyDouble());
    }

    @Test
    void missingTargetIsANoOp() {
        // No victim in context — nothing to strike, nothing thrown.
        new StrikeLightningAction(Target.VICTIM, 3.0).run(
                HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(mock(Player.class)).build());
    }
}
