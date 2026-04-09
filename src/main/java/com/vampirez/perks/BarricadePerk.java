package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.*;

public class BarricadePerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 25000;

    public BarricadePerk() {
        super("barricade", "Barricade", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.GLASS,
                "Right-click: place 3-wide 2-tall glass",
                "wall for 5s (25s cooldown)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.SUGAR_CANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Barricade" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Place a glass wall", ChatColor.YELLOW + "Cooldown: 25s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.SUGAR_CANE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Barricade")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.SUGAR_CANE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Barricade")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Barricade on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Calculate perpendicular direction
        Vector facing = player.getLocation().getDirection().setY(0).normalize();
        Vector perp = new Vector(-facing.getZ(), 0, facing.getX());

        Location center = player.getLocation().add(facing.multiply(2)).getBlock().getLocation();
        List<Block> placedBlocks = new ArrayList<>();

        for (int offset = -1; offset <= 1; offset++) {
            for (int y = 0; y < 2; y++) {
                Location blockLoc = center.clone().add(perp.clone().multiply(offset)).add(0, y, 0);
                Block block = blockLoc.getBlock();
                if (block.getType() == Material.AIR) {
                    block.setType(Material.GLASS);
                    placedBlocks.add(block);
                }
            }
        }

        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 1.0f);
        player.sendMessage(ChatColor.AQUA + "Barricade placed!");
        incrementStat(uuid, "walls_placed");

        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            for (Block block : placedBlocks) {
                if (block.getType() == Material.GLASS) {
                    block.setType(Material.AIR);
                }
            }
        }, 100L);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("walls_placed", "Walls Placed");
        return labels;
    }
}
