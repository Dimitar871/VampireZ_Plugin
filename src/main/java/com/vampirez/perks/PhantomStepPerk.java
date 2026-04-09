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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PhantomStepPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final long COOLDOWN_MS = 15000;

    public PhantomStepPerk() {
        super("phantom_step", "Phantom Step", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.PHANTOM_MEMBRANE,
                "After taking damage: 2s invisibility",
                "+ Speed I (15s cooldown)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 40, 0, false, true), true);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, false, true), true);
        victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        victim.sendMessage(ChatColor.DARK_PURPLE + "Phantom Step! You vanished!");
        incrementStat(uuid, "vanishes");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("vanishes", "Vanishes");
        return labels;
    }
}
