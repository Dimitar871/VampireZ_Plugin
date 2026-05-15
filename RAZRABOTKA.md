# VampireZ - Преглед на разработката

Документ за разработчици, които ще работят по плъгина или искат да разберат защо
кодовата база изглежда така, както изглежда. Описва **текущата** архитектура (към
v2.2.1), използваните библиотеки и причините зад ключовите решения.

> Хронологичните детайли (как сме стигнали до тук - modernization phases v1 → v2.2)
> са документирани в `MODERNIZATION_PLAN.md`. Този документ е снимка на текущото
> състояние. За цялостна архитектурна разходка с диаграми вижте `ARCHITECTURE.md`.

---

## Какво е VampireZ

Spigot/Paper минигейм плъгин: **Хора оцеляват срещу Вампири за 25 минути**. Хора,
които умрат, се обръщат във Вампири. Победителите се определят от това дали хората
оцелеят таймера или вампирите успеят да обърнат всички.

- **Технологичен стек**: Java 21, Maven, Paper 1.21.4 API
- **Точка на влизане**: `com.vampirez.VampireZPlugin`
- **165 perks** общо (122 Java класа + 43 data-driven YAML записа)

---

## Външни библиотеки - какво и защо

Всички външни библиотеки се **shaded и relocated** под `com.vampirez.lib.*` чрез
`maven-shade-plugin`. Това е стандартна практика за Bukkit плъгини: предотвратява
class-loader конфликти, ако друг плъгин на същия сървър shade-ва същите класове.

### Adventure / MiniMessage (идва с Paper)
- **Какво**: Модерният текст API за Minecraft (богат текст, hover, click, цветове).
- **Защо го избрахме**: Заместя deprecated `ChatColor` и `BungeeChat`. Paper вече
  изоставя legacy API-тата; plugin-и, които не мигрират, ще се счупят при следваща
  major версия. MiniMessage синтаксисът (`<red>текст</red>`) е по-четлив от
  hex-кодове и се парсва директно в `Component`.
- **Изключения**: `ScoreboardManager` и `PerkTier#getColor()` все още използват
  `ChatColor`, защото FastBoard 2.x работи със string-based API.

### ConfigLib (`de.exlll:configlib-yaml:4.8.1`)
- **Какво**: Type-safe POJO ↔ YAML маппинг с `@Configuration` и `@Comment`
  анотации, kebab-case име на полетата автоматично.
- **Защо го избрахме**: Заместя сурови `getConfig().getInt("game.min-players", 10)`
  извиквания. Преди: ако има typo в ключа, връща default тихо. Сега: компилаторът
  улавя грешките, IDE дава autocomplete, новите полета се пишат в `config.yml`
  автоматично с коментари.
- **Преди / след**:
  ```java
  // Преди (сурово, нетипизирано, лесно за typo):
  int minPlayers = plugin.getConfig().getInt("game.min-players", 10);
  long combatMs  = plugin.getConfig().getLong("timings.combat-tag-ms", 7000);

  // След (POJO дефиниция, IDE autocomplete, compile-time проверка):
  @Configuration
  public static class GameSection {
      @Comment("Minimum players required to start a game")
      public int minPlayers = 10;
  }
  // На сайта на ползване:
  int minPlayers = plugin.getPluginConfig().game.minPlayers;
  ```
- **Къде**: `com.vampirez.config.PluginConfig` дефинира 8 секции
  (`game`, `economy`, `perks`, `arena`, `day-night`, `spawns`, `messages`,
  `timings`, `discord`).
- **Зареждане на boot**: `YamlConfigurations.update(path, PluginConfig.class, props)` -
  чете съществуващия YAML, попълва липсващите полета с defaults и пише обратно с
  всички `@Comment` анотации.

### cloud-command-framework (`org.incendo:cloud-paper:2.0.0-beta.10`)
- **Какво**: Декларативна система за Bukkit команди с типизирани аргументи,
  permission gates и tab completion built-in.
- **Защо го избрахме**: Преди имахме `GameCommands implements CommandExecutor` с
  огромен `switch` стейтмънт за всичките 28 subcommands и ръчно изграден
  `TabCompleter`. С Cloud всеки subcommand се регистрира с builder pattern,
  permission се закача с един `.permission()` ред, а tab completion се случва
  автоматично от declared argument types.
