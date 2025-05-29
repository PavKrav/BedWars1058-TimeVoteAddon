# ⏰ TimeVoteAddon

**TimeVoteAddon** is a simple and convenient plugin for [BedWars1058](https://www.spigotmc.org/resources/bedwars1058.97376/) that allows players to vote for the time of day before the game starts.  
Enhance the gameplay by letting participants choose between **morning**, **day**, **evening**, or **night** for their arena!

---

## ✨ Features

- 🕐 Voting is available in the arena lobby during the `waiting` and `starting` game states.
- ⏳ Players receive **special clocks** — interactive items for voting.
- 🖱️ User-friendly **GUI menu** to select the time of day.
- 🗳️ Votes are counted and the **winning time** is automatically applied when the game begins.
- 🧹 Automatic cleanup after the game starts or when players leave.
- 🏟️ Supports **multiple arenas** simultaneously.

---

## 🌅 Time Options

- 🌄 **Morning** (6:00)
- ☀️ **Day** (12:00)
- 🌇 **Evening** (18:00)
- 🌙 **Night** (0:00)

---

## 🛠 Requirements

- ✅ [BedWars1058](https://www.spigotmc.org/resources/bedwars1058.97376/)
- ✅ Minecraft server with **Bukkit**, **Spigot**, or **Paper** support

---

## 🔧 Setup

1. Move the plugin `.jar` file into your server’s `plugins` folder.
2. Add the following to your BedWars1058 `config.yml`:

```yaml
pre-game-items:
  clock:
    command: vote
    material: CLOCK
    data: 0
    enchanted: true
    slot: 2
