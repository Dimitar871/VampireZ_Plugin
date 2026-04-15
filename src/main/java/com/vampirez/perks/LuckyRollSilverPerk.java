package com.vampirez.perks;

import com.vampirez.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class LuckyRollSilverPerk extends Perk {

    public LuckyRollSilverPerk() {
        super("lucky_roll_silver", "Lucky Roll", PerkTier.SILVER, PerkTeam.BOTH,
                Material.SUNFLOWER,
                "Replaces itself with a random",
                ChatColor.GOLD + "Gold" + ChatColor.GRAY + " tier perk!");
    }

    @Override
    public void apply(Player player) {
        // Schedule replacement for next tick to avoid concurrent modification
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (!player.isOnline()) return;
            VampireZPlugin plugin = (VampireZPlugin) getPlugin();
            PerkManager pm = plugin.getGameManager().getPerkManager();
            java.util.UUID uuid = player.getUniqueId();

            // Determine player team
            PerkTeam playerTeam = plugin.getGameManager().isVampire(uuid) ? PerkTeam.VAMPIRE : PerkTeam.HUMAN;

            // Get a random Gold perk
            List<Perk> options = pm.getRandomPerks(PerkTier.GOLD, playerTeam, 1, uuid);
            if (options.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No Gold perks available! Lucky Roll refunded.");
                return;
            }

            // Remove self, add the Gold perk
            pm.removePerk(uuid, this);
            pm.addPerkToPlayer(uuid, options.get(0));

            // Effects
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0);
            player.sendMessage(ChatColor.GREEN + "Lucky Roll! " + ChatColor.GOLD + "You received: " +
                    options.get(0).getTier().getColor() + options.get(0).getDisplayName());
        }, 1L);
    }

    @Override
    public void remove(Player player) {}
}
