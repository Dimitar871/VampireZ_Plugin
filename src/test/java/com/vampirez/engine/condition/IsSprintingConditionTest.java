package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IsSprintingConditionTest {

    @Test
    void sprintingAttacker_passes() {
        Player attacker = mock(Player.class);
        when(attacker.isSprinting()).thenReturn(true);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).attacker(attacker).build();

        assertTrue(new IsSprintingCondition(Target.ATTACKER).test(ctx));
    }

    @Test
    void notSprinting_fails() {
        Player attacker = mock(Player.class);
        when(attacker.isSprinting()).thenReturn(false);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).attacker(attacker).build();

        assertFalse(new IsSprintingCondition(Target.ATTACKER).test(ctx));
    }

    @Test
    void targetMissing_fails() {
        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).build();
        assertFalse(new IsSprintingCondition(Target.ATTACKER).test(ctx));
    }
}
