# VampireZ

![VampireZ Map](VampireZ_Final_Render.png)

A Minecraft minigame plugin where **Humans survive against Vampires** for 25 minutes. Humans who die are converted into Vampires. Vampires win by converting all humans; humans win by surviving the timer.

Built for **Spigot/Paper 1.20.1** with Java 17.

## Features

- **Two teams** - Humans (iron gear, bows) vs Vampires (leather gear, leap ability)
- **139 unique perks** across 3 tiers (Silver, Gold, Prismatic)
- **Gold economy** - earn gold passively, from kills, and assists to buy perks
- **Day/Night cycle** - vampires get Speed at night, Slowness during the day
- **Perk shop GUI** - browse tiers, pick from 3 random options
- **Free perks on timers** - Silver at 5 min, Gold at 10 min, Prismatic at 15 min
- **Human-to-Vampire conversion** - dead humans join the vampire team with new perk choices
- **Custom combat system** - balanced damage formula with armor reduction and damage cap
- **Multi-server safe** - works alongside survival worlds with full inventory isolation
- **Clickable event announcements** - admins broadcast a join button to the whole server
- **Crash-safe inventory saving** - player inventories saved to disk before entering the game

## Installation

### Quick Install (Pre-built)

1. Download `VampireZ-1.0.0.jar` from the [Releases](../../releases) page
2. Drop it into your server's `plugins/` folder
3. Start or restart your server
4. (Optional) Download `VampireZ-Map.zip` from Releases and extract the `world` folder into your server directory

### Build from Source

Requires **Java 17** and **Maven**.

```bash
mvn clean package
```

The built JAR will be at `target/VampireZ-1.0.0.jar`. Copy it to your server's `plugins/` folder.

## Setup

### First-Time Setup (one time only)

1. Use `/vz arena` to teleport to the arena world
2. Walk to the lobby area and run `/vz setlobby`
3. Walk to the human spawn point and run `/vz sethumanspawn`
4. Walk to the vampire spawn point and run `/vz setvampspawn`

Spawn locations are saved to `config.yml` and persist across server restarts.

### Running an Event

1. `/vz announce` - broadcasts a clickable join message to all players on the server
2. Players click the message or type `/vz join` to enter the lobby
3. `/vz start` to begin the game (or `/vz forcestart` to bypass the minimum player count)

