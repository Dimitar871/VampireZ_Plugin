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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class NaturalLeaderPerk extends Perk {

    private final Map<UUID, Long> lastGoldTick = new HashMap<>();
    private final Map<UUID, Long> lastRegenCheck = new HashMap<>();
    private static final long GOLD_INTERVAL_MS = 3000;
    private static final long REGEN_INTERVAL_MS = 1000;
    private static final double REGEN_CHANCE = 0.01; // 1% per second
    private static final double NEARBY_RADIUS = 15.0;

    public NaturalLeaderPerk() {
        super("natural_leader", "Natural Leader", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.TORCH,
                "Glow visible through walls.",
                "+1g/3s per nearby human.",
                "1%/s: Regen I (4s) to humans in 15 blocks.");
    }

    @Override
    public void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(PotionEffectType.GLOWING);
        lastGoldTick.remove(player.getUniqueId());
        lastRegenCheck.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        VampireZPlugin vPlugin = (VampireZPlugin) getPlugin();

        // Count nearby humans (excluding self)
        List<Player> nearbyHumans = new ArrayList<>();
        for (UUID humanUUID : vPlugin.getGameManager().getHumanTeam()) {
            if (humanUUID.equals(uuid)) continue;
            Player human = Bukkit.getPlayer(humanUUID);
            if (human != null && human.isOnline() && human.getWorld().equals(player.getWorld())) {
                if (human.getLocation().distance(player.getLocation()) <= NEARBY_RADIUS) {
                    nearbyHumans.add(human);
                }
            }
        }

        // +1 gold per nearby human every 3 seconds
        Long lastGold = lastGoldTick.get(uuid);
        if (lastGold == null || (now - lastGold) >= GOLD_INTERVAL_MS) {
            lastGoldTick.put(uuid, now);
            int nearbyCount = nearbyHumans.size();
            if (nearbyCount > 0) {
                vPlugin.getGameManager().getEconomyManager().addGold(uuid, nearbyCount);
                incrementStat(uuid, "gold_earned", nearbyCount);
            }
        }

        // 1% chance per second: Regen I (4s) to all humans within 15 blocks (including self)
        Long lastRegen = lastRegenCheck.get(uuid);
        if (lastRegen == null || (now - lastRegen) >= REGEN_INTERVAL_MS) {
            lastRegenCheck.put(uuid, now);
            if (Math.random() < REGEN_CHANCE) {
                // Apply to self + nearby humans
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, true, true, true));
                for (Player human : nearbyHumans) {
                    human.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 80, 0, true, true, true));
                }
                incrementStat(uuid, "regen_procs");
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2.2, 0), 5, 0.5, 0.3, 0.5, 0);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
                player.sendMessage(ChatColor.GREEN + "Natural Leader: Regeneration I granted to nearby humans!");
            }
        }
    }

    private void incrementStat(UUID uuid, String key, int amount) {
        addStat(uuid, key, amount);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("gold_earned", "Gold Earned");
        labels.put("regen_procs", "Regen Procs");
        return labels;
    }
}
