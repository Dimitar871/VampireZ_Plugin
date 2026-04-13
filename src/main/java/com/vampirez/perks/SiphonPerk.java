package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class SiphonPerk extends Perk {

    private final Map<UUID, Map<UUID, Long>> targetCooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 8000;

    public SiphonPerk() {
        super("siphon", "Siphon", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GHAST_TEAR,
                "Melee hit steals 1 heart from target",
                "(8s cooldown per target)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        targetCooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        UUID attackerUUID = attacker.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        long now = System.currentTimeMillis();

        Map<UUID, Long> cooldowns = targetCooldowns.computeIfAbsent(attackerUUID, k -> new HashMap<>());
        Long last = cooldowns.get(targetUUID);
        if (last != null && (now - last) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;
        cooldowns.put(targetUUID, now);

        // Extra 2 HP damage
        event.setDamage(event.getDamage() + 2.0);

        // Heal self 2 HP (use getValue() to include modifiers from other perks)
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = attacker.getHealth();
        double actualHeal = Math.min(2.0, maxHealth - currentHealth);
        attacker.setHealth(Math.min(currentHealth + 2.0, maxHealth));

        attacker.getWorld().spawnParticle(Particle.DUST, attacker.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f));
        attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2, 0), 5, 0.3, 0.3, 0.3, 0);
        addStat(attackerUUID, "damage_stolen", 2.0);
        if (actualHeal > 0) {
            addStat(attackerUUID, "health_stolen", actualHeal);
        }
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.5f);
        attacker.sendMessage(ChatColor.RED + "Siphon! Stole 1 heart from " + target.getName());
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("damage_stolen", "Damage Stolen");
        labels.put("health_stolen", "Health Stolen");
        return labels;
    }
}
