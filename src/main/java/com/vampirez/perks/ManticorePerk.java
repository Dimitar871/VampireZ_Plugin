package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ManticorePerk extends Perk {

    private static final double AOE_RANGE = 15.0;
    private static final int DEBUFF_DURATION_TICKS = 30 * 20; // 30 seconds
    private static final long BUFF_DURATION_MS = 30_000L;       // 30 seconds

    // One entry per kill: the amount of bonus max HP granted and when it expires.
    private static final class HealthGrant {
        final double amount;
        final long expiresAt;
        HealthGrant(double amount, long expiresAt) {
            this.amount = amount;
            this.expiresAt = expiresAt;
        }
    }

    // killer UUID -> list of active temporary max-HP grants
    private final Map<UUID, List<HealthGrant>> activeGrants = new HashMap<>();

    public ManticorePerk() {
        super("manticore", "Manticore", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.TURTLE_SCUTE,
                "On kill: gain half the victim's max hearts",
                "as temporary bonus HP for 30s.",
                "Nearby enemies get Weakness I + Slowness I (30s).");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        List<HealthGrant> grants = activeGrants.remove(uuid);
        if (grants == null || grants.isEmpty()) return;
        double total = 0;
        for (HealthGrant g : grants) total += g.amount;
        if (total > 0) {
            subtractMaxHealth(player, total);
        }
    }

    @Override
    public void onKill(Player killer, Player victim) {
        UUID uuid = killer.getUniqueId();

        // 1. Gain bonus max hearts equal to HALF the victim's max HP.
        double victimMax = 20.0;
        if (victim.getAttribute(Attribute.MAX_HEALTH) != null) {
            victimMax = victim.getAttribute(Attribute.MAX_HEALTH).getValue();
        }
        double bonus = victimMax / 2.0;
        if (bonus > 0) {
            double currentMax = killer.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
            double newMax = currentMax + bonus;
            killer.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMax);
            // Heal up into the new bonus so it is immediately useful.
            killer.setHealth(Math.min(killer.getHealth() + bonus, newMax));

            activeGrants.computeIfAbsent(uuid, k -> new ArrayList<>())
                    .add(new HealthGrant(bonus, System.currentTimeMillis() + BUFF_DURATION_MS));

            addStat(uuid, "hp_gained", bonus);
            killer.sendMessage(MM.parse("<dark_red>Manticore: +" + String.format("%.1f", bonus / 2.0)
                    + " hearts for 30s!"));
        }

        // 2. Afflict all enemies within 15 blocks with Weakness I + Slowness I for 30s.
        int afflicted = 0;
        for (Entity entity : killer.getNearbyEntities(AOE_RANGE, AOE_RANGE, AOE_RANGE)) {
            if (entity instanceof Player target && !target.equals(killer) && !isSameTeam(killer, target)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, DEBUFF_DURATION_TICKS, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, DEBUFF_DURATION_TICKS, 0, false, true));
                target.getWorld().spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0,
                        new Particle.DustOptions(Color.fromRGB(120, 30, 30), 1.5f));
                afflicted++;
            }
        }

        // Feedback / cinematic cue on the killer.
        killer.getWorld().spawnParticle(Particle.DUST, killer.getLocation().add(0, 1, 0), 40, 0.6, 0.8, 0.6, 0,
                new Particle.DustOptions(Color.fromRGB(160, 20, 20), 2.0f));
        killer.playSound(killer.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.4f);

        incrementStat(uuid, "kills");
        if (afflicted > 0) addStat(uuid, "afflicted", afflicted);
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        List<HealthGrant> grants = activeGrants.get(uuid);
        if (grants == null || grants.isEmpty()) return;

        long now = System.currentTimeMillis();
        double expiredTotal = 0;
        Iterator<HealthGrant> it = grants.iterator();
        while (it.hasNext()) {
            HealthGrant g = it.next();
            if (now >= g.expiresAt) {
                expiredTotal += g.amount;
                it.remove();
            }
        }

        if (expiredTotal > 0) {
            subtractMaxHealth(player, expiredTotal);
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_HURT, 0.4f, 1.6f);
        }
        if (grants.isEmpty()) {
            activeGrants.remove(uuid);
        }
    }

    /**
     * Removes the given amount of max health, never dropping below the 20.0 vanilla
     * baseline, and clamps current health so the player is never left in an over-max
     * (broken) state.
     */
    private void subtractMaxHealth(Player player, double amount) {
        if (player.getAttribute(Attribute.MAX_HEALTH) == null) return;
        double current = player.getAttribute(Attribute.MAX_HEALTH).getBaseValue();
        double newMax = Math.max(current - amount, 20.0);
        player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(newMax);
        if (player.getHealth() > newMax) {
            player.setHealth(newMax);
        }
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("kills", "Kills");
        labels.put("hp_gained", "Bonus HP Gained");
        labels.put("afflicted", "Enemies Afflicted");
        return labels;
    }
}
