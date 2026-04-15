package com.vampirez;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.vampirez.perks.BardPerk;
import com.vampirez.perks.BlackCleaverPerk;
import com.vampirez.perks.BlackShieldPerk;
import com.vampirez.perks.CurseOfDecayPerk;
import com.vampirez.perks.WarDrumsPerk;

import java.util.List;

public class PerkListener implements Listener {

    private final JavaPlugin plugin;
    private final GameManager gameManager;
    private final PerkManager perkManager;
    private final StatAnvilManager statAnvilManager;
    private BukkitTask tickTask;

    public PerkListener(JavaPlugin plugin, GameManager gameManager, PerkManager perkManager, StatAnvilManager statAnvilManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.perkManager = perkManager;
        this.statAnvilManager = statAnvilManager;
    }

    public void startTickTask() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // Tick fires for any player who has perks (works during game AND testing)
            for (Player player : Bukkit.getOnlinePlayers()) {
                List<Perk> perks = perkManager.getPlayerPerks(player.getUniqueId());
                if (perks.isEmpty()) continue;
                for (Perk perk : perks) {
                    perk.onTick(player);
                }
            }
        }, 20L, 20L);
    }

    public void stopTickTask() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDamage(EntityDamageByEntityEvent event) {
        // Handle attacker perks — fires if player has any perks
        Player attacker = resolveAttacker(event);
        if (attacker != null) {
            List<Perk> perks = perkManager.getPlayerPerks(attacker.getUniqueId());
            if (!perks.isEmpty()) {
                // Black Shield: if victim is shielded, block ALL attacker perk effects
                boolean blocked = false;
                if (event.getEntity() instanceof Player victim) {
                    if (BlackShieldPerk.isShielded(victim.getUniqueId())) {
                        // Only consume if attacker actually has perks that do something on hit
                        boolean hasOffensivePerk = perks.stream().anyMatch(p -> !(p instanceof BlackShieldPerk));
                        if (hasOffensivePerk) {
                            blocked = BlackShieldPerk.consumeShield(victim.getUniqueId(), victim, attacker, "perk abilities");
                        }
                    }
                }
                if (!blocked) {
                    for (Perk perk : perks) {
                        perk.onDamageDealt(attacker, event.getEntity(), event);
                    }
                }
            }
        }

        // Handle victim perks
        if (event.getEntity() instanceof Player victim) {
            List<Perk> perks = perkManager.getPlayerPerks(victim.getUniqueId());
            if (!perks.isEmpty()) {
                Entity attackerEntity = event.getDamager();
                for (Perk perk : perks) {
                    perk.onDamageTaken(victim, attackerEntity, event);
                }
            }
        }

        // War Drums: +10% damage if attacker is near a War Drums ally
        if (!event.isCancelled() && attacker != null && WarDrumsPerk.isBoosted(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 1.10);
        }

        // Bard Damage Aura: +1 heart (2 HP) per hit
        if (!event.isCancelled() && attacker != null && BardPerk.isDamageBoosted(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() + 2.0);
        }

        // Black Cleaver: amplify damage if victim is cleaved
        if (!event.isCancelled() && event.getEntity() instanceof Player victim) {
            int stacks = BlackCleaverPerk.getStacks(victim.getUniqueId());
            if (stacks > 0) {
                event.setDamage(event.getDamage() * (1.0 + 0.05 * stacks));
            }
        }

        // Stat Anvil: damage multiplier
        if (!event.isCancelled() && attacker != null) {
            double dmgMult = statAnvilManager.getDamageMultiplier(attacker.getUniqueId());
            if (dmgMult > 1.0) {
                event.setDamage(event.getDamage() * dmgMult);
            }
        }

        // Cap max PvP damage at 7.0 HP (3.5 hearts) to prevent multiplicative perk stacking
        if (!event.isCancelled() && event.getEntity() instanceof Player) {
            if (event.getDamage() > 7.0) {
                event.setDamage(7.0);
            }
        }

        // Stat Anvil: lifesteal (after cap, based on final damage)
        if (!event.isCancelled() && attacker != null && event.getEntity() instanceof Player) {
            double lifesteal = statAnvilManager.getLifestealPercent(attacker.getUniqueId());
            if (lifesteal > 0) {
                double heal = event.getDamage() * lifesteal;
                double newHealth = Math.min(attacker.getHealth() + heal, attacker.getMaxHealth());
                attacker.setHealth(newHealth);
            }
        }
    }

    /**
     * HIGHEST priority: runs AFTER GameListener (HIGH) sets base damage.
     * Applies Nether Blade damage multiplier on top of the final base damage.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamagePost(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player attacker = resolveAttacker(event);
        if (attacker == null) return;

        Double mult = com.vampirez.perks.NetherBladePerk.consumeDamageMultiplier(attacker.getUniqueId());
        if (mult != null) {
            event.setDamage(event.getDamage() * mult);
            // Re-apply damage cap
            if (event.getDamage() > 7.0) {
                event.setDamage(7.0);
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        List<Perk> victimPerks = perkManager.getPlayerPerks(victim.getUniqueId());
        if (!victimPerks.isEmpty()) {
            for (Perk perk : victimPerks) {
                perk.onDeath(victim, event);
            }
        }

        Player killer = victim.getKiller();
        if (killer != null) {
            List<Perk> killerPerks = perkManager.getPlayerPerks(killer.getUniqueId());
            if (!killerPerks.isEmpty()) {
                for (Perk perk : killerPerks) {
                    perk.onKill(killer, victim);
                }
            }
        }
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Curse of Decay: reduce healing by 50% for cursed players
        if (CurseOfDecayPerk.isCursed(player.getUniqueId())) {
            event.setAmount(event.getAmount() * (1.0 - CurseOfDecayPerk.HEALING_REDUCTION));
        }

        List<Perk> perks = perkManager.getPlayerPerks(player.getUniqueId());
        if (!perks.isEmpty()) {
            for (Perk perk : perks) {
                perk.onHealthRegain(player, event);
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;

        List<Perk> perks = perkManager.getPlayerPerks(player.getUniqueId());
        for (Perk perk : perks) {
            if (perk.negatesFallDamage()) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        List<Perk> perks = perkManager.getPlayerPerks(player.getUniqueId());
        if (!perks.isEmpty()) {
            for (Perk perk : perks) {
                perk.onInteract(player, event);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        List<Perk> perks = perkManager.getPlayerPerks(player.getUniqueId());
        if (perks.isEmpty()) return;

        // Delay perk re-application to after respawn completes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Perk perk : perks) {
                perk.onRespawn(player);
            }
        }, 5L);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            return (Player) event.getDamager();
        }
        if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }
}
