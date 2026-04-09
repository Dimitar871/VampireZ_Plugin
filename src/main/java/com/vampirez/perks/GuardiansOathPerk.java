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

public class GuardiansOathPerk extends Perk {

    public GuardiansOathPerk() {
        super("guardians_oath", "Guardian's Oath", PerkTier.SILVER, PerkTeam.HUMAN,
                Material.IRON_BOOTS,
                "Take 15% less damage when",
                "an ally is within 5 blocks");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        for (Entity entity : victim.getNearbyEntities(5, 5, 5)) {
            if (entity instanceof Player ally && !ally.getUniqueId().equals(victim.getUniqueId())
                    && isSameTeam(victim, ally)) {
                event.setDamage(event.getDamage() * 0.85);
                addStat(victim.getUniqueId(), "damage_reduced", event.getDamage() * 0.15);
                victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 0.6f, 0.8f);
                return;
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("damage_reduced", "Damage Reduced");
        return labels;
    }
}
