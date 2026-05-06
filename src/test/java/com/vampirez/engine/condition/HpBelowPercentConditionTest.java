package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HpBelowPercentConditionTest {

    @Test
    void victimAtThirtyFivePercent_passesFortyPercentThreshold() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(7.0);
        when(victim.getMaxHealth()).thenReturn(20.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        assertTrue(new HpBelowPercentCondition(Target.VICTIM, 0.40).test(ctx));
    }

    @Test
    void victimAtFortyOnePercent_failsFortyPercentThreshold() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(8.2);
        when(victim.getMaxHealth()).thenReturn(20.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        assertFalse(new HpBelowPercentCondition(Target.VICTIM, 0.40).test(ctx));
    }

    @Test
    void noVictimInContextReturnsFalse() {
        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).build();
        assertFalse(new HpBelowPercentCondition(Target.VICTIM, 0.5).test(ctx));
    }

    @Test
    void zeroMaxHealthReturnsFalse() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(5.0);
        when(victim.getMaxHealth()).thenReturn(0.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        assertFalse(new HpBelowPercentCondition(Target.VICTIM, 0.5).test(ctx));
    }
}
