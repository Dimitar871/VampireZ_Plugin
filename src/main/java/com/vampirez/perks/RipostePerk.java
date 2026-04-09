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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RipostePerk extends Perk {

    private final Map<UUID, Long> lastHitTime = new HashMap<>();
    private static final long WINDOW_MS = 3000;

    public RipostePerk() {
        super("riposte", "Riposte", PerkTier.SILVER, PerkTeam.BOTH,
                Material.IRON_SWORD,
                "After being hit, next attack within",
                "3s deals +40% damage");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastHitTime.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        lastHitTime.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        UUID uuid = attacker.getUniqueId();
        Long lastHit = lastHitTime.get(uuid);
        if (lastHit != null && (System.currentTimeMillis() - lastHit) <= WINDOW_MS) {
            lastHitTime.remove(uuid);
            event.setDamage(event.getDamage() * 1.4);
            attacker.getWorld().spawnParticle(Particle.ENCHANTED_HIT, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.5f);
            attacker.sendMessage(ChatColor.WHITE + "Riposte! +40% damage!");
            incrementStat(uuid, "ripostes");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("ripostes", "Ripostes");
        return labels;
    }
}