When the game ends or a player types `/vz leave`, their full inventory, location, XP, health, and game mode are restored exactly as they were before joining.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/vz join` | -- | Join the VampireZ lobby |
| `/vz leave` | -- | Leave and restore your inventory |
| `/vz shop` | -- | Open perk shop |
| `/vz perks` | -- | List your active perks |
| `/vz gold` | -- | Show gold balance |
| `/vz status` | -- | Game state and team counts |
| `/vz announce` | `vampirez.admin` | Broadcast clickable join message to server |
| `/vz start` | `vampirez.admin` | Start game |
| `/vz forcestart` | `vampirez.admin` | Start ignoring player count |
| `/vz stop` | `vampirez.admin` | Stop running game |
| `/vz setlobby` | `vampirez.admin` | Set lobby spawn |
| `/vz sethumanspawn` | `vampirez.admin` | Set human spawn |
| `/vz setvampspawn` | `vampirez.admin` | Set vampire spawn |
| `/vz arena` | `vampirez.admin` | Teleport to arena world |
| `/vz test` | `vampirez.admin` | Open perk test menu |
| `/vz reload` | `vampirez.admin` | Reload config |

## Game Flow

1. **Lobby** - Admin runs `/vz announce`, players click to join or type `/vz join`
2. **Starting** (15s countdown) - Teams assigned (30% vampires), teleported to spawns
3. **Active** (25 minutes) - Fight! Humans earn gold, buy perks, survive. Dead humans become vampires
4. **Win** - Humans survive the timer, or Vampires convert everyone
5. **End** - All players restored to their previous inventory, location, and game mode

### Disconnect Handling

- If a human disconnects during a game, they are automatically converted to a vampire
- If they reconnect while the game is still running, they rejoin as a vampire with auto-assigned perks
- If the game ends before they reconnect, their original inventory is restored on next login

## Perks (139 total)

**Team Key:** 🔵 Human only | 🔴 Vampire only | ⚪ Both teams

### Silver Tier (50 perks) - Cost: 50g

| Team | Perk | Description |
|:----:|------|-------------|
| ⚪ | Blunt Force | +20% melee damage |
| ⚪ | Deft | Permanent Speed I |
| ⚪ | Heavy Hitter | +4% of max HP as bonus damage per hit |
| ⚪ | Goredrink | 15% lifesteal on damage dealt |
| ⚪ | Escape Plan (Weak) | Below 4 hearts: Speed I + Absorption I for 3s (30s cd) |
| ⚪ | Vitality | +2 max hearts |
| ⚪ | Tough Skin | -10% damage taken |
| ⚪ | Swift Strikes | +15% attack speed |
| ⚪ | Dash | Right-click Feather to dash forward (5s cd) |
| ⚪ | Lightweight | Jump Boost I + no fall damage |
| ⚪ | Second Wind | Regen I when below 50% HP |
| ⚪ | Adrenaline Rush | Taking damage grants Speed I for 3s |
| ⚪ | Riposte | After being hit, next attack within 3s deals +40% damage |
| ⚪ | Flame Arrows | Bow gets Flame I |
| ⚪ | Momentum | Consecutive hits within 3s stack +8% damage (max +24%) |
| ⚪ | Gravity Well | Melee hits pull target 1 block toward you (3s cd) |
| ⚪ | Supply Drop | Every 90s: chest with 2 golden apples + 8 arrows (15s) |
| ⚪ | Scaling | Stats scale up as game progresses |
| ⚪ | Black Shield | Block next perk ability used against you |
| ⚪ | Trail | Sprinting leaves a trail: allies get Speed I, enemies get Slowness I |
| ⚪ | Combo Shield | Every 4th hit taken is negated |
| ⚪ | Haste | Permanent Haste I |
| ⚪ | Sunder | Attacks reduce victim's armor for 5s |
| ⚪ | Regenerative | Permanent slow health regen |
| ⚪ | Headhunter | Bonus damage to targets above 80% HP |
| 🔵 | First-Aid Kit | +30% healing received |
| 🔵 | Buff Buddies | Allies within 10 blocks get Resistance I |
| 🔵 | Steady Aim | +20% projectile damage |
| 🔵 | Iron Wall | Gives Shield item to block attacks |
| 🔵 | Thorns | Reflect 10% melee damage to attacker |
| 🔵 | Archer's Quiver | Infinity + Power I bow |
| 🔵 | Deflect | 30% chance to negate projectile damage |
| 🔵 | Fortify | Crouching grants Resistance I |
| 🔵 | Guardian's Oath | -15% damage taken when ally within 5 blocks |
| 🔵 | Wolf Pack | On vampire kill: summon 2 wolves (8s) |
| 🔵 | Sharpness Boost | Sword upgraded to Sharpness II |
| 🔵 | Protection Boost | All armor upgraded to Protection II |
| 🔵 | Arrow Supply | +16 extra arrows (40 total) |
| 🔵 | Healing Potions | 3 Instant Health potions, regen 1 every 2 min |
| 🔵 | Fire Aspect | Sword gets Fire Aspect I |
| 🔵 | Speed Potions | 3 Speed potions, regen 1 every 2 min |
| 🔵 | Rally Cry | On kill: nearby allies get Speed I + Regen I for 4s |
| 🔵 | Natural Leader | Glow effect, bonus gold and regen to nearby humans |
| 🔵 | Cooker | Every 60s, feed nearby humans +2 saturation |
| 🔵 | Medic | 3 healing snowballs, regen 1 every 15s |
| 🔵 | Heavyweight | Double max HP |
| 🔵 | Porcupine | Attackers take thorns damage and knockback |
| 🔵 | War Drums | Nearby allies deal +10% damage |
| 🔵 | Fortune Teller | Reveal the next free perk tier on scoreboard |
| 🔴 | Homeguard | Speed III for 5s on respawn |
| 🔴 | Dive Bomber | On death: explode 4 hearts to nearby enemies |
| 🔴 | Scavenger | Kills drop a Golden Apple |
| 🔴 | Backstab | +30% damage when hitting from behind |
| 🔴 | Bloodlust | Each kill grants +1 max heart (up to +3) |
| 🔴 | Poison Fang | Attacks apply Poison I for 3s |
| 🔴 | Pack Hunter | +10% damage per ally near target (max +30%) |
| 🔴 | Bone Armor | First hit every 12s reduced by 50% |
| 🔴 | Leech Swarm | On death: spawn 3 silverfish (5s) |
| 🔴 | Undead Horde | Every 30s: spawn 2 zombies (10s) |
| 🔴 | Blood Scent | Enemies below 50% HP glow through walls |
| 🔴 | Feral Charge | Sprinting attacks deal +30% damage (6s cd) |
| 🔴 | Infectious Bite | On kill: heal nearby vampires for 4 hearts |

### Gold Tier (46 perks) - Cost: 150g

| Team | Perk | Description |
|:----:|------|-------------|
| ⚪ | Celestial Body | +4 max hearts, -10% damage dealt |
| ⚪ | Executioner | +30% damage to targets below 40% HP |
| ⚪ | Get Excited | On kill: Speed II + Strength I for 6s |
| ⚪ | It's Critical | 30% chance for 1.5x damage |
| ⚪ | Berserker | Below 30% HP: Strength I + Speed I |
| ⚪ | Smoke Bomb | Right-click: Blindness to nearby enemies for 5s (20s cd) |
| ⚪ | Ender Pearl Supply | 3 Ender Pearls, regen 1 every 30s |
| ⚪ | Iron Rations | +8 cooked beef + 2 golden apples |
| ⚪ | Last Stand | Below 25% HP: immune to knockback + 25% more damage |
| ⚪ | Siphon | Melee hit steals 1 heart (8s cd per target) |
| ⚪ | Heartsteal | Nearby hit for 5s+: permanent +0.5 max hearts (60s cd) |
| ⚪ | Black Cleaver | Each hit shreds 5% armor (max 25%, 10s duration) |
| ⚪ | Bounty Hunter | Kills grant +15 bonus gold |
| ⚪ | Whirlwind | Right-click: 3 hearts AoE to nearby enemies (12s cd) |
| ⚪ | Life Link | Ally within 15 blocks: both get +2 bonus hearts |
| ⚪ | Selfish | Increased gold from kills, no assist gold to allies |
| ⚪ | Gore Drinker | Lifesteal scales with missing HP |
| ⚪ | War Horse | Permanent speed boost + mount-style charge damage |
| 🔵 | Dawnbringer's Resolve | Auto-regen 1 heart/2s when below 4 hearts |
| 🔵 | All For You | Splash healing potions heal 50% more |
| 🔵 | Armored Up | Armor upgraded to Diamond |
| 🔵 | Phoenix Down | One-time auto-revive at half HP |
| 🔵 | Crossbow Expert | Crossbow with Quick Charge II + Piercing |
| 🔵 | Mirror Shield | 20% chance negate damage + reflect to attacker |
| 🔵 | Golden Guard | Fatal damage: spend 30g to survive at 2 hearts |
| 🔵 | Barricade | Right-click: 3-wide glass wall for 5s (25s cd) |
| 🔵 | Power Shot | Bow gets Power I |
| 🔵 | Blast Shield | Armor gets Blast Protection II |
| 🔵 | Strength Potions | 3 Strength potions, regen 1 every 2 min |
| 🔵 | Golden Feast | +3 extra Golden Apples (6 total) |
| 🔵 | Knockback | Sword gets Knockback I |
| 🔵 | Chain Armor | Leather armor upgraded to Chainmail |
| 🔵 | Poison Quiver | 8 Poison arrows, regen 1 every 10s |
| 🔵 | Consecrated Ground | Right-click: holy zone heals allies/damages enemies (40s cd) |
| 🔵 | Martyr | On death: nearby allies heal 4 hearts + Absorption II |
| 🔵 | Shield | Right-click sword: 3 absorption hearts for 4s (45s cd) |
| 🔵 | Sunfire Cape | Melee attackers get set on fire |
| 🔵 | Ricochet Shot | Arrows bounce to a second nearby target |
| 🔵 | Overcharge | Charged bow shots deal bonus damage |
| 🔵 | Always Connected | Reveal all humans + heal 1 heart each (45s cd) |
| 🔵 | Fight or be Forgotten | Fatal damage: 30s invulnerability then convert to vampire |
| 🔴 | Blunt Force II | +20% melee damage (stacks with Silver) |
| 🔴 | Shadow Strike | Teleport behind nearest enemy (15s cd) |
| 🔴 | Frost Bite | Attacks apply Slowness I + Frost Walker boots |
| 🔴 | Phantom Step | After taking damage: 2s invisibility + Speed I (15s cd) |
| 🔴 | Blood Price | Sacrifice 3 hearts, next hit deals 2x (20s cd) |
| 🔴 | Tether | Pull nearest enemy toward you (15s cd) |
| 🔴 | Skeleton Archers | Every 30s: spawn 2 skeleton archers (10s) |
| 🔴 | Harming Potions | 3 Splash Harming potions, regen 1 every 2 min |
| 🔴 | Hemophilia | Attacks inflict Bleeding: 0.5 hearts/sec for 4s |
| 🔴 | Nocturnal | Night: +2 hearts + 15% damage; Day: -1 heart |
| 🔴 | Corpse Explosion | On kill: victim explodes, 3 hearts AoE |
| 🔴 | Blood Beacon | Place healing beacon for nearby vampires (60s cd) |
| 🔴 | Shadow Ambush | 3s invisibility, next hit +50% damage (30s cd) |
| 🔴 | Spider Climb | Wall climbing ability |
| 🔴 | Plague Carrier | Spread poison to nearby enemies over time |

### Prismatic Tier (43 perks) - Cost: 400g

| Team | Perk | Description |
|:----:|------|-------------|
| ⚪ | Can't Touch This | Every 30s: 8s invulnerability |
| ⚪ | Glass Cannon | -30% max HP, +35% damage dealt |
| ⚪ | Goliath | +6 max hearts, +10% damage, Slowness I |
| ⚪ | Thunderstrike | Every 5th hit summons lightning |
| ⚪ | Earthquake | Right-click: 6-block AoE knockback + 3 hearts (30s cd) |
| ⚪ | Chain Lightning | Melee hits chain to nearest enemy for 50% damage |
| ⚪ | Death's Gambit | Fatal damage: 50/50 survive at 1 HP or take 50% more |
| ⚪ | Plague Doctor | Immune to all negative potion effects |
| ⚪ | Decoy | Spawn a decoy of yourself to confuse enemies |
| 🔵 | Courage of the Colossus | Hit a player to gain 2 absorption hearts (30s cd) |
| 🔵 | Escape Plan (Strong) | Below 4 hearts: Absorption III + Speed II for 5s (45s cd) |
| 🔵 | Giant Slayer | Speed I + 25% damage to targets with more max HP |
| 🔵 | Netherite Arsenal | Netherite Sword + Helmet + Chestplate |
| 🔵 | Guardian Angel | Auto-revive 50% HP + 5s invulnerability (3 min cd) |
| 🔵 | Trapper | 5 placeable cobwebs to slow enemies |
| 🔵 | Marksman | Power III + Punch I bow, 32 arrows |
| 🔵 | Citadel | 5-block zone: Resistance I + Regen I for allies (60s cd) |
| 🔵 | Holy Shield | Absorb 10 damage then auto-explode 4 hearts AoE |
| 🔵 | Temporal Shield | Freeze enemies within 5 blocks for 2s (45s cd) |
| 🔵 | Iron Guardian | On vampire kill: summon Iron Golem (12s) |
| 🔵 | Diamond Edge | Iron sword upgraded to Diamond (Sharpness I) |
| 🔵 | Thorns Enchant | All armor gets Thorns II + Unbreaking III |
| 🔵 | Regeneration Potions | 3 Regen potions, regen 1 every 2 min |
| 🔵 | Radiant Aura | Enemies within 6 blocks take 1 heart/3s + Glowing |
| 🔵 | Time Warp | Fatal damage: rewind to position + HP from 3s ago (90s cd) |
| 🔵 | Bard | Aura that buffs nearby allies with various effects |
| 🔵 | Plant Master | Summon vines and plants to slow and damage enemies |
| 🔵 | Dimensional Pocket | Store and retrieve items across lives |
| 🔴 | Erosion | Hits apply Weakness I for 3s |
| 🔴 | Final Form | Every 60s: Absorption IV + 25% lifesteal for 8s |
| 🔴 | Firebrand | Attacks set target on fire + 0.5 heart bonus damage |
| 🔴 | Double Tap | Crits deal 1.75x + Slowness I |
| 🔴 | Bat Form | Invisible + Speed III for 5s (25s cd) |
| 🔴 | Soul Eater | Each kill grants +10% permanent damage (max +30%) |
| 🔴 | Summoner | Summon 2 wolves to attack enemies (45s cd) |
| 🔴 | Void Walker | Teleport to random enemy + 3 heart AoE (25s cd) |
| 🔴 | Reaper's Mark | Mark enemy for +20% damage, kill within 30s = full heal (45s cd) |
| 🔴 | Wither Guard | Every 30s: spawn 2 wither skeletons (12s) |
| 🔴 | Blood Moon | Allies get Speed I + 20% lifesteal for 8s (90s cd) |
| 🔴 | Wraith Walk | 4s ghost form, on expiry AoE Blindness + Slow (35s cd) |
| 🔴 | Curse of Decay | Attacks curse enemies: 50% reduced healing for 6s |

## Configuration

All settings are in `plugins/VampireZ/config.yml` after first run. Key settings:

- `game.min-players` - Minimum players to start (default: 10)
- `game.game-duration-seconds` - Game length (default: 1500 = 25 min)
- `game.vampire-ratio` - Fraction that start as vampires (default: 0.3)
- `economy.kill-reward` / `economy.assist-reward` - Gold rewards
- `perks.max-perks-per-player` - Perk limit (default: 4)

## Multi-Server Compatibility

This plugin is designed to run safely on servers with existing survival worlds:

- **Inventory isolation** - Player inventories, XP, health, food, potion effects, and game mode are saved before joining and fully restored on leave or game end
- **Crash-safe** - Player states are written to disk, so a server crash mid-game won't lose inventories
- **Arena-only effects** - Day/night cycle, mob spawning, and game mechanics only affect the arena world
- **No leaking** - Perk effects, gold income, scoreboards, and broadcasts are scoped to VampireZ players only

## Requirements

- Spigot or Paper 1.20.1+
- Java 17+

## License

This project is open source. Feel free to use, modify, and learn from it.
