package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.LinkedHashMap;
import java.util.Map;

public class PackHunterPerk extends Perk {

    public PackHunterPerk() {
        super("pack_hunter", "Pack Hunter", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.WOLF_SPAWN_EGG,
                "Deal +10% damage per ally within",
                "6 blocks of target (max +30%)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        int allyCount = 0;
        for (Entity entity : victim.getNearbyEntities(6, 6, 6)) {
            if (entity instanceof Player ally && !ally.getUniqueId().equals(attacker.getUniqueId())
                    && isSameTeam(attacker, ally)) {
                allyCount++;
            }
        }
        if (allyCount > 0) {
            double bonus = Math.min(allyCount * 0.10, 0.30);
            double bonusDamage = event.getDamage() * bonus;
            event.setDamage(event.getDamage() + bonusDamage);
            addStat(attacker.getUniqueId(), "bonus_damage", bonusDamage);
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 0.5f, 1.2f);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}
