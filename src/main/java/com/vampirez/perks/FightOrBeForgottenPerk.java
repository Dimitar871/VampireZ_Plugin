package com.vampirez.perks;

import com.vampirez.*;
import com.vampirez.MM;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class FightOrBeForgottenPerk extends Perk {

    private final Set<UUID> activated = new HashSet<>();

    public FightOrBeForgottenPerk() {
        super("fight_or_be_forgotten", "Fight or be Forgotten", PerkTier.GOLD, PerkTeam.HUMAN,
                Material.GOLDEN_SWORD,
                "On fatal damage: become invulnerable for 30s",
                "with Strength II + Speed I.",
                "After 30s, you die and become a Vampire.");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        activated.remove(player.getUniqueId());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        if (activated.contains(uuid)) {
            // Already in last stand — invulnerable
            event.setCancelled(true);
            return;
        }

        double healthAfter = victim.getHealth() - event.getFinalDamage();
        if (healthAfter <= 0) {
            // Trigger last stand
            event.setCancelled(true);
            activated.add(uuid);

            victim.setHealth(victim.getMaxHealth());
            // 30 seconds = 600 ticks
            victim.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 600, 4, false, true)); // Resistance V = invuln
            victim.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 600, 1, false, true)); // Strength II
            victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 600, 0, false, true)); // Speed I

            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 40, 0.5, 1, 0.5, 0.05);
            // MAX DRAMA — loudest moment in the game. Lightning + giant red shockwave +
            // layered explosion + persistent red atom orbit for the full 30s last-stand.
            com.vampirez.fx.VFX.fx().lightningFlash(victim.getLocation());
            com.vampirez.fx.VFX.fx().shockwave(victim.getLocation(), org.bukkit.Color.fromRGB(255, 0, 0), 8.0, 35);
            com.vampirez.fx.VFX.fx().atomAround(victim, org.bukkit.Color.fromRGB(255, 0, 0), 600); // 30s
            com.vampirez.fx.VFX.sound().playExplosion(victim.getLocation());
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.8f, 1.2f);
            victim.sendMessage(MM.parse("<red><bold>FIGHT OR BE FORGOTTEN! 30 seconds to live!"));

            VampireZPlugin fobfPlugin = (VampireZPlugin) getPlugin();
            if (fobfPlugin.getGameManager() != null) {
                for (Player p : fobfPlugin.getGameManager().getJoinedOnlinePlayers()) {
                    if (!p.equals(victim)) {
                        p.sendMessage(MM.parse("<red>" + victim.getName() + " enters their last stand!"));
                    }
                }
            }

            incrementStat(uuid, "activations");

            // After 30 seconds, kill and convert
            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                if (!victim.isOnline()) return;
                VampireZPlugin plugin = (VampireZPlugin) getPlugin();
                if (plugin.getGameManager().getState() != GameState.ACTIVE) return;
                if (!plugin.getGameManager().isHuman(uuid)) return; // already converted somehow

                // Remove buffs
                victim.removePotionEffect(PotionEffectType.RESISTANCE);
                victim.removePotionEffect(PotionEffectType.STRENGTH);
                victim.removePotionEffect(PotionEffectType.SPEED);

                // Convert to vampire — dramatic dark expiry: shockwave + vortex + death-rattle
                victim.sendMessage(MM.parse("<dark_red>Your time is up... you rise as a Vampire!"));
                victim.playSound(victim.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.8f);
                victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, victim.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.02);
                com.vampirez.fx.VFX.fx().shockwave(victim.getLocation(), org.bukkit.Color.fromRGB(80, 0, 0), 7.0, 30);
                com.vampirez.fx.VFX.fx().vortex(victim.getLocation(), org.bukkit.Color.fromRGB(40, 0, 0), 40);
                com.vampirez.fx.VFX.sound().playDeathRattle(victim.getLocation());

                plugin.getGameManager().convertHumanToVampire(victim);
            }, 600L); // 30 seconds
        }
    }

    @Override
    public void onTick(Player player) {
        if (!activated.contains(player.getUniqueId())) return;
        // Blazing last-stand aura - flames + golden particles
        double angle = System.currentTimeMillis() * 0.003;
        double radius = 0.7;
        for (int i = 0; i < 3; i++) {
            double a = angle + (i * 2.094); // 120 degrees apart
            double ox = Math.cos(a) * radius;
            double oz = Math.sin(a) * radius;
            player.getWorld().spawnParticle(Particle.FLAME,
                    player.getLocation().add(ox, 0.8 + Math.sin(a * 2) * 0.3, oz), 1, 0, 0.05, 0, 0.01);
        }
        player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 150, 0), 1.5f));
        player.getWorld().spawnParticle(Particle.LAVA, player.getLocation().add(0, 0.5, 0), 1, 0.3, 0.1, 0.3, 0);
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("activations", "Last Stands");
        return labels;
    }
}
