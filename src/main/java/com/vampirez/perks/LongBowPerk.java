package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class LongBowPerk extends Perk {

    private static final double DAMAGE_PER_BLOCK = 0.01; // 1% per block
    private static final double MAX_BONUS = 0.50;         // 50% cap
    private static final double EXECUTE_DISTANCE = 75.0;   // Instant kill threshold

    public LongBowPerk() {
        super("long_bow", "Long Bow", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.BOW,
                "+1% arrow damage per block traveled",
                "Capped at +50%. Shots from 75+",
                "blocks away instantly kill.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (!(event.getDamager() instanceof Projectile)) return;
        if (event.isCancelled()) return;

        // Distance from attacker to where the arrow hit
        double distance = attacker.getLocation().distance(target.getLocation());

        // 75+ blocks = instant kill
        if (distance >= EXECUTE_DISTANCE) {
            // Set damage to victim's current health + absorption to guarantee kill
            double killDamage = target.getHealth() + target.getAbsorptionAmount() + 100;
            event.setDamage(killDamage);

            target.getWorld().spawnParticle(Particle.EXPLOSION, target.getLocation().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, 0.6f, 1.5f);
            attacker.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "LONG SHOT! " +
                    ChatColor.RESET + ChatColor.RED + "Instant kill from " + (int) distance + " blocks!");
            target.sendMessage(ChatColor.RED + "Sniped from " + (int) distance + " blocks away!");

            incrementStat(attacker.getUniqueId(), "executes");
            incrementStat(attacker.getUniqueId(), "hits");
            return;
        }

        // 1% bonus per block, capped at 50%
        double bonus = Math.min(distance * DAMAGE_PER_BLOCK, MAX_BONUS);
        event.setDamage(event.getDamage() * (1.0 + bonus));

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.5f, 1.8f);
        attacker.sendMessage(ChatColor.GOLD + "Long Bow: " + ChatColor.GRAY +
                (int) distance + " blocks (+" + (int) (bonus * 100) + "% damage)");

        incrementStat(attacker.getUniqueId(), "hits");
        addStat(attacker.getUniqueId(), "total_bonus", bonus * 100);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hits", "Long Shots");
        labels.put("executes", "Instant Kills");
        labels.put("total_bonus", "Total Bonus Dmg %");
        return labels;
    }
}
