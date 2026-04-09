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

1. Join the server and set spawn points:
   - `/vz setlobby` - where players wait
   - `/vz sethumanspawn` - where humans spawn
   - `/vz setvampspawn` - where vampires spawn
2. Start a game with `/vz start` (requires `vampirez.admin` permission)
3. Minimum 10 players by default (use `/vz forcestart` to bypass)

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/vz help` | -- | Show command list |
| `/vz shop` | -- | Open perk shop |
| `/vz perks` | -- | List your active perks |
| `/vz gold` | -- | Show gold balance |
| `/vz status` | -- | Game state and team counts |
| `/vz start` | `vampirez.admin` | Start game |
| `/vz forcestart` | `vampirez.admin` | Start ignoring player count |
| `/vz stop` | `vampirez.admin` | Stop running game |
| `/vz setlobby` | `vampirez.admin` | Set lobby spawn |
| `/vz sethumanspawn` | `vampirez.admin` | Set human spawn |
| `/vz setvampspawn` | `vampirez.admin` | Set vampire spawn |
| `/vz test` | `vampirez.admin` | Open perk test menu |
| `/vz reload` | `vampirez.admin` | Reload config |

## Game Flow

1. **Lobby** - Players join and wait for minimum player count
2. **Starting** (15s countdown) - Teams assigned (30% vampires), teleported to spawns
3. **Active** (25 minutes) - Fight! Humans earn gold, buy perks, survive. Dead humans become vampires.
4. **Win** - Humans survive the timer, or Vampires convert everyone

## Configuration

All settings are in `plugins/VampireZ/config.yml` after first run. Key settings:

- `game.min-players` - Minimum players to start (default: 10)
- `game.game-duration-seconds` - Game length (default: 1500 = 25 min)
- `game.vampire-ratio` - Fraction that start as vampires (default: 0.3)
- `economy.kill-reward` / `economy.assist-reward` - Gold rewards
- `perks.max-perks-per-player` - Perk limit (default: 4)

## Requirements

- Spigot or Paper 1.20.1+
- Java 17+

## License

This project is open source. Feel free to use, modify, and learn from it.
