package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FortuneTellerPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int INTERVAL_TICKS = 20; // 20 onTick calls = 20 seconds

    public FortuneTellerPerk() {
        super("fortune_teller", "Fortune Teller", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.ENDER_EYE,
                "Every 20s: nearest vampire is marked",
                "with Glowing for 5s (visible through walls).");
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

        if (ticks < INTERVAL_TICKS) return;
        tickCounters.put(uuid, 0);

        VampireZPlugin plugin = (VampireZPlugin) getPlugin();
        if (plugin.getGameManager() == null) return;

        Set<UUID> vampires = plugin.getGameManager().getVampireTeam();
        Player nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (UUID vampUUID : vampires) {
            Player vamp = Bukkit.getPlayer(vampUUID);
            if (vamp == null || !vamp.isOnline()) continue;
            if (!vamp.getWorld().equals(player.getWorld())) continue;

            double dist = vamp.getLocation().distanceSquared(player.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = vamp;
            }
        }

        if (nearest == null) return;

        // Apply Glowing effect for 5 seconds (100 ticks) — visible through walls
        nearest.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false));

        // Particles on the marked vampire
        nearest.getWorld().spawnParticle(Particle.DUST, nearest.getLocation().add(0, 2.2, 0), 10, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.PURPLE, 1.5f));

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
        player.sendMessage(org.bukkit.ChatColor.LIGHT_PURPLE + "Fortune Teller marked the nearest vampire!");
        incrementStat(uuid, "pings");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("pings", "Vampire Pings");
        return labels;
    }
}