- **Преди / след**:
  ```java
  // Преди:
  switch (args[0].toLowerCase()) {
      case "settime" -> {
          if (!player.hasPermission("vampirez.admin")) { /* deny */ return true; }
          if (args.length < 2) { /* usage */ return true; }
          try { int s = Integer.parseInt(args[1]); api.setRemainingSeconds(s); }
          catch (NumberFormatException e) { /* error */ }
      }
      // ... 27 more cases ...
  }
  // + ръчно tab complete метод с подобен switch.

  // След:
  manager.command(manager.commandBuilder("vz")
      .literal("settime")
      .permission("vampirez.admin")
      .required("seconds", IntegerParser.integerParser(0))
      .handler(ctx -> handlers.handleSetTime(ctx.sender(),
          new String[]{"settime", String.valueOf(ctx.<Integer>get("seconds"))})));
  ```
- **Къде**: `com.vampirez.VampireZCloudCommands` регистрира всички 28 команди.
  `GameCommands` остава като "service" клас с handler логиката, но не е вече
  `CommandExecutor`.
- **Suggestion providers**: Custom `SuggestionProvider`-и за играчи (онлайн
  списък), perk IDs, активни/деактивирани perk IDs - tab complete-ът показва
  само валидните стойности в текущия context.

### triumph-gui (`dev.triumphteam:triumph-gui:3.1.11`)
- **Какво**: Inventory GUI builder с `GuiItem` (item + click handler в едно).
- **Защо го избрахме**: Преди - сурови `Bukkit.createInventory()`, magic slot
  numbers, string-title routing (`if (event.getView().getTitle().equals("§4Perk Shop"))`),
  ръчно `event.setCancelled(true)`. Тъмна страна: ако някой смени цветовия код в
  заглавието, click routing-ът тихо се чупи. С triumph-gui всеки click handler е
  закачен директно за съответния `GuiItem`, GUI lifecycle (open/close) се
  управлява от библиотеката, и `disableAllInteractions()` предотвратява всякакви
  shift-click експлойти.
- **Преди / след**:
  ```java
  // Преди (отделен Listener клас + string title routing):
  Inventory inv = Bukkit.createInventory(null, 27, "§4§lPerk Shop");
  inv.setItem(11, silverItem);
  player.openInventory(inv);

  @EventHandler
  public void onClick(InventoryClickEvent e) {
      if (!e.getView().getTitle().equals("§4§lPerk Shop")) return;
      e.setCancelled(true);
      if (e.getSlot() == 11) handleSilverClick((Player) e.getWhoClicked());
  }

  // След (всичко в един метод, click handler closure):
  Gui gui = Gui.gui()
      .title(Component.text("Perk Shop").color(NamedTextColor.GOLD))
      .rows(6).disableAllInteractions().create();
  gui.setItem(11, new GuiItem(silverItem, e -> handleSilverClick(player)));
  gui.open(player);
  ```
- **PaginatedGui**: `PerkTestGUI` използва `Gui.paginated()` - библиотеката
  управлява next/prev страници automatically.
- **Къде**: 4 GUI-та (`PerkShopGUI`, `PerkSelectionGUI`, `PerkTestGUI`, `LeaderboardGUI`).
  Никой от тях вече не е `Listener` - GUI lifecycle е изцяло в библиотеката.

### SQLite + HikariCP + Caffeine (`org.xerial:sqlite-jdbc:3.45.1.0`, `com.zaxxer:HikariCP:5.1.0`)
- **Какво**: Embedded relational database + connection pool за player stats
  persistence.
- **Защо го избрахме**: Преди - player stats се пазеха в `player-stats.yml`,
  парс-натo с `YamlConfiguration` при всеки read/write, blocking на main thread.
  При няколко стотин играча файлът става неманевреen. SQLite е zero-config
  embedded DB (един `.db` файл), HikariCP дава connection pooling за thread-safe
  достъп, и WAL journal mode прави write-а почти безплатен.
- **Защо не директно JDBC**: HikariCP управлява connection lifecycle и
  автоматично reconnect-ва при грешки.
- **Къде**: `com.vampirez.db.DatabaseManager` (pool + schema migrations) +
  `com.vampirez.db.PlayerStatsRepository` (DAO с batched UPSERT-и).
- **Миграция**: При първи boot със SQLite, ако съществува `player-stats.yml`,
  данните автоматично се мигрират и старият YAML се преименува на `.migrated`.
- **Caffeine**: Включена в pom-а, но засега неизползвана - за нашия размер на
  данни `HashMap` е достатъчен.

