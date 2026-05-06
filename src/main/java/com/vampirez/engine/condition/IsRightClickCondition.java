package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.event.block.Action;

/**
 * True if the current interact event was a right-click (air or block). Lets right-click
 * ability perks share the on_interact hook without firing on left-click too.
 */
public class IsRightClickCondition implements Condition {

    @Override
    public boolean test(HookContext ctx) {
        if (ctx.interactEvent == null) return false;
        Action a = ctx.interactEvent.getAction();
        return a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
    }
}
