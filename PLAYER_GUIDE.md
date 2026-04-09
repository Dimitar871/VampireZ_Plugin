# VampireZ - Player Guide

## How to Play

VampireZ is a team-based survival minigame. Players are split into **Humans** and **Vampires**. Humans must survive for **25 minutes** while Vampires try to convert all humans before the timer runs out.

### Objective

- **Humans Win:** Survive until the timer reaches zero.
- **Vampires Win:** Convert every human into a vampire before time runs out.

### Game Flow

1. **Lobby** - Players gather in the lobby. The game starts when enough players join.
2. **Team Assignment** - 30% of players become Vampires, the rest are Humans.
3. **Scouting Phase** - Vampires can see the arena but cannot interact or fight yet.
4. **Game Start** - All players are teleported to their spawns and receive gear. A free Silver perk is given to everyone.
5. **Active Game (25 minutes)** - Fight, survive, and earn gold to buy perks.
6. **Game End** - Humans or Vampires win based on the conditions above.

### Death & Conversion

- **Humans who die become Vampires.** You respawn on the vampire team with vampire gear. Your human-only perks are removed and replaced with free vampire perk picks.
- **Vampires who die respawn** after 4 seconds at the vampire spawn with fresh gear.

---

## Economy

| Source | Gold | Notes |
|--------|------|-------|
| Passive Income | +2g | Every 10 seconds, all players |
| Kill | +15g | To the player who got the final hit |
| Assist | +5g | Any player who damaged the victim within 10 seconds |

### Spending Gold

- **Perk Shop** - Right-click the Emerald in slot 9 (or use `/vz shop`)
  - Silver Perks: 50g
  - Gold Perks: 150g
  - Prismatic Perks: 400g
- **Armor Repair** - 25g (available in the Perk Shop)

### Free Perks

You receive free perks at these milestones during the game:

| Time Elapsed | Free Perk Tier |
|-------------|----------------|
| Game Start | Silver |
| 5 minutes | Silver |
| 10 minutes | Gold |
| 15 minutes | Prismatic |

Each time, you pick 1 of 3 random options.

---

## Teams & Gear

### Human Gear

| Item | Details |
|------|---------|
| Iron Sword | Sharpness I |
| Bow | Standard |
| Golden Apple x3 | Healing |
| Cooked Beef x12 | Food |
| Arrow x24 | Ammunition |
| Emerald | Perk Shop (right-click) |
| Full Iron Armor | Protection I on all pieces |

### Vampire Gear

| Item | Details |
|------|---------|
| Stone Sword | Standard |
| Rotten Flesh x8 | Food |
| Ghast Tear | Vampire Leap (right-click, 8s cooldown) |
| Emerald | Perk Shop (right-click) |
| Full Leather Armor | Unbreakable |
| Night Vision | Permanent |

### Vampire Abilities

- **Vampire Leap** - Right-click the Ghast Tear to launch yourself in the direction you're looking. 8-second cooldown.
- **Blood Compass** - At 10 minutes remaining, all vampires receive a compass that points toward the nearest human.

---

## Day/Night Cycle

| Phase | Duration | Vampire Effect |
|-------|----------|----------------|
| Day | 6 minutes | Slowness I |
| Night | 4 minutes | Speed I |

The cycle repeats throughout the game. Vampires are stronger at night and weaker during the day.

---

## Combat

- **Human base damage:** 2.5 hearts per hit
- **Vampire base damage:** 2.0 hearts per hit
- Damage scales with your attack cooldown (swing timer) - wait for full cooldown for maximum damage
- Must hold a Sword or Axe to deal weapon damage
- Only 30% of armor reduction applies (combat is faster-paced than vanilla)
- Maximum damage per hit is capped at 3.5 hearts
- Friendly fire is disabled

---

## Commands

| Command | Description |
|---------|-------------|
| `/vz shop` | Open the perk shop |
| `/vz perks` | List your active perks |
| `/vz gold` | Show your gold balance |
| `/vz status` | Show game state, time left, team counts |
| `/vz help` | Show all commands |

---

## Perk List

You can hold up to **4 perks** at a time. Perks are bought from the shop or received for free at milestones. When buying, you pick 1 of 3 random options from your chosen tier.

### Silver Tier (Cost: 50g)

#### Both Teams