### JDA (`net.dv8tion:JDA:5.6.1`)
- **Какво**: Java Discord API library - пълен Discord bot client.
- **Защо го избрахме**: Алтернативите бяха (1) webhook само (един път, без
  presence), (2) HTTP slash commands без persistent connection. Потребителят
  поиска real bot с presence + edited live status embed + event announcements,
  което изисква gateway connection - само JDA и Discord4J предлагат това.
- **Изключихме voice support** (`opus-java`) - спестява няколко MB, не ни трябва.
- **Threading model**: JDA има свой собствен thread pool. Bukkit events идват на
  main thread; ние четем състояние synchronously, после `RestAction.queue()`
  пуска заявката към JDA executor-а - main thread никога не блокира.
- **Къде**: `com.vampirez.discord` пакет (4 класа - виж по-долу).

### EffectLib (`com.elmakers.mine.bukkit:EffectLib:11.0`)
- **Какво**: Библиотека за анимирани particle effects (sphere, helix, vortex,
  beam, atom orbit и т.н.) с автоматичен lifecycle и iteration counting.
- **Защо го избрахме**: Vanilla `World.spawnParticle()` пуска частиците и
  изчезва. EffectLib дава анимация - например "expanding shockwave" е sphere
  чийто radius расте всеки tick. Ръчно това би било `BukkitRunnable` с math за
  всеки perk; с EffectLib е един `.shockwave(loc, color, radius, duration)` ред.
- **Тегло**: 264 KB shaded - изненадващо leko.
- **Къде**: Достъп през `com.vampirez.fx.EffectKit` (high-level wrapper).

### FastBoard (`fr.mrmicky:fastboard:2.1.5`)
- **Какво**: Lightweight scoreboard library без packet-magic.
- **Защо го избрахме**: Bukkit `ScoreboardManager` е известен с flicker-ите си.
  FastBoard поддържа per-player scoreboard-ове с под-1ms update време.

---

## Архитектурен преглед

### Точка на влизане

`VampireZPlugin.onEnable()` строи всички мениджъри в **dependency order** (без
post-construction setter injection). Циркулярни зависимости (DayNightManager
зависи от GameManager, който зависи от DayNightManager) се решават с
`Supplier<X>` за lazy lookup.

```
1. DatabaseManager       (HikariCP pool + schema)
2. EffectKit + SoundKit  (FX framework)
3. EconomyManager, PerkManager, GearManager, ScoreboardManager, PlayerStateManager, StatAnvilManager
4. registerAllPerks()    (Java perks + YAML perks)
5. DayNightManager(plugin, () -> getGameManager())
6. GameManager(everything)
7. GUIs (PerkShopGUI, PerkSelectionGUI, PerkTestGUI)
8. PerkStatsManager, PlayerStatsManager (с DB repository)
9. VampireZAPI registration в ServicesManager
10. (опционално) Discord bot, ако discord.enabled=true
11. LeaderboardGUI + cloud commands
12. Bukkit listener-и (GameListener, PerkListener, etc.)
```

### GameManager и неговите 6 поделени класа

`GameManager` беше "god class" с 1356 реда. В Phase 2A го разделихме на 6
focused класа, които `GameManager` държи като полета (facade pattern - външният
public API остава непокътнат):

| Клас | Отговорност |
|---|---|
| `SpawnManager` | Lobby/Human/Vampire spawn точки - load/save към `PluginConfig`, teleportToX helpers. |
| `TeamManager` | Хора/Вампири `Set<UUID>` колекциите, combat tag tracking, `vampiresReleased` + `bloodCompassGiven` флагове. |
| `GameStateManager` | `GameState` enum (LOBBY/STARTING/ACTIVE/ENDING) + `lastStartForced`. |
| `GameTimerManager` | Притежава 5-те `BukkitTask`-а (timer, scoreboard, countdown, vampireRelease, autoStart) + `remainingSeconds`, `firedTimedMilestones`. `cancelAllTasks()` е централизиран. |
| `GameAnnouncer` | Pure I/O - broadcast на messages/sounds/titles до всички joined-online играчи. Получава `Supplier<Collection<Player>>` за audience. |
| `BossBarManager` | Adventure native `BossBar` показващ time/phase/team counts по време на ACTIVE игра. |

Самият `GameManager` остава като orchestrator: държи `joinedPlayers` сета,
имплементира `joinGame`/`leaveGame`/`handlePlayerJoin`/`handlePlayerQuit` (които
докосват много подсистеми наведнъж) и публичните transition методи (`startGame`,
`endGame`, `stopGame`, `resetToLobby`).

### Perks - два вкуса

