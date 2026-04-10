package com.vampirez;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Saves and restores full player state (inventory, location, HP, XP, effects, etc.)
 * so that joining a VampireZ game never loses or duplicates items from the survival world.
 *
 * States are persisted to disk (YAML files) so a server crash mid-game won't lose inventories.
 */
public class PlayerStateManager {

    private final JavaPlugin plugin;
    private final File dataFolder;

    public PlayerStateManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "saved-states");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    /**
     * Saves the player's complete state to disk and clears them for the minigame.
     * Must be called BEFORE any inventory/state changes for the game.
     */
    public void saveAndClear(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolder, uuid + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();

        // --- Save location ---
        Location loc = player.getLocation();
        cfg.set("location.world", loc.getWorld().getName());
        cfg.set("location.x", loc.getX());
        cfg.set("location.y", loc.getY());
        cfg.set("location.z", loc.getZ());
        cfg.set("location.yaw", (double) loc.getYaw());
        cfg.set("location.pitch", (double) loc.getPitch());

        // --- Save inventory (main + armor + offhand) ---
        cfg.set("inventory.contents", itemArrayToList(player.getInventory().getContents()));
        cfg.set("inventory.armor", itemArrayToList(player.getInventory().getArmorContents()));
        cfg.set("inventory.offhand", player.getInventory().getItemInOffHand());

        // --- Save XP ---
        cfg.set("xp.level", player.getLevel());
        cfg.set("xp.exp", (double) player.getExp());

        // --- Save health / food ---
        cfg.set("health", player.getHealth());
        cfg.set("max-health", player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getBaseValue());
        cfg.set("food-level", player.getFoodLevel());
        cfg.set("saturation", (double) player.getSaturation());
        cfg.set("exhaustion", (double) player.getExhaustion());

        // --- Save gamemode ---
        cfg.set("gamemode", player.getGameMode().name());

        // --- Save potion effects ---
        List<Map<String, Object>> effects = new ArrayList<>();
        for (PotionEffect effect : player.getActivePotionEffects()) {
            effects.add(effect.serialize());
        }
        cfg.set("potion-effects", effects);

        // --- Save misc ---
        cfg.set("fire-ticks", player.getFireTicks());
        cfg.set("walk-speed", (double) player.getWalkSpeed());
        cfg.set("fly-speed", (double) player.getFlySpeed());
        cfg.set("allow-flight", player.getAllowFlight());
        cfg.set("flying", player.isFlying());

        // --- Write to disk BEFORE clearing (crash safety) ---
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player state for " + player.getName() + ": " + e.getMessage());
            return;
        }

        // --- Now clear the player for the minigame ---
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setLevel(0);
        player.setExp(0f);
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setExhaustion(0f);
        player.setFireTicks(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        player.setGameMode(GameMode.SURVIVAL);

        plugin.getLogger().info("Saved state for " + player.getName());
    }

    /**
     * Restores the player's complete state from disk.
     * Clears all minigame items/effects BEFORE restoring, preventing any transfer.
     */
    public void restore(Player player) {
        UUID uuid = player.getUniqueId();
        File file = new File(dataFolder, uuid + ".yml");
        if (!file.exists()) {
            plugin.getLogger().warning("No saved state for " + player.getName() + " — nothing to restore.");
            return;
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        // --- Step 1: Completely wipe the player (remove all minigame data) ---
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setItemInOffHand(null);
        player.setLevel(0);
        player.setExp(0f);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Reset all attribute modifiers (perks may have added these)
        org.bukkit.attribute.Attribute[] attrs = {
                org.bukkit.attribute.Attribute.MAX_HEALTH,
                org.bukkit.attribute.Attribute.ATTACK_SPEED,
                org.bukkit.attribute.Attribute.MOVEMENT_SPEED,
                org.bukkit.attribute.Attribute.ARMOR,
                org.bukkit.attribute.Attribute.ARMOR_TOUGHNESS,
                org.bukkit.attribute.Attribute.KNOCKBACK_RESISTANCE
        };
        for (org.bukkit.attribute.Attribute attr : attrs) {
            org.bukkit.attribute.AttributeInstance instance = player.getAttribute(attr);
            if (instance != null) {
                for (org.bukkit.attribute.AttributeModifier mod : new ArrayList<>(instance.getModifiers())) {
                    instance.removeModifier(mod);
                }
            }
        }

        // --- Step 2: Restore saved state ---

        // Restore max health base BEFORE setting health
        double maxHealth = cfg.getDouble("max-health", 20.0);
        player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).setBaseValue(maxHealth);

        // Restore health (clamped to max)
        double health = cfg.getDouble("health", maxHealth);
        player.setHealth(Math.min(health, maxHealth));

        // Restore food
        player.setFoodLevel(cfg.getInt("food-level", 20));
        player.setSaturation((float) cfg.getDouble("saturation", 20.0));
        player.setExhaustion((float) cfg.getDouble("exhaustion", 0.0));

        // Restore XP
        player.setLevel(cfg.getInt("xp.level", 0));
        player.setExp((float) cfg.getDouble("xp.exp", 0.0));

        // Restore inventory
        List<?> contentsList = cfg.getList("inventory.contents");
        if (contentsList != null) {
            ItemStack[] contents = listToItemArray(contentsList, 41);
            player.getInventory().setContents(contents);
        }
        List<?> armorList = cfg.getList("inventory.armor");
        if (armorList != null) {
            ItemStack[] armor = listToItemArray(armorList, 4);
            player.getInventory().setArmorContents(armor);
        }
        ItemStack offhand = cfg.getItemStack("inventory.offhand");
        if (offhand != null) {
            player.getInventory().setItemInOffHand(offhand);
        }

        // Restore potion effects
        List<?> effectsList = cfg.getList("potion-effects");
        if (effectsList != null) {
            for (Object obj : effectsList) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    PotionEffect effect = new PotionEffect((Map<String, Object>) obj);
                    player.addPotionEffect(effect);
                }
            }
        }

        // Restore gamemode
        String gm = cfg.getString("gamemode", "SURVIVAL");
        try {
            player.setGameMode(GameMode.valueOf(gm));
        } catch (IllegalArgumentException e) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Restore misc
        player.setFireTicks(cfg.getInt("fire-ticks", 0));
        player.setWalkSpeed((float) cfg.getDouble("walk-speed", 0.2));
        player.setFlySpeed((float) cfg.getDouble("fly-speed", 0.1));
        boolean allowFlight = cfg.getBoolean("allow-flight", false);
        player.setAllowFlight(allowFlight);
        if (allowFlight && cfg.getBoolean("flying", false)) {
            player.setFlying(true);
        }

        // Restore location
        String worldName = cfg.getString("location.world");
        if (worldName != null) {
            org.bukkit.World world = org.bukkit.Bukkit.getWorld(worldName);
            if (world != null) {
                Location savedLoc = new Location(world,
                        cfg.getDouble("location.x"),
                        cfg.getDouble("location.y"),
                        cfg.getDouble("location.z"),
                        (float) cfg.getDouble("location.yaw"),
                        (float) cfg.getDouble("location.pitch"));
                player.teleport(savedLoc);
            }
        }

        // --- Step 3: Delete saved file (successful restore) ---
        file.delete();
        plugin.getLogger().info("Restored state for " + player.getName());
    }

    /**
     * Returns true if the player has a saved state on disk (e.g. from a crash).
     */
    public boolean hasSavedState(UUID uuid) {
        return new File(dataFolder, uuid + ".yml").exists();
    }

    // ===== Serialization helpers =====

    private List<ItemStack> itemArrayToList(ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack item : items) {
            list.add(item); // null entries are preserved
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private ItemStack[] listToItemArray(List<?> list, int expectedSize) {
        ItemStack[] items = new ItemStack[Math.max(list.size(), expectedSize)];
        for (int i = 0; i < list.size(); i++) {
            Object obj = list.get(i);
            if (obj instanceof ItemStack) {
                items[i] = (ItemStack) obj;
            }
        }
        return items;
    }
}
