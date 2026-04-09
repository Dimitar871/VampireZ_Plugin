package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuardianAngelPerk extends Perk {

    private final Map<UUID, Long> lastTrigger = new HashMap<>();
    private static final long COOLDOWN_MS = 180000; // 3 minutes

    public GuardianAngelPerk() {
        super("guardian_angel", "Guardian Angel", PerkTier.PRISMATIC, PerkTeam.HUMAN,
                Material.TOTEM_OF_UNDYING,
                "Auto-revive with half HP + 5s invuln",
                "3 minute cooldown (reusable)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastTrigger.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        double healthAfter = victim.getHealth() - event.getFinalDamage();

        if (healthAfter <= 0) {
            long now = System.currentTimeMillis();
            Long last = lastTrigger.get(uuid);
            if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

            // Prevent death
            event.setCancelled(true);
            lastTrigger.put(uuid, now);

            victim.setHealth(victim.getMaxHealth() / 2.0);
            victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 4, false, true));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1, false, true));
            victim.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, victim.getLocation().add(0, 1, 0), 60, 1, 1.5, 1, 0.3);
            victim.playSound(victim.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.2f);
            victim.sendMessage(ChatColor.GOLD + "Guardian Angel saved you! (3m cooldown)");
        }
    }
}
