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

public class LuckyRollGoldPerk extends Perk {

    public LuckyRollGoldPerk() {
        super("lucky_roll_gold", "Lucky Roll (Gold)", PerkTier.GOLD, PerkTeam.BOTH,
                Material.SUNFLOWER,
                "Replaces itself with a random",
                "Prismatic tier perk!");
    }

    @Override
    public void apply(Player player) {
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            if (!player.isOnline()) return;
            VampireZPlugin plugin = (VampireZPlugin) getPlugin();
            PerkManager pm = plugin.getGameManager().getPerkManager();
            java.util.UUID uuid = player.getUniqueId();

            PerkTeam playerTeam = plugin.getGameManager().isVampire(uuid) ? PerkTeam.VAMPIRE : PerkTeam.HUMAN;

            List<Perk> options = pm.getRandomPerks(PerkTier.PRISMATIC, playerTeam, 1, uuid);
            if (options.isEmpty()) {
                player.sendMessage(MM.parse("<red>No Prismatic perks available! Lucky Roll refunded."));
                return;
            }

            pm.removePerk(uuid, this);
            pm.addPerkToPlayer(uuid, options.get(0));

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1.5, 0), 20, 0.5, 0.5, 0.5, 0);
            player.sendMessage(MM.parse("<green>Lucky Roll! <light_purple>You received: ").append(
                    Component.text(options.get(0).getDisplayName()).color(options.get(0).getTier().getTextColor())));
        }, 1L);
    }

    @Override
    public void remove(Player player) {}
}
