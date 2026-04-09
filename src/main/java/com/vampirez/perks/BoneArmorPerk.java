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

public class BoneArmorPerk extends Perk {

    private final Map<UUID, Long> lastProc = new HashMap<>();
    private static final long COOLDOWN_MS = 12000;

    public BoneArmorPerk() {
        super("bone_armor", "Bone Armor", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.BONE_BLOCK,
                "First hit every 12s is",
                "reduced by 50%");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastProc.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastProc.get(uuid);
        if (last == null || (now - last) >= getEffectiveCooldown(victim, COOLDOWN_MS)) {
            lastProc.put(uuid, now);
            event.setDamage(event.getDamage() * 0.5);
            victim.getWorld().spawnParticle(Particle.BLOCK, victim.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0,
                    Material.BONE_BLOCK.createBlockData());
            victim.playSound(victim.getLocation(), Sound.BLOCK_BONE_BLOCK_BREAK, 1.0f, 1.0f);
            victim.sendMessage(ChatColor.WHITE + "Bone Armor absorbed 50% damage!");
            incrementStat(uuid, "hits_absorbed");
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("hits_absorbed", "Hits Absorbed");
        return labels;
    }
}
