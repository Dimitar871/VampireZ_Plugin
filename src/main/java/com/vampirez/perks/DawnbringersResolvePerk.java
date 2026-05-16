package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.fx.VFX;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DawnbringersResolvePerk extends Perk {

    private static final Color SUNRISE_GOLD = Color.fromRGB(255, 200, 80);

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

        // Ambient gold aura while in the low-HP state (every onTick second)
        Location ambient = player.getLocation().add(0, 1.0, 0);
        player.getWorld().spawnParticle(Particle.DUST, ambient, 6, 0.4, 0.6, 0.4, 0,
                new Particle.DustOptions(SUNRISE_GOLD, 1.0f));
        player.getWorld().spawnParticle(Particle.END_ROD, ambient, 2, 0.4, 0.5, 0.4, 0.005);

        int count = tickCounters.getOrDefault(player.getUniqueId(), 0) + 1;
        tickCounters.put(player.getUniqueId(), count);

        // Every 2 ticks (2 seconds since onTick fires every second)
        if (count >= 2) {
            tickCounters.put(player.getUniqueId(), 0);
            double newHealth = Math.min(player.getHealth() + 2.0, player.getMaxHealth()); // 1 heart
            player.setHealth(newHealth);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.4, 0.3, 0.4, 0);
            VFX.fx().helix(player.getLocation(), SUNRISE_GOLD, 14);
            player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.4f, 1.6f);
        }
    }
}
