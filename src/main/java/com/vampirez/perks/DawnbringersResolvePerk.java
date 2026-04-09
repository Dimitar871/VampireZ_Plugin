package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DawnbringersResolvePerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();

    public DawnbringersResolvePerk() {
        super("dawnbringers_resolve", "Dawnbringer's Resolve", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.SUNFLOWER,
                "Auto-regen 1 heart/2s when below 4 hearts");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        if (player.getHealth() >= 8.0) { // 4 hearts
            tickCounters.remove(player.getUniqueId());
            return;
        }

        int count = tickCounters.getOrDefault(player.getUniqueId(), 0) + 1;
        tickCounters.put(player.getUniqueId(), count);

        // Every 2 ticks (2 seconds since onTick fires every second)
        if (count >= 2) {
            tickCounters.put(player.getUniqueId(), 0);
            double newHealth = Math.min(player.getHealth() + 2.0, player.getMaxHealth()); // 1 heart
            player.setHealth(newHealth);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.3, 0.3, 0);
        }
    }
}