| Perk | Description |
|------|-------------|
| Blunt Force | +20% melee damage |
| Deft | Permanent Speed I |
| Heavy Hitter | +4% of your max HP as bonus damage per hit |
| Goredrink | 15% lifesteal on damage dealt |
| Escape Plan (Weak) | Below 4 hearts: Speed I + Absorption I for 3s (30s cd) |
| Vitality | +2 max hearts |
| Tough Skin | -10% damage taken |
| Swift Strikes | +15% attack speed |
| Dash | Right-click Feather to dash forward (5s cd) |
| Second Wind | Regeneration I when below 50% HP |
| Adrenaline Rush | Taking damage grants Speed I for 3s |
| Riposte | After being hit, next attack within 3s deals +40% damage |
| Momentum | Consecutive hits within 3s stack +8% damage (max 3 stacks = +24%) |
| Gravity Well | Melee hits pull target 1 block toward you (3s cd) |
| Supply Drop | Every 90s: chest spawns with 2 golden apples + 8 arrows (15s duration) |
| Trail | Sprinting leaves a 25-block trail. Enemies on the trail get Slowness I. Allies get Speed I |
| Black Shield | Passively blocks the next enemy perk ability used against you (60s cd) |
| Combo Shield | Hit enemies 4 times without being hit to gain Absorption hearts |
| Haste | Reduces cooldown of all activatable abilities by 30% |
| Sunder | Attacks deal bonus damage equal to 5% of enemy's current HP |
| Regenerative | When damaged, regenerate 30% of the damage taken over 10s |
| Headhunter | Bow headshots (above chest height) deal 2x damage |

#### Human Only

| Perk | Description |
|------|-------------|
| First-Aid Kit | +30% healing received |
| Buff Buddies | Allies within 10 blocks get Resistance I |
| Steady Aim | +20% projectile damage |
| Iron Wall | Gives Shield item to block attacks |
| Thorns | Reflect 10% melee damage to attacker |
| Archer's Quiver | Infinity + Power I bow |
| Deflect | 30% chance to negate projectile damage |
| Fortify | Crouching grants Resistance I |
| Guardian's Oath | -15% damage taken when ally within 5 blocks |
| Wolf Pack | On vampire kill: summon 2 wolves (8s) |
| Sharpness Boost | Sword upgraded to Sharpness II |
| Protection Boost | All armor upgraded to Protection II |
| Arrow Supply | +16 extra arrows (40 total) |
| Healing Potions | 3 Instant Health potions, regen 1 every 2 min |
| Fire Aspect | Sword gets Fire Aspect I |
| Speed Potions | 3 Speed potions, regen 1 every 2 min |
| Rally Cry | On kill: allies within 8 blocks get Speed I + Regen I for 4s |
| Natural Leader | Glow effect, +1g/3s per nearby human, 1%/s Regen I to nearby humans |
| Cooker | Every 60s, feed nearby humans +2 saturation |
| Medic | 3 healing snowballs (heal 3 hearts), regen 1 every 15s |
| Flame Arrows | Bow gets Flame I |
| Scaling | Grow stronger over time! 10 min: Protection I. More later |
| Heavyweight | Double your max hearts. -50% damage dealt |
| Porcupine | Stand still for 2s: attackers take 2 hearts + Slowness I 2s (5s cd) |
| War Drums | Allies within 12 blocks deal +10% damage. You deal -10% damage |
| Fortune Teller | Every 20s: nearest vampire is marked with Glowing for 5s (visible through walls) |

#### Vampire Only

| Perk | Description |
|------|-------------|
| Homeguard | Speed III for 5s on respawn (removed on damage) |
| Dive Bomber | On death: explode 4 hearts to enemies within 4 blocks |
| Scavenger | Kills drop a Golden Apple |
| Backstab | +30% damage when hitting from behind |
| Bloodlust | Each kill grants +1 max heart (up to +3) |
| Poison Fang | Attacks apply Poison I for 3s |
| Pack Hunter | +10% damage per ally within 6 blocks of target (max +30%) |
| Bone Armor | First hit every 12s reduced by 50% |
| Leech Swarm | On death: spawn 3 silverfish at death location (5s) |
| Undead Horde | Every 30s: spawn 2 zombies that fight for you (10s) |
| Blood Scent | Enemies below 50% HP glow through walls |
| Feral Charge | Sprinting attacks deal +30% damage (6s cd) |
| Infectious Bite | On kill: heal all vampires within 8 blocks for 4 hearts |
| Chain Armor | Upgrades leather armor to chainmail armor |

---

### Gold Tier (Cost: 150g)

#### Both Teams

