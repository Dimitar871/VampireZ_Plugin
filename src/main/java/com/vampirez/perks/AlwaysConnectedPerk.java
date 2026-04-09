package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.VampireZPlugin;
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

public class AlwaysConnectedPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 45000;

    public AlwaysConnectedPerk() {
        super("always_connected", "Always Connected", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.RECOVERY_COMPASS,
                "Reveal all humans to your team",
                "for 5s + heal all humans 1 heart.",
                "45s cooldown.");
    }

    @Override
    public void apply(Player player) {
        ItemStack compass = new ItemStack(Material.RECOVERY_COMPASS);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Always Connected" + ChatColor.GRAY + " (Right-Click)");
            meta.setLore(Arrays.asList(
                    "",
                    ChatColor.ITALIC + "" + ChatColor.GRAY + "\"No matter where you are,",
                    ChatColor.ITALIC + "" + ChatColor.GRAY + " everyone is always connected.\"",
                    "",
                    ChatColor.YELLOW + "Cooldown: 45s"
            ));
            compass.setItemMeta(meta);
        }
        player.getInventory().addItem(compass);
    }

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.RECOVERY_COMPASS && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().contains("Always Connected")) {
                player.getInventory().remove(item);
            }
        }
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() != Material.RECOVERY_COMPASS || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().contains("Always Connected")) return;

        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(player, COOLDOWN_MS)) {
            long remaining = (getEffectiveCooldown(player, COOLDOWN_MS) - (now - last)) / 1000 + 1;
            player.sendMessage(ChatColor.RED + "Always Connected on cooldown! " + remaining + "s");
            return;
        }
        cooldowns.put(uuid, now);

        VampireZPlugin plugin = (VampireZPlugin) getPlugin();
        if (plugin.getGameManager() == null) return;

        Set<UUID> humans = plugin.getGameManager().getHumanTeam();

        // Play metallic sound for ALL players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1.5f, 0.6f);
            p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.3f, 2.0f);
        }

        // Apply glowing + heal to all humans
        for (UUID humanUUID : humans) {
            Player human = Bukkit.getPlayer(humanUUID);
            if (human == null || !human.isOnline()) continue;

            // Glowing for 5 seconds (100 ticks) - visible to teammates
            human.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0, false, false, true), true);

            // Heal 1 heart (2 HP)
            double newHealth = Math.min(human.getHealth() + 2.0, human.getMaxHealth());
            human.setHealth(newHealth);

            // Particles around each human
            human.getWorld().spawnParticle(Particle.DUST, human.getLocation().add(0, 1.5, 0),
                    15, 0.5, 0.5, 0.5, 0, new Particle.DustOptions(Color.AQUA, 1.5f));
            human.getWorld().spawnParticle(Particle.HEART, human.getLocation().add(0, 2.2, 0),
                    3, 0.3, 0.2, 0.3, 0);
        }

        // Message to all humans
        for (UUID humanUUID : humans) {
            Player human = Bukkit.getPlayer(humanUUID);
            if (human == null || !human.isOnline()) continue;
            if (human.getUniqueId().equals(uuid)) {
                human.sendMessage(ChatColor.AQUA + "Always Connected! All humans revealed and healed!");
            } else {
                human.sendMessage(ChatColor.AQUA + player.getName() + " activated Always Connected! Humans revealed!");
            }
        }

        incrementStat(uuid, "activations");
        addStat(uuid, "humans_healed", humans.size());
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Activations");
        labels.put("humans_healed", "Humans Healed");
        return labels;
    }
}
