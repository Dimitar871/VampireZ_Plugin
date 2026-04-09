package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class SunfireCapePerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();

    public SunfireCapePerk() {
        super("sunfire_cape", "Sunfire Cape", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.FIRE_CHARGE,
                "Emit a burning aura around you.",
                "Enemies within 5 blocks take",
                "half a heart every 3 seconds.");
    }

    @Override
    public void apply(Player player) {
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int ticks = tickCounters.getOrDefault(uuid, 0) + 1;
        tickCounters.put(uuid, ticks);

        // Ambient particles every tick (every 1 second)
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 6, 1.5, 0.2, 1.5, 0.01);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.3, 0), 4, 2.0, 0.1, 2.0, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 100, 0), 1.2f));

        // Damage every 3 seconds (3 ticks of onTick since it fires every 1s)
        if (ticks >= 3) {
            tickCounters.put(uuid, 0);

            int hits = 0;
            for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
                if (!(entity instanceof Player target)) continue;
                if (isSameTeam(player, target)) continue;

                target.damage(1.0); // half a heart
                target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.02);
                hits++;
            }

            if (hits > 0) {
                addStat(uuid, "aura_damage", hits * 1.0);
                incrementStat(uuid, "burn_ticks");
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("aura_damage", "Aura Damage");
        labels.put("burn_ticks", "Burn Pulses");
        return labels;
    }
}