`PerkManager` държи `Map<String, Perk>` registry + `Map<UUID, List<Perk>>` за
кой играч какво има. `Perk` е abstract клас с hook методи (`onDamageDealt`,
`onKill`, `onTick`, etc.). Има два конкретни subtype-а:

1. **Hand-written Java perks** (~122 класа в `com.vampirez.perks`). Всеки extend-ва
   `Perk` и override-ва съответните hook-ове. Подходящи за perks с уникална
   логика (Phoenix Down, Decoy, Soul Eater, Time Warp).

2. **DataDrivenPerk** (43 perks в `perks.yml`). Един клас, конфигуриран от YAML -
   виж следващата секция.

`PerkListener` е bridge-а: Bukkit fire-ва `EntityDamageByEntityEvent`, listener-ът
итерира player's perks и вика `perk.onDamageDealt(...)` - никой код извън
`PerkListener` не знае дали perk-ът е Java или YAML-driven.

### Discord интеграция (`com.vampirez.discord` пакет)

| Клас | Роля |
|---|---|
| `DiscordBot` | JDA lifecycle wrapper. `startAsync()` пуска gateway connect off-main, `shutdown()` затваря с bounded 5s wait, `sendOrEditStatusEmbed()` + `postAnnouncement()` са guard-нати с `isReady()`. |
| `DiscordEventListener` | Bukkit listener (MONITOR priority) - слуша `VampireZGameStartEvent`, `VampireZGameEndEvent`, `PlayerConvertedEvent`, `DayPhaseChangeEvent` + Bukkit join/quit. Превежда ги в Discord embed-и. |
| `DiscordStatusUpdater` | Два repeating `BukkitTask`-а: presence (на всеки 30s) + live status embed (на всеки 10s по време на ACTIVE). `kick()` тригърва веднага при state transition. |
| `DiscordEmbedFactory` | Pure builder - създава `MessageEmbed` обекти. Player names се resolve-ват от listener-а на main thread, после се подават към factory-то. |

Ботът е **opt-in** - целият блок се skip-ва, ако `discord.enabled = false`,
така че никакви JDA класове не се зареждат при default конфигурация.

### FX framework (`com.vampirez.fx` пакет)

| Клас | Роля |
|---|---|
| `EffectKit` | High-level wrapper над EffectLib - методи като `shockwave()`, `runeCircle()`, `vortex()`, `atomAround()`, `helix()`, `bloodSpray()`. Притежава един shared `EffectManager`. |
| `SoundKit` | Vanilla-only layered sound presets - `playSpellCast()`, `playExplosion()`, `playBuff()`, `playDeathRattle()`, `playPortal()`. Не изисква resource pack - комбинира съществуващи `Sound` enum записи с timing/pitch. |
| `VFX` | Static accessor - `VFX.fx().shockwave(...)` и `VFX.sound().playExplosion(...)`. Резолвва кит-овете от плъгина при първо извикване и кешира ги. Това спестява perk-ите от това да държат plugin reference. |

8 flagship perks са мигрирани да го използват: Earthquake, Smoke Bomb, Blood
Moon, Wraith Walk, Holy Shield, Shadow Strike, Time Warp, Vampire Leap.
Останалите perks могат да бъдат мигрирани postupно.

---

## Threading модел и event приоритети

Bukkit/Paper има **един main thread** на който се изпълнява game loop-ът.
Почти всички API извиквания (`player.teleport()`, `inventory.setItem()`,
`world.spawnParticle()`, всеки event handler) трябва да са на main thread,
иначе хвърлят `IllegalStateException` или causat data corruption.

VampireZ използва три различни thread-а:

| Thread | Кой работи на него | Какво е разрешено |
|---|---|---|
| Main thread | Bukkit game loop, всички event handlers, всички scheduled tasks от `runTaskTimer` | Целият Bukkit API. Не блокирай (никакви sleeps, никакви I/O) |
| Bukkit async pool | `runTaskAsynchronously` workers | SQLite I/O (HikariCP е thread-safe), HTTP заявки, file write-ове |
| JDA thread pool | Собствен на JDA - reads/writes към Discord gateway + REST API | Само JDA операции. Никакъв Bukkit API |

**Критични правила**:
- `RestAction.queue()` пуска заявката към JDA pool-а - main thread не се блокира.
- ConfigLib YAML write (`plugin.savePluginConfig()`) **трябва** да е на main thread.
  Затова `DiscordBot` използва `Bukkit.getScheduler().runTask(...)` за да върне
  callback-а от JDA обратно на main thread преди да запише `statusMessageId`.
