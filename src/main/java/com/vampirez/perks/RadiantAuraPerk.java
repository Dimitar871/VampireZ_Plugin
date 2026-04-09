package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class RadiantAuraPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int TICKS_BETWEEN_DAMAGE = 3; // Every 3 ticks = 3s

    public RadiantAuraPerk() {
        super("radiant_aura", "Radiant Aura", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.SUNFLOWER,
                "Enemies within 6 blocks take 1 heart/3s",
                "+ get Glowing. You emit golden particles.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;
        tickCounters.put(uuid, count);

        // Continuous gold aura particles
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 30, 0.8, 0.8, 0.8, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.5f));

        // Damage + glow every 3 ticks
        if (count % TICKS_BETWEEN_DAMAGE != 0) return;

        for (Entity entity : player.getNearbyEntities(6, 6, 6)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;

            target.damage(2.0); // 1 heart
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 70, 0, false, false));
            addStat(uuid, "damage_dealt", 2.0);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("damage_dealt", "Damage Dealt");
        return labels;
    }
}