| Perk | Description |
|------|-------------|
| Celestial Body | +4 max hearts, -10% damage dealt |
| Executioner | +30% damage to targets below 40% HP |
| Get Excited | On kill: Speed II + Strength I for 6s |
| It's Critical | 30% chance for 1.5x damage |
| Berserker | Below 30% HP: Strength I + Speed I |
| Smoke Bomb | Right-click: Blindness to enemies within 10 blocks for 5s (20s cd) |
| Ender Pearl Supply | 3 Ender Pearls, regen 1 every 30s |
| Iron Rations | +8 cooked beef + 2 golden apples |
| Last Stand | Below 25% HP: immune to knockback + 25% more damage |
| Siphon | Melee hit steals 1 heart from target (8s cd per target) |
| Heartsteal | Hit player near you 5+ seconds: +0.5 max hearts permanent (60s cd per target) |
| Black Cleaver | Each hit shreds 5% armor (max 5 stacks = 25%, 10s duration) |
| Bounty Hunter | Kills grant +15 bonus gold |
| Whirlwind | Right-click: 3 hearts to all enemies within 3.5 blocks (12s cd) |
| Life Link | Ally within 15 blocks: both get +2 bonus hearts |
| Lightweight | +25% damage dealt, -4 max hearts |
| Selfish | No teammates within 25 blocks: +6 max hearts + Strength I. Teammate nearby: -3 max hearts |
| Gore Drinker | Right-click your sword: slash all enemies within 5 blocks for 1 heart each. Heal per target hit (20s cd) |
| War Horse | Spawn 2 war horses (2x speed, 3x HP, diamond armor, anyone can ride). Goat Horn to call them back |

#### Human Only

| Perk | Description |
|------|-------------|
| Dawnbringer's Resolve | Auto-regen 1 heart/2s when below 4 hearts |
| All For You | Splash healing potions heal 50% more |
| Armored Up | Armor upgraded to Diamond |
| Phoenix Down | One-time auto-revive at half HP (consumed) |
| Crossbow Expert | Gives Crossbow with Quick Charge II + Piercing |
| Mirror Shield | 20% chance negate damage + reflect to attacker |
| Golden Guard | Fatal damage: spend 30g to survive at 2 hearts (once per life) |
| Barricade | Right-click: 3-wide 2-tall glass wall for 5s (25s cd) |
| Power Shot | Bow gets Power I |
| Blast Shield | Armor gets Blast Protection II |
| Strength Potions | 3 Strength potions, regen 1 every 2 min |
| Golden Feast | +9 extra Golden Apples (12 total) |
| Knockback | Sword gets Knockback I |
| Poison Quiver | 8 Poison arrows, regen 1 every 10s |
| Consecrated Ground | Right-click: holy zone heals allies/damages enemies 8s (40s cd) |
| Martyr | On death: allies within 10 blocks heal 4 hearts + Absorption II 5s |
| Shield | Right-click sword: 3 absorption hearts for 4s (45s cd) |
| Fight or be Forgotten | On fatal damage: become invulnerable for 30s with Strength II + Speed I. Must get a kill or die |
| Sunfire Cape | Emit a burning aura. Enemies within 5 blocks take fire damage |
| Ricochet Shot | Arrows bounce to nearest enemy within 8 blocks for 50% damage |
| Overcharge | Hold bow 3+s, next shot deals 2x damage + explosion (20s cd) |
| Always Connected | Reveal all humans to your team for 5s + heal all humans 1 heart (cd) |

#### Vampire Only

| Perk | Description |
|------|-------------|
| Blunt Force II | +20% melee damage (stacks with Silver version) |
| Shadow Strike | Right-click: teleport behind nearest enemy within 15 blocks (15s cd) |
| Frost Bite | Attacks apply Slowness I 3s + Frost Walker on boots |
| Phantom Step | After taking damage: 2s invisibility + Speed I (15s cd) |
| Blood Price | Right-click: sacrifice 3 hearts, next hit within 8s deals 2x (20s cd) |
| Tether | Right-click: pull nearest enemy within 12 blocks toward you (15s cd) |
| Skeleton Archers | Every 30s: spawn 2 skeleton archers (10s) |
| Harming Potions | 3 Splash Harming potions, regen 1 every 2 min |
| Hemophilia | Attacks inflict Bleeding: 0.5 hearts/sec for 4s (refreshes) |
| Nocturnal | Night: +2 max hearts + 15% bonus damage; Day: -1 max heart |
| Corpse Explosion | On kill: victim explodes after 1s, 3 hearts AoE 5 blocks |
| Blood Beacon | Right-click bone: place beacon granting Regen I to nearby vampires 20s (60s cd) |
| Shadow Ambush | Right-click flint: 3s invisibility, next hit +50% damage (30s cd) |
| Spider Climb | Near walls while sneaking: Levitation I to climb. No fall damage |
| Plague Carrier | Damage spreads victim's negative effects to enemies within 4 blocks |

---

### Prismatic Tier (Cost: 400g)

#### Both Teams