- Async DB write (`PlayerStatsManager.save()`): прави snapshot на `stats` map-а
  на main thread (за да е consistent), после `runTaskAsynchronously` пише в SQLite.

### Event приоритети (защо PerkListener е HIGHEST)

Bukkit вика `@EventHandler`-ите по приоритет: `LOWEST → LOW → NORMAL → HIGH →
HIGHEST → MONITOR`. По-високи приоритети виждат резултатите от по-ниските.

VampireZ използва:

```
HIGH      GameListener.onEntityDamage
          ├── Cancel friendly fire (същ team)
          ├── Compute base damage from weapon + attack cooldown
          ├── Apply 30% от vanilla armor reduction
          └── event.setDamage(finalDamage)

HIGHEST   PerkListener.onDamage
          ├── Resolve attacker (player или projectile shooter)
          ├── Викaт се onDamageDealt() за всички perks на attacker-а
          ├── Викaт се onDamageTaken() за всички perks на victim-а
          ├── Black Cleaver static stack-овете умножават damage-а
          ├── Stat Anvil multiplier
          └── Cap total damage at 7.0 HP

MONITOR   DebugBookManager.onPostDamage (read-only)
          └── Action-bar readout: "DMG 4.20  Player: 20.0 → 15.8"
```

`PerkListener` **трябва** да е HIGHEST защото `GameListener` (HIGH) пише
`event.setDamage(finalDamage)` - ако perks бяха на NORMAL, техните модификации
щяха да бъдат overwriting от GameListener-а. Има regression test
(`PerkListenerPriorityTest`) който reflectively чете `@EventHandler(priority = ...)`
анотацията и проверява invariant-а.

---

## Защо YAML за някои perks

`perks.yml` е data-driven engine за perks с repetitive patterns (damage
multipliers, attribute boosts, potion-on-hit, enchant-on-apply, mob spawners,
threshold buffs). Идеята: вместо да пишем `BluntForcePerk.java` с 30-40 реда
boilerplate (constructor + override + `addStat`-ове), пишем **3 реда YAML**:

```yaml
blunt_force:
  name: "Blunt Force"
  tier: SILVER
  team: BOTH
  icon: IRON_INGOT
  hooks:
    on_damage_dealt:
      - actions:
          - type: multiply_damage
            factor: 1.2
```

`PerkConfigLoader` превръща всеки YAML запис в `DataDrivenPerk` instance.
`DataDrivenPerk` дispatch-ва hook-овете към `TriggerEntry` обекти, всеки с
`List<Condition>` (`from_behind`, `hp_below_percent`, `cooldown` и т.н.) и
`List<Action>` (`multiply_damage`, `apply_potion`, `give_item`, `spawn_mob`...).

**Ползите**:
- Балансиране е config edit, не Java rebuild - `factor: 1.2` → `factor: 1.25`,
  restart, готово.
- 43 unit теста покриват primitive-ите (`MultiplyDamageAction`,
  `HpBelowPercentCondition` и т.н.) и така валидират **всички** YAML perks,
  които използват тези primitives.
- Header-ът на `perks.yml` е жива документация на наличните action/condition
  типове.

**Защо не всички perks**: ~122 perks остават в Java, защото имат уникална логика
(static cross-perk state, custom right-click abilities, complex multi-step
mechanics). Да ги моделираме в YAML би означавало по един primitive за всеки
perk, което побеждава целта.

### Engine vocabulary (за добавяне на нов YAML perk)

| Хук | Какво вика |
|---|---|
| `apply` | Когато perk-ът се даде на играч (вкл. на всеки respawn) |
| `remove` | Когато се отнема (conversion, clear, leave) |
| `on_damage_dealt` | Играчът ударя някого |
| `on_damage_taken` | Играчът е ударен |
| `on_kill` | Играчът убие друг играч |
| `on_death` | Играчът умре |
| `on_tick` | Всяка секунда (20 ticks), за всички онлайн играчи с perk |
| `on_health_regain` | Играчът регенерира HP |
| `on_interact` | Right-click event |
| `on_respawn` | Играчът respawn-ва |

