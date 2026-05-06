package com.vampirez.engine;

import com.vampirez.engine.action.Action;
import com.vampirez.engine.condition.Condition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerEntryTest {

    @Test
    void allConditionsTrue_matches() {
        TriggerEntry entry = new TriggerEntry(
                List.of(ctx -> true, ctx -> true, ctx -> true),
                List.of()
        );
        assertTrue(entry.matches(HookContext.builder(HookType.ON_TICK).build()));
    }

    @Test
    void anyConditionFalse_doesNotMatch() {
        TriggerEntry entry = new TriggerEntry(
                List.of(ctx -> true, ctx -> false, ctx -> true),
                List.of()
        );
        assertFalse(entry.matches(HookContext.builder(HookType.ON_TICK).build()));
    }

    @Test
    void emptyConditions_alwaysMatches() {
        TriggerEntry entry = new TriggerEntry(List.of(), List.of());
        assertTrue(entry.matches(HookContext.builder(HookType.ON_TICK).build()));
    }

    @Test
    void execute_runsAllActionsInOrder() {
        AtomicInteger counter = new AtomicInteger();
        Action a1 = ctx -> counter.set(counter.get() * 10 + 1);
        Action a2 = ctx -> counter.set(counter.get() * 10 + 2);
        Action a3 = ctx -> counter.set(counter.get() * 10 + 3);

        TriggerEntry entry = new TriggerEntry(List.of(), List.of(a1, a2, a3));
        entry.execute(HookContext.builder(HookType.ON_TICK).build());

        assertEquals(123, counter.get());
    }
}
