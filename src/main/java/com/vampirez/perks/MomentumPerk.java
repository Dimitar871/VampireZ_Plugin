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

public class MomentumPerk extends Perk {

    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private final Map<UUID, Integer> stacks = new HashMap<>();
    private static final long WINDOW_MS = 3000;
    private static final int MAX_STACKS = 3;
    private static final double DAMAGE_PER_STACK = 0.08;

    public MomentumPerk() {
        super("momentum", "Momentum", PerkTier.SILVER, PerkTeam.BOTH,
                Material.BLAZE_POWDER,
                "Consecutive melee hits within 3s",
                "stack +8% damage, max 3 stacks (+24%)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastHitTime.remove(player.getUniqueId());
        stacks.remove(player.getUniqueId());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;

        UUID uuid = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastHitTime.get(uuid);

        if (last != null && (now - last) <= WINDOW_MS) {
            int current = Math.min(stacks.getOrDefault(uuid, 0) + 1, MAX_STACKS);
            stacks.put(uuid, current);
        } else {
            stacks.put(uuid, 1);
        }
        lastHitTime.put(uuid, now);

        int currentStacks = stacks.get(uuid);
        double bonus = event.getDamage() * (currentStacks * DAMAGE_PER_STACK);
        event.setDamage(event.getDamage() + bonus);
        addStat(uuid, "bonus_damage", bonus);

        if (currentStacks > 1) {
            attacker.sendMessage(ChatColor.GOLD + "Momentum x" + currentStacks + "! (+" + (int)(currentStacks * DAMAGE_PER_STACK * 100) + "% damage)");
        }

        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.5f, 1.5f);
        // Orange particles on hit
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(Color.ORANGE, 1.5f));
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}
