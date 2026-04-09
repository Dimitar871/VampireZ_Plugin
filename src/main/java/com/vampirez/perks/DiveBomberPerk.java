package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DiveBomberPerk extends Perk {

    public DiveBomberPerk() {
        super("dive_bomber", "Dive Bomber", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.CREEPER_HEAD,
                "On death: explode dealing 4 hearts",
                "to enemies within 4 blocks");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        Location loc = player.getLocation();

        // Visual explosion
        loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3, 1, 1, 1, 0);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.0f);

        // Damage all nearby living entities (players and mobs)
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            if (entity instanceof LivingEntity target && !entity.getUniqueId().equals(player.getUniqueId())) {
                target.damage(8.0); // 4 hearts = 8 damage
            }
        }
    }
}
