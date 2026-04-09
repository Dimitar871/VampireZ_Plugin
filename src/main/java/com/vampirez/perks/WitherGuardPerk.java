package com.vampirez.perks;

import com.vampirez.Perk;
import com.vampirez.PerkTeam;
import com.vampirez.PerkTier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class WitherGuardPerk extends Perk {

    private final Map<UUID, Long> lastSpawn = new HashMap<>();
    private static final long SPAWN_INTERVAL_MS = 30000;

    public WitherGuardPerk() {
        super("wither_guard", "Wither Guard", PerkTier.PRISMATIC, PerkTeam.VAMPIRE,
                Material.WITHER_SKELETON_SKULL,
                "Every 30s, automatically spawn",
                "2 wither skeletons that fight for you (12s)");
    }

    @Override
    public void apply(Player player) {}

    @Override
    public void remove(Player player) {
        lastSpawn.remove(player.getUniqueId());
    }

    @Override
    public void onTick(Player player) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSpawn.get(uuid);
        if (last != null && (now - last) < SPAWN_INTERVAL_MS) return;

        lastSpawn.put(uuid, now);
        incrementStat(uuid, "summons");

        for (int i = 0; i < 2; i++) {
            WitherSkeleton wSkeleton = (WitherSkeleton) player.getWorld().spawnEntity(
                    player.getLocation().add(i == 0 ? 1.5 : -1.5, 0, 0), EntityType.WITHER_SKELETON);
            wSkeleton.setCustomName(ChatColor.DARK_RED + player.getName() + "'s Wither Guard");
            wSkeleton.setCustomNameVisible(true);
            wSkeleton.getEquipment().setItemInMainHand(new ItemStack(Material.STONE_SWORD));
            wSkeleton.getEquipment().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
            wSkeleton.getEquipment().setItemInMainHandDropChance(0);
            wSkeleton.getEquipment().setHelmetDropChance(0);
            wSkeleton.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "VAMPIRE"));

            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                if (!wSkeleton.isDead()) {
                    wSkeleton.getWorld().spawnParticle(Particle.SOUL, wSkeleton.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.02);
                    wSkeleton.remove();
                }
            }, 240L);
        }

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 0.5, 0), 20, 0.5, 0.3, 0.5, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 0.8f);
        player.sendMessage(ChatColor.DARK_RED + "Wither Guard! 2 wither skeletons summoned!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("summons", "Summons");
        return labels;
    }
}
