package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class PhantomStepPerk extends Perk {

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, ItemStack[]> storedArmor = new HashMap<>();
    private final Set<UUID> phantomActive = new HashSet<>();
    private static final long COOLDOWN_MS = 15000;

    public PhantomStepPerk() {
        super("phantom_step", "Phantom Step", PerkTier.GOLD, PerkTeam.VAMPIRE,
                Material.PHANTOM_MEMBRANE,
                "After taking damage: 1s full invisibility",
                "(armor hidden, protection kept) + Speed I",
                "15s cooldown");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        UUID uuid = player.getUniqueId();
        cooldowns.remove(uuid);
        phantomActive.remove(uuid);
        if (storedArmor.containsKey(uuid)) {
            player.getInventory().setArmorContents(storedArmor.remove(uuid));
        }
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        UUID uuid = victim.getUniqueId();
        if (phantomActive.contains(uuid)) return; // Already invisible
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(uuid);
        if (last != null && (now - last) < getEffectiveCooldown(victim, COOLDOWN_MS)) return;

        cooldowns.put(uuid, now);
        phantomActive.add(uuid);

        // Store armor and hide it visually
        storedArmor.put(uuid, victim.getInventory().getArmorContents().clone());
        victim.getInventory().setArmorContents(new ItemStack[4]);

        victim.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20, 0, false, false), true);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20, 0, false, true), true);
        victim.getWorld().spawnParticle(Particle.LARGE_SMOKE, victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.02);
        victim.playSound(victim.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);
        victim.sendMessage(ChatColor.DARK_PURPLE + "Phantom Step! You vanished!");
        incrementStat(uuid, "vanishes");

        // Restore armor after 1s (20 ticks)
        Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
            phantomActive.remove(uuid);
            if (storedArmor.containsKey(uuid)) {
                Player p = Bukkit.getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.getInventory().setArmorContents(storedArmor.remove(uuid));
                } else {
                    storedArmor.remove(uuid);
                }
            }
        }, 20L);
    }

    /** Returns true if the player is currently in phantom step (armor hidden but protected). */
    public boolean isPhantomActive(UUID uuid) {
        return phantomActive.contains(uuid);
    }

    /** Get stored armor items for protection calculation. */
    public ItemStack[] getStoredArmor(UUID uuid) {
        return storedArmor.get(uuid);
    }

    /** Get stored armor points for damage calculation during phantom step. */
    public double getStoredArmorValue(UUID uuid) {
        ItemStack[] armor = storedArmor.get(uuid);
        if (armor == null) return 0;
        double total = 0;
        for (ItemStack piece : armor) {
            if (piece == null) continue;
            total += getArmorPoints(piece.getType());
        }
        return total;
    }

    private double getArmorPoints(Material mat) {
        return switch (mat) {
            case LEATHER_HELMET -> 1; case LEATHER_CHESTPLATE -> 3; case LEATHER_LEGGINGS -> 2; case LEATHER_BOOTS -> 1;
            case CHAINMAIL_HELMET -> 2; case CHAINMAIL_CHESTPLATE -> 5; case CHAINMAIL_LEGGINGS -> 4; case CHAINMAIL_BOOTS -> 1;
            case IRON_HELMET -> 2; case IRON_CHESTPLATE -> 6; case IRON_LEGGINGS -> 5; case IRON_BOOTS -> 2;
            case DIAMOND_HELMET -> 3; case DIAMOND_CHESTPLATE -> 8; case DIAMOND_LEGGINGS -> 6; case DIAMOND_BOOTS -> 3;
            case NETHERITE_HELMET -> 3; case NETHERITE_CHESTPLATE -> 8; case NETHERITE_LEGGINGS -> 6; case NETHERITE_BOOTS -> 3;
            default -> 0;
        };
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("vanishes", "Vanishes");
        return labels;
    }
}
