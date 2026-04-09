package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BlackCleaverPerk extends Perk {

    // Static shared state — debuff is visible to all attackers
    // int[0] = stacks (1-5)
    public static final Map<UUID, int[]> cleavedTargets = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> cleavedExpiry = new ConcurrentHashMap<>();

    private static final int MAX_STACKS = 5;
    private static final long DURATION_MS = 10000;

    public BlackCleaverPerk() {
        super("black_cleaver", "Black Cleaver", PerkTier.GOLD, PerkTeam.BOTH,
                Material.IRON_AXE,
                "Each hit shreds 5% armor (max 5 stacks = 25%).",
                "Lasts 10s, refreshed on hit.",
                "Debuff is shared — allies benefit too.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        UUID targetUUID = target.getUniqueId();
        long now = System.currentTimeMillis();

        // Add or increment stacks
        int[] stacks = cleavedTargets.computeIfAbsent(targetUUID, k -> new int[]{0});
        if (stacks[0] < MAX_STACKS) {
            stacks[0]++;
        }
        cleavedExpiry.put(targetUUID, now + DURATION_MS);

        // Particles
        target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 0.8f);
        incrementStat(attacker.getUniqueId(), "stacks_applied");

        if (stacks[0] == MAX_STACKS) {
            target.sendMessage(ChatColor.DARK_RED + "Black Cleaver! Your armor is fully shredded (25%)!");
        }
    }

    @Override
    public void onTick(Player player) {
        // Clean up expired entries
        long now = System.currentTimeMillis();
        cleavedExpiry.entrySet().removeIf(entry -> {
            if (now > entry.getValue()) {
                cleavedTargets.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    public static boolean isCleaved(UUID victim) {
        Long expiry = cleavedExpiry.get(victim);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            cleavedTargets.remove(victim);
            cleavedExpiry.remove(victim);
            return false;
        }
        return true;
    }

    public static int getStacks(UUID victim) {
        if (!isCleaved(victim)) return 0;
        int[] stacks = cleavedTargets.get(victim);
        return stacks != null ? stacks[0] : 0;
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("stacks_applied", "Armor Shred Stacks Applied");
        return labels;
    }
}
