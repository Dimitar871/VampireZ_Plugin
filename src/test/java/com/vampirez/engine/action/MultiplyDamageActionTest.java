package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MultiplyDamageActionTest {

    @Test
    void scalesEventDamageByFactor() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(5.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).damageEvent(event).build();
        new MultiplyDamageAction(1.2).run(ctx);

        verify(event).setDamage(6.0);
    }

    @Test
    void factorBelowOneReducesDamage() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getDamage()).thenReturn(10.0);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_TAKEN).damageEvent(event).build();
        new MultiplyDamageAction(0.9).run(ctx);

        verify(event).setDamage(9.0);
    }

    @Test
    void noDamageEventInContextIsNoOp() {
        HookContext ctx = HookContext.builder(HookType.ON_TICK).build();
        // No exception, no NPE — should silently skip.
        new MultiplyDamageAction(1.5).run(ctx);
    }
}
