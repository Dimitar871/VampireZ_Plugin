package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.*;

public class BloodlustPerk extends Perk {

    private final Map<UUID, Integer> killStacks = new HashMap<>();
    private static final int MAX_STACKS = 3;

    public BloodlustPerk() {
        super("bloodlust", "Bloodlust", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.REDSTONE,
                "Each kill grants +1 max heart",
                "Up to +3 hearts");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        int stacks = killStacks.getOrDefault(player.getUniqueId(), 0);
        if (stacks > 0) {
            double current = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(Math.max(current - stacks * 2.0, 20.0));
        }
        killStacks.remove(player.getUniqueId());
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();
        int stacks = killStacks.getOrDefault(uuid, 0);
        if (stacks >= MAX_STACKS) return;

        stacks++;
        killStacks.put(uuid, stacks);
        double current = killer.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        killer.getAttribute(Attribute.MAX_HEALTH).setBaseValue(current + 2.0);
        killer.setHealth(Math.min(killer.getHealth() + 2.0, current + 2.0));
        killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(139, 0, 0), 2.0f));
        killer.playSound(killer.getLocation(), Sound.ENTITY_WITHER_HURT, 0.7f, 1.2f);
        incrementStat(uuid, "kills");
        killer.sendMessage(ChatColor.DARK_RED + "Bloodlust: +" + stacks + " hearts!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("kills", "Kills");
        return labels;
    }
}
