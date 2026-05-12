package com.vampirez.perks;

import com.vampirez.*;
import org.bukkit.Bukkit;
import com.vampirez.MM;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

public class LuckyRollPrismaticPerk extends Perk {

    public LuckyRollPrismaticPerk() {
        super("lucky_roll_prismatic", "Lucky Roll (Prismatic)", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.SUNFLOWER,
                "Replaces itself with",
                "2 random Prismatic perks!");
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
                player.sendMessage(MM.parse("<red>No Prismatic perks available! Lucky Roll refunded."));
                return;
            }

            // Remove self first to free a slot
            pm.removePerk(uuid, this);

            // Force-add both perks (bypasses max check since this is a special perk)
            Component msgComponent = Component.empty();
            for (int i = 0; i < options.size(); i++) {
                Perk perk = options.get(i);
                pm.forceAddPerkToPlayer(uuid, perk);
                if (i > 0) msgComponent = msgComponent.append(MM.parse("<green> + "));
                msgComponent = msgComponent.append(net.kyori.adventure.text.Component.text(perk.getDisplayName()).color(perk.getTier().getTextColor()));
            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 30, 0.5, 0.5, 0.5, 0);
            player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().add(0, 2, 0), 15, 0.3, 0.3, 0.3, 0.05);
            player.sendMessage(MM.parse("<green>Lucky Roll! <light_purple>You received: ").append(msgComponent));
        }, 1L);
    }

    @Override
    public void remove(Player player) {}
}
