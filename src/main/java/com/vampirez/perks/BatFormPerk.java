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
    private static final long COOLDOWN_MS = 25000;

    public BatFormPerk() {
        super("bat_form", "Bat Form", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.PHANTOM_MEMBRANE,
                "Right-click to become invisible",
                "with Speed III for 5s (25s cooldown)");
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

        // Store and hide armor
        storedArmor.put(uuid, player.getInventory().getArmorContents().clone());
        player.getInventory().setArmorContents(new ItemStack[4]);

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
