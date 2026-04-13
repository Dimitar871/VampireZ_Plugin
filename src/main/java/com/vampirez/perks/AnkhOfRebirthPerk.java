package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class AnkhOfRebirthPerk extends Perk {

    private final Map<UUID, Location> deathLocations = new HashMap<>();

    public AnkhOfRebirthPerk() {
        super("ankh_of_rebirth", "Ankh of Rebirth", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.TOTEM_OF_UNDYING,
                "On death: save location. On respawn:",
                "teleport to death location with 60% HP");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        deathLocations.remove(player.getUniqueId());
    }

    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        deathLocations.put(player.getUniqueId(), player.getLocation().clone());
    }

    @Override
    public void onRespawn(Player player) {
        super.onRespawn(player);
        UUID uuid = player.getUniqueId();
        Location deathLoc = deathLocations.remove(uuid);
        if (deathLoc != null) {
            player.teleport(deathLoc);
            double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
            player.setHealth(maxHealth * 0.6);

            player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, deathLoc.clone().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.1);
            player.playSound(deathLoc, Sound.ITEM_TOTEM_USE, 0.8f, 1.2f);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Ankh of Rebirth! Returned to your death location!");
            incrementStat(uuid, "rebirths");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("rebirths", "Rebirths");
        return labels;
    }
}
