package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class DashPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 5000;

    public DashPerk() {
        super("dash", "Dash", PerkTier.SILVER, PerkTeam.BOTH,
                Material.FEATHER,
                "Right-click Feather to dash forward",
                "5 second cooldown");
    }

    @Override
    public void apply(Player player) {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Dash" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Dash forward", ChatColor.YELLOW + "Cooldown: 5s"));
            feather.setItemMeta(meta);
        }
        player.getInventory().addItem(feather);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.FEATHER && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Dash")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.FEATHER || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Dash")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Dash on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        Vector dir = player.getLocation().getDirection();
        dir.setY(0); // flatten to horizontal
        dir.normalize().multiply(1.5); // forward boost
        dir.setY(0.15); // slight upward lift
        player.setVelocity(dir);
        // Launch burst
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 50, 0.4, 0.15, 0.4, 0.06);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 25, 0.4, 0.15, 0.4, 0,
                new Particle.DustOptions(org.bukkit.Color.WHITE, 1.5f));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.6f, 1.8f);

        // Speed trail particles during dash
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 8) { cancel(); return; }
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 0.3, 0), 5, 0.15, 0.1, 0.15, 0.02);
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0,
                        new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 220, 255), 1.0f));
                ticks++;
            }
        }.runTaskTimer(getPlugin(), 1L, 1L);
        incrementStat(uuid, "dashes");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("dashes", "Dashes");
        return labels;
    }
}
