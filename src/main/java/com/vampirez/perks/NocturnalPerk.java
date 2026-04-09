package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class NocturnalPerk extends Perk {

    private final Map<UUID, Boolean> isNightState = new HashMap<>();

    public NocturnalPerk() {
        super("nocturnal", "Nocturnal", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.CLOCK,
                "Night: +2 max hearts + 15% bonus damage",
                "Day: -1 max heart");
    }

    @Override
    public void apply(Player player) {
        // State will be set on first tick
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        Boolean wasNight = isNightState.remove(uuid);
        if (wasNight == null) return;

        double current = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        if (wasNight) {
            // Remove night bonus (+4 HP = +2 hearts)
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.max(current - 4.0, 20.0));
        } else {
            // Remove day penalty (-2 HP = -1 heart)
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(current + 2.0);
        }
        if (player.getHealth() > player.getAttribute(Attribute.MAX_HEALTH).getBaseValue()) {
            player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getBaseValue());
        }
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long time = player.getWorld().getTime();
        boolean isNight = time >= 13000 && time < 23000;
        Boolean previousState = isNightState.get(uuid);

        if (previousState != null && previousState == isNight) return; // No change

        double current = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();

        // Remove previous state effects
        if (previousState != null) {
            if (previousState) {
                current -= 4.0; // Remove night bonus
            } else {
                current += 2.0; // Remove day penalty
            }
        }

        // Apply new state
        if (isNight) {
            current += 4.0; // +2 hearts
            player.sendMessage(ChatColor.DARK_PURPLE + "Nocturnal: Night falls! +2 hearts, +15% damage!");
        } else {
            current -= 2.0; // -1 heart
            player.sendMessage(ChatColor.GRAY + "Nocturnal: Daylight weakens you... -1 heart");
        }

        current = Math.max(current, 2.0); // Minimum 1 heart
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(current);
        if (player.getHealth() > current) {
            player.setHealth(current);
        }

        isNightState.put(uuid, isNight);
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        if (!(victim instanceof Player)) return;
        UUID uuid = attacker.getUniqueId();
        Boolean isNight = isNightState.get(uuid);
        if (isNight != null && isNight) {
            double bonus = event.getDamage() * 0.15;
            event.setDamage(event.getDamage() + bonus);
            addStat(uuid, "bonus_damage", bonus);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_PHANTOM_BITE, 0.5f, 1.0f);
            // Purple particles when night buff is active
            attacker.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                    new Particle.DustOptions(Color.PURPLE, 1.5f));
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}
