package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;

import java.util.ArrayList;

public class GlassCannonPerk extends Perk {

    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("vampirez", "glass_cannon");

    public GlassCannonPerk() {
        super("glass_cannon", "Glass Cannon", PerkTier.PRISMATIC, PerkTeam.BOTH,
                Material.TNT, "-30% max HP, +35% damage dealt");
    }

    @Override
    public void apply(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        attr.addModifier(new AttributeModifier(MODIFIER_KEY, -0.3, AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.ANY));
        if (player.getHealth() > attr.getValue()) {
            player.setHealth(attr.getValue());
        }
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        for (AttributeModifier mod : new ArrayList<>(attr.getModifiers())) {
            if (MODIFIER_KEY.equals(mod.getKey())) attr.removeModifier(mod);
        }
        // Health stays where it is — max just went up
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        event.setDamage(event.getDamage() * 1.35);
        attacker.playSound(attacker.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.5f, 1.5f);
        victim.getWorld().spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1, 0), 15, 0.3, 0.4, 0.3, 0.15);
        victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0,
                Material.GLASS.createBlockData());
    }
}
