package com.vampirez.perks;

import com.vampirez.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class LuckyRollPrismaticPerk extends Perk {

    public LuckyRollPrismaticPerk() {
        super("lucky_roll_prismatic", "Lucky Roll", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.SUNFLOWER,
                "Replaces itself with",
                ChatColor.LIGHT_PURPLE + "2 random Prismatic" + ChatColor.GRAY + " perks!");
    }

    @Override
    public void apply(Player player) {
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (!player.isOnline()) return;
            VampireZPlugin plugin = (VampireZPlugin) getPlugin();
            PerkManager pm = plugin.getGameManager().getPerkManager();
            java.util.UUID uuid = player.getUniqueId();

            PerkTeam playerTeam = plugin.getGameManager().isVampire(uuid) ? PerkTeam.VAMPIRE : PerkTeam.HUMAN;

            List<Perk> options = pm.getRandomPerks(PerkTier.PRISMATIC, playerTeam, 2, uuid);
            if (options.isEmpty()) {
                player.sendMessage(ChatColor.RED + "No Prismatic perks available! Lucky Roll refunded.");
                return;
            }

            // Remove self first to free a slot
            pm.removePerk(uuid, this);

            // Force-add both perks (bypasses max check since this is a special perk)
            StringBuilder msg = new StringBuilder();
            for (int i = 0; i < options.size(); i++) {
                Perk perk = options.get(i);
                pm.forceAddPerkToPlayer(uuid, perk);
                if (i > 0) msg.append(ChatColor.GREEN + " + ");
                msg.append(perk.getTier().getColor()).append(perk.getDisplayName());
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 15, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(ChatColor.GREEN + "Lucky Roll! " + ChatColor.LIGHT_PURPLE + "You received: " + msg);
        }, 1L);
    }

    @Override
    public void remove(Player player) {}
}
