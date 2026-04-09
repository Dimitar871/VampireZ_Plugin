package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SummonerPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;

    public SummonerPerk() {
        super("summoner", "Summoner", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.BONE,
                "Right-click to summon 2 wolves",
                "that attack nearby enemies (45s cd)");
    }

    @Override
    public void apply(Player player) {
        ItemStack item = new ItemStack(Material.BONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Summon Wolves" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(ChatColor.GRAY + "Summon 2 tamed wolves", ChatColor.YELLOW + "Cooldown: 45s"));
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.BONE && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Summon Wolves")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.BONE || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Summon Wolves")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            player.sendMessage(ChatColor.RED + "Summon on cooldown! " + ((getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1) + "s");
            return;
        }
        cooldowns.put(uuid, now);

        // Summon 2 wolves
        for (int i = 0; i < 2; i++) {
            Wolf wolf = (Wolf) player.getWorld().spawnEntity(
                    player.getLocation().add(i == 0 ? 1 : -1, 0, 0), EntityType.WOLF);
            wolf.setTamed(true);
            wolf.setOwner(player);
            wolf.setAngry(true);
            wolf.setCustomName(ChatColor.RED + player.getName() + "'s Wolf");
            wolf.setCustomNameVisible(true);
            wolf.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "VAMPIRE"));
        }

        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 0.5, 0), 20, 1, 0.5, 1, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
        player.sendMessage(ChatColor.RED + "You summoned 2 wolves!");
    }
}
