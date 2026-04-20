<p align="center">
  <img src="VampireZ.gif" alt="VampireZ" width="640" />
</p>

# VampireZ Wiki

Everything behind the curtain: game flow, combat math, economy, day/night, perk interactions, and the full 145-perk catalogue.

For install & usage, see the [README](README.md).

---

## Table of Contents

1. [Game Flow](#game-flow)
2. [Combat System](#combat-system)
3. [Economy](#economy)
4. [Day / Night Cycle](#day--night-cycle)
5. [Teams & Conversion](#teams--conversion)
6. [Gear Loadouts](#gear-loadouts)
7. [Perk System](#perk-system)
8. [Full Perk Catalogue](#full-perk-catalogue)
9. [Commands Reference](#commands-reference)
10. [Configuration Reference](#configuration-reference)
11. [Arena Map](#arena-map)

---

## Game Flow

The game lives in a single state machine: `LOBBY → STARTING → ACTIVE → ENDING → LOBBY`.

### LOBBY
- Players join via `/vz join` or clicking the broadcast from `/vz announce`.
- The lobby scoreboard shows current / minimum players.
- No combat, no perks, no gear — just holding area.

### STARTING (15 s countdown)
- Announcements at **10, 5, 4, 3, 2, 1 s**.
- Teams assigned: `vampireCount = max(1, round(players × 0.3))`.
- Players teleported to human / vampire spawns and given starting gear.
- A free **Silver** perk selection opens 1 s after the game starts (20 ticks).

### ACTIVE (25 minutes / 1500 s)
| Timer | Event |
|-------|-------|
| every 10 s | +2 gold passive income to every player |
| 5:00 elapsed | Free perk pick — **random tier** (roll animation + 3 perk choices) |
| 10:00 elapsed | Free perk pick — **random tier** |
| 15:00 elapsed | Free perk pick — **random tier** |
| 5:00 / 1:00 / 0:30 remaining | Milestone broadcast |
| last 5 s | Final countdown |

The day/night cycle also runs during this phase — see [below](#day--night-cycle).

### Win Conditions
- **Humans win** when the 25-min timer reaches 0.
- **Vampires win** when the human team is empty (everyone converted).

### ENDING
- Win broadcast, fireworks, 10 s wind-down.
- Every player’s **original inventory, location, XP, health, food, potion effects, and game mode** are restored — see [Teams & Conversion](#teams--conversion) for crash-safety details.

---

## Combat System

VampireZ replaces vanilla PvP damage with a custom formula to keep fights tactical and stop perks from stacking into one-shot combos.

A sword or axe in the main hand is required for the custom formula to trigger — bare-fist hits fall back to vanilla damage (usually 1 HP).

### Step 1 — Base damage

$$
\text{base} = \begin{cases} 5.0 \text{ HP} & \text{attacker is Human} \\ 4.0 \text{ HP} & \text{attacker is Vampire} \end{cases}
$$

### Step 2 — Weapon material bonus

Added to the base before any scaling.

$$
\text{material bonus} = \begin{cases}
+0.0 & \text{Wood, Stone, Gold, Iron} \\
+0.5 & \text{Diamond} \\
+1.0 & \text{Netherite}
\end{cases}
$$

### Step 3 — Sharpness enchantment

$$
\text{sharpness bonus} = 0.5 \times \text{sharpness level}
$$

### Step 4 — Attack-cooldown scaling

The attack cooldown is a value from 0 (just-swung) to 1 (fully recharged).

$$
\text{raw damage} = (\text{base} + \text{material bonus} + \text{sharpness bonus}) \times \text{cooldown}
$$

Spamming clicks sends cooldown toward 0, so raw damage shrinks with it — a fully recharged hit is worth ten spammed hits.

### Step 5 — Armor reduction (at 30 % strength)

Vanilla Minecraft reduces damage aggressively through armor. VampireZ keeps only **30 %** of that effect so iron armor stays meaningful without trivialising attacks.

$$
\text{armor reduction} = \min\left(\frac{\text{armor points}}{25},\ 0.80\right)
$$

$$
\text{after armor} = \text{raw damage} \times (1 - \text{armor reduction} \times 0.30)
$$

> *Example:* Full Iron armor = 20 armor points. Vanilla would reduce damage by 80 %. Here we keep 30 % of that → **24 % reduction**.

### Step 6 — Protection enchantment

Each level of Protection on any armor piece reduces damage by 4 %, capped at 80 %.

$$
\text{protection reduction} = \min\left(0.04 \times \sum \text{Protection levels},\ 0.80\right)
$$

$$
\text{final base} = \text{after armor} \times (1 - \text{protection reduction})
$$

> *Example:* Full Iron with Protection I on every piece = `4 × 4 % = 16 %` further reduction.

### Step 7 — Perk modifiers (in order)

Perks layer on top of the base damage. The order matters because some are multiplicative and others additive:

$$
\begin{aligned}
\text{d}_1 &= \text{final base} &\text{(attacker's own perks adjust first)} \\
\text{d}_2 &= \text{d}_1 &\text{(then victim's defensive perks)} \\
\text{d}_3 &= \text{d}_2 \times 1.10 &\text{if attacker has War Drums aura} \\
\text{d}_4 &= \text{d}_3 + 2.0 &\text{if attacker has Bard damage aura} \\
\text{d}_5 &= \text{d}_4 \times (1 + 0.05 \cdot \text{cleaver stacks}) &\text{up to } \times 1.25 \\
\text{d}_6 &= \text{d}_5 \times \text{anvil multiplier} &\text{if any}
\end{aligned}
$$

### Step 8 — Damage cap (7 HP)

After every modifier, damage is clamped so no combo can one-shot:

$$
\text{damage} = \min(\text{d}_6,\ 7.0 \text{ HP})
$$

### Step 9 — Special weapons

If the attacker's sword has a special post-processing buff (Nether Blade at max tier), that multiplier is applied *after* the cap, then the cap is re-enforced:

$$
\text{damage} = \min(\text{damage} \times \text{special mult},\ 7.0 \text{ HP})
$$

### Other combat rules
- **Friendly fire** is cancelled before damage is calculated.
- **Fall damage** is negated entirely if any owned perk grants fall immunity.
- **Hidden armor** (Phantom Step, Bat Form) still counts — invisible vampires don't become glass.
- **Curse of Decay** reduces all healing the cursed player receives by 50 %.

---

## Economy

| Source | Amount | Cadence |
|--------|-------:|---------|
| Passive income | **+2 g** | Every 10 s (200 ticks) |
| Kill | **+15 g** | On kill — killer only |
| Assist | **+5 g** | Per assister within 10 s damage window |

### Perk costs
| Tier | Cost |
|------|-----:|
| Silver | 50 g |
| Gold | 150 g |
| Prismatic | 400 g |

### Armor repair
25 g via the shop GUI — restores durability on all armor pieces.

### Assist tracking
Every time a player takes damage, the attacker is remembered for a rolling **10-second window**. When that player dies, everyone who hit them in the window — except the final killer — earns the assist reward.

---

## Day / Night Cycle

| Phase | Duration | World time | Vampire effect |
|-------|---------:|-----------:|----------------|
| Day | 7200 ticks (6 min) | 1000 | **Slowness I** |
| Night | 4800 ticks (4 min) | 13000 | **Speed I** |

- The cycle starts on **Day** the moment the game begins.
- Transitions broadcast a message + sound to the whole arena.
- Effects are re-applied on respawn, conversion, and phase change so they never drop.

---

## Teams & Conversion

### Starting ratio

$$
\text{vampire count} = \max(1,\ \text{round}(\text{players} \times 0.30))
$$

10 players → 3 vampires, 7 humans. Picks are randomised each game.

### Human death → Vampire conversion
1. On death: drops cleared, death message suppressed.
2. Kill rewards distributed (+15 g to the killer, +5 g to each assister).
3. The player moves from the human team to the vampire team.
4. **All Human-only perks are removed** from their loadout.
5. After a 2-second delay: vampire gear given, remaining perks re-applied, day/night effect re-applied.
6. For every perk that got stripped, a free **Vampire perk selection** GUI opens so they can pick a replacement.

### Vampire respawn
1. Death plays the "vampire cackle" sound.
2. After 80 ticks (4 s): teleport to vampire spawn, restore gear, re-apply perks and day/night effect.

### Disconnect handling
- If a human disconnects mid-game they're auto-converted to vampire — no advantage from rage-quitting.
- A reconnecting vampire is restored with gear and has missing perk slots auto-assigned.
- Inventories are written to disk before join (**crash-safe**) and restored on leave / game end / next login.

### Multi-server isolation
- Perk effects, gold income, scoreboards, and broadcasts are scoped to players **inside** the VampireZ instance.
- Day/night, block protection, and mob targeting rules only apply in the arena world.
- Player inventory / XP / health / hunger / potion effects / game mode are saved and restored per-join.

---

## Gear Loadouts

### Human
| Slot | Item | Enchants |
|:---:|------|----------|
| 0 | Iron Sword | Sharpness I |
| 1 | Bow | — |
| 2 | Golden Apple × 3 | — |
| 3 | Cooked Beef × 12 | — |
| 4 | Arrow × 24 | — |
| 8 | Emerald "Perk Shop" | right-click to open |
| Helmet | Iron Helmet | Protection I |
| Chest | Iron Chestplate | Protection I |
| Legs | Iron Leggings | Protection I |
| Boots | Iron Boots | Protection I |

### Vampire
| Slot | Item | Notes |
|:---:|------|-------|
| 0 | Stone Sword | — |
| 1 | Rotten Flesh × 8 | — |
| 2 | Ghast Tear "Vampire Leap" | right-click, 8 s cooldown |
| 8 | Emerald "Perk Shop" | right-click to open |
| Helmet | Custom "Vampire" Player Head | unbreakable, skin texture `16b76d73…c5cf5c` |
| Chest / Legs / Boots | Black-dyed Leather | unbreakable |
| Effect | Night Vision | permanent (999999 ticks) |

---

## Perk System

### Tiers & Teams
- **Tiers:** Silver (50 g), Gold (150 g), Prismatic (400 g).
- **Teams:** Human-only, Vampire-only, or Both — the perk pool you see is filtered by your current team.
- **Max per player:** 4 slots by default.

### Selection flow
When you earn a pick (free or purchased), the system rolls **3 random perks** from the chosen tier — filtered to your team and excluding anything you already own. Clicking one:
1. Deducts the gold cost (if you bought it).
2. Grants the perk immediately — potion effects, items, enchantments, and everything else are applied on the spot.
3. Slots it into your active perk list for the rest of the game.

### Free perk timers
Everyone automatically gets a free **roll** at set times. The only guaranteed tier is the one at game start — every later roll **rolls its tier randomly too**, shown with a slot-machine animation before the GUI opens.

| Time | What you get |
|------|--------------|
| Game start | Silver (guaranteed) + 3 random Silver perks to pick from |
| 5:00 elapsed | **Random tier** (Silver / Gold / Prismatic) + 3 random perks of that tier |
| 10:00 elapsed | **Random tier** + 3 random perks of that tier |
| 15:00 elapsed | **Random tier** + 3 random perks of that tier |

So you might luck into a Prismatic at the 5-minute mark, or roll three Silvers back-to-back — it's all up to the dice.

If a player closes the selection GUI, it re-opens up to 3 times; after that a random qualifying perk from that rolled tier is auto-assigned so slots are never wasted.

### Admin perk test menu
`/vz test` opens a paginated browser of every perk — click to toggle on yourself, with a "Clear All" button. Only works in the lobby and for players with `vampirez.admin`.

---

## Full Perk Catalogue

**Team key:** 🔵 Human only · 🔴 Vampire only · ⚪ Both teams


### Silver Tier (50 perks · 50 g)


| Icon | Team | Perk | Effect |
|:----:|:----:|------|--------|
| <img src="images/perks/iron_ingot.png" width="24"> | ⚪ | Blunt Force | +20 % melee damage |
| <img src="images/perks/feather.png" width="24"> | ⚪ | Deft | Permanent Speed I |
| <img src="images/perks/anvil.png" width="24"> | ⚪ | Heavy Hitter | +4 % of max HP as bonus damage per hit |
| <img src="images/perks/redstone.png" width="24"> | ⚪ | Goredrink | 15 % lifesteal on damage dealt |
| <img src="images/perks/leather_boots.png" width="24"> | ⚪ | Escape Plan (Weak) | Below 4 ♥: Speed I + Absorption I for 3 s (30 s cd) |
| <img src="images/perks/apple.png" width="24"> | ⚪ | Vitality | +2 max hearts |
| <img src="images/perks/iron_ingot.png" width="24"> | ⚪ | Tough Skin | −10 % damage taken |
| <img src="images/perks/gold_ingot.png" width="24"> | ⚪ | Swift Strikes | +15 % attack speed |
| <img src="images/perks/feather.png" width="24"> | ⚪ | Dash | Right-click Feather to dash forward (5 s cd) |
| <img src="images/perks/feather.png" width="24"> | ⚪ | Lightweight | Jump Boost I + no fall damage |
| <img src="images/perks/ghast_tear.png" width="24"> | ⚪ | Second Wind | Regen I when below 50 % HP |
| <img src="images/perks/sugar.png" width="24"> | ⚪ | Adrenaline Rush | Taking damage grants Speed I for 3 s |
| <img src="images/perks/iron_sword.png" width="24"> | ⚪ | Riposte | After being hit, next attack within 3 s deals +40 % |
| <img src="images/perks/spectral_arrow.png" width="24"> | ⚪ | Flame Arrows | Bow gets Flame I |
| <img src="images/perks/blaze_powder.png" width="24"> | ⚪ | Momentum | Consecutive hits within 3 s stack +8 % dmg (max +24 %) |
| <img src="images/perks/ender_eye.png" width="24"> | ⚪ | Gravity Well | Melee hits pull target 1 block toward you (3 s cd) |
| <img src="images/perks/chest.png" width="24"> | ⚪ | Supply Drop | Every 90 s: chest with 2 golden apples + 8 arrows (15 s) |
| <img src="images/perks/experience_bottle.png" width="24"> | ⚪ | Scaling | Stats scale up as game progresses |
| <img src="images/perks/netherite_scrap.png" width="24"> | ⚪ | Black Shield | Block next perk ability used against you |
| <img src="images/perks/soul_torch.png" width="24"> | ⚪ | Trail | Sprinting trail: allies Speed I, enemies Slowness I |
| <img src="images/perks/shield.png" width="24"> | ⚪ | Combo Shield | Every 4th hit taken is negated |
| <img src="images/perks/sugar.png" width="24"> | ⚪ | Haste | Permanent Haste I |
| <img src="images/perks/iron_axe.png" width="24"> | ⚪ | Sunder | Attacks reduce victim's armor for 5 s |
| <img src="images/perks/ghast_tear.png" width="24"> | ⚪ | Regenerative | Permanent slow health regen |
| <img src="images/perks/skeleton_skull.png" width="24"> | ⚪ | Headhunter | Bonus damage to targets above 80 % HP |
| <img src="images/perks/sunflower.png" width="24"> | ⚪ | Lucky Roll (Silver) | Replaces itself with a random Gold perk |
| <img src="images/perks/golden_apple.png" width="24"> | 🔵 | First-Aid Kit | +30 % healing received |
| <img src="images/perks/beacon.png" width="24"> | 🔵 | Buff Buddies | Allies within 10 blocks get Resistance I |
| <img src="images/perks/target.png" width="24"> | 🔵 | Steady Aim | +20 % projectile damage |
| <img src="images/perks/shield.png" width="24"> | 🔵 | Iron Wall | Gives Shield item to block attacks |
| <img src="images/perks/cactus.png" width="24"> | 🔵 | Thorns | Reflect 10 % melee damage to attacker |
| <img src="images/perks/bow.png" width="24"> | 🔵 | Archer's Quiver | Infinity + Power I bow |
| <img src="images/perks/shield.png" width="24"> | 🔵 | Deflect | 30 % chance to negate projectile damage |
| <img src="images/perks/iron_chestplate.png" width="24"> | 🔵 | Fortify | Crouching grants Resistance I |
| <img src="images/perks/iron_boots.png" width="24"> | 🔵 | Guardian's Oath | −15 % damage taken when ally within 5 blocks |
| <img src="images/perks/bone.png" width="24"> | 🔵 | Wolf Pack | On vampire kill: summon 2 wolves (8 s) |
| <img src="images/perks/iron_sword.png" width="24"> | 🔵 | Sharpness Boost | Sword upgraded to Sharpness II |
| <img src="images/perks/iron_chestplate.png" width="24"> | 🔵 | Protection Boost | All armor upgraded to Protection II |
| <img src="images/perks/tipped_arrow.png" width="24"> | 🔵 | Arrow Supply | +16 extra arrows (40 total) |
| <img src="images/perks/splash_potion.png" width="24"> | 🔵 | Healing Potions | 3 Instant Health potions, regen 1 every 2 min |
| <img src="images/perks/blaze_powder.png" width="24"> | 🔵 | Fire Aspect | Sword gets Fire Aspect I |
| <img src="images/perks/splash_potion.png" width="24"> | 🔵 | Speed Potions | 3 Speed potions, regen 1 every 2 min |
| <img src="images/perks/goat_horn.png" width="24"> | 🔵 | Rally Cry | On kill: nearby allies get Speed I + Regen I for 4 s |
| <img src="images/perks/torch.png" width="24"> | 🔵 | Natural Leader | Glow effect, +1 g / 3 s per nearby human, 1 %/s Regen |
| <img src="images/perks/campfire.png" width="24"> | 🔵 | Cooker | Every 60 s, feed nearby humans +2 saturation |
| <img src="images/perks/snowball.png" width="24"> | 🔵 | Medic | 3 healing snowballs, regen 1 every 15 s |
| <img src="images/perks/iron_block.png" width="24"> | 🔵 | Heavyweight | Double max HP |
| <img src="images/perks/cactus.png" width="24"> | 🔵 | Porcupine | Attackers take thorns damage and knockback |
| <img src="images/perks/note_block.png" width="24"> | 🔵 | War Drums | Nearby allies deal +10 % damage |
| <img src="images/perks/ender_eye.png" width="24"> | 🔵 | Fortune Teller | Reveal the next free perk tier on scoreboard |
| <img src="images/perks/golden_boots.png" width="24"> | 🔴 | Homeguard | Speed III for 5 s on respawn |
| <img src="images/perks/creeper_head.png" width="24"> | 🔴 | Dive Bomber | On death: explode 4 ♥ to enemies within 4 blocks |
| <img src="images/perks/golden_apple.png" width="24"> | 🔴 | Scavenger | Kills drop a Golden Apple |
| <img src="images/perks/iron_sword.png" width="24"> | 🔴 | Backstab | +30 % damage when hitting from behind |
| <img src="images/perks/redstone.png" width="24"> | 🔴 | Bloodlust | Each kill grants +1 max ♥ (up to +3) |
| <img src="images/perks/spider_eye.png" width="24"> | 🔴 | Poison Fang | Attacks apply Poison I for 3 s |
| <img src="images/perks/wolf_spawn_egg.png" width="24"> | 🔴 | Pack Hunter | +10 % dmg per ally within 6 blocks of target (max +30 %) |
| <img src="images/perks/bone_block.png" width="24"> | 🔴 | Bone Armor | First hit every 12 s reduced by 50 % |
| <img src="images/perks/fermented_spider_eye.png" width="24"> | 🔴 | Leech Swarm | On death: spawn 3 silverfish (5 s) |
| <img src="images/perks/zombie_head.png" width="24"> | 🔴 | Undead Horde | Every 30 s: spawn 2 zombies (10 s) |
| <img src="images/perks/spider_eye.png" width="24"> | 🔴 | Blood Scent | Enemies below 50 % HP glow through walls |
| <img src="images/perks/rabbit_foot.png" width="24"> | 🔴 | Feral Charge | Sprinting attacks deal +30 % damage (6 s cd) |
| <img src="images/perks/spider_eye.png" width="24"> | 🔴 | Infectious Bite | On kill: heal nearby vampires for 4 ♥ |

### Gold Tier (49 perks · 150 g)


| Icon | Team | Perk | Effect |
|:----:|:----:|------|--------|
| <img src="images/perks/golden_chestplate.png" width="24"> | ⚪ | Celestial Body | +4 max ♥, −10 % damage dealt |
| <img src="images/perks/diamond_axe.png" width="24"> | ⚪ | Executioner | +30 % damage to targets below 40 % HP |
| <img src="images/perks/sugar.png" width="24"> | ⚪ | Get Excited | On kill: Speed II + Strength I for 6 s |
| <img src="images/perks/diamond_sword.png" width="24"> | ⚪ | It's Critical | 30 % chance for 1.5× damage |
| <img src="images/perks/blaze_powder.png" width="24"> | ⚪ | Berserker | Below 30 % HP: Strength I + Speed I |
| <img src="images/perks/gunpowder.png" width="24"> | ⚪ | Smoke Bomb | Right-click: Blindness AoE for 5 s (20 s cd) |
| <img src="images/perks/ender_pearl.png" width="24"> | ⚪ | Ender Pearl Supply | 3 Ender Pearls, regen 1 every 30 s |
| <img src="images/perks/cooked_beef.png" width="24"> | ⚪ | Iron Rations | +8 cooked beef + 2 golden apples |
| <img src="images/perks/shield.png" width="24"> | ⚪ | Last Stand | Below 25 % HP: knockback-immune + 25 % more dmg |
| <img src="images/perks/ghast_tear.png" width="24"> | ⚪ | Siphon | Melee hit steals 1 ♥ (8 s cd per target) |
| <img src="images/perks/golden_apple.png" width="24"> | ⚪ | Heartsteal | Nearby hit for 5 s+: permanent +0.5 max ♥ (60 s cd) |
| <img src="images/perks/iron_axe.png" width="24"> | ⚪ | Black Cleaver | Each hit shreds 5 % armor (max 25 %, 10 s) |
| <img src="images/perks/gold_ingot.png" width="24"> | ⚪ | Bounty Hunter | Kills grant +15 bonus gold |
| <img src="images/perks/blaze_rod.png" width="24"> | ⚪ | Whirlwind | Right-click: 3 ♥ AoE (12 s cd) |
| <img src="images/perks/lead.png" width="24"> | ⚪ | Life Link | Ally within 15 blocks: both get +2 bonus ♥ |
| <img src="images/perks/golden_apple.png" width="24"> | ⚪ | Selfish | +gold from kills, no assist gold to allies |
| <img src="images/perks/netherite_sword.png" width="24"> | ⚪ | Gore Drinker | Lifesteal scales with missing HP |
| <img src="images/perks/saddle.png" width="24"> | ⚪ | War Horse | Permanent speed + mount-style charge damage |
| <img src="images/perks/sunflower.png" width="24"> | ⚪ | Lucky Roll (Gold) | Replaces itself with a random Prismatic perk |
| <img src="images/perks/wooden_sword.png" width="24"> | ⚪ | Nether Blade | 5-tier upgradable wooden sword; max tier = healing dash |
| <img src="images/perks/sunflower.png" width="24"> | 🔵 | Dawnbringer's Resolve | Auto-regen 1 ♥ / 2 s when below 4 ♥ |
| <img src="images/perks/splash_potion.png" width="24"> | 🔵 | All For You | Splash healing potions heal 50 % more |
| <img src="images/perks/diamond_chestplate.png" width="24"> | 🔵 | Armored Up | Armor upgraded to Diamond |
| <img src="images/perks/blaze_powder.png" width="24"> | 🔵 | Phoenix Down | One-time auto-revive at half HP |
| <img src="images/perks/crossbow.png" width="24"> | 🔵 | Crossbow Expert | Crossbow with Quick Charge II + Piercing |
| <img src="images/perks/shield.png" width="24"> | 🔵 | Mirror Shield | 20 % chance negate + reflect damage |
| <img src="images/perks/gold_block.png" width="24"> | 🔵 | Golden Guard | Fatal damage: spend 30 g to survive at 2 ♥ |
| <img src="images/perks/glass.png" width="24"> | 🔵 | Barricade | Right-click: 3-wide glass wall for 5 s (25 s cd) |
| <img src="images/perks/bow.png" width="24"> | 🔵 | Power Shot | Bow gets Power I |
| <img src="images/perks/tnt.png" width="24"> | 🔵 | Blast Shield | Armor gets Blast Protection II |
| <img src="images/perks/splash_potion.png" width="24"> | 🔵 | Strength Potions | 3 Strength potions, regen 1 every 2 min |
| <img src="images/perks/golden_apple.png" width="24"> | 🔵 | Golden Feast | +3 extra Golden Apples (6 total) |
| <img src="images/perks/piston.png" width="24"> | 🔵 | Knockback | Sword gets Knockback I |
| <img src="images/perks/chainmail_chestplate.png" width="24"> | 🔵 | Chain Armor | Leather armor → Chainmail |
| <img src="images/perks/tipped_arrow.png" width="24"> | 🔵 | Poison Quiver | 8 Poison arrows, regen 1 every 10 s |
| <img src="images/perks/golden_shovel.png" width="24"> | 🔵 | Consecrated Ground | Right-click: holy zone heals / damages (40 s cd) |
| <img src="images/perks/totem_of_undying.png" width="24"> | 🔵 | Martyr | On death: nearby allies heal 4 ♥ + Absorption II |
| <img src="images/perks/shield.png" width="24"> | 🔵 | Shield | Right-click sword: 3 absorption ♥ for 4 s (45 s cd) |
| <img src="images/perks/fire_charge.png" width="24"> | 🔵 | Sunfire Cape | Melee attackers get set on fire |
| <img src="images/perks/spectral_arrow.png" width="24"> | 🔵 | Ricochet Shot | Arrows bounce to a second nearby target |
| <img src="images/perks/tnt.png" width="24"> | 🔵 | Overcharge | Charged bow shots deal bonus damage |
| <img src="images/perks/recovery_compass.png" width="24"> | 🔵 | Always Connected | Reveal all humans + heal 1 ♥ each (45 s cd) |
| <img src="images/perks/golden_sword.png" width="24"> | 🔵 | Fight or be Forgotten | Fatal: 30 s invuln then convert to vampire |
| <img src="images/perks/bow.png" width="24"> | 🔵 | Long Bow | +1 % arrow dmg per block (cap +50 %); 75+ blocks = instakill |
| <img src="images/perks/gold_ingot.png" width="24"> | 🔴 | Blunt Force II | +20 % melee damage (stacks with Silver) |
| <img src="images/perks/chorus_fruit.png" width="24"> | 🔴 | Shadow Strike | Teleport behind nearest enemy (15 s cd) |
| <img src="images/perks/blue_ice.png" width="24"> | 🔴 | Frost Bite | Attacks apply Slowness I + Frost Walker boots |
| <img src="images/perks/phantom_membrane.png" width="24"> | 🔴 | Phantom Step | After taking damage: 2 s invis + Speed I (15 s cd) |
| <img src="images/perks/wither_rose.png" width="24"> | 🔴 | Blood Price | Sacrifice 3 ♥, next hit 2× (20 s cd) |
| <img src="images/perks/chain.png" width="24"> | 🔴 | Tether | Pull nearest enemy toward you (15 s cd) |
| <img src="images/perks/skeleton_skull.png" width="24"> | 🔴 | Skeleton Archers | Every 30 s: spawn 2 archers (10 s) |
| <img src="images/perks/splash_potion.png" width="24"> | 🔴 | Harming Potions | 3 Splash Harming potions, regen 1 every 2 min |
| <img src="images/perks/redstone.png" width="24"> | 🔴 | Hemophilia | Attacks bleed: 0.5 ♥/s for 4 s |
| <img src="images/perks/clock.png" width="24"> | 🔴 | Nocturnal | Night: +2 ♥ + 15 % dmg; Day: −1 ♥ |
| <img src="images/perks/tnt.png" width="24"> | 🔴 | Corpse Explosion | On kill: victim explodes, 3 ♥ AoE |
| <img src="images/perks/bone.png" width="24"> | 🔴 | Blood Beacon | Place healing beacon for nearby vampires (60 s cd) |
| <img src="images/perks/flint.png" width="24"> | 🔴 | Shadow Ambush | 3 s invis, next hit +50 % damage (30 s cd) |
| <img src="images/perks/cobweb.png" width="24"> | 🔴 | Spider Climb | Wall climbing ability |
| <img src="images/perks/fermented_spider_eye.png" width="24"> | 🔴 | Plague Carrier | Spread poison to nearby enemies over time |

### Prismatic Tier (45 perks · 400 g)


| Icon | Team | Perk | Effect |
|:----:|:----:|------|--------|
| <img src="images/perks/totem_of_undying.png" width="24"> | ⚪ | Can't Touch This | Every 30 s: 8 s invulnerability |
| <img src="images/perks/tnt.png" width="24"> | ⚪ | Glass Cannon | −30 % max HP, +35 % damage dealt |
| <img src="images/perks/diamond_chestplate.png" width="24"> | ⚪ | Goliath | +6 max ♥, +10 % damage, Slowness I |
| <img src="images/perks/lightning_rod.png" width="24"> | ⚪ | Thunderstrike | Every 5th hit summons lightning |
| <img src="images/perks/brown_dye.png" width="24"> | ⚪ | Earthquake | Right-click: 6-block AoE knockback + 3 ♥ (30 s cd) |
| <img src="images/perks/trident.png" width="24"> | ⚪ | Chain Lightning | Melee hits chain for 50 % damage |
| <img src="images/perks/soul_lantern.png" width="24"> | ⚪ | Death's Gambit | Fatal: 50/50 survive at 1 HP or take 50 % more |
| <img src="images/perks/brewing_stand.png" width="24"> | ⚪ | Plague Doctor | Immune to all negative potion effects |
| <img src="images/perks/armor_stand.png" width="24"> | ⚪ | Decoy | Spawn a decoy of yourself |
| <img src="images/perks/sunflower.png" width="24"> | ⚪ | Lucky Roll (Prismatic) | Replaces itself with 2 random Prismatic perks |
| <img src="images/perks/bow.png" width="24"> | ⚪ | Galeforce | 4-tier upgradable bow; max tier = dash + regen |
| <img src="images/perks/shield.png" width="24"> | 🔵 | Courage of the Colossus | Hit a player → 2 absorption ♥ (30 s cd) |
| <img src="images/perks/ender_pearl.png" width="24"> | 🔵 | Escape Plan (Strong) | Below 4 ♥: Absorption III + Speed II for 5 s (45 s cd) |
| <img src="images/perks/rabbit_foot.png" width="24"> | 🔵 | Giant Slayer | Speed I + 25 % dmg to targets with more max HP |
| <img src="images/perks/netherite_sword.png" width="24"> | 🔵 | Netherite Arsenal | Netherite Sword + Helmet + Chestplate |
| <img src="images/perks/totem_of_undying.png" width="24"> | 🔵 | Guardian Angel | Auto-revive 50 % HP + 5 s invuln (3 min cd) |
| <img src="images/perks/cobweb.png" width="24"> | 🔵 | Trapper | 5 placeable cobwebs |
| <img src="images/perks/bow.png" width="24"> | 🔵 | Marksman | Power III + Punch I bow, 32 arrows |
| <img src="images/perks/nether_star.png" width="24"> | 🔵 | Citadel | 5-block zone: Resistance I + Regen I for allies (60 s cd) |
| <img src="images/perks/golden_chestplate.png" width="24"> | 🔵 | Holy Shield | Absorb 10 dmg → auto-explode 4 ♥ AoE |
| <img src="images/perks/clock.png" width="24"> | 🔵 | Temporal Shield | Freeze enemies within 5 blocks for 2 s (45 s cd) |
| <img src="images/perks/iron_block.png" width="24"> | 🔵 | Iron Guardian | On vampire kill: summon Iron Golem (12 s) |
| <img src="images/perks/diamond_sword.png" width="24"> | 🔵 | Diamond Edge | Iron sword → Diamond (Sharpness I) |
| <img src="images/perks/cactus.png" width="24"> | 🔵 | Thorns Enchant | All armor gets Thorns II + Unbreaking III |
| <img src="images/perks/splash_potion.png" width="24"> | 🔵 | Regeneration Potions | 3 Regen potions, regen 1 every 2 min |
| <img src="images/perks/sunflower.png" width="24"> | 🔵 | Radiant Aura | Enemies within 6 blocks take 1 ♥/3 s + Glowing |
| <img src="images/perks/recovery_compass.png" width="24"> | 🔵 | Time Warp | Fatal: rewind to position + HP from 3 s ago (90 s cd) |
| <img src="images/perks/note_block.png" width="24"> | 🔵 | Bard | Aura buffs nearby allies |
| <img src="images/perks/poppy.png" width="24"> | 🔵 | Plant Master | Vines + plants slow and damage enemies |
| <img src="images/perks/ender_eye.png" width="24"> | 🔵 | Dimensional Pocket | Store items across lives |
| <img src="images/perks/netherite_pickaxe.png" width="24"> | 🔴 | Erosion | Hits apply Weakness I for 3 s |
| <img src="images/perks/nether_star.png" width="24"> | 🔴 | Final Form | Every 60 s: Absorption IV + 25 % lifesteal for 8 s |
| <img src="images/perks/fire_charge.png" width="24"> | 🔴 | Firebrand | Sets target on fire + 0.5 ♥ bonus damage |
| <img src="images/perks/spectral_arrow.png" width="24"> | 🔴 | Double Tap | Crits deal 1.75× + Slowness I |
| <img src="images/perks/phantom_membrane.png" width="24"> | 🔴 | Bat Form | Invisible + Speed III for 5 s (25 s cd) |
| <img src="images/perks/wither_skeleton_skull.png" width="24"> | 🔴 | Soul Eater | Each kill grants +10 % permanent damage (max +30 %) |
| <img src="images/perks/bone.png" width="24"> | 🔴 | Summoner | Summon 2 wolves to attack (45 s cd) |
| <img src="images/perks/ender_pearl.png" width="24"> | 🔴 | Void Walker | Teleport to random enemy + 3 ♥ AoE (25 s cd) |
| <img src="images/perks/spectral_arrow.png" width="24"> | 🔴 | Reaper's Mark | Mark enemy +20 % dmg, kill within 30 s = full heal (45 s cd) |
| <img src="images/perks/wither_skeleton_skull.png" width="24"> | 🔴 | Wither Guard | Every 30 s: spawn 2 wither skeletons (12 s) |
| <img src="images/perks/nether_star.png" width="24"> | 🔴 | Blood Moon | Allies: Speed I + 20 % lifesteal for 8 s (90 s cd) |
| <img src="images/perks/echo_shard.png" width="24"> | 🔴 | Wraith Walk | 4 s ghost form, expiry AoE Blindness + Slow (35 s cd) |
| <img src="images/perks/wither_skeleton_skull.png" width="24"> | 🔴 | Curse of Decay | Attacks curse enemies: 50 % reduced healing for 6 s |

---

## Commands Reference

| Command | Permission | Description |
|---------|-----------|-------------|
| `/vz help` | — | Show command list |
| `/vz join` | — | Join the VampireZ lobby |
| `/vz leave` | — | Leave and restore your inventory |
| `/vz shop` | — | Open perk shop (active game only) |
| `/vz perks` | — | List your active perks |
| `/vz gold` | — | Show gold balance |
| `/vz status` | — | Game state, time, team counts |
| `/vz announce` | `vampirez.admin` | Broadcast clickable join message |
| `/vz start` | `vampirez.admin` | Start game (checks min players + spawns) |
| `/vz forcestart` | `vampirez.admin` | Start ignoring player count |
| `/vz stop` | `vampirez.admin` | Stop running game |
| `/vz setlobby` | `vampirez.admin` | Set lobby spawn |
| `/vz sethumanspawn` | `vampirez.admin` | Set human spawn |
| `/vz setvampspawn` | `vampirez.admin` | Set vampire spawn |
| `/vz arena` | `vampirez.admin` | Teleport to arena world |
| `/vz test` | `vampirez.admin` | Open perk test menu |
| `/vz reload` | `vampirez.admin` | Reload config (lobby only) |

---

## Configuration Reference

All settings live in `plugins/VampireZ/config.yml` after first run.

```yaml
game:
  min-players: 10
  game-duration-seconds: 1500          # 25 minutes
  vampire-ratio: 0.3                   # 30 % become vampires
  vampire-respawn-delay-ticks: 80      # 4 seconds
  lobby-countdown-seconds: 30
  start-countdown-seconds: 15

economy:
  passive-income-amount: 2
  passive-income-interval-ticks: 200   # 10 seconds
  kill-reward: 15
  assist-reward: 5
  assist-time-window-ms: 10000         # 10 seconds

perks:
  max-perks-per-player: 6              # Note: effective cap is 4
  silver-cost: 50
  gold-cost: 150
  prismatic-cost: 400
  options-per-purchase: 3

day-night:
  enabled: true
  day-duration-ticks: 7200             # 6 minutes
  night-duration-ticks: 4800           # 4 minutes

spawns:
  lobby:   { world, x, y, z, yaw, pitch }
  human:   { world, x, y, z, yaw, pitch }
  vampire: { world, x, y, z, yaw, pitch }

messages:
  prefix: "&8[&4VampireZ&8] "
  game-start, vampires-win, humans-win, human-death, night-fall, day-break
```

### Useful tweaks
- **Shorter games for testing:** drop `game-duration-seconds` to 120 and `min-players` to 2.
- **Vampire-heavy games:** bump `vampire-ratio` to 0.4 – 0.5.
- **Bigger perk pools:** raise `max-perks-per-player`. Note: the effective cap is 4 unless you also change the hard-coded limit in the source.

---

## Arena Map

<p align="center">
  <img src="VampireZ_Final_Render.png" alt="VampireZ arena render" width="720" />
</p>

The bundled map is distributed as `VampireZ-Map.zip` — extract into your server directory alongside `world/`, then set spawns with `/vz setlobby`, `/vz sethumanspawn`, `/vz setvampspawn`.