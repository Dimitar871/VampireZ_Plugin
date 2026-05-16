package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
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

public class PhoenixDownPerk extends Perk {

    private final Set<UUID> used = new HashSet<>();

    public PhoenixDownPerk() {
        super("phoenix_down", "Phoenix Down", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.BLAZE_POWDER,
                "One-time auto-revive: instead of dying,",
                "heal to half HP (consumed after use)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        used.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        if (used.contains(uuid)) return;

        double healthAfter = victim.getHealth() - event.getFinalDamage();
        if (healthAfter <= 0) {
            // Prevent death
            event.setCancelled(true);
            used.add(uuid);
            victim.setHealth(victim.getMaxHealth() / 2.0);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, true));
            victim.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victim.getLocation().add(0, 1, 0), 50, 1, 1, 1, 0.2);
            // Resurrection cue: orange helix + atom orbit during invuln + buff cue
            com.vampirez.fx.VFX.fx().helix(victim.getLocation(), org.bukkit.Color.fromRGB(255, 140, 0), 30);
            com.vampirez.fx.VFX.fx().atomAround(victim, org.bukkit.Color.fromRGB(255, 140, 0), 60);
            com.vampirez.fx.VFX.sound().playBuff(victim);
            victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            victim.sendMessage(MM.parse("<gold>Phoenix Down activated! You were saved from death!"));
        }
    }
}
