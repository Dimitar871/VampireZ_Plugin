package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EveryNthHitConditionTest {

    private static HookContext ctxFor(Player owner) {
        return HookContext.builder(HookType.ON_DAMAGE_DEALT).owner(owner).build();
    }

    private static Player playerWithUuid() {
        Player p = mock(Player.class);
        when(p.getUniqueId()).thenReturn(UUID.randomUUID());
        return p;
    }

    @Test
    void firesExactlyOnEveryNthCall() {
        EveryNthHitCondition cond = new EveryNthHitCondition(3);
        Player p = playerWithUuid();

        assertFalse(cond.test(ctxFor(p)));
        assertFalse(cond.test(ctxFor(p)));
        assertTrue(cond.test(ctxFor(p)), "3rd hit fires");
        assertFalse(cond.test(ctxFor(p)), "counter resets after firing");
        assertFalse(cond.test(ctxFor(p)));
        assertTrue(cond.test(ctxFor(p)), "6th hit fires again");
    }

    @Test
    void countersAreIndependentPerPlayer() {
        EveryNthHitCondition cond = new EveryNthHitCondition(2);
        Player a = playerWithUuid();
        Player b = playerWithUuid();

        assertFalse(cond.test(ctxFor(a)));
        assertFalse(cond.test(ctxFor(b)), "b's first hit must not inherit a's count");
        assertTrue(cond.test(ctxFor(a)));
        assertTrue(cond.test(ctxFor(b)));
    }

    @Test
    void nOfOneAlwaysFires() {
        EveryNthHitCondition cond = new EveryNthHitCondition(1);
        Player p = playerWithUuid();
        assertTrue(cond.test(ctxFor(p)));
        assertTrue(cond.test(ctxFor(p)));
    }

    @Test
    void missingOwnerNeverFiresAndDoesNotAdvanceCounters() {
        EveryNthHitCondition cond = new EveryNthHitCondition(2);
        assertFalse(cond.test(HookContext.builder(HookType.ON_DAMAGE_DEALT).build()));
    }
}
