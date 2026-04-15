package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class NetherBladePerk extends Perk {

    private final Map<UUID, Integer> playerTier = new HashMap<>();
    private final Map<UUID, Integer> playerProgress = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();

    // Pending damage multiplier — set in onDamageDealt, read by PerkListener at HIGHEST priority
    private static final Map<UUID, Double> pendingDamageMult = new HashMap<>();

    /**
     * Returns and clears the pending Nether Blade damage multiplier for a player.
     * Called by PerkListener at HIGHEST priority (after GameListener sets base damage).
     */
    public static Double consumeDamageMultiplier(UUID uuid) {
        return pendingDamageMult.remove(uuid);
    }

    private static final long DASH_COOLDOWN_MS = 30000;

    // Damage multipliers per tier: tier 2 = 1.0 (matches starting sword)
    private static final double[] DAMAGE_MULT = {0.4, 0.7, 1.0, 1.3, 1.6, 1.9};

    // Requirements per tier (to unlock the NEXT tier)
    // Type: "hits" or "kills", Amount needed
    private static final String[] REQ_TYPE = {"hits", "hits", "kills", "hits", "kills"};
    private static final int[] REQ_AMOUNT = {25, 75, 5, 150, 20};

    private static final String SWORD_TAG = ChatColor.DARK_RED + "Nether Blade";

    public NetherBladePerk() {
        super("nether_blade", "Nether Blade", PerkTier.GOLD, PerkTeam.BOTH,
                Material.WOODEN_SWORD,
                "Unbreakable sword that upgrades",
                "through combat. 5 tiers with",
                "increasing power. Max tier",
                "unlocks a healing dash.");
    }

    @Override
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        playerTier.putIfAbsent(uuid, 0);
        playerProgress.putIfAbsent(uuid, 0);
        giveSword(player);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        playerTier.remove(uuid);
        playerProgress.remove(uuid);
        dashCooldowns.remove(uuid);
        removeSword(player);
    }

    private boolean isNetherBlade(ItemStack item) {
        return item != null && item.getType() == Material.WOODEN_SWORD
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("Nether Blade");
    }

    private void giveSword(Player player) {
        // Remove existing Nether Blade first
        removeSword(player);

        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        int progress = playerProgress.getOrDefault(uuid, 0);

        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SWORD_TAG + getTierSuffix(tier));
            meta.setUnbreakable(true);

            // Add sharpness for visual glow (actual damage handled via onDamageDealt)
            if (tier > 0) {
                meta.addEnchant(Enchantment.SHARPNESS, tier, true);
            }

            meta.setLore(buildLore(tier, progress));
            sword.setItemMeta(meta);
        }
        player.getInventory().addItem(sword);
    }

    private void removeSword(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isNetherBlade(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    private void updateSwordInHand(Player player) {
        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        int progress = playerProgress.getOrDefault(uuid, 0);

        // Update every Nether Blade in inventory
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isNetherBlade(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(SWORD_TAG + getTierSuffix(tier));
                    if (tier > 0) {
                        meta.addEnchant(Enchantment.SHARPNESS, tier, true);
                    }
                    meta.setLore(buildLore(tier, progress));
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private String getTierSuffix(int tier) {
        if (tier == 0) return "";
        return " " + ChatColor.GRAY + "[" + ChatColor.RED + tierToRoman(tier) + ChatColor.GRAY + "]";
    }

    private String tierToRoman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(tier);
        };
    }

    private List<String> buildLore(int tier, int progress) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(ChatColor.DARK_RED + "Tier " + (tier) + "/5");

        // Damage indicator
        int dmgPercent = (int) (DAMAGE_MULT[tier] * 100);
        lore.add(ChatColor.GRAY + "Damage: " + ChatColor.WHITE + dmgPercent + "%");

        lore.add("");

        if (tier >= 5) {
            lore.add(ChatColor.GREEN + "\u2714 MAX TIER");
            lore.add(ChatColor.AQUA + "Right-click: Nether Dash");
            lore.add(ChatColor.GRAY + "Dash forward + heal 50% max HP");
            lore.add(ChatColor.GRAY + "Cooldown: 30s");
        } else {
            // Show progress to next tier
            String reqType = REQ_TYPE[tier];
            int reqAmount = REQ_AMOUNT[tier];
            String label = reqType.equals("hits") ? "Hits" : "Kills";
            ChatColor progressColor = progress >= reqAmount ? ChatColor.GREEN : ChatColor.YELLOW;
            lore.add(ChatColor.GRAY + "Next tier: " + progressColor + progress + "/" + reqAmount + " " + label);

            // Preview next tier bonus
            int nextDmgPercent = (int) (DAMAGE_MULT[tier + 1] * 100);
            lore.add(ChatColor.GRAY + "Next tier damage: " + ChatColor.WHITE + nextDmgPercent + "%");
            if (tier == 4) {
                lore.add(ChatColor.DARK_PURPLE + "Tier V unlocks Nether Dash!");
            }
        }

        return lore;
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (event.isCancelled()) return;
        if (isSameTeam(attacker, target)) return;

        ItemStack mainHand = attacker.getInventory().getItemInMainHand();
        if (!isNetherBlade(mainHand)) return;

        UUID uuid = attacker.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);

        // Store damage multiplier for PerkListener to apply AFTER GameListener sets base damage
        pendingDamageMult.put(uuid, DAMAGE_MULT[tier]);

        // Only count full swings as hits (attack cooldown >= 0.9 = proper hit, not spam click)
        if (attacker.getAttackCooldown() < 0.9f) return;

        // Track hits for progression
        String reqType = tier < 5 ? REQ_TYPE[tier] : null;
        if (reqType != null && reqType.equals("hits")) {
            int progress = playerProgress.getOrDefault(uuid, 0) + 1;
            playerProgress.put(uuid, progress);

            // Sound feedback for stacking
            float pitch = Math.min(0.5f + (progress / (float) REQ_AMOUNT[tier]) * 1.5f, 2.0f);
            attacker.playSound(attacker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.3f, pitch);

            if (progress >= REQ_AMOUNT[tier]) {
                upgradeTier(attacker);
            } else {
                updateSwordInHand(attacker);
                // Show progress in action bar
                attacker.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        new net.md_5.bungee.api.chat.TextComponent(
                                ChatColor.DARK_RED + "Nether Blade: " + ChatColor.YELLOW +
                                        progress + "/" + REQ_AMOUNT[tier] + " Hits"));
            }
        }

        incrementStat(uuid, "hits");
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        if (tier >= 5) return;

        String reqType = REQ_TYPE[tier];
        if (!reqType.equals("kills")) return;

        int progress = playerProgress.getOrDefault(uuid, 0) + 1;
        playerProgress.put(uuid, progress);

        // Sound feedback for kill progress
        float pitch = Math.min(0.5f + (progress / (float) REQ_AMOUNT[tier]) * 1.5f, 2.0f);
        killer.playSound(killer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, pitch);

        if (progress >= REQ_AMOUNT[tier]) {
            upgradeTier(killer);
        } else {
            updateSwordInHand(killer);
            killer.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    new net.md_5.bungee.api.chat.TextComponent(
                            ChatColor.DARK_RED + "Nether Blade: " + ChatColor.YELLOW +
                                    progress + "/" + REQ_AMOUNT[tier] + " Kills"));
        }

        incrementStat(uuid, "kills");
    }

    private void upgradeTier(Player player) {
        UUID uuid = player.getUniqueId();
        int newTier = playerTier.getOrDefault(uuid, 0) + 1;
        playerTier.put(uuid, newTier);
        playerProgress.put(uuid, 0);

        updateSwordInHand(player);

        // Upgrade effects
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 0.8f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.05);
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.3, 0.3, 0);

        int dmgPercent = (int) (DAMAGE_MULT[newTier] * 100);
        player.sendMessage(ChatColor.DARK_RED + "" + ChatColor.BOLD + "NETHER BLADE UPGRADED!" +
                ChatColor.RESET + ChatColor.GOLD + " Tier " + tierToRoman(newTier) +
                ChatColor.GRAY + " (" + dmgPercent + "% damage)");

        if (newTier == 5) {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "NETHER DASH UNLOCKED! " +
                    ChatColor.RESET + ChatColor.GRAY + "Right-click to dash + heal!");
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }

        incrementStat(uuid, "upgrades");
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isNetherBlade(item)) return;

        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        if (tier < 5) return; // Dash only at max tier

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = dashCooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, DASH_COOLDOWN_MS)) {
            long remaining = (getEffectiveCooldown(player, DASH_COOLDOWN_MS) - (now - last)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Nether Dash on cooldown! " + remaining + "s");
            return;
        }
        dashCooldowns.put(uuid, now);

        // Dash forward (mostly horizontal, slight vertical)
        Vector dir = player.getLocation().getDirection();
        dir.setY(Math.max(dir.getY() * 0.2, 0.1)); // flatten, slight lift
        dir.normalize().multiply(2.5);
        player.setVelocity(dir);

        // Heal for 50% of max HP
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        double healAmount = maxHealth * 0.5;
        player.setHealth(Math.min(player.getHealth() + healAmount, maxHealth));

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 0.6f);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.6f, 1.5f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 40, 0.3, 0.2, 0.3, 0.08);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 20, 0.4, 0.4, 0.4, 0,
                new Particle.DustOptions(Color.fromRGB(180, 0, 0), 1.5f));

        // Trail particles
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 10) { cancel(); return; }
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.3, 0), 8, 0.15, 0.1, 0.15, 0.02);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 5, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 50, 0), 1.2f));
                ticks++;
            }
        }.runTaskTimer(getPlugin(), 1L, 1L);

        player.sendMessage(ChatColor.DARK_RED + "Nether Dash! " + ChatColor.GREEN + "Healed " +
                String.format("%.1f", healAmount / 2.0) + " hearts!");
        incrementStat(uuid, "dashes");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hits", "Hits");
        labels.put("kills", "Kills");
        labels.put("upgrades", "Upgrades");
        labels.put("dashes", "Nether Dashes");
        return labels;
    }
}
