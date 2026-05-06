package com.vampirez.engine.action;

import com.vampirez.engine.HookContext;
import com.vampirez.engine.HookType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SetDamageActionTest {

    @Test
    void overwritesEventDamageWithFixedValue() {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);

        HookContext ctx = HookContext.builder(HookType.ON_DAMAGE_DEALT).damageEvent(event).build();
        new SetDamageAction(100.0).run(ctx);

        verify(event).setDamage(100.0);
    }

    @Test
    void noDamageEventIsNoOp() {
        HookContext ctx = HookContext.builder(HookType.ON_TICK).build();
        new SetDamageAction(50.0).run(ctx);
    }
}
