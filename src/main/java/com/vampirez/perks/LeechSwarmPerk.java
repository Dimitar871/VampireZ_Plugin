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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.LinkedHashMap;
import java.util.Map;

public class LeechSwarmPerk extends Perk {

    public LeechSwarmPerk() {
        super("leech_swarm", "Leech Swarm", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.FERMENTED_SPIDER_EYE,
                "On death, spawn 3 silverfish",
                "at your death location for 5s");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        Location deathLoc = player.getLocation().clone();
        incrementStat(player.getUniqueId(), "swarms_spawned");

        deathLoc.getWorld().spawnParticle(Particle.LARGE_SMOKE, deathLoc.add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.02);
        deathLoc.getWorld().playSound(deathLoc, Sound.ENTITY_SILVERFISH_AMBIENT, 1.0f, 0.8f);

        for (int i = 0; i < 3; i++) {
            Entity silverfish = deathLoc.getWorld().spawnEntity(
                    deathLoc.clone().add(Math.random() - 0.5, 0, Math.random() - 0.5), EntityType.SILVERFISH);
            silverfish.setCustomName(ChatColor.RED + "Leech");
            silverfish.setCustomNameVisible(true);
            silverfish.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "VAMPIRE"));

            Bukkit.getScheduler().runTaskLater(getPlugin(), silverfish::remove, 100L);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("swarms_spawned", "Swarms Spawned");
        return labels;
    }
}
