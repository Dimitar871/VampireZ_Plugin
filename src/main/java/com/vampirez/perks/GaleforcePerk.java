package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import com.vampirez.MM;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class GaleforcePerk extends Perk {

    private final Map<UUID, Integer> playerTier = new HashMap<>();
    private final Map<UUID, Integer> playerProgress = new HashMap<>();
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();

    private static final long DASH_COOLDOWN_MS = 30000;

    // Requirements per tier (to unlock the NEXT tier)
    // All tiers require hits (arrow hits on enemies)
    private static final int[] REQ_AMOUNT = {25, 50, 100, 200};

    private static final String BOW_TAG = "Galeforce";

    public GaleforcePerk() {
        super("galeforce", "Galeforce", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.BOW,
                "Unbreakable bow that upgrades",
                "through arrow hits. 4 tiers with",
                "increasing power. Max tier",
                "unlocks a dash + regen ability.");
    }

    @Override
    public void apply(Player player) {
        UUID uuid = player.getUniqueId();
        playerTier.putIfAbsent(uuid, 0);
        playerProgress.putIfAbsent(uuid, 0);
        giveBow(player);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        playerTier.remove(uuid);
        playerProgress.remove(uuid);
        dashCooldowns.remove(uuid);
        removeBow(player);
        removeArrow(player);
    }

    private boolean isGaleforceBow(ItemStack item) {
        return item != null && item.getType() == Material.BOW
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().contains("Galeforce");
    }

    private void giveBow(Player player) {
        // Remove existing Galeforce bow and arrow first
        removeBow(player);
        removeArrow(player);

        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        int progress = playerProgress.getOrDefault(uuid, 0);

        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.displayName(getBowDisplayName(tier));
            meta.setUnbreakable(true);

            // Infinity on all tiers
            meta.addEnchant(Enchantment.INFINITY, 1, true);

            // Tier-based enchants
            applyTierEnchants(meta, tier);

            meta.lore(buildLore(tier, progress));
            bow.setItemMeta(meta);
        }

        // Remove existing bows (replace the default one)
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.BOW && !isGaleforceBow(item)) {
                player.getInventory().setItem(i, null);
            }
        }

        player.getInventory().addItem(bow);

        // Give 1 arrow (needed for Infinity to work)
        // Remove existing arrows first, then give exactly 1
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.ARROW) {
                player.getInventory().setItem(i, null);
            }
        }
        player.getInventory().addItem(new ItemStack(Material.ARROW, 1));
    }

    private void removeBow(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isGaleforceBow(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    private void removeArrow(Player player) {
        // Don't remove arrows — other perks/gear may use them
    }

    private void applyTierEnchants(ItemMeta meta, int tier) {
        // Tier 1 (25 hits): Power II
        // Tier 2 (50 hits): Power IV
        // Tier 3 (100 hits): Power V + Speed II (potion, applied separately)
        // Tier 4 (200 hits): Power V + Flame II + Punch VII + dash ability
        if (tier >= 4) {
            meta.addEnchant(Enchantment.POWER, 5, true);
            meta.addEnchant(Enchantment.FLAME, 2, true);
            meta.addEnchant(Enchantment.PUNCH, 7, true);
        } else if (tier >= 3) {
            meta.addEnchant(Enchantment.POWER, 5, true);
        } else if (tier >= 2) {
            meta.addEnchant(Enchantment.POWER, 4, true);
        } else if (tier >= 1) {
            meta.addEnchant(Enchantment.POWER, 2, true);
        }
    }

    private void updateBowInHand(Player player) {
        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        int progress = playerProgress.getOrDefault(uuid, 0);

        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (isGaleforceBow(item)) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.displayName(getBowDisplayName(tier));

                    // Clear old enchants and reapply
                    meta.removeEnchant(Enchantment.POWER);
                    meta.removeEnchant(Enchantment.FLAME);
                    meta.removeEnchant(Enchantment.PUNCH);
                    meta.addEnchant(Enchantment.INFINITY, 1, true);
                    applyTierEnchants(meta, tier);

                    meta.lore(buildLore(tier, progress));
                    item.setItemMeta(meta);
                }
            }
        }
    }

    private Component getBowDisplayName(int tier) {
        if (tier == 0) {
            return Component.text(BOW_TAG).color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false);
        }
        return Component.text(BOW_TAG + " ").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false)
                .append(Component.text("[").color(NamedTextColor.GRAY))
                .append(Component.text(tierToRoman(tier)).color(NamedTextColor.AQUA))
                .append(Component.text("]").color(NamedTextColor.GRAY));
    }

    private String tierToRoman(int tier) {
        return switch (tier) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(tier);
        };
    }

    private List<Component> buildLore(int tier, int progress) {
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text("Tier " + tier + "/4").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));

        // Current enchants summary
        lore.add(Component.text("Enchants: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(getEnchantsLabel(tier)).color(NamedTextColor.WHITE)));

        if (tier >= 3) {
            lore.add(Component.text("Bonus: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text("Speed II").color(NamedTextColor.WHITE)));
        }

        lore.add(Component.empty());

        if (tier >= 4) {
            lore.add(Component.text("\u2714 MAX TIER").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Left-click: Gale Dash").color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Dash forward + Regen II (10s)").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Cooldown: 30s").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        } else {
            // Show progress to next tier
            int reqAmount = REQ_AMOUNT[tier];
            NamedTextColor progressColor = progress >= reqAmount ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            lore.add(Component.text("Next tier: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(progress + "/" + reqAmount + " Hits").color(progressColor)));

            // Preview next tier bonus
            lore.add(Component.text("Next: ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(getNextTierPreview(tier)).color(NamedTextColor.WHITE)));
        }

        return lore;
    }

    private String getEnchantsLabel(int tier) {
        return switch (tier) {
            case 0 -> "Infinity";
            case 1 -> "Infinity, Power II";
            case 2 -> "Infinity, Power IV";
            case 3 -> "Infinity, Power V";
            case 4 -> "Infinity, Power V, Flame II, Punch VII";
            default -> "Infinity";
        };
    }

    private String getNextTierPreview(int tier) {
        return switch (tier) {
            case 0 -> "Power II";
            case 1 -> "Power IV";
            case 2 -> "Power V + Speed II";
            case 3 -> "Flame II + Punch VII + Gale Dash";
            default -> "";
        };
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player target)) return;
        if (event.isCancelled()) return;
        if (isSameTeam(attacker, target)) return;

        // Only count arrow hits, not melee
        if (!(event.getDamager() instanceof Projectile)) return;

        // Verify attacker is holding or has the Galeforce bow
        boolean hasBow = false;
        for (ItemStack item : attacker.getInventory().getContents()) {
            if (isGaleforceBow(item)) {
                hasBow = true;
                break;
            }
        }
        if (!hasBow) return;

        UUID uuid = attacker.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        if (tier >= 4) {
            // Max tier, just track stats
            incrementStat(uuid, "hits");
            return;
        }

        int progress = playerProgress.getOrDefault(uuid, 0) + 1;
        playerProgress.put(uuid, progress);

        // Sound feedback for stacking — rising pitch
        float pitch = Math.min(0.5f + (progress / (float) REQ_AMOUNT[tier]) * 1.5f, 2.0f);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.4f, pitch);

        if (progress >= REQ_AMOUNT[tier]) {
            upgradeTier(attacker);
        } else {
            updateBowInHand(attacker);
            // Show progress in action bar
            attacker.sendActionBar(MM.parse("<aqua>Galeforce: <yellow>" + progress + "/" + REQ_AMOUNT[tier] + " Hits"));
        }

        incrementStat(uuid, "hits");
    }

    private void upgradeTier(Player player) {
        UUID uuid = player.getUniqueId();
        int newTier = playerTier.getOrDefault(uuid, 0) + 1;
        playerTier.put(uuid, newTier);
        playerProgress.put(uuid, 0);

        updateBowInHand(player);

        // Upgrade effects
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.4f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation().add(0, 1, 0), 30, 0.4, 0.5, 0.4, 0.1);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 15, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(0, 200, 255), 1.5f));

        player.sendMessage(MM.parse("<aqua><bold>GALEFORCE UPGRADED!</bold> <gold>Tier " + tierToRoman(newTier) + " <gray>(" + getEnchantsLabel(newTier) + ")"));

        // Tier 3+: grant Speed II
        if (newTier >= 3) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false, true));
            player.sendMessage(MM.parse("<green>Speed II activated!"));
        }

        if (newTier == 4) {
            player.sendMessage(MM.parse("<light_purple><bold>GALE DASH UNLOCKED! </bold><gray>Left-click with bow to dash + regen!"));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
        }

        incrementStat(uuid, "upgrades");
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        // Left-click with bow = dash ability (tier 4 only)
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isGaleforceBow(item)) return;

        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        if (tier < 4) return;

        event.setCancelled(true);

        long now = System.currentTimeMillis();
        Long last = dashCooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, DASH_COOLDOWN_MS)) {
            long remaining = (getEffectiveCooldown(player, DASH_COOLDOWN_MS) - (now - last)) / 1000 + 1;
            player.sendMessage(MM.parse("<red>Gale Dash on cooldown! " + remaining + "s"));
            return;
        }
        dashCooldowns.put(uuid, now);

        // Dash forward
        Vector dir = player.getLocation().getDirection();
        dir.setY(Math.max(dir.getY() * 0.3, 0.15));
        dir.normalize().multiply(2.5);
        player.setVelocity(dir);

        // Regeneration II for 10 seconds (200 ticks)
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, false, true, true));

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.2f);
        player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 0.6f, 1.5f);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.5, 0), 30, 0.3, 0.2, 0.3, 0.06);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.5f));

        // Trail particles
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 10) { cancel(); return; }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.3, 0), 6, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 4, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(Color.fromRGB(100, 220, 255), 1.0f));
                ticks++;
            }
        }.runTaskTimer(getPlugin(), 1L, 1L);

        player.sendMessage(MM.parse("<aqua>Gale Dash! <green>Regeneration II for 10s!"));
        incrementStat(uuid, "dashes");
    }

    @Override
    public void onRespawn(Player player) {
        // Reapply Speed II if tier 3+
        UUID uuid = player.getUniqueId();
        int tier = playerTier.getOrDefault(uuid, 0);
        if (tier >= 3) {
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                if (player.isOnline()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1, false, false, true));
                }
            }, 5L);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hits", "Arrow Hits");
        labels.put("upgrades", "Upgrades");
        labels.put("dashes", "Gale Dashes");
        return labels;
    }
}
