# CCWeapons

![Paper 1.21.11](https://img.shields.io/badge/Paper-1.21.11-blue)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![License: ARR](https://img.shields.io/badge/license-All%20Rights%20Reserved-red)

CCWeapons adds 19 custom weapons to your Paper server. Every weapon has its own ability, its own rules and its own crafting recipe, so they feel different from vanilla gear and from each other. Progress like souls, charges and levels is stored inside the item itself.

**Everything is configurable.** Every weapon can be disabled completely, and every crafting recipe can be changed or disabled on its own, all in one simple `config.yml`.

## Installation

1. Make sure your server runs **Paper 1.21.11** with **Java 21**.
2. Download the latest `CCWeapons-*.jar` from the [Releases](../../releases) page or from [Modrinth](https://modrinth.com/plugin/ccweapons).
3. Put the jar into your server's `plugins` folder.
4. Start the server. A `config.yml` is generated in `plugins/CCWeapons/`.
5. Edit the config if you like and run `/ccweapons reload`.

## Commands and features

| Command | Description | Permission |
|---|---|---|
| `/ccweapons` | Shows the command help | none |
| `/ccweapons guide` | Opens the in-game weapon guide (click a weapon to see its compatible enchantments) | none |
| `/ccweapons enchantments <weapon>` | Lists the compatible enchantments of a weapon | none |
| `/ccweapons give <player> <weapon> [amount]` | Gives a weapon to a player (up to 64) | `ccweapons.admin.give` |
| `/ccweapons reload` | Reloads the config and all recipes | `ccweapons.admin.reload` |

Aliases: `/weapons` and `/ccw`. The permission `ccweapons.admin` includes both admin permissions and is given to operators by default.

**Features**

- In-game weapon guide with a 54-slot GUI
- Disable any weapon completely in the config
- Change or disable any crafting recipe separately (shape, ingredients and amount)
- Reload everything with one command, no restart needed
- Ability status (cooldowns, charges, souls) is shown in the action bar or on the item
- Weapons can only receive enchantments that fit them, and they cannot be upgraded at the smithing table
- Weapons are recognised by item data, so renaming them in an anvil does not break them

## Weapons

| Weapon | Base item | Stats | Ability |
|---|---|---|---|
| Legacy Wooden / Stone / Iron / Golden / Diamond Sword | matching sword | 4 / 5 / 6 / 4 / 7 damage, no attack cooldown | Combat like Minecraft 1.8. Hold right-click to block (50% less damage from melee, projectiles and explosions). Sweep attacks are disabled. Sharpness adds 1.25 damage per level, Smite 2.5 against undead, Bane of Arthropods 2.5 against arthropods and slows them. |
| Vampiric Mace | mace | 5 damage, 0.6 attack speed | Heals you for 20% of the damage you deal. Every player kill gives +1 permanent heart (max health up to 1024). All bonus hearts are lost when you die. |
| Vampiric Netherite Sword | netherite sword | 7 damage, 1.6 attack speed | Same as Vampiric Mace |
| Vampiric Netherite Axe | netherite axe | 9 damage, 1.0 attack speed | Same as Vampiric Mace |
| Cursed Dagger | iron sword | exactly 4 damage, 2.4 attack speed, 2 blocks reach | Ignores armor, shields and defensive effects, and the damage can never be increased or reduced. Only the blocking of a Legacy Sword still works against it. Damage enchantments are disabled. |
| Evolving Sword | netherite sword | starts at 2 damage, 1.6 attack speed | Gains weapon XP when you hit something: 5 XP for players, 1 XP for other living entities. Each level adds +0.5 damage, up to level 128. A level needs 50 XP plus 25 more for every level already reached. |
| Mobility Dagger | iron sword | 6 damage, 2.0 attack speed | Speed IV while held. Right-click to teleport 4 blocks forward (6 second cooldown). Fails if the destination is blocked. |
| Teleport Bow | bow | normal bow | Teleports you to where the arrow lands, on blocks and on entities. Needs enough free space at the destination. |
| Wind Charge Axe | diamond axe | 7 damage, 1.0 attack speed | Right-click to fire a wind charge. Holds up to 64 charges and restores 1 per second, even while it is anywhere in your inventory. |
| Dash Sword | diamond sword | normal diamond sword | Right-click to dash in the direction you are looking. Holds up to 3 dashes and restores 1 every 4 seconds, even while it is anywhere in your inventory. Dashing resets fall damage. |
| Frost Scythe | netherite hoe | normal netherite hoe | Hits apply Slowness for 2 seconds. Right-click to release a frost wave up to 10 blocks forward that applies Slowness IV and prevents jumping for 2 seconds. Holds 2 charges and restores 1 every 8 seconds, even while it is anywhere in your inventory. |
| Thunder Hammer | mace | normal mace | Every successful hit grants 1 charge. At 5 charges, right-click to strike lightning at the block or enemy you are aiming at (up to 30 blocks). It deals 8 damage and knocks back everything within 5 blocks, except you. |
| Gravity Mace | mace | normal mace | Right-click to pull all living entities within 7 blocks towards you and launch them into the air. Sneak and right-click to push them away instead. Grants brief Resistance II. 24 second cooldown. |
| Soul Reaper | netherite hoe | 8 damage, 0.8 attack speed | Collects souls: 1 for creature kills, 5 for player kills, up to 120. Right-click spends 10 souls on a shockwave (6 blocks, 6 damage, Slowness II), sneak and right-click spends 20 souls on a strong one (9 blocks, 10 damage, Slowness IV). 1 second cooldown. At 25 souls: 12 damage. At 50 souls: 10% life steal. At 100 souls: sword-like attack speed. |
| Blood Pact Blade | netherite sword | normal netherite sword | Right-click to sacrifice 8 hearts and gain Strength VI for 30 seconds. You need more than 8 hearts, so the ability can never kill you. 60 second cooldown. |

## Configuration

Everything is set in `config.yml`. For every weapon you can set:

- `enabled`: turns the whole weapon on or off. A disabled weapon cannot be crafted or given, is hidden in the guide, and its abilities stop working on items that already exist.
- `recipe.enabled`: turns only the crafting recipe on or off, the weapon itself stays available through `/ccweapons give`.
- `recipe.shape`, `recipe.ingredients` and `recipe.amount`: change how the weapon is crafted and how many you get.

After editing the file, run `/ccweapons reload`.

<details>
<summary>Default crafting recipes</summary>

All recipes are shaped (3x3 grid) and every one of them needs a Nether Star. The exact layout is in `config.yml`.

| Weapon | Ingredients |
|---|---|
| Legacy Wooden / Stone / Iron / Golden / Diamond Sword | 2 oak planks / cobblestone / iron ingots / gold ingots / diamonds, 1 Nether Star |
| Vampiric Mace | Heavy Core, Breeze Rod, Netherite Ingot, 2 Nether Stars |
| Vampiric Netherite Sword | 2 Netherite Ingots, Stick, 2 Nether Stars |
| Vampiric Netherite Axe | 3 Netherite Ingots, 2 Sticks, 2 Nether Stars |
| Cursed Dagger | Dragon Head, 2 Enchanting Tables, Iron Ingot, Stick, 2 Nether Stars |
| Evolving Sword | 2 Experience Bottles, Netherite Ingot, Stick, Nether Star |
| Mobility Dagger | Ender Pearl, 2 Iron Ingots, Breeze Rod, Nether Star |
| Teleport Bow | 3 Ender Pearls, 3 Strings, 2 Nether Stars |
| Wind Charge Axe | 2 Diamonds, 2 Wind Charges, 2 Breeze Rods, Nether Star |
| Dash Sword | 3 Breeze Rods, Ender Pearl, 2 Netherite Ingots, Nether Star |
| Frost Scythe | 2 Packed Ice, 2 Blue Ice, Netherite Ingot, Nether Star |
| Thunder Hammer | 2 Lightning Rods, Mace, Breeze Rod, Nether Star |
| Gravity Mace | 2 Ender Pearls, Mace, Obsidian, Nether Star |
| Soul Reaper | 2 Soul Sand, 2 Netherite Ingots, Bone, Nether Star |
| Blood Pact Blade | 4 Redstone Blocks, Netherite Ingot, Blaze Rod, Nether Star |

</details>

## Addon API

CCWeapons can be extended by other plugins. The full documentation is in [ADDON_API.md](ADDON_API.md). The `standartaddon` folder contains a complete example addon.

CCWeapons is also available on [Modrinth](https://modrinth.com/plugin/ccweapons).

## Requirements

- Paper 1.21.11
- Java 21

## License

Copyright © LAPDK66. **All Rights Reserved.**

This project is source-visible for reference only. You may not copy, modify, redistribute or sell the code, the plugin or any part of it without explicit written permission from the author. Using the compiled plugin on your own server is allowed.

**Exception:** the files in the `standartaddon` folder may be used, copied and modified freely to write your own addon for CCWeapons.

<details>
<summary>AI usage disclosure (click to open)</summary>

In the interest of transparency, here is where generative AI was used in this project:

- **Project description:** this page was written with the help of an AI assistant.
- **Configuration file:** the structure and the comments of `config.yml` were created with the help of an AI assistant.
- **In-game messages:** most of the messages shown in game (chat and action bar messages, menu texts and item descriptions) were written with the help of an AI assistant.

</details>
