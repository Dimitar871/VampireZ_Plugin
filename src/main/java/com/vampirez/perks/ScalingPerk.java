package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
import com.vampirez.GameManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ScalingPerk extends Perk {

    private final Set<UUID> reachedTier1 = new HashSet<>();
    private final Set<UUID> reachedTier2 = new HashSet<>();
    private final Set<UUID> reachedTier3 = new HashSet<>();

    public ScalingPerk() {
        super("scaling", "Scaling", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.EXPERIENCE_BOTTLE,
                "Grow stronger over time!",
                "10 min: Armor gains Protection I",
                "15 min: Armor upgrades to Protection III",
                "20 min: Double your max hearts");
    }

    @Override
    public void apply(Player player) {
        // No immediate effect - upgrades come over time
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        // Reset max health if tier 3 was applied
        if (reachedTier3.contains(uuid)) {
            double baseMax = 20.0;
            // Check for other perks that modify max health by resetting to base
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(baseMax);
        }
        reachedTier1.remove(uuid);
        reachedTier2.remove(uuid);
        reachedTier3.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        VampireZPlugin vPlugin = (VampireZPlugin) getPlugin();
        GameManager gm = vPlugin.getGameManager();
        int elapsed = gm.getGameDurationSeconds() - gm.getRemainingSeconds();
        UUID uuid = player.getUniqueId();

        // Tier 1: 10 minutes (600s) - Protection I on all armor
        if (elapsed >= 600 && !reachedTier1.contains(uuid)) {
            reachedTier1.add(uuid);
            applyProtectionLevel(player, 1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Scaling" + ChatColor.GRAY + " upgraded! Armor gained " + ChatColor.WHITE + "Protection I");
            incrementStat(uuid, "upgrades");
        }

        // Tier 2: 15 minutes (900s) - Protection III on all armor
        if (elapsed >= 900 && !reachedTier2.contains(uuid)) {
            reachedTier2.add(uuid);
            applyProtectionLevel(player, 3);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Scaling" + ChatColor.GRAY + " upgraded! Armor gained " + ChatColor.WHITE + "Protection III");
            incrementStat(uuid, "upgrades");
        }

        // Tier 3: 20 minutes (1200s) - Double max hearts
        if (elapsed >= 1200 && !reachedTier3.contains(uuid)) {
            reachedTier3.add(uuid);
            double currentMax = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMax * 2);
            player.setHealth(Math.min(player.getHealth() + currentMax, currentMax * 2));
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.5f);
            player.sendMessage(ChatColor.GREEN + "Scaling" + ChatColor.GRAY + " upgraded! " + ChatColor.RED + "Max hearts doubled!");
            incrementStat(uuid, "upgrades");
        }
    }

    private void applyProtectionLevel(Player player, int level) {
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType() == Material.AIR) continue;
            ItemMeta meta = armor.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.PROTECTION, level, true);
                armor.setItemMeta(meta);
            }
        }
    }

    @Override
    public void onRespawn(Player player) {
        // Re-apply upgrades the player has already earned
        UUID uuid = player.getUniqueId();
        if (reachedTier2.contains(uuid)) {
            applyProtectionLevel(player, 3);
        } else if (reachedTier1.contains(uuid)) {
            applyProtectionLevel(player, 1);
        }
        if (reachedTier3.contains(uuid)) {
            double currentMax = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            // Only double if not already doubled (base is 20)
            if (currentMax < 40) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(currentMax * 2);
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("upgrades", "Upgrades Received");
        return labels;
    }
}