**Налични actions** (~17 типа):
- `multiply_damage(factor)` - умножава `event.getDamage()`
- `set_damage(amount)` - override-ва damage
- `apply_potion(target, type, duration_ticks, amplifier)` - дава potion effect
- `add_enchant(material_contains, enchantment, level)` - enchant-ва item-и в inventory
- `add_attribute_modifier(attribute, name, amount, operation)` - permanent attribute boost
- `give_item(material, amount, displayName?, lore?, enchants?)` - дава item
- `spawn_mob(entity_type, count, lifetime_ticks, custom_name?, equipment?)` - спавна mob-ове
- `dash(strength)` - изстрелва играча в посоката, в която гледа
- `aoe_heal(radius, amount)` / `aoe_damage(radius, amount)` - area effects
- `set_velocity(x, y, z)` - knockback / launch
- `play_sound(sound, volume, pitch)`, `spawn_particle(type, count, ...)` - FX
- `send_message(text)` - chat съобщение
- `add_stat(key, amount)` / `increment_stat(key)` - tracking за GUI

**Налични conditions** (~13 типа):
- `from_behind` - удар от гърба (dot product на directions > 0.5)
- `hp_below_percent(target, percent)` / `hp_below_absolute(target, hp)`
- `cooldown(seconds)` - per-player cooldown trigger
- `is_sprinting(target)`, `is_crouching(target)`
- `item_in_hand(material)` - в main hand
- `target_team(team)` - hit-target е от определен team
- `time_of_day(start, end)` - day/night gate
- `random_chance(percent)` - probabilistic
- `attribute_above/below(target, attribute, value)`

**Шаблон за нов perk**:

```yaml
my_new_perk:
  name: "My New Perk"
  tier: GOLD                # SILVER / GOLD / PRISMATIC
  team: VAMPIRE             # HUMAN / VAMPIRE / BOTH
  icon: BLAZE_ROD           # Bukkit Material name
  description:
    - "First lore line"
    - "Second lore line"
  hooks:
    on_damage_dealt:
      - conditions:
          - type: from_behind
          - type: hp_below_percent
            target: VICTIM
            percent: 50
        actions:
          - type: multiply_damage
            factor: 2.0
          - type: apply_potion
            target: VICTIM
            potion: SLOWNESS
            duration_ticks: 60
            amplifier: 1
```

YAML anchor (`&name` / `*name`) се поддържа - ползвай го за да share-ваш един и
същ action list между `apply` и `on_tick` (например за perk който трябва да
re-apply enchant-а след respawn).

---

## Конфигурация

`config.yml` се парсва автоматично в `PluginConfig` POJO при boot.
`reloadPluginConfig()` препрочита от диск (lobby state only). Промени за
`perks.yml` все още изискват restart (планирана подобрение).

Ключови tunables:
- `game.*` - min players, duration, vampire ratio
- `economy.*` - passive income, kill/assist rewards
- `timings.*` - combat tag, blood compass unlock, free perk milestones, vampire
  leap cooldown (преди бяха hardcoded constants)
- `discord.*` - bot token, channel IDs, color hex codes
- `messages.*` - broadcast text за game start/win/death/phase change

---

## Тестове

`mvn test` стартира 97 теста (към v2.2.1):

- **Engine primitives** (43): per-action, per-condition, end-to-end
  `DataDrivenPerk` тестове, `PerkConfigLoader` schema validation.
- **Phase 2A split classes** (24): `TeamManagerTest`, `GameStateManagerTest`,
  `GameTimerManagerTest` - pure logic, използват Mockito за `BukkitTask` mocks.
- **PlayerStatsManager** (14): in-memory CRUD, top-N queries.
- **Discord** (16): `DiscordEmbedFactoryTest` (mm:ss формат, hex parsing) и
  `PresenceTextTest` (table-driven за всеки `GameState`).

**Защо не MockBukkit**: Опитахме, не пасва на нашия Java 21 + Paper 1.21 stack.
Vanilla JUnit + Mockito покриват достатъчно - pure-logic класовете се тестват
без Bukkit context, а end-to-end сценарии се тестват live на dev сървъра.

---

## Деплоймент

```bash
mvn clean package          # Build → target/VampireZ-2.2.1.jar (~34 MB)
                           # 97 теста минават; ако паднат, build fail
build-deploy-and-run.bat   # Copy jar → ../test-server/plugins/, start Paper
```

JAR-ът включва всички shaded библиотеки (JDA, EffectLib, triumph-gui, Cloud,
SQLite/Hikari, ConfigLib и т.н.) под `com.vampirez.lib.*`, така че плъгинът е
self-contained - никакви runtime depends.

GitHub releases включват jar-а директно като asset, така че сървърни админи
изтеглят и пускат - без manual build стъпка.

---

## Сървърна съвместимост

### Изисквано сървърно software и версии

