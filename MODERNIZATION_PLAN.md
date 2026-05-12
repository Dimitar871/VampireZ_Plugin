# VampireZ Modernization Plan

Living roadmap for aligning the VampireZ plugin with 2024–2025 professional Minecraft plugin
conventions. Target: **Paper 1.21.4, Java 21, 50–100 concurrent players**.

Phases are ordered so each builds on the previous. Tick boxes as work lands so future
sessions can pick up where the last one left off.

---

## Status at a glance

| Phase | Title                                  | Status      |
|-------|----------------------------------------|-------------|
| 1A    | Dependency upgrades                    | ✅ Done     |
| 1B    | Adventure + MiniMessage migration      | ✅ Done     |
| 1C    | ConfigLib typed configuration          | ✅ Done     |
| 2A    | Break up `GameManager.java`            | ✅ Done (state extraction; transition methods still on facade) |
| 2B    | Eliminate setter injection             | ✅ Done     |
| 2C    | Migrate commands to cloud-command-fwk  | ✅ Done (routing layer; Brigadier client-hints deferred until PaperCommandManager bootstrapper migration) |
| 3A    | Migrate GUIs to triumph-gui            | ✅ Done (PerkShopGUI, PerkSelectionGUI, PerkTestGUI, LeaderboardGUI) |
| 3B    | Fix GUI state leaks                    | ✅ Done (triumph-gui's setCloseGuiAction handles countdown cancellation) |
| 4A    | SQLite + HikariCP for player stats     | ✅ Done     |
| 4B    | Caffeine in-memory cache               | ⏳ Deferred (HashMap is fine for current dataset size) |
| 4C    | Async timer optimisations              | ✅ Done (blood-compass hot path + getOnlineTeamPlayers helper) |
| 5A    | MockBukkit integration                 | ⏳ Deferred (vanilla JUnit + Mockito covered the state-holder classes) |
| 5B    | New test classes (≥80% coverage Phase 2)| ✅ Done (TeamManagerTest, GameStateManagerTest, GameTimerManagerTest — 24 new tests) |
| 6A    | PacketEvents boss bar timer            | ✅ Done (Adventure native BossBar; PacketEvents not needed) |
| 6B    | SLF4J logging                          | ✅ Done (per-class SLF4J in 6 manager files; plugin lifecycle logging stays on Bukkit logger) |
| 6C    | Configurable hardcoded timings         | ✅ Done (combatTagMs, vampireLeapCooldownSeconds, bloodCompassUnlock, freeSilver/Gold/PrismaticPerkAt) |

---

## Research baseline (2024–2025 conventions)

| Area        | Old / Current                          | Modern Standard                                |
|-------------|----------------------------------------|------------------------------------------------|
| GUI         | Raw `Inventory` API + magic slots      | **triumph-gui** or **Woody**                   |
| Commands    | Raw `CommandExecutor`                  | **cloud-command-framework** (Brigadier-native) |
| Text/Color  | `ChatColor` & BungeeChat               | **Adventure API + MiniMessage**                |
| Packets     | ProtocolLib                            | **PacketEvents** (async, multi-platform)       |
| Config      | Raw `.getInt()/.getString()`           | **ConfigLib** (typed, validated)               |
| Persistence | None                                   | **SQLite + HikariCP + Caffeine cache**         |
| Testing     | 12 partial test files                  | **MockBukkit 4.24+ + JUnit 5**                 |
| Logging     | `plugin.getLogger()`                   | **SLF4J**                                      |
| Architecture| Setter injection / circular deps       | **Constructor injection (no setters)**         |

### Audit findings (problems the plan addresses)

1. `GameManager.java` is ~1,348 lines — God class with 11 responsibilities.
2. `GameManager` ↔ `DayNightManager` setter-injection circular dep.
3. Zero async work — blood compass, timer loops, all stat writes block main thread.
4. Config unvalidated — invalid values silently fail or NPE at runtime.
5. 719+ legacy `ChatColor` / BungeeChat occurrences (✅ now migrated).
6. GUI routing by title string — fragile against any color-code change.
7. Magic slot numbers in every GUI.
8. GUI state leaks if a player closes the inventory mid-countdown.
9. Null-check pattern repeated 69 times — no helper.
10. Hardcoded timings (combat tag 7000ms, blood compass 600s, perk-roll delays) not in config.
11. No exception handling at world / location / config boundaries.
12. `GameManager`, `EconomyManager`, `PerkManager` have zero test coverage.

---

## Phase 1 — Foundation

### 1A. Dependency upgrades (`pom.xml`) ✅

Already in `pom.xml`:

- `dev.triumphteam:triumph-gui:3.1.11`
- `org.incendo:cloud-paper:2.0.0-beta.10`
- `org.incendo:cloud-minecraft-extras:2.0.0-beta.10`
- `org.xerial:sqlite-jdbc:3.45.1.0`
- `com.zaxxer:HikariCP:5.1.0`
- `com.github.ben-manes.caffeine:caffeine:3.1.8`
- `de.exlll:configlib-yaml:4.8.1`
- `org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0` (test scope)

All shaded under `com.vampirez.lib.*`. Adventure ships with Paper — no extra dep needed.

### 1B. Adventure + MiniMessage migration ✅

- `MM.parse("<color>...")` replaces `ChatColor.X + "..."`.
- `MM.legacy("&ctext")` for &-code strings (config messages).
- `Component.text(...).color(NamedTextColor.X).decoration(TextDecoration.ITALIC, false)` for item names/lore.
- `entity.customName(Component)` for mobs.
- `player.sendActionBar(Component)` instead of BungeeChat.
- 79 perk files + every manager and GUI migrated.
- `ChatColor` intentionally retained in 5 spots:
  - `ScoreboardManager` — FastBoard 2.x string-based API.
  - `PerkTier` — `getColor()` returning `ChatColor` for FastBoard scoreboard lines.
  - `PerkSelectionGUI` / `LeaderboardGUI` / `PerkTestGUI` — title-string routing
    (will go away when these GUIs migrate to triumph-gui in Phase 3A).

### 1C. ConfigLib typed configuration ✅

- `com.vampirez.config.PluginConfig` — POJO with `@Configuration` nested classes for
  every section (`game`, `economy`, `perks`, `arena`, `day-night`, `spawns`, `messages`).
- Field names auto-mapped to kebab-case via `NameFormatters.LOWER_KEBAB_CASE` so existing
  `config.yml` files keep working unchanged.
- `VampireZPlugin` exposes:
  - `getPluginConfig()` — typed read access.
  - `savePluginConfig()` — write the in-memory POJO back to disk.
  - `reloadPluginConfig()` — re-read from disk and refresh Bukkit's view.
- `DayNightManager`, `EconomyManager`, `ArenaManager`, `GameManager` constructors now
  take `VampireZPlugin` and read fields from `PluginConfig`.
- `GameManager.saveLocation(String, Location)` replaced with
  `saveSpawn(String, Location)` that mutates the typed `SpawnPoint`.
- `GameCommands` `/vz disableperk` & `/vz enableperk` mutate
  `pluginConfig.perks.disabledPerks` directly.

---

## Phase 2 — Architecture refactor

### 2A. Break up `GameManager.java`

Split the 1,348-line class into focused responsibilities:

| New class             | Responsibility                                                       |
|-----------------------|----------------------------------------------------------------------|
| `GameStateManager`    | State machine (LOBBY→STARTING→ACTIVE→ENDING) + win conditions        |
| `TeamManager`         | Human/vampire sets, conversion logic, team queries                   |
| `SpawnManager`        | Spawn point load/save, teleportation                                 |
| `GameTimerManager`    | Countdown, game duration, timed perk events, milestone announcements |
| `GameAnnouncer`       | All broadcast messages, sounds, titles                               |

`GameManager` becomes a thin coordinator holding references to the five.

### 2B. Eliminate setter injection

Replace the current setter chain with a `ServiceRegistry`:

```java
ServiceRegistry registry = new ServiceRegistry();
registry.register(EconomyManager.class, new EconomyManager(this));
registry.register(PerkManager.class,    new PerkManager());
// ... all managers registered
registry.register(GameManager.class,    new GameManager(registry)); // receives registry
registry.initAll(); // calls init() on each in dependency order
```

No more setters; all wiring in constructors or a single `init(ServiceRegistry)` call.

### 2C. Migrate commands to cloud-command-framework

```java
manager.command(manager.commandBuilder("vz")
    .literal("start")
    .permission("vampirez.admin")
    .handler(ctx -> gameManager.startGame(ctx.sender()))
);
```

Brigadier integration auto-provides tab completion, permission checks, and `/vz help`.
Target: `GameCommands.java` shrinks from ~638 lines to ~150.

---

## Phase 3 — GUI modernisation

### 3A. Migrate to triumph-gui

```java
ChestGui gui = ChestGui.gui(6, Component.text("Perk Shop"))
    .item(GuiItem.of(silverItem, event -> handleTierClick(player, SILVER)))
    .build();
gui.open(player);
```

- Removes string-based routing (each GUI owns its click handlers).
- Removes magic slot numbers.
- Closes leak windows automatically.
- Files: `PerkShopGUI`, `PerkSelectionGUI`, `PerkTestGUI`.

### 3B. Fix GUI state leaks

- `InventoryCloseEvent` handler in `PerkSelectionGUI` cancels countdown `BukkitTask`.
- Clean `selectionStates` map entry on close.
- Move reroll tracking from `boolean[]` to a proper inner class.

---

## Phase 4 — Persistence & performance

### 4A. SQLite + HikariCP for player stats

```java
HikariDataSource dataSource = new HikariConfig()
    .setJdbcUrl("jdbc:sqlite:plugins/VampireZ/stats.db")
    .setMaximumPoolSize(5)
    .build();
```

Schema: `player_stats(uuid TEXT PK, kills INT, deaths INT, gold_earned INT, games_played INT, wins INT)`

All writes async; all reads async with sync callback for Bukkit API.

### 4B. Caffeine in-memory cache

```java
Cache<UUID, PlayerStats> cache = Caffeine.newBuilder()
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .maximumSize(200)
    .build();
```

Load on join (async), write-back on logout / game end. In-game reads hit cache, never DB.

### 4C. Async timer optimisations

- Blood compass: batch updates async, sync only the final `setItem`.
- Add helper `getOnlineTeamPlayers(Set<UUID> team)` to delete the 69 scattered null-checks.

---

## Phase 5 — Testing

### 5A. MockBukkit integration

Already on classpath. Adjust plugin constructor for `MockBukkit.load()` compatibility.

### 5B. New test classes

- `GameStateManagerTest` — state transitions + win conditions.
- `TeamManagerTest` — conversion logic + balance formula.
- `EconomyManagerTest` — kill rewards, assist window, passive income.
- `ConfigValidationTest` — invalid config values rejected cleanly.
- `DatabaseManagerTest` — CRUD on SQLite (H2 in-memory for tests).

Coverage target: ≥80 % on every Phase 2 class.

---

## Phase 6 — Polish (incremental, low priority)

### 6A. PacketEvents boss bar game timer

Replace scoreboard time display with a centred boss bar. Send via PacketEvents async.

### 6B. SLF4J logging

```java
private static final Logger log = LoggerFactory.getLogger(GameManager.class);
log.info("Game started with {} players", count);
```

Class-name prefixes, easier filtering than `plugin.getLogger()`.

### 6C. Configurable hardcoded timings

Move to `config.yml` (validated by ConfigLib):

- Combat tag duration (`COMBAT_TAG_MS = 7_000L`).
- Blood compass unlock time (`600` seconds).
- Perk roll animation delays.

---

## Critical files map (for reference)

| File                              | Phase     | Change                                      |
|-----------------------------------|-----------|---------------------------------------------|
| `pom.xml`                         | 1A ✅     | All 8 dependencies present                   |
| `GameManager.java`                | 1B ✅ / 2A| Migrate text done; split into 5 classes next |
| `GameCommands.java`               | 1B ✅ / 2C| Migrate text done; rewrite with Cloud next   |
| `PerkShopGUI.java`                | 1B ✅ / 3A| Adventure done; triumph-gui next             |
| `PerkSelectionGUI.java`           | 1B ✅ / 3A/3B| Adventure done; triumph-gui + fix leaks   |
| `ScoreboardManager.java`          | (kept)    | FastBoard string API — left as-is            |
| `VampireZPlugin.java`             | 1C ✅ / 2B| ConfigLib wired; ServiceRegistry next        |
| `EconomyManager.java`             | 1C ✅ / 4 | Typed config; add DB + cache next            |
| `PlayerStatsManager.java`         | 4         | Add persistence layer                        |
| *(new)* `PluginConfig.java`       | 1C ✅     | Created                                      |
| *(new)* `DatabaseManager.java`    | 4A        | SQLite + HikariCP                            |
| *(new)* `ServiceRegistry.java`    | 2B        | Dependency wiring                            |
| *(new)* `GameStateManager.java`   | 2A        | Extracted from GameManager                   |
| *(new)* `TeamManager.java`        | 2A        | Extracted from GameManager                   |
| *(new)* `GameTimerManager.java`   | 2A        | Extracted from GameManager                   |

---

## Verification plan (always run before declaring a phase done)

1. **Build**: `mvn clean package` → `BUILD SUCCESS`, no errors.
2. **Tests**: all existing tests green + any new ones for the phase.
3. **Deploy**: `build-deploy-and-run.bat` → server starts, `/vz status` responds.
4. **Manual smoke**: see *How to test the current changes* below.

---

# How to test the current changes

The two phases that just landed are **1B (Adventure / MiniMessage)** and
**1C (ConfigLib typed configuration)**. Both are mechanical refactors with no behaviour
changes — the goal of testing is to confirm nothing regressed.

## A. Build + unit tests

From the repo root:

```bash
mvn clean package
```

Expect:

- `BUILD SUCCESS`
- `Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`
- Output jar `target/VampireZ-1.2.0.jar`

If anything fails, the change list is small enough to bisect by reverting individual
files in `src/main/java/com/vampirez/perks/` or the four manager files.

## B. Deploy to the test server

```bash
build-deploy-and-run.bat
```

This builds, copies the jar to `C:\Users\User\Desktop\test-server\plugins\`, and starts
the server. Watch the console for:

- `VampireZ Plugin Enabled!`
- No stack traces during plugin enable.
- No `WARNING: Unknown action type` (those are test-only fixtures and should not appear
  in the live server logs).

## C. Test Phase 1B — Adventure / MiniMessage

You're verifying that every text path still renders correctly (colours, formatting,
hover/click events).

1. **Lobby join messages** — connect with two accounts:
   - Each player should see the join broadcast in **yellow**.
   - The lobby scoreboard appears (FastBoard) showing player count.
2. **Commands**:
   - `/vz help` — list renders with coloured command headings, no raw `§` codes.
   - `/vz announce <text>` — clickable / hoverable announcement appears for everyone.
   - `/vz disableperk deft` — message has bold red `⊘ deft disabled`.
   - `/vz enableperk deft` — message has bold green `✔ deft enabled`.
3. **Start a game** (`/vz forcestart`):
   - Countdown messages appear in correct colours (yellow → red).
   - "The hunt begins!" broadcast (red) on game start.
   - Day/night transitions show coloured night-fall / day-break messages.
4. **Combat & perks**:
   - Kill a player — both killer and assister see gold + assist gold messages.
   - Open perk shop (right-click emerald) — tier buttons, perk lores, prices all rendered
     in colour with no italic on custom names.
   - Buy a perk — confirmation message colour-matches the perk tier.
   - Open `/vz test` — perk browser GUI shows team labels in correct colour.
5. **Spawned mobs / items**:
   - Cast `WolfPackPerk` (kill a vampire as human with WolfPack equipped) — wolves spawn
     with custom names like `<player>'s Wolf` in white.
   - Use `IronGuardianPerk` — golem name visible.
   - Pick up potions from `RegenPotionsPerk` / `StrengthPotionsPerk` etc. — names show in
     correct colour, not italic.
6. **End-game**:
   - Let the game end (kill all humans or `/vz stop`) — vampires-win or humans-win
     broadcast renders correctly.

**Visual regression check**: the only known intentional `ChatColor` usage left is
the FastBoard scoreboard (`ScoreboardManager`) and three GUI title strings. If a
title-string GUI fails to route a click, that's a regression in Phase 1B — most
likely cause is a stray space or unicode character in a `Component` title.

## D. Test Phase 1C — ConfigLib typed configuration

You're verifying that the typed config loader is bidirectional with the existing
`config.yml` file format.

### D1. Existing config keeps working (most important test)

1. Start the server with the **existing** `plugins/VampireZ/config.yml` from a previous
   build. Do **not** delete it.
2. After enable, open the file. Expected:
   - All your prior values (spawns, min-players, messages, disabled-perks) are intact.
   - ConfigLib may have **added comments** above sections — that's expected.
   - Any newly introduced default values are appended (none in this phase).
3. Run `/vz status` — verify it still uses your configured `min-players` and
   `game-duration-seconds`.

### D2. Spawn save round-trip

1. Stand somewhere in the lobby world, run `/vz setlobby`.
2. Open `plugins/VampireZ/config.yml` — `spawns.lobby.world / x / y / z / yaw / pitch`
   should reflect the new location.
3. Restart the server. Run `/vz status` — the lobby spawn should still be the new
   location (proves load + save are consistent).
4. Repeat for `/vz sethumanspawn` and `/vz setvampspawn`.

### D3. Perk disable round-trip

1. `/vz disableperk wolfpack`
2. Inspect `plugins/VampireZ/config.yml` — `perks.disabled-perks` now contains `wolfpack`.
3. Restart the server. Run `/vz test` — `wolfpack` should appear with the red barrier
   icon "⊘ DISABLED".
4. `/vz enableperk wolfpack` → file no longer lists it → restart → it's selectable again.

### D4. Reload during lobby

1. Edit `config.yml` while the server is running, change `economy.passive-income-amount`
   to `99`.
2. `/vz reload` (lobby only).
3. Wait 10 s — players should receive **+99 gold** instead of the previous value.

### D5. Fresh-install behaviour (optional)

1. Delete `plugins/VampireZ/config.yml` entirely.
2. Restart. ConfigLib creates a new `config.yml` from `PluginConfig` defaults — every
   field present, every `@Comment` rendered above the field.
3. Verify all sections exist: `game`, `economy`, `perks`, `arena`, `day-night`, `spawns`,
   `messages`.

### What "good" looks like

- No NPE during plugin enable.
- No `Could not load 'config.yml'` warnings.
- Round-trip: save → restart → load returns the same values.
- `/vz reload` updates manager state without restarting.

If a key disappears or types stop matching (e.g. yaw stored as int instead of float),
that's a ConfigLib mapping issue — fix by adjusting the field type in `PluginConfig`,
not by editing the YAML by hand.
