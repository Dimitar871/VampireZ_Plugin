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
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class BatFormPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ItemStack[]> storedArmor = new HashMap<>();
    private final Set<UUID> batFormActive = new HashSet<>();
    private static final long COOLDOWN_MS = 25000;

    public BatFormPerk() {
        super("bat_form", "Bat Form", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.PHANTOM_MEMBRANE,
                "Right-click to become invisible",
                "with Speed III for 5s (25s cooldown)",
                "Armor hidden but protection kept");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_PURPLE + "Bat Form" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Invisibility + Speed III", ChatColor.YELLOW + "Cooldown: 25s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        batFormActive.remove(uuid);
        // Restore armor if stored
        if (storedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(storedArmor.remove(uuid));
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.PHANTOM_MEMBRANE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Bat Form")) {
                player.getInventory().remove(item);
            }
        }
    }

    /** Returns true if the player is in bat form (armor hidden but protected). */
    public boolean isBatFormActive(UUID uuid) {
        return batFormActive.contains(uuid);
    }

    /** Get stored armor items for protection calculation. */
    public ItemStack[] getStoredArmor(UUID uuid) {
        return storedArmor.get(uuid);
    }

    /** Get stored armor points for damage calculation during bat form. */
    public double getStoredArmorValue(UUID uuid) {
        ItemStack[] armor = storedArmor.get(uuid);
        if (armor == null) return 0;
        double total = 0;
        for (ItemStack piece : armor) {
            if (piece == null) continue;
            total += getArmorPoints(piece.getType());
        }
        return total;
    }

    private double getArmorPoints(Material mat) {
        return switch (mat) {
            case LEATHER_HELMET -> 1; case LEATHER_CHESTPLATE -> 3; case LEATHER_LEGGINGS -> 2; case LEATHER_BOOTS -> 1;
            case CHAINMAIL_HELMET -> 2; case CHAINMAIL_CHESTPLATE -> 5; case CHAINMAIL_LEGGINGS -> 4; case CHAINMAIL_BOOTS -> 1;
            case IRON_HELMET -> 2; case IRON_CHESTPLATE -> 6; case IRON_LEGGINGS -> 5; case IRON_BOOTS -> 2;
            case DIAMOND_HELMET -> 3; case DIAMOND_CHESTPLATE -> 8; case DIAMOND_LEGGINGS -> 6; case DIAMOND_BOOTS -> 3;
            case NETHERITE_HELMET -> 3; case NETHERITE_CHESTPLATE -> 8; case NETHERITE_LEGGINGS -> 6; case NETHERITE_BOOTS -> 3;
            default -> 0;
        };
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.PHANTOM_MEMBRANE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Bat Form")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Bat Form on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Store and hide armor visually (protection kept via damage formula)
        storedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        player.getInventory().setArmorContents(new ItemStack[4]);
        batFormActive.add(uuid);

        // Apply bat form effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2, false, true));
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.05);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0,
                new Particle.DustOptions(Color.fromRGB(48, 0, 48), 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.8f);
        player.sendMessage(ChatColor.DARK_PURPLE + "You transform into a bat!");

        incrementStat(uuid, "activations");

        // Schedule armor restore after 5s (100 ticks)
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            batFormActive.remove(uuid);
            if (storedArmor.containsKey(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.getInventory().setArmorContents(storedArmor.remove(uuid));
                } else {
                    storedArmor.remove(uuid);
                }
            }
        }, 100L);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Times Activated");
        return labels;
    }
}
