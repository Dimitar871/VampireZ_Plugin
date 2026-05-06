package com.vampirez;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guard for the event-priority fix that was applied after damage-multiplier perks
 * were silently no-op'd. The bug: PerkListener.onDamage used to run at NORMAL priority — but
 * GameListener.onEntityDamage runs at HIGH and overwrites event.setDamage(finalDamage),
 * wiping out everything PerkListener had done. The fix bumped PerkListener handlers to HIGHEST
 * so they run AFTER GameListener.
 *
 * If anyone ever changes either listener's priority such that PerkListener no longer runs
 * after GameListener, this test fails — telling them exactly why and where.
 */
class PerkListenerPriorityTest {

    @Test
    void perkListener_onDamage_isHighestPriority() throws Exception {
        EventPriority p = priorityOf(PerkListener.class, "onDamage", EntityDamageByEntityEvent.class);
        assertEquals(EventPriority.HIGHEST, p,
                "PerkListener.onDamage MUST run at HIGHEST so perk damage modifications survive "
                + "GameListener.onEntityDamage (HIGH) overwriting event.setDamage(finalDamage).");
    }

    @Test
    void perkListener_onDamagePost_isHighestPriority() throws Exception {
        EventPriority p = priorityOf(PerkListener.class, "onDamagePost", EntityDamageByEntityEvent.class);
        assertEquals(EventPriority.HIGHEST, p,
                "PerkListener.onDamagePost (Black Cleaver / damage cap) MUST run at HIGHEST.");
    }

    @Test
    void perkListenerHighestRunsStrictlyAfterGameListenerHigh() throws Exception {
        EventPriority perk = priorityOf(PerkListener.class, "onDamage", EntityDamageByEntityEvent.class);
        EventPriority game = priorityOf(GameListener.class, "onEntityDamage", EntityDamageByEntityEvent.class);

        // Bukkit fires LOWEST → LOW → NORMAL → HIGH → HIGHEST → MONITOR. Higher ordinal = runs later.
        assertTrue(perk.ordinal() > game.ordinal(),
                "PerkListener (" + perk + ") must run AFTER GameListener (" + game + ") for damage perks to work. "
                + "If you changed one of these priorities, you have re-introduced the silent no-op bug.");
    }

    private static EventPriority priorityOf(Class<?> listener, String methodName, Class<?>... params) throws Exception {
        Method m = listener.getMethod(methodName, params);
        EventHandler annotation = m.getAnnotation(EventHandler.class);
        assertNotNull(annotation, listener.getSimpleName() + "." + methodName + " is missing @EventHandler");
        return annotation.priority();
    }
}