| | Изискване | Защо |
|---|---|---|
| **Server software** | Paper 1.21.4+ | Спираме до Paper API. Spigot ще зареди jar-а, но Adventure API-та (използвани навсякъде) са Paper-only. На Spigot `Component`-ите ще се показват като raw JSON. Folia не е тестван (вероятно ще се счупи поради regional thread model и shared `joinedPlayers` set) |
| **Java** | Java 21 | `pom.xml` е compiler.source/target = 21. Switch expressions, records, pattern matching са навсякъде в кодовата база. Java 17 ще fail-не на компилация |
| **MC client версии** | 1.21+ (всичко което Paper 1.21.4 поддържа) | Boss bar API, registry-backed enums (Sound, Enchantment, Particle, Attribute) изискват 1.21+ |
| **Memory** | Препоръчано 2 GB heap minimum за 50-100 онлайн играча | `-Xms2G -Xmx4G` е добра starting точка. SQLite + HikariCP + JDA + EffectLib имат собствени buffer-и |

**Защо не Spigot**: Adventure API е Paper-exclusive. Component-ите минават през Bukkit's serializer на Spigot и се показват като escape JSON. Цялата плъгина използва Adventure (`player.sendMessage(Component)`, `meta.displayName(Component)`, etc.) - migrating обратно към `String` + `ChatColor` би бил undo на Phase 1B.

**Защо не Folia**: Folia дели worlds на regions, всеки със собствен thread. `GameManager.joinedPlayers` е shared mutable state, който в момента живее на main thread. За Folia compatibility трябва: (1) per-region task scheduling, (2) lock-free joinedPlayers (CHM), (3) audit на всеки `Bukkit.getScheduler()` call. Не е невъзможно, но е отделна Phase.

### Други плъгини - какво пасва и какво не

**Тествани и съвместими**:
- **EssentialsX** - не пипа inventory-та докато играч е в active VampireZ game, защото `PlayerStateManager` save-ва preserved state.
- **WorldGuard** - аркадният свят (`vampirez_arena_X`) може да има WG regions - flag-овете се подчиняват normal. Но не слагай `pvp deny` flag в arena-та или ще counter-нете game-а.
- **LuckPerms** - permission-ите `vampirez.admin` и `vampirez.play` работят normal. Cloud command framework чете permissions от `Player.hasPermission()` директно.
- **PlaceholderAPI** - VampireZ не expose-ва placeholder-и в момента (планирано за бъдеща версия).

**Известни конфликти**:
- **Други JDA-using plugins** (DiscordSRV, EssentialsXDiscord) - всеки JDA instance пуска отделен gateway connection. Multiple instance-и работят, но всеки ползва ~200 MB heap. Ако имаш DiscordSRV, можеш да изключиш VampireZ Discord (`discord.enabled: false`) и да слушаш VampireZ events директно от DiscordSRV's API.
- **Друга inventory-replace плъгина** (например custom shop GUI плъгини) - ако triggers на "open shop" с right-click emerald, ще конфликтнеш с VampireZ's perk shop. Workaround: дай permission node за тяхното GUI само на админи.
- **Anti-cheat** (NoCheatPlus, Vulcan) - ако имат "anti-fly" detection, vampire leap (Y velocity boost) може да trigger-не false positive. Whitelist VampireZ или disabl-ни anti-fly за vampire team UUIDs.

### Сървърни конфигурации

**Препоръчани `server.properties` settings**:
```
gamemode=survival
difficulty=hard
allow-flight=false           # vampire leap използва velocity, не fly
view-distance=10             # 16+ натоварва server-а с 50+ играчи
spawn-protection=0           # няма vanilla spawn protection в minigame
online-mode=true             # production, false е само за testing
```

**`paper-global.yml` препоръки**:
- `chunk-loading.autosave-interval`: 6000 (5 мин) - default-а 1500 (75 sec) save-ва прекалено често по време на game
- `unsupported-settings.allow-old-keybind-combos`: false

**Permissions setup** (LuckPerms пример):
```bash
/lp group default permission set vampirez.play true
/lp group admin permission set vampirez.admin true
```

**Arena world setup** (one-time):
1. Build твоята арена в creative режим в нов свят (например в localhost survival)
2. Stop server, copy world folder в `plugins/VampireZ/arena-template/`
3. Start server - VampireZ ще `arenaManager.loadArenaWorld()` и ще clone-ва template-а в `vampirez_arena_1` за всеки game
4. `/vz setlobby`, `/vz sethumanspawn`, `/vz setvampspawn` за да set-неш точките

---

## Outbound интеграции

VampireZ говори към няколко external системи. Ето как и защо:

### Discord (опционално)

