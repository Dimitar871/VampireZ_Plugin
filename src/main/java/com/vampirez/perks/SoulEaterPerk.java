package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoulEaterPerk extends Perk {

    private final Map<UUID, Integer> killStacks = new HashMap<>();
    private static final int MAX_STACKS = 3;
    private static final String MODIFIER_NAME = "soul_eater_damage";

    public SoulEaterPerk() {
        super("soul_eater", "Soul Eater", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.WITHER_SKELETON_SKULL,
                "Each kill grants +10% permanent damage",
                "(stacks up to 3 times, +30% max)");
    }

    @Override
    public void apply(Player player) {
        killStacks.put(player.getUniqueId(), 0);
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        killStacks.remove(uuid);
        AttributeInstance attr = player.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr != null) {
            attr.getModifiers().stream()
                    .filter(m -> m.getName().equals(MODIFIER_NAME))
                    .forEach(attr::removeModifier);
        }
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();
        int stacks = killStacks.getOrDefault(uuid, 0);
        if (stacks >= MAX_STACKS) return;

        stacks++;
        killStacks.put(uuid, stacks);

        // Update damage modifier
        AttributeInstance attr = killer.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attr != null) {
            attr.getModifiers().stream()
                    .filter(m -> m.getName().equals(MODIFIER_NAME))
                    .forEach(attr::removeModifier);
            double bonus = stacks * 0.10; // 10% per stack
            attr.addModifier(new AttributeModifier(MODIFIER_NAME, bonus, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }

        // Dark soul absorption vortex
        killer.getWorld().spawnParticle(Particle.SOUL, killer.getLocation().add(0, 1, 0), 50, 0.8, 1.5, 0.8, 0.08);
        killer.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, killer.getLocation().add(0, 0.5, 0), 20, 0.5, 0.5, 0.5, 0.03);
        killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 2, 0), 25, 0.6, 0.6, 0.6, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(40, 200, 200), 2.0f));
        // Soul rising from victim
        victim.getWorld().spawnParticle(Particle.SOUL, victim.getLocation().add(0, 1, 0), 30, 0.3, 1.5, 0.3, 0.1);
        killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.5f, 1.5f);
        killer.sendMessage(ChatColor.DARK_PURPLE + "Soul consumed! Damage +" + (stacks * 10) + "%");
    }
}
