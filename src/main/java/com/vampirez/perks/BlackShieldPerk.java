package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import com.vampirez.MM;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class BlackShieldPerk extends Perk {

    private static final long COOLDOWN_MS = 30_000; // 30 seconds
    private static final Map<UUID, Long> cooldowns = new HashMap<>();
    private static final Set<UUID> shielded = new HashSet<>();

    public BlackShieldPerk() {
        super("black_shield", "Black Shield", PerkTier.SILVER, PerkTeam.BOTH,
                Material.NETHERITE_SCRAP,
                "Passively blocks the next enemy",
                "perk ability used against you.",
                "Blocks damage perks, teleports, pulls, etc.",
                "30 second cooldown after blocking");
    }

    @Override
    public void apply(Player player) {
        shielded.add(player.getUniqueId());
        player.sendMessage(MM.parse("<dark_gray>Black Shield <green>active!"));
    }

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        shielded.remove(uuid);
        cooldowns.remove(uuid);
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        // Auto-refresh shield when cooldown expires
        if (!shielded.contains(uuid)) {
            long now = System.currentTimeMillis();
            Long lastUse = cooldowns.get(uuid);
            if (lastUse == null || now - lastUse >= getEffectiveCooldown(player, COOLDOWN_MS)) {
                shielded.add(uuid);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 8, 0.3, 0.5, 0.3, 0.01);
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.3f, 2.0f);
                player.sendMessage(MM.parse("<dark_gray>Black Shield <green>recharged!"));
                incrementStat(uuid, "recharges");
            }
        }
    }

    // ===== Static API for other classes to check/consume the shield =====

    /**
     * Check if a player currently has Black Shield active.
     */
    public static boolean isShielded(UUID uuid) {
        return shielded.contains(uuid);
    }

    /**
     * Consume the shield on a player, notifying both parties.
     * Returns true if the shield was active and consumed.
     */
    public static boolean consumeShield(UUID victimUUID, Player victim, Player attacker, String abilityName) {
        if (!shielded.contains(victimUUID)) return false;

        shielded.remove(victimUUID);
        cooldowns.put(victimUUID, System.currentTimeMillis());

        // Visual feedback on the shielded player
        victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, victim.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);
        victim.getWorld().spawnParticle(Particle.DUST, victim.getLocation().add(0, 1.2, 0), 15, 0.5, 0.5, 0.5, 0,
                new Particle.DustOptions(org.bukkit.Color.fromRGB(20, 20, 20), 1.5f));
        victim.playSound(victim.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 0.6f);

        // Notify both players
        victim.sendMessage(MM.parse("<dark_gray>Black Shield <white>blocked <red>" + abilityName + "<white>! <gray>(30s cooldown)"));
        if (attacker != null) {
            attacker.sendMessage(MM.parse("<red>Your " + abilityName + " was blocked by Black Shield!"));
        }

        return true;
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("recharges", "Times Recharged");
        return labels;
    }
}
