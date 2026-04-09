package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class Perk {

    protected final String id;
    protected final String displayName;
    protected final PerkTier tier;
    protected final PerkTeam team;
    protected final Material icon;
    protected final String[] description;

    // ===== Stat tracking =====
    private final Map<UUID, Map<String, Double>> playerStats = new HashMap<>();

    protected void addStat(UUID player, String key, double value) {
        playerStats.computeIfAbsent(player, k -> new HashMap<>()).merge(key, value, Double::sum);
    }

    protected void incrementStat(UUID player, String key) {
        addStat(player, key, 1);
    }

    public Map<String, Double> getPlayerStats(UUID player) {
        return playerStats.getOrDefault(player, Collections.emptyMap());
    }

    public void clearAllStats() {
        playerStats.clear();
    }

    /** Override in each perk to define display labels for stats. */
    public Map<String, String> getStatLabels() {
        return Collections.emptyMap();
    }

    public Perk(String id, String displayName, PerkTier tier, PerkTeam team, Material icon, String... description) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.team = team;
        this.icon = icon;
        this.description = description;
    }

    public abstract void apply(Player player);

    public abstract void remove(Player player);

    // ===== Optional event hooks (override as needed) =====

    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {}

    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {}

    public void onKill(Player killer, Player victim) {}

    public void onDeath(Player player, PlayerDeathEvent event) {}

    public void onTick(Player player) {}

    public void onHealthRegain(Player player, EntityRegainHealthEvent event) {}

    public void onInteract(Player player, PlayerInteractEvent event) {}

    public boolean negatesFallDamage() { return false; }

    public void onRespawn(Player player) {
        // No-op by default. GameManager's reapplyPerks() already calls apply()
        // after respawn. Having both caused HP-modifying perks to stack each death.
        // Override in specific perks that need extra respawn behavior (e.g. Homeguard).
    }

    // ===== Helpers =====

    /**
     * Returns the effective cooldown for an ability, accounting for the Haste perk (30% reduction).
     */
    public static long getEffectiveCooldown(Player player, long baseCooldownMs) {
        VampireZPlugin plugin = (VampireZPlugin) Bukkit.getPluginManager().getPlugin("VampireZ");
        if (plugin != null && plugin.getGameManager() != null) {
            PerkManager pm = plugin.getGameManager().getPerkManager();
            if (pm.hasPerk(player.getUniqueId(), "haste")) {
                return (long) (baseCooldownMs * 0.7);
            }
        }
        return baseCooldownMs;
    }

    protected boolean isSameTeam(Player a, Player b) {
        Team teamA = a.getScoreboard().getEntryTeam(a.getName());
        return teamA != null && teamA.hasEntry(b.getName());
    }

    protected JavaPlugin getPlugin() {
        return (JavaPlugin) Bukkit.getPluginManager().getPlugin("VampireZ");
    }

    // ===== Getters =====

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public PerkTier getTier() { return tier; }
    public PerkTeam getTeam() { return team; }
    public Material getIcon() { return icon; }
    public String[] getDescription() { return description; }

    public ItemStack createDisplayItem() {
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(tier.getColor() + displayName);
            List<String> lore = new ArrayList<>();
            lore.add("");
            for (String line : description) {
                lore.add(org.bukkit.ChatColor.GRAY + line);
            }
            lore.add("");
            lore.add(org.bukkit.ChatColor.YELLOW + "Tier: " + tier.getColor() + tier.getDisplayName());
            lore.add(org.bukkit.ChatColor.YELLOW + "Cost: " + org.bukkit.ChatColor.GREEN + tier.getCost() + " gold");
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
