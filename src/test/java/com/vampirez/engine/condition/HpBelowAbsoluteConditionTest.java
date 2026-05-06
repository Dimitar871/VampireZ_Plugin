package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HpBelowAbsoluteConditionTest {

    @Test
    void victimAt25hp_passesThreeHpThreshold() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(2.5);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        assertTrue(new HpBelowAbsoluteCondition(Target.VICTIM, 3.0).test(ctx));
    }

    @Test
    void victimAtThresholdExactly_passes() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(3.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        // Implementation uses <= so equality is "below" by convention (matches original Executioner)
        assertTrue(new HpBelowAbsoluteCondition(Target.VICTIM, 3.0).test(ctx));
    }

    @Test
    void victimAboveThreshold_fails() {
        Player victim = mock(Player.class);
        when(victim.getHealth()).thenReturn(3.5);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).victimPlayer(victim).build();

        assertFalse(new HpBelowAbsoluteCondition(Target.VICTIM, 3.0).test(ctx));
    }
}
