package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EscapePlanWeakPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 30000;

    public EscapePlanWeakPerk() {
        super("escape_plan_weak", "Escape Plan", PerkTier.SILVER, PerkTeam.BOTH,
                Material.LEATHER_BOOTS,
                "Below 4 hearts: gain Speed I + Absorption I",
                "for 3 seconds. 30s cooldown.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        double healthAfter = victim.getHealth() - event.getFinalDamage();
        if (healthAfter > 8.0) return; // 4 hearts = 8 health

        UUID uuid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUsed = cooldowns.get(uuid);
        if (lastUsed != null && (now - lastUsed) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60, 0, false, true));
        victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.8f);
    }
}
