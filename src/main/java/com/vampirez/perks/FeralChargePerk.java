package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class FeralChargePerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 6000;
    private static final double BONUS_MULTIPLIER = 0.30;

    public FeralChargePerk() {
        super("feral_charge", "Feral Charge", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.RABBIT_FOOT,
                "Sprinting attacks deal +30% damage",
                "6s cooldown");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;
        if (!attacker.isSprinting()) return;

        UUID uuid = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(attacker, COOLDOWN_MS)) return;
        cooldowns.put(uuid, now);

        double bonus = event.getDamage() * BONUS_MULTIPLIER;
        event.setDamage(event.getDamage() + bonus);
        addStat(uuid, "bonus_damage", bonus);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_RAVAGER_ATTACK, 0.5f, 1.5f);
        attacker.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);
        attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.RED, 1.5f));
        attacker.sendMessage(ChatColor.RED + "Feral Charge! +30% damage!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}
