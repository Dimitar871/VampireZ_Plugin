package com.vampirez.engine;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Bag of references passed to every Action and Condition. Not all fields are populated
 * for every hook — code that uses a field should check the hook type or null.
 */
public class HookContext {
    public final HookType hook;
    public final Player owner;
    public final Player attacker;
    public final Player killer;
    public final Player victimPlayer;
    public final Entity victim;
    public final EntityDamageByEntityEvent damageEvent;
    public final EntityRegainHealthEvent regainEvent;
    public final PlayerDeathEvent deathEvent;
    public final PlayerInteractEvent interactEvent;

    private HookContext(Builder b) {
        this.hook = b.hook;
        this.owner = b.owner;
        this.attacker = b.attacker;
        this.killer = b.killer;
        this.victimPlayer = b.victimPlayer;
        this.victim = b.victim;
        this.damageEvent = b.damageEvent;
        this.regainEvent = b.regainEvent;
        this.deathEvent = b.deathEvent;
        this.interactEvent = b.interactEvent;
    }

    public static Builder builder(HookType hook) {
        return new Builder(hook);
    }

    public static class Builder {
        private final HookType hook;
        private Player owner;
        private Player attacker;
        private Player killer;
        private Player victimPlayer;
        private Entity victim;
        private EntityDamageByEntityEvent damageEvent;
        private EntityRegainHealthEvent regainEvent;
        private PlayerDeathEvent deathEvent;
        private PlayerInteractEvent interactEvent;

        Builder(HookType hook) { this.hook = hook; }

        public Builder owner(Player p) { this.owner = p; return this; }
        public Builder attacker(Player p) { this.attacker = p; return this; }
        public Builder killer(Player p) { this.killer = p; return this; }
        public Builder victimPlayer(Player p) { this.victimPlayer = p; return this; }
        public Builder victim(Entity e) { this.victim = e; return this; }
        public Builder damageEvent(EntityDamageByEntityEvent e) { this.damageEvent = e; return this; }
        public Builder regainEvent(EntityRegainHealthEvent e) { this.regainEvent = e; return this; }
        public Builder deathEvent(PlayerDeathEvent e) { this.deathEvent = e; return this; }
        public Builder interactEvent(PlayerInteractEvent e) { this.interactEvent = e; return this; }

        public HookContext build() { return new HookContext(this); }
    }
}
