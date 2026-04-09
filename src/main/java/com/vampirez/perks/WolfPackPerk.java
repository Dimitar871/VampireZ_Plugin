package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.LinkedHashMap;
import java.util.Map;

public class WolfPackPerk extends Perk {

    public WolfPackPerk() {
        super("wolf_pack", "Wolf Pack", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.BONE,
                "On vampire kill: summon 2 wolves",
                "that fight for you (8s)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onKill(Player killer, Player victim) {
        incrementStat(killer.getUniqueId(), "summons");

        for (int i = 0; i < 2; i++) {
            Wolf wolf = (Wolf) killer.getWorld().spawnEntity(
                    killer.getLocation().add(i == 0 ? 1 : -1, 0, 0), EntityType.WOLF);
            wolf.setTamed(true);
            wolf.setOwner(killer);
            wolf.setAngry(true);
            wolf.setCustomName(ChatColor.GREEN + killer.getName() + "'s Wolf");
            wolf.setCustomNameVisible(true);
            wolf.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "HUMAN"));

            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                if (!wolf.isDead()) wolf.remove();
            }, 160L);
        }

        killer.getWorld().spawnParticle(Particle.LARGE_SMOKE, killer.getLocation().add(0, 0.5, 0), 15, 0.5, 0.3, 0.5, 0.02);
        killer.playSound(killer.getLocation(), Sound.ENTITY_WOLF_GROWL, 1.0f, 1.0f);
        killer.sendMessage(ChatColor.GREEN + "Wolf Pack! 2 wolves summoned!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("summons", "Summons");
        return labels;
    }
}
