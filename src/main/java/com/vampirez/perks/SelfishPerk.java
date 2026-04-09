package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import org.bukkit.Bukkit;
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

    public SelfishPerk() {
        super("selfish", "Selfish", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GOLDEN_APPLE,
                "No teammates within 25 blocks:",
                "  +6 max hearts + Strength I.",
                "Teammate enters range: bonus removed.");
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
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        VampireZPlugin plugin = (VampireZPlugin) getPlugin();
        if (plugin.getGameManager() == null) return;

        boolean alone = isAlone(player, plugin);

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
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 1.5f);
        } else if (alone && activeState.contains(uuid)) {
            // Refresh Strength I
            player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, true, true, true), true);
        }
    }

    private boolean isAlone(Player player, VampireZPlugin plugin) {
        UUID uuid = player.getUniqueId();
        Set<UUID> team;

        // Check which team the player is on
        if (plugin.getGameManager().getHumanTeam().contains(uuid)) {
            team = plugin.getGameManager().getHumanTeam();
        } else if (plugin.getGameManager().getVampireTeam().contains(uuid)) {
            team = plugin.getGameManager().getVampireTeam();
        } else {
            return false;
        }

        for (UUID teammateUUID : team) {
            if (teammateUUID.equals(uuid)) continue;
            Player teammate = Bukkit.getPlayer(teammateUUID);
            if (teammate != null && teammate.isOnline() && teammate.getWorld().equals(player.getWorld())) {
                if (teammate.getLocation().distance(player.getLocation()) <= CHECK_RADIUS) {
                    return false;
                }
            }
        }
        return true;
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
