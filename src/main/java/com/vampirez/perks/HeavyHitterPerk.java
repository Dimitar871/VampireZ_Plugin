package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.*;

public class HeavyHitterPerk extends Perk {

    public HeavyHitterPerk() {
        super("heavy_hitter", "Heavy Hitter", PerkTier.SILVER, PerkTeam.BOTH,
                Material.ANVIL, "+4% of your max HP as bonus damage per hit");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {}

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        double bonus = attacker.getMaxHealth() * 0.04;
        event.setDamage(event.getDamage() + bonus);
        addStat(attacker.getUniqueId(), "bonus_damage", bonus);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.5f, 1.0f);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("bonus_damage", "Bonus Damage");
        return labels;
    }
}
