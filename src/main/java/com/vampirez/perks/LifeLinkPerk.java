package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class LifeLinkPerk extends Perk {

    private static final String MODIFIER_NAME = "LifeLink";
    private final Set<UUID> hasBuff = new HashSet<>();

    public LifeLinkPerk() {
        super("life_link", "Life Link", PerkTier.GOLD, PerkTeam.BOTH,
                Material.LEAD,
                "When an ally is within 15 blocks,",
                "you both get +2 bonus hearts");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        removeModifier(player);
        hasBuff.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        boolean allyNearby = false;

        for (Entity entity : player.getNearbyEntities(15, 15, 15)) {
            if (entity instanceof Player ally && !ally.getUniqueId().equals(uuid)
                    && isSameTeam(player, ally)) {
                allyNearby = true;
                break;
            }
        }

        if (allyNearby && !hasBuff.contains(uuid)) {
            applyModifier(player);
            hasBuff.add(uuid);
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 3, 0.3, 0.2, 0.3, 0);
        } else if (!allyNearby && hasBuff.contains(uuid)) {
            removeModifier(player);
            hasBuff.remove(uuid);
        }
    }

    private void applyModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        removeModifier(player);
        attr.addModifier(new AttributeModifier(MODIFIER_NAME, 4.0, AttributeModifier.Operation.ADD_NUMBER));
    }

    private void removeModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        for (AttributeModifier mod : attr.getModifiers()) {
            if (mod.getName().equals(MODIFIER_NAME)) {
                attr.removeModifier(mod);
            }
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        return Collections.emptyMap();
    }
}
