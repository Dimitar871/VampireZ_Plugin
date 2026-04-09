package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class SmokeBombPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 20000;

    public SmokeBombPerk() {
        super("smoke_bomb", "Smoke Bomb", PerkTier.GOLD, PerkTeam.BOTH,
                Material.GUNPOWDER,
                "Right-click: blindness to enemies",
                "within 10 blocks for 5s (20s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.GUNPOWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GRAY + "Smoke Bomb" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Blindness cloud", ChatColor.YELLOW + "Cooldown: 20s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.GUNPOWDER && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Smoke Bomb")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.GUNPOWDER || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Smoke Bomb")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Smoke Bomb on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Create blindness cloud
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 1, 0), 150, 2, 1, 2, 0.05);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 60, 2, 1, 2, 0,
                new Particle.DustOptions(org.bukkit.Color.GRAY, 2.0f));
        player.playSound(player.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 1.0f, 2.0f);

        int playersBlinded = 0;
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof Player target)) continue;
            if (entity.getUniqueId().equals(uuid)) continue;
            if (isSameTeam(player, target)) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0, false, true));
            playersBlinded++;
        }
        incrementStat(uuid, "activations");
        addStat(uuid, "players_blinded", playersBlinded);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("players_blinded", "Players Blinded");
        return labels;
    }
}
