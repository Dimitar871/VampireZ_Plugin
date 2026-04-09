package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TemporalShieldPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;

    public TemporalShieldPerk() {
        super("temporal_shield", "Temporal Shield", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.CLOCK,
                "Right-click: freeze enemies within",
                "5 blocks for 2s (45s cooldown)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Temporal Shield" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Freeze nearby enemies", ChatColor.YELLOW + "Cooldown: 45s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.CLOCK && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Temporal Shield")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.CLOCK || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Temporal Shield")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Temporal Shield on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 1, 0), 60, 2.5, 1, 2.5, 0.02);
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 2.5, 1, 2.5, 0,
                new Particle.DustOptions(org.bukkit.Color.AQUA, 1.5f));
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 2.0f);

        int frozen = 0;
        for (Entity entity : player.getNearbyEntities(5, 5, 5)) {
            if (!(entity instanceof Player target)) continue;
            if (isSameTeam(player, target)) continue;

            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 254, false, true), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 254, false, true), true);
            target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 40, 200, false, true), true);
            target.sendMessage(ChatColor.AQUA + "You've been frozen by Temporal Shield!");
            frozen++;
        }

        incrementStat(uuid, "activations");
        addStat(uuid, "enemies_frozen", frozen);
        player.sendMessage(ChatColor.AQUA + "Temporal Shield! Froze " + frozen + " enemies!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("enemies_frozen", "Enemies Frozen");
        return labels;
    }
}
