# CCWeapons StandartAddon

This folder contains a complete example Paper plugin using the CCWeapons API.
Every weapon has its own class below `weapon/impl`, while recipes and enabled
states are stored in this addon's own `config.yml`.

Included examples:

- `PulseBladeWeapon`: a left-click ability with a cooldown and a restricted
  enchantment list.
- `EmberAxeWeapon`: an on-hit ability with a separate restricted enchantment
  list.

## Build

First install the main plugin into the local Maven repository from the project
root:

```text
mvn clean install
```

Then build this addon:

```text
cd standartaddon
mvn clean package
```

Copy both generated JAR files into the server's `plugins` folder. The addon
declares `depend: [CCWeapons]`, so Paper loads the main plugin first.

After editing this addon's config while the server is running, reload the
server or call `CCWeaponsApi#reloadAddon(...)` from an addon command. The main
plugin's `/ccweapons reload` command reloads the main plugin config only.