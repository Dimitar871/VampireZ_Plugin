package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WarDrumsPerk extends Perk {

    private static final Map<UUID, Set<UUID>> boostedByDrummer = new HashMap<>();
    private static final double AURA_RADIUS = 12.0;

    public WarDrumsPerk() {
        super("war_drums", "War Drums", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.NOTE_BLOCK,
                "Allies within 12 blocks deal +10% damage.",
                "You deal -10% damage.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        boostedByDrummer.remove(player.getUniqueId());
    }

    public static boolean isBoosted(UUID uuid) {
        for (Set<UUID> boosted : boostedByDrummer.values()) {
            if (boosted.contains(uuid)) return true;
        }
        return false;
    }

    @Override
    public void onTick(Player player) {
        UUID drummerUUID = player.getUniqueId();
        Set<UUID> boosted = new HashSet<>();

        // Add allies within range
        for (Entity entity : player.getNearbyEntities(AURA_RADIUS, AURA_RADIUS, AURA_RADIUS)) {
            if (!(entity instanceof Player ally)) continue;
            if (ally.getUniqueId().equals(drummerUUID)) continue;
            if (!isSameTeam(player, ally)) continue;

            boosted.add(ally.getUniqueId());
        }

        boostedByDrummer.put(drummerUUID, boosted);

        // Ambient particles
        player.getWorld().spawnParticle(Particle.NOTE, player.getLocation().add(0, 2.2, 0),
                2, 0.3, 0.1, 0.3, 0);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        // Self penalty: -10% damage
        event.setDamage(event.getDamage() * 0.9);
    }

    public static void applyBoost(EntityDamageByEntityEvent event) {
        // Called externally from PerkListener if needed, but we handle via static check
    }
}
