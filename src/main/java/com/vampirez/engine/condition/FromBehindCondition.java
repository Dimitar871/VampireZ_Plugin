package com.vampirez.engine.condition;

import com.vampirez.engine.HookContext;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * True if the attacker is positioned behind the victim, based on a horizontal
 * dot product between the victim's facing direction and the vector from victim
 * to attacker. Works for any LivingEntity (mobs and players both have a facing
 * direction).
 */
public class FromBehindCondition implements Condition {

    @Override
    public boolean test(HookContext ctx) {
        Player attacker = ctx.attacker;
        if (attacker == null) return false;

        LivingEntity victim;
        if (ctx.victimPlayer != null) {
            victim = ctx.victimPlayer;
        } else if (ctx.victim instanceof LivingEntity) {
            victim = (LivingEntity) ctx.victim;
        } else {
            return false;
        }

        Vector victimDir = victim.getLocation().getDirection();
        Vector toAttacker = attacker.getLocation().toVector().subtract(victim.getLocation().toVector());

        victimDir.setY(0);
        toAttacker.setY(0);
        if (victimDir.lengthSquared() == 0 || toAttacker.lengthSquared() == 0) return false;
        victimDir.normalize();
        toAttacker.normalize();

        return victimDir.dot(toAttacker) < 0;
    }
}
