package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class HemophiliaPerk extends Perk {

    // attackerUUID -> (targetUUID -> timestamp)
    private final Map<UUID, Map<UUID, Long>> bleedsByAttacker = new HashMap<>();
    private static final long BLEED_DURATION_MS = 4000;

    public HemophiliaPerk() {
        super("hemophilia", "Hemophilia", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.REDSTONE,
                "Attacks inflict Bleeding:",
                "0.5 hearts/sec for 4s, refreshes on hit");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        bleedsByAttacker.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID attackerUUID = player.getUniqueId();
        Map<UUID, Long> targets = bleedsByAttacker.get(attackerUUID);
        if (targets == null || targets.isEmpty()) return;

        long now = System.currentTimeMillis();

        Iterator<Map.Entry<UUID, Long>> it = targets.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID targetUUID = entry.getKey();
            long expiry = entry.getValue() + BLEED_DURATION_MS;

            if (now > expiry) {
                it.remove();
                continue;
            }

            Player target = Bukkit.getPlayer(targetUUID);
            if (target != null && target.isOnline()) {
                target.damage(1.0); // 0.5 hearts
                // Dripping dark red particles
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1.5, 0), 25, 0.3, 0.8, 0.3,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 1.2f));
                addStat(attackerUUID, "bleed_damage", 1.0);
            }
        }
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;

        UUID attackerUUID = attacker.getUniqueId();
        bleedsByAttacker.computeIfAbsent(attackerUUID, k -> new HashMap<>())
                .put(target.getUniqueId(), System.currentTimeMillis());
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.5f, 0.6f);
        // Blood spray on hit
        target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(200, 0, 0), 1.0f));
        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1.2, 0), 5, 0.2, 0.3, 0.2, 0.02);
        incrementStat(attackerUUID, "bleeds_applied");
        target.sendMessage(ChatColor.DARK_RED + "You are bleeding!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bleeds_applied", "Bleeds Applied");
        labels.put("bleed_damage", "Bleed Damage");
        return labels;
    }
}
