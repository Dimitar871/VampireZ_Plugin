package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HomeguardPerk extends Perk {

    private final Set<UUID> hasHomeguardSpeed = new HashSet<>();

    public HomeguardPerk() {
        super("homeguard", "Homeguard", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.GOLDEN_BOOTS,
                "Speed V for 5s on respawn",
                "Removed when taking damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        hasHomeguardSpeed.remove(player.getUniqueId());
    }

    /** Check if a player currently has Homeguard speed active. */
    public static boolean hasActiveSpeed(UUID uuid) {
        // Static check via perk instance not possible, use onRespawn flag
        return activeHomeguards.contains(uuid);
    }

    private static final Set<UUID> activeHomeguards = new HashSet<>();

    @Override
    public void onRespawn(Player player) {
        // Speed V (amplifier 4) for 5s
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 4, false, true));
        hasHomeguardSpeed.add(player.getUniqueId());
        activeHomeguards.add(player.getUniqueId());
        // Golden speed trail burst on respawn
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 40, 0.6, 1.0, 0.6, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 215, 0), 1.8f));
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 20, 0.5, 0.3, 0.5, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.5f);
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        if (hasHomeguardSpeed.remove(victim.getUniqueId())) {
            activeHomeguards.remove(victim.getUniqueId());
            victim.removePotionEffect(PotionEffectType.SPEED);
            victim.playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 1.8f);
        }
    }
}
