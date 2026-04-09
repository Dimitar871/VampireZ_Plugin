package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class CookerPerk extends Perk {

    private final Map<UUID, Long> lastFeed = new HashMap<>();
    private static final long FEED_INTERVAL_MS = 60000;
    private static final double NEARBY_RADIUS = 15.0;

    public CookerPerk() {
        super("cooker", "Cooker", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.CAMPFIRE,
                "Every 60s, feed nearby human",
                "teammates +2 saturation bars.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastFeed.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastFeed.get(uuid);
        if (last != null && (now - last) < FEED_INTERVAL_MS) return;

        lastFeed.put(uuid, now);

        VampireZPlugin vPlugin = (VampireZPlugin) getPlugin();
        int fed = 0;

        for (UUID humanUUID : vPlugin.getGameManager().getHumanTeam()) {
            if (humanUUID.equals(uuid)) continue;
            Player human = Bukkit.getPlayer(humanUUID);
            if (human != null && human.isOnline() && human.getWorld().equals(player.getWorld())) {
                if (human.getLocation().distance(player.getLocation()) <= NEARBY_RADIUS) {
                    float newSat = Math.min(human.getSaturation() + 4.0f, 20.0f); // 2 bars = 4 saturation points
                    human.setSaturation(newSat);
                    human.getWorld().spawnParticle(Particle.HEART, human.getLocation().add(0, 2.2, 0), 3, 0.3, 0.2, 0.3, 0);
                    human.sendMessage(ChatColor.GREEN + "You feel well-fed! (Cooker)");
                    fed++;
                }
            }
        }

        if (fed > 0) {
            addStat(uuid, "teammates_fed", fed);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.7f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Cooker: Fed " + fed + " nearby teammate" + (fed > 1 ? "s" : "") + "!");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("teammates_fed", "Teammates Fed");
        return labels;
    }
}
