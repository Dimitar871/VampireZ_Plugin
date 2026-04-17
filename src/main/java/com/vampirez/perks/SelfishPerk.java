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
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SelfishPerk extends Perk {

    private static final double CHECK_RADIUS = 25.0;
    private static final double BONUS_HEARTS = 6.0; // +6 max hearts = +12 HP
    private final Set<UUID> activeState = new HashSet<>();
    // Track which teammates are currently in range to avoid spamming notifications
    private final Map<UUID, Set<UUID>> nearbyTeammates = new HashMap<>();

    public SelfishPerk() {
        super("selfish", "Selfish", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GOLDEN_APPLE,
                "No teammates within 25 blocks:",
                "  +6 max hearts + Strength I.",
                "Nearby teammates are marked",
                "and notified they're in your zone.");
    }

    @Override
    public void apply(Player player) {
        // State managed by onTick
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        if (activeState.remove(uuid)) {
            removeBonus(player);
        }
        nearbyTeammates.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        VampireZPlugin plugin = (VampireZPlugin) getPlugin();
        if (plugin.getGameManager() == null) return;

        // Find all teammates in range
        List<Player> inRange = getTeammatesInRange(player, plugin);
        Set<UUID> currentNearby = new HashSet<>();
        for (Player t : inRange) currentNearby.add(t.getUniqueId());
        Set<UUID> previousNearby = nearbyTeammates.getOrDefault(uuid, Collections.emptySet());

        // Notify on new entries
        for (Player teammate : inRange) {
            if (!previousNearby.contains(teammate.getUniqueId())) {
                // New teammate entered range — notify both
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "SELFISH: " +
                        ChatColor.RESET + ChatColor.YELLOW + teammate.getName() +
                        ChatColor.GRAY + " entered your zone! Bonus removed.");
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.5f);

                teammate.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "WARNING: " +
                        ChatColor.RESET + ChatColor.GRAY + "You entered " +
                        ChatColor.YELLOW + player.getName() + "'s " +
                        ChatColor.GOLD + "Selfish" + ChatColor.GRAY + " zone! Stay 25+ blocks away.");
                teammate.playSound(teammate.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            }
            // Glowing effect on nearby teammates so selfish player can see them
            teammate.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 25, 0, false, false, false), true);
        }

        nearbyTeammates.put(uuid, currentNearby);

        boolean alone = inRange.isEmpty();

        if (alone && !activeState.contains(uuid)) {
            // Activate bonus
            activeState.add(uuid);
            applyBonus(player);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.2f);
            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } else if (!alone && activeState.contains(uuid)) {
            // Deactivate bonus
            activeState.remove(uuid);
            removeBonus(player);
        } else if (alone && activeState.contains(uuid)) {
            // Refresh Strength I
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, true, true, true), true);
        }
    }

    private List<Player> getTeammatesInRange(Player player, VampireZPlugin plugin) {
        UUID uuid = player.getUniqueId();
        Set<UUID> team;

        if (plugin.getGameManager().getHumanTeam().contains(uuid)) {
            team = plugin.getGameManager().getHumanTeam();
        } else if (plugin.getGameManager().getVampireTeam().contains(uuid)) {
            team = plugin.getGameManager().getVampireTeam();
        } else {
            return Collections.emptyList();
        }

        List<Player> result = new ArrayList<>();
        for (UUID teammateUUID : team) {
            if (teammateUUID.equals(uuid)) continue;
            Player teammate = Bukkit.getPlayer(teammateUUID);
            if (teammate != null && teammate.isOnline() && teammate.getWorld().equals(player.getWorld())) {
                if (teammate.getLocation().distance(player.getLocation()) <= CHECK_RADIUS) {
                    result.add(teammate);
                }
            }
        }
        return result;
    }

    private void applyBonus(Player player) {
        double baseMax = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseMax + BONUS_HEARTS * 2);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, true, true, true), true);
    }

    private void removeBonus(Player player) {
        double currentMax = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMax - BONUS_HEARTS * 2);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
