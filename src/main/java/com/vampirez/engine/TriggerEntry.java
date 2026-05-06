package com.vampirez.engine;

import com.vampirez.engine.action.Action;
import com.vampirez.engine.condition.Condition;

import java.util.List;

public class TriggerEntry {
    private final List<Condition> conditions;
    private final List<Action> actions;

    public TriggerEntry(List<Condition> conditions, List<Action> actions) {
        this.conditions = conditions;
        this.actions = actions;
    }

    public boolean matches(HookContext ctx) {
        for (Condition c : conditions) {
            if (!c.test(ctx)) return false;
        }
        return true;
    }

    public void execute(HookContext ctx) {
        for (Action a : actions) {
            a.run(ctx);
        }
    }
}
