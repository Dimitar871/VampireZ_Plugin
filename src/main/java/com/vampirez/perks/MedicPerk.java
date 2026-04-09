package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class MedicPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int REGEN_TICKS = 15; // 15 seconds (onTick fires every 1s)
    private static final int MAX_SNOWBALLS = 3;

    public MedicPerk() {
        super("medic", "Medic", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.SNOWBALL,
                "Start with 3 healing snowballs.",
                "Hit a teammate to heal 3 hearts.",
                "Regen 1 snowball every 15s.");
    }

    @Override
    public void apply(Player player) {
        ItemStack snowballs = new ItemStack(Material.SNOWBALL, 3);
        ItemMeta meta = snowballs.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Healing Snowball");
            snowballs.setItemMeta(meta);
        }
        player.getInventory().addItem(snowballs);
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SNOWBALL) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        // Check if this was a snowball hit
        if (!(event.getDamager() instanceof Snowball)) return;
        if (!(victim instanceof Player target)) return;

        // If teammate, cancel damage and heal
        if (isSameTeam(attacker, target)) {
            event.setCancelled(true);
            double maxHealth = target.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double healAmount = 6.0; // 3 hearts
            target.setHealth(Math.min(target.getHealth() + healAmount, maxHealth));
            target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0, 2, 0), 6, 0.3, 0.3, 0.3, 0);
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
            attacker.sendMessage(ChatColor.GREEN + "You healed " + target.getName() + " for 3 hearts!");
            target.sendMessage(ChatColor.GREEN + attacker.getName() + " healed you for 3 hearts!");
            addStat(attacker.getUniqueId(), "healing", 6.0);
        }
        // If enemy, let normal snowball damage proceed (basically 0)
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;

        if (count >= REGEN_TICKS) {
            count = 0;
            // Count current snowballs
            int current = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == Material.SNOWBALL) {
                    current += item.getAmount();
                }
            }
            if (current < MAX_SNOWBALLS) {
                player.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
                player.sendMessage(ChatColor.GREEN + "Medic: Snowball regenerated! (" + (current + 1) + "/" + MAX_SNOWBALLS + ")");
            }
        }
        tickCounters.put(uuid, count);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("healing", "HP Healed");
        return labels;
    }
}
