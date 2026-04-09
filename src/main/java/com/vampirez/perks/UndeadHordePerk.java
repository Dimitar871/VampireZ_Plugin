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
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class UndeadHordePerk extends Perk {

    private final Map<UUID, Long> lastSpawn = new HashMap<>();
    private static final long SPAWN_INTERVAL_MS = 30000;

    public UndeadHordePerk() {
        super("undead_horde", "Undead Horde", PerkTier.SILVER, PerkTeam.VAMPIRE,
                Material.ZOMBIE_HEAD,
                "Every 30s, automatically spawn",
                "2 zombies that fight for you (10s)");
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
            Zombie zombie = (Zombie) player.getWorld().spawnEntity(
                    player.getLocation().add(i == 0 ? 1 : -1, 0, 0), EntityType.ZOMBIE);
            zombie.setCustomName(ChatColor.RED + player.getName() + "'s Zombie");
            zombie.setCustomNameVisible(true);
            zombie.setBaby(false);
            zombie.getEquipment().setItemInMainHand(new ItemStack(Material.WOODEN_SWORD));
            zombie.getEquipment().setItemInMainHandDropChance(0);
            zombie.setMetadata("vampirez_team", new FixedMetadataValue(getPlugin(), "VAMPIRE"));

            Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
                if (!zombie.isDead()) {
                    zombie.getWorld().spawnParticle(Particle.LARGE_SMOKE, zombie.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);
                    zombie.remove();
                }
            }, 200L);
        }

        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation().add(0, 0.5, 0), 15, 0.5, 0.3, 0.5, 0.02);
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.8f);
        player.sendMessage(ChatColor.RED + "Undead Horde! 2 zombies summoned!");
    }

    @Override
    public Map<String, String> getStatLabels() {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("summons", "Summons");
        return labels;
    }
}
