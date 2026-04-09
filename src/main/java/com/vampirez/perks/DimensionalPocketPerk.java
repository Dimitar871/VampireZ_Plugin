package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class DimensionalPocketPerk extends Perk {

    private final Map<UUID, Double> storedHP = new HashMap<>();
    private final Map<UUID, Long> storeTime = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long STORE_DURATION_MS = 30000;
    private static final long COOLDOWN_MS = 45000;

    public DimensionalPocketPerk() {
        super("dimensional_pocket", "Dimensional Pocket", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.ENDER_EYE,
                "Right-click: store HP. Click again",
                "within 30s: swap to stored HP (45s cd).");
    }

    @Override
    public void apply(Player player) {
        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = eye.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Dimensional Pocket" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Store your HP, then swap back",
                    ChatColor.YELLOW + "Cooldown: 45s"
            ));
            eye.setItemMeta(meta);
        }
        player.getInventory().addItem(eye);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        storedHP.remove(uuid);
        storeTime.remove(uuid);
        cooldowns.remove(uuid);
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.ENDER_EYE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Dimensional Pocket")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.ENDER_EYE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Dimensional Pocket")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Check cooldown
        Long lastUse = cooldowns.get(uuid);
        if (lastUse != null && (now - lastUse) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            long remaining = (getEffectiveCooldown(player, COOLDOWN_MS) - (now - lastUse)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Dimensional Pocket on cooldown! " + remaining + "s");
            return;
        }

        Double stored = storedHP.get(uuid);
        Long storedTime = storeTime.get(uuid);

        if (stored == null) {
            // Store current HP
            double currentHP = player.getHealth();
            storedHP.put(uuid, currentHP);
            storeTime.put(uuid, now);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.5);
            player.sendMessage(ChatColor.DARK_PURPLE + "HP stored! (" + String.format("%.1f", currentHP / 2) + " hearts) Click again within 30s to swap.");
        } else if (storedTime != null && (now - storedTime) <= STORE_DURATION_MS) {
            // Swap HP
            double currentHP = player.getHealth();
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double swapHP = Math.min(stored, maxHealth);

            player.setHealth(Math.max(swapHP, 1.0)); // Don't kill the player
            storedHP.remove(uuid);
            storeTime.remove(uuid);
            cooldowns.put(uuid, now);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
            player.sendMessage(ChatColor.DARK_PURPLE + "HP swapped! " + String.format("%.1f", currentHP / 2) + " -> " + String.format("%.1f", swapHP / 2) + " hearts.");
            incrementStat(uuid, "swaps");
        } else {
            // Expired
            storedHP.remove(uuid);
            storeTime.remove(uuid);
            player.sendMessage(ChatColor.RED + "Stored HP expired! Storing new HP...");
            // Store fresh
            double currentHP = player.getHealth();
            storedHP.put(uuid, currentHP);
            storeTime.put(uuid, now);
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);
            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.5);
            player.sendMessage(ChatColor.DARK_PURPLE + "HP stored! (" + String.format("%.1f", currentHP / 2) + " hearts) Click again within 30s to swap.");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("swaps", "HP Swaps");
        return labels;
    }
}
