package com.vampirez.engine;

import com.vampirez.Perk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A Perk implementation whose behavior is driven by a YAML config rather than hand-written
 * Java code. Each {@link HookType} maps to a list of {@link TriggerEntry}; each entry runs
 * its conditions and (if all pass) executes its actions.
 */
public class DataDrivenPerk extends Perk {

    private final Map<HookType, List<TriggerEntry>> triggers;

    public DataDrivenPerk(PerkConfig config) {
        super(config.id, config.displayName, config.tier, config.team, config.icon, config.description);
        this.triggers = config.triggers;
    }

    private void dispatch(HookType hook, HookContext ctx) {
        List<TriggerEntry> entries = triggers.getOrDefault(hook, Collections.emptyList());
        for (TriggerEntry entry : entries) {
            if (entry.matches(ctx)) {
                entry.execute(ctx);
            }
        }
    }

    @Override
    public void apply(Player player) {
        dispatch(HookType.APPLY, HookContext.builder(HookType.APPLY).owner(player).build());
    }

    @Override
    public void remove(Player player) {
        dispatch(HookType.REMOVE, HookContext.builder(HookType.REMOVE).owner(player).build());
    }

    @Override
    public void onDamageDealt(Player attacker, Entity victim, EntityDamageByEntityEvent event) {
        Player victimPlayer = victim instanceof Player ? (Player) victim : null;
        dispatch(HookType.ON_DAMAGE_DEALT, HookContext.builder(HookType.ON_DAMAGE_DEALT)
                .owner(attacker)
                .attacker(attacker)
                .victim(victim)
                .victimPlayer(victimPlayer)
                .damageEvent(event)
                .build());
    }

    @Override
    public void onDamageTaken(Player victim, Entity attacker, EntityDamageByEntityEvent event) {
        Player attackerPlayer = attacker instanceof Player ? (Player) attacker : null;
        dispatch(HookType.ON_DAMAGE_TAKEN, HookContext.builder(HookType.ON_DAMAGE_TAKEN)
                .owner(victim)
                .attacker(attackerPlayer)
                .victim(victim)
                .victimPlayer(victim)
                .damageEvent(event)
                .build());
    }

    @Override
    public void onKill(Player killer, Player victim) {
        dispatch(HookType.ON_KILL, HookContext.builder(HookType.ON_KILL)
                .owner(killer)
                .killer(killer)
                .victim(victim)
                .victimPlayer(victim)
                .build());
    }

    @Override
    public void onDeath(Player player, PlayerDeathEvent event) {
        dispatch(HookType.ON_DEATH, HookContext.builder(HookType.ON_DEATH)
                .owner(player)
                .victimPlayer(player)
                .deathEvent(event)
                .build());
    }

    @Override
    public void onTick(Player player) {
        dispatch(HookType.ON_TICK, HookContext.builder(HookType.ON_TICK).owner(player).build());
    }

    @Override
    public void onHealthRegain(Player player, EntityRegainHealthEvent event) {
        dispatch(HookType.ON_HEALTH_REGAIN, HookContext.builder(HookType.ON_HEALTH_REGAIN)
                .owner(player)
                .regainEvent(event)
                .build());
    }

    @Override
    public void onInteract(Player player, PlayerInteractEvent event) {
        dispatch(HookType.ON_INTERACT, HookContext.builder(HookType.ON_INTERACT)
                .owner(player)
                .interactEvent(event)
                .build());
    }

    @Override
    public void onRespawn(Player player) {
        dispatch(HookType.ON_RESPAWN, HookContext.builder(HookType.ON_RESPAWN).owner(player).build());
    }
}