| Perk | Description |
|------|-------------|
| Glass Cannon | -30% max HP, +35% damage dealt |
| Goliath | +6 max hearts, +10% damage, Slowness I |
| Thunderstrike | Every 5th hit summons lightning on target |
| Earthquake | Right-click: 6-block AoE knockback + 3 hearts (30s cd) |
| Chain Lightning | Melee hits chain to nearest enemy within 5 blocks for 50% damage + lightning |
| Death's Gambit | Fatal damage: 50% survive at 1 HP + 3s invuln, 50% take 50% MORE (once per life) |
| Plague Doctor | Immune to all negative effects (poison, wither, slowness, weakness, blindness, etc.) |
| Decoy | At 20% HP: spawn 4 clones + go fully invisible 2s (120s cd) |
| Ankh of Rebirth | On death: save location. On respawn: teleport to death location with 60% HP |

#### Human Only

| Perk | Description |
|------|-------------|
| Can't Touch This | 8s invulnerability every 30s (white particles when active) |
| Courage of the Colossus | Hit a player to gain 2 absorption hearts (30s cd) |
| Escape Plan (Strong) | Below 4 hearts: Absorption III + Speed II for 5s (45s cd) |
| Giant Slayer | Speed I + 25% damage to targets with more max HP |
| Netherite Arsenal | Netherite Sword (Sharp I) + Netherite Helmet & Chestplate |
| Guardian Angel | Auto-revive 50% HP + 5s invuln (3 min cd, reusable) |
| Trapper | 5 placeable cobwebs to slow enemies |
| Marksman | Power III + Punch I bow, 32 arrows |
| Citadel | Right-click: 5-block zone Resistance I + Regen I for allies 8s (60s cd) |
| Holy Shield | Absorb 10 damage then auto-explode 4 hearts AoE 5 blocks |
| Temporal Shield | Right-click: freeze enemies within 5 blocks for 2s (45s cd) |
| Iron Guardian | On vampire kill: summon Iron Golem (12s) |
| Diamond Edge | Iron sword replaced with Diamond Sword (Sharpness I) |
| Thorns Enchant | All armor gets Thorns II + Unbreaking III |
| Regeneration Potions | 3 Regen potions, regen 1 every 2 min |
| Radiant Aura | Enemies within 6 blocks take 1 heart/3s + Glowing |
| Time Warp | Fatal damage: rewind to position + HP from 3s ago (90s cd) |
| Bard | Weakness IV (weak melee). Hold Melon: Regen I to allies. Hold Blaze Rod: Strength I to allies. Hold Iron Nugget: Jump Boost IV + Speed II to self |
| Plant Master | 4 magical flowers to plant anywhere. Each creates an AoE zone for 10s (30s cd) |
| Dimensional Pocket | Right-click: store HP. Click again within 30s: swap to stored HP (45s cd) |

#### Vampire Only

| Perk | Description |
|------|-------------|
| Erosion | Hits apply Weakness I for 3s (stacking duration) |
| Final Form | Every 60s: Absorption IV + 25% lifesteal for 8s |
| Firebrand | Attacks set target on fire 3s + 0.5 heart bonus damage |
| Double Tap | Crits deal 1.75x + Slowness I 2s |
| Bat Form | Right-click: invisible + Speed III for 5s (25s cd) |
| Soul Eater | Each kill grants +10% permanent damage (max 3 stacks = +30%) |
| Summoner | Right-click: summon 2 wolves to attack enemies (45s cd) |
| Void Walker | Right-click: teleport to random enemy within 30 blocks + 3 heart AoE + Slowness (25s cd) |
| Reaper's Mark | Right-click: mark nearest enemy +20% damage 30s, kill within 30s = full heal (45s cd) |
| Wither Guard | Every 30s: spawn 2 wither skeletons (12s) |
| Blood Moon | Right-click: allies within 15 blocks get Speed I + 20% lifesteal 8s (90s cd) |
| Wraith Walk | Right-click: 4s ghost form (invulnerable, can't attack), on expiry AoE Blindness + Slow (35s cd) |
| Curse of Decay | Attacks curse enemies: 50% reduced healing for 6s (refreshes) |

---

## Tips

### For Humans
- Stick together! Many perks benefit from having allies nearby (Buff Buddies, Guardian's Oath, Life Link).
- Golden Apples are your best healing - use them wisely.
- Save gold for Gold/Prismatic perks at the free perk milestones, or buy cheap Silver perks early for an advantage.
- Watch the day/night cycle - vampires are faster at night.

### For Vampires
- Coordinate attacks with your team. Pack Hunter rewards group plays.
- Use Vampire Leap aggressively to close gaps on humans.
- At night you gain Speed I - use it to chase down isolated humans.
- The Blood Compass (given at 10 minutes remaining) helps you find hiding humans.
- Dying isn't permanent for vampires - play aggressively!

### General
- You get free perks at 5, 10, and 15 minutes - don't forget to choose!
- The Perk Shop emerald in your hotbar opens the shop with right-click.
- Perks with cooldowns benefit from the Haste perk (30% cooldown reduction).
- Maximum 4 perks at a time - choose wisely!