**Кога**: Само ако `discord.enabled: true`. Цялата `com.vampirez.discord` package е optional - `if (pluginConfig.discord.enabled) { ... new DiscordBot(this).startAsync(); }` гард-нат блок в `onEnable`.

**Какво прие в Discord**:
- Game start embed (с full team roster)
- Game end embed (с winner + duration)
- (Опционално) Conversion announcements
- (Опционално) Day/night phase change announcements
- Live status embed - **един** message в configured channel, edited every 10s по време на ACTIVE
- Bot presence text - update every 30s

**Какво НЕ прави**: Не приема commands от Discord (нямаме slash commands). Не slack-ва player chat към Discord. Не synchronize-ва player roles/permissions от Discord.

**Конфигурация**: 12 полета в `discord:` секцията на `config.yml`:
- `enabled` - master switch
- `token` - bot token (КЕЙТ TI SECRET!)
- `status-channel-id`, `announce-channel-id` - Discord channel snowflakes
- `presence-update-seconds` (min 15), `status-embed-update-seconds` (min 5)
- `announce-conversions`, `announce-day-night` - boolean toggles
- `color-lobby`, `color-active`, `color-ended` - hex цветове за embed colors
- `status-message-id` - bot-managed, persisted (за да оцелее restart)

### SQLite база данни

**Кога**: Винаги (вътрешна, не optional).

**Какво пишем**: `player_stats` таблица с колони `uuid`, `name`, `kills`, `wins`, `losses`. Schema е версионирана чрез `IF NOT EXISTS` и индексирана по `kills DESC` + `wins DESC` за бързи top-N queries за leaderboard-а.

**Файл**: `plugins/VampireZ/vampirez.db` (SQLite WAL mode - има `.db-shm` + `.db-wal` companion файлове).

**Достъп от външни tool-и**: SQLite файлът може да се чете със стандартен `sqlite3` CLI или с DB Browser. Read-only достъп е safe докато plugin-ът работи (WAL mode позволява paralleren read).

**Backup стратегия**: Спри сървъра преди да copy-ваш `.db` файла. WAL файлът съдържа uncommitted данни.

### Bukkit ServicesManager

**Какво регистрираме**: `VampireZAPI` interface е публикуван в `Bukkit.getServicesManager()` с `ServicePriority.Normal`. Други плъгини могат да я consume-ват:

```java
// От external plugin:
RegisteredServiceProvider<VampireZAPI> rsp =
    Bukkit.getServicesManager().getRegistration(VampireZAPI.class);
if (rsp != null) {
    VampireZAPI api = rsp.getProvider();
    api.givePerk(playerUuid, "blunt_force");
    int gold = api.getGold(playerUuid);
}
```

Същото може да се прави и през `VampireZPlugin.getAPI()` static accessor.

**Events за консумация**: 7 публични event-а в `com.vampirez.api.event`:
- `VampireZGameStartEvent`, `VampireZGameEndEvent`
- `PlayerConvertedEvent`, `PlayerVampireRespawnEvent`
- `PlayerPerkGainedEvent`, `PlayerPerkLostEvent`
- `DayPhaseChangeEvent`

External plugin може да register-ва Bukkit listener-и за тях по стандартния начин (`@EventHandler public void on(VampireZGameStartEvent e)`).

### Файлова система

VampireZ пише в `plugins/VampireZ/`:
- `config.yml` - main config (managed от ConfigLib)
- `perks.yml` - data-driven perk definitions (saved on first boot, not auto-overwritten)
- `vampirez.db` + WAL companions - SQLite database
- `perk-stats.yml` - perk pick frequency tracking (legacy YAML, not migrated yet)
- `player-stats.yml.migrated` - стария YAML player stats после миграция към SQLite
- `saved-states/<uuid>.yml` - inventory snapshots на joined играчи (restored on leave)
- `arena-template/` - source folder за clone-ване
- `vampirez_arena_N/` - active arena instance (живее в `serverDir`, не в plugin folder)

### Логване

Per-class SLF4J loggers (Phase 6B). Log lines имат form-а:
```
[com.vampirez.ArenaManager] Arena world 'vampirez_arena_1' loaded.
[com.vampirez.discord.DiscordBot] Discord bot connected as VampireZ Bot#6896
[com.vampirez.lib.hikari.HikariDataSource] VampireZ-SQLite - Start completed.
```

Class-name prefix-ите правят filtering лесно - `grep "discord"` показва само Discord-related логове. Bukkit's plugin logger (`getLogger()`) се използва само за plugin lifecycle messages (enable/disable).

---


