package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SupplyDropPerk extends Perk {

    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private static final int DROP_INTERVAL_TICKS = 90; // 90 seconds (onTick fires every 1s)
    private static final int CHEST_DURATION_TICKS = 300; // 15 seconds in game ticks (for scheduler)

    public SupplyDropPerk() {
        super("supply_drop", "Supply Drop", PerkTier.SILVER, PerkTeam.BOTH,
                Material.CHEST,
                "Every 90s, a supply chest appears",
                "at your location with 2 golden",
                "apples + 8 arrows. Auto-removes 15s.");
    }

    @Override
    public void apply(Player player) {
        tickCounters.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        tickCounters.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        int count = tickCounters.getOrDefault(uuid, 0) + 1;

        if (count >= DROP_INTERVAL_TICKS) {
            count = 0;
            placeSupplyChest(player);
        }
        tickCounters.put(uuid, count);
    }

    private void placeSupplyChest(Player player) {
        Location loc = player.getLocation().getBlock().getLocation();
        Block block = loc.getBlock();

        // Don't overwrite important blocks
        if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR) {
            // Try one block up
            block = loc.add(0, 1, 0).getBlock();
            if (block.getType() != Material.AIR && block.getType() != Material.CAVE_AIR) {
                player.sendMessage(ChatColor.YELLOW + "Supply Drop: No space for chest!");
                return;
            }
        }

        block.setType(Material.CHEST);
        if (block.getState() instanceof Chest chest) {
            chest.getInventory().addItem(
                    new ItemStack(Material.GOLDEN_APPLE, 2),
                    new ItemStack(Material.ARROW, 8)
            );
        }

        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1, 0.5), 15, 0.5, 0.5, 0.5, 0);
        player.playSound(block.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.sendMessage(ChatColor.GOLD + "Supply Drop arrived!");
        incrementStat(player.getUniqueId(), "drops");

        // Auto-remove chest after 15s (300 ticks)
        final Block chestBlock = block;
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (chestBlock.getType() == Material.CHEST) {
                if (chestBlock.getState() instanceof Chest c) {
                    c.getInventory().clear();
                }
                chestBlock.setType(Material.AIR);
            }
        }, CHEST_DURATION_TICKS);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("drops", "Supply Drops");
        return labels;
    }
}
