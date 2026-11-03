# CCWeapons Addon API

An addon is a normal Paper plugin that declares `CCWeapons` as a dependency.
It can subclass `CustomWeapon` and register the instance through the public
`CCWeaponsApi`. Registered weapons are automatically included in combat event
handling, commands, the paginated guide, recipe views, and enchantment views.

## 1. Add CCWeapons as a build dependency

Install this project in your local Maven repository with `mvn install`, then
add the following dependency to the addon's `pom.xml`:

```xml
<dependency>
    <groupId>dev.lap</groupId>
    <artifactId>CCWeapons</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

The addon must also use Paper 1.21.11 and Java 21, matching CCWeapons.

## 2. Declare the runtime dependency

The addon's `plugin.yml` must contain `depend: [CCWeapons]`. This guarantees
that the API is available when the addon is enabled.

```yaml
name: ExampleWeaponsAddon
version: 1.0.0
main: example.weapons.ExampleWeaponsAddon
api-version: '1.21.11'
depend:
  - CCWeapons
```

## 3. Put recipes in the addon's own config

The weapon ID in the config must exactly match `CustomWeapon#getId()`:

```yaml
weapons:
  emerald_blade:
    enabled: true
    recipe:
      enabled: true
      amount: 1
      shape:
        - " E "
        - " E "
        - " S "
      ingredients:
        E: EMERALD
        S: STICK
```

## 4. Implement and register the weapon

```java
package example.weapons;

import dev.lap.cCWeapons.api.CCWeaponsApi;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class ExampleWeaponsAddon extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();

        RegisteredServiceProvider<CCWeaponsApi> registration =
                Bukkit.getServicesManager().getRegistration(
                        CCWeaponsApi.class
                );

        if (registration == null) {
            throw new IllegalStateException("CCWeapons API is unavailable.");
        }

        registration.getProvider().registerWeapon(
                this,
                new EmeraldBlade()
        );
    }

    private static final class EmeraldBlade extends CustomWeapon {

        private final List<Enchantment> compatibleEnchantments;

        private EmeraldBlade() {
            super(
                    "emerald_blade",
                    "Emerald Blade",
                    Material.DIAMOND_SWORD,
                    List.of("An example weapon supplied by an addon.")
            );

            ItemStack vanillaSword = new ItemStack(Material.DIAMOND_SWORD);
            List<Enchantment> enchantments = new ArrayList<>();
            for (Enchantment enchantment : Registry.ENCHANTMENT) {
                if (enchantment.canEnchantItem(vanillaSword)) {
                    enchantments.add(enchantment);
                }
            }
            compatibleEnchantments = List.copyOf(enchantments);
        }

        @Override
        public List<Enchantment> getCompatibleEnchantments() {
            return compatibleEnchantments;
        }
    }
}
```

All hooks exposed by `CustomWeapon` (`onHit`, `onLeftClick`, `onRightClick`,
`onProjectileShot`, tick hooks, and others) work for addon weapons as well.
CCWeapons automatically removes addon-owned weapons when their plugin is
disabled. An addon can call `CCWeaponsApi#reloadAddon(this)` after changing its
configuration to reload its own recipes and enabled flags.

## 5. `CustomWeapon` hook overview

Every addon weapon extends `CustomWeapon`. Override only the hooks the weapon
actually needs. All default implementations do nothing, so existing and simple
weapons do not need to implement every method.

| Method | Called when | Typical use |
| --- | --- | --- |
| `usesLeftClickAbility()` | CCWeapons checks a left-click | Declare an active left-click ability |
| `onLeftClick(Player)` | Main-hand left-click on air or a block | Simple left-click ability |
| `onLeftClick(Player, PlayerInteractEvent)` | Main-hand left-click on air or a block | Read the clicked block/action or modify the event |
| `usesRightClickAbility()` | CCWeapons checks a right-click | Declare an active right-click ability |
| `onRightClick(Player)` | Main-hand right-click on air, a block, or an entity | One shared right-click ability |
| `onRightClick(Player, PlayerInteractEvent)` | Right-click on air or a block | Read the clicked block/action or modify the event |
| `onRightClick(Player, PlayerInteractEntityEvent)` | Right-click on an entity | Access the clicked entity |
| `onHit(Player, LivingEntity, EntityDamageByEntityEvent)` | Direct melee hit with the weapon | Damage changes and on-hit effects |
| `onPlayerKill(...)` | A player is killed by a recognized melee hit | Player-specific kill effects |
| `onEntityKill(...)` | A non-player living entity is killed by a recognized melee hit | Mob kill effects |
| `onHeldTick(Player)` | Every server tick while held in the main hand | Continuous held effects |
| `onInventoryTick(Player)` | Every 10 ticks while at least one copy is in the inventory | One effect per weapon type/player |
| `onInventoryItemTick(Player, ItemStack)` | Every 10 ticks for every individual copy in the inventory | Per-item persistent data/state |
| `onInventorySlotChange(Player, PlayerInventorySlotChangeEvent)` | A slot changes and its new item is this weapon | React to item movement/replacement |
| `onProjectileShot(Player, Projectile, EntityShootBowEvent)` | A projectile is fired with the weapon | Configure and mark the projectile |
| `onProjectileHit(ProjectileHitEvent)` | A correctly marked weapon projectile hits | Impact abilities |
| `getCompatibleEnchantments()` | Guide, enchanting, and anvil validation | Allow only selected enchantments |
| `getEnchantmentHints()` | Text/console enchantment information | Add explanatory enchantment text |
| `configureItem(CCWeapons, ItemMeta)` | A new weapon item is created | Attributes, model data, flags, static enchantments |
| `configureItemStack(CCWeapons, ItemStack)` | After the completed item meta was applied | Paper data components and stack-level properties |

The sections below explain each hook and its parameters in more detail.

### Which method should I use?

The following quick guide maps common weapon features to the method that should
normally be overridden:

| I want the weapon to... | Override | Also required |
| --- | --- | --- |
| perform a simple ability when left-clicking | `onLeftClick(Player)` | `usesLeftClickAbility()` must return `true` |
| inspect the clicked block during a left-click | `onLeftClick(Player, PlayerInteractEvent)` | `usesLeftClickAbility()` must return `true` |
| perform the same ability for every right-click target | `onRightClick(Player)` | `usesRightClickAbility()` must return `true` |
| distinguish right-clicking air/blocks from entities | both typed `onRightClick(...)` overloads as needed | `usesRightClickAbility()` must return `true` |
| add an effect when directly hitting an entity | `onHit(...)` | nothing else |
| react after killing a player | one `onPlayerKill(...)` overload | nothing else |
| react after killing a mob | one `onEntityKill(...)` overload | nothing else |
| apply an effect only while held | `onHeldTick(Player)` | keep the method lightweight |
| apply one passive effect while present in inventory | `onInventoryTick(Player)` | nothing else |
| store or update data on each individual weapon copy | `onInventoryItemTick(Player, ItemStack)` | use the supplied `ItemStack` |
| react immediately when the weapon enters/changes a slot | `onInventorySlotChange(...)` | nothing else |
| modify an arrow or another fired projectile | `onProjectileShot(...)` | the base material must actually fire a projectile |
| react when that projectile lands | `onProjectileHit(...)` | mark it in `onProjectileShot(...)` as shown in section 10 |
| allow only selected enchantments | `getCompatibleEnchantments()` | return exactly the allowed enchantments |
| add permanent item attributes or model data | `configureItem(...)` | nothing else |
| modify the final completed item stack | `configureItemStack(...)` | use only when `ItemMeta` is insufficient |

These methods are callbacks: CCWeapons invokes them automatically for every
registered and enabled weapon. The addon does **not** need to register its own
Bukkit listener for left-clicks, right-clicks, hits, kills, inventory ticks, or
projectile events. A separate listener is only needed for behavior that no
`CustomWeapon` hook covers.

## 6. Left-click abilities

### Simple left-click hook

Use these two methods for an ability that does not need information about the
clicked block:

```java
@Override
public boolean usesLeftClickAbility() {
    return true;
}

@Override
public void onLeftClick(Player player) {
    player.setVelocity(player.getLocation().getDirection().multiply(1.2));
}
```

`usesLeftClickAbility()` should return `true` for an active left-click ability.
CCWeapons then cancels the normal left-click interaction after invoking the
hook. The hook is called only for the main hand and for:

- `LEFT_CLICK_AIR`
- `LEFT_CLICK_BLOCK`

### Left-click hook with the Bukkit event

Use the event overload when the ability needs the clicked block, the action,
the held item, or direct control over the event:

```java
@Override
public boolean usesLeftClickAbility() {
    return true;
}

@Override
public void onLeftClick(
        Player player,
        PlayerInteractEvent event
) {
    if (event.getClickedBlock() != null) {
        player.sendMessage(
                "Clicked: " + event.getClickedBlock().getType()
        );
    }
}
```

The default implementation of the event overload calls
`onLeftClick(Player)`. Therefore:

- override `onLeftClick(Player)` for one simple implementation;
- override `onLeftClick(Player, PlayerInteractEvent)` when the event is needed;
- do not normally override both;
- if both are overridden, call `super.onLeftClick(player, event)` from the
  event overload if the simple method should run too.

The `PulseBladeWeapon` in `standartaddon` is a complete left-click example with
a cooldown, particles, sound, and a potion effect.

## 7. Right-click abilities

### One ability for air, blocks, and entities

The simple method receives all supported right-click types:

```java
@Override
public boolean usesRightClickAbility() {
    return true;
}

@Override
public void onRightClick(Player player) {
    player.getWorld().strikeLightningEffect(player.getTargetBlock(null, 30)
            .getLocation());
}
```

Returning `true` from `usesRightClickAbility()` tells CCWeapons this is an
active ability. For block interactions, CCWeapons denies use of the clicked
block and allows use of the weapon in hand. For entity interactions, the normal
entity interaction is cancelled after the hook runs.

Only main-hand interactions are forwarded. This prevents an ability from
running twice because of main-hand and off-hand events.

### Separate behavior for air/blocks and entities

Use the typed overloads to distinguish interaction targets:

```java
@Override
public boolean usesRightClickAbility() {
    return true;
}

@Override
public void onRightClick(
        Player player,
        PlayerInteractEvent event
) {
    // RIGHT_CLICK_AIR or RIGHT_CLICK_BLOCK
    if (event.getClickedBlock() != null) {
        player.sendMessage("You clicked a block.");
    }
}

@Override
public void onRightClick(
        Player player,
        PlayerInteractEntityEvent event
) {
    // The exact clicked entity is available here.
    player.sendMessage("Target: " + event.getRightClicked().getName());
}
```

Both typed default implementations delegate to `onRightClick(Player)`. The
same override rule as for left-click applies: use the simple method for shared
behavior or the event methods for specialized behavior. If a typed override
should also invoke the simple implementation, explicitly call `super`.

Useful `PlayerInteractEvent` methods include:

- `getAction()`
- `getClickedBlock()`
- `getBlockFace()`
- `getItem()`
- `setCancelled(boolean)`

Useful `PlayerInteractEntityEvent` methods include:

- `getRightClicked()`
- `getHand()`
- `setCancelled(boolean)`

## 8. Melee hit and kill hooks

### `onHit`

This hook runs when a player directly damages a `LivingEntity` with the weapon
in the main hand:

```java
@Override
public void onHit(
        Player player,
        LivingEntity target,
        EntityDamageByEntityEvent event
) {
    // Add two points (one heart) of damage.
    event.setDamage(event.getDamage() + 2.0);

    // Apply an additional effect to the target.
    target.setFreezeTicks(Math.max(target.getFreezeTicks(), 60));
}
```

Parameters:

- `player`: the attacking player;
- `target`: the living entity that was hit;
- `event`: Bukkit's complete damage event. It can be cancelled and its damage
  can be read or changed.

This hook is intended for direct melee attacks. Projectile damage and sweep
attack damage are not forwarded through `onHit`. The `EmberAxeWeapon` in
`standartaddon` is a complete on-hit example.

### Player kill hooks

Use the short version when only killer and victim are required:

```java
@Override
public void onPlayerKill(Player killer, Player victim) {
    killer.setHealth(Math.min(
            killer.getAttribute(Attribute.MAX_HEALTH).getValue(),
            killer.getHealth() + 4.0
    ));
}
```

Use the detailed version when data from the exact weapon item is required:

```java
@Override
public void onPlayerKill(
        Player killer,
        Player victim,
        ItemStack weaponItem
) {
    Damageable damageable = (Damageable) weaponItem.getItemMeta();
    damageable.setDamage(Math.max(0, damageable.getDamage() - 10));
    weaponItem.setItemMeta(damageable);
}
```

Available signatures:

```java
public void onPlayerKill(Player killer, Player victim)

public void onPlayerKill(
        Player killer,
        Player victim,
        ItemStack weaponItem
)
```

The detailed default implementation calls the short method. Override one of
the two variants, not both, unless that delegation is intentionally handled.

### Non-player entity kill hooks

The same two variants exist for mobs and other non-player living entities:

```java
@Override
public void onEntityKill(
        Player killer,
        LivingEntity victim,
        ItemStack weaponItem
) {
    killer.giveExp(3);
}
```

Available signatures:

```java
public void onEntityKill(Player killer, LivingEntity victim)

public void onEntityKill(
        Player killer,
        LivingEntity victim,
        ItemStack weaponItem
)
```

Kill hooks are associated with a recognized, fatal direct melee hit. To avoid
applying an effect to the wrong item, the player must still have the same
weapon in the same hotbar slot when the victim's death is processed.

## 9. Held and inventory hooks

### `onHeldTick(Player)`

Runs every server tick while the weapon is in the player's main hand. There
are normally 20 ticks per second.

```java
@Override
public void onHeldTick(Player player) {
    player.addPotionEffect(new PotionEffect(
            PotionEffectType.NIGHT_VISION,
            40,
            0,
            false,
            false,
            true
    ));
}
```

Use this for effects that must exist only while actively holding the weapon.
Keep this method lightweight because it can run 20 times per second for every
online player holding that weapon.

### `onInventoryTick(Player)`

Runs every 10 ticks (twice per second) if the player has at least one copy of
this weapon ID anywhere in the inventory. It runs only once per player and
weapon ID, even if the player owns multiple copies.

```java
@Override
public void onInventoryTick(Player player) {
    player.addPotionEffect(new PotionEffect(
            PotionEffectType.LUCK,
            30,
            0,
            false,
            false,
            true
    ));
}
```

Use it for one player-wide passive effect that should not become stronger when
multiple copies exist.

### `onInventoryItemTick(Player, ItemStack)`

Also runs every 10 ticks, but once for every individual weapon item in the
player's inventory:

```java
@Override
public void onInventoryItemTick(Player player, ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    meta.getPersistentDataContainer().set(
            chargeKey,
            PersistentDataType.INTEGER,
            100
    );
    item.setItemMeta(meta);
}
```

Use this variant for state stored in each specific `ItemStack`, such as charge,
level, collected souls, or a per-item cooldown. Do not use it for effects that
should run only once per player; use `onInventoryTick` for those.

### `onInventorySlotChange`

This Paper-specific hook is invoked when an inventory slot changes and the new
item in that slot is this weapon:

```java
@Override
public void onInventorySlotChange(
        Player player,
        PlayerInventorySlotChangeEvent event
) {
    player.sendActionBar(Component.text("Weapon inventory state changed"));
}
```

The event provides the changed slot and its old/new item state. Use it when an
effect should react immediately instead of waiting for the next inventory tick.

## 10. Projectile hooks

### `onProjectileShot`

Runs after a player fires a projectile using this custom weapon. It is useful
for changing velocity, damage, fire ticks, gravity, or other projectile data:

```java
@Override
public void onProjectileShot(
        Player player,
        Projectile projectile,
        EntityShootBowEvent event
) {
    projectile.setVelocity(projectile.getVelocity().multiply(1.5));
}
```

Parameters:

- `player`: the shooter;
- `projectile`: the spawned projectile entity;
- `event`: the original shoot event, including bow, consumable, and force.

### Connecting a shot to `onProjectileHit`

CCWeapons must be able to identify which weapon created a projectile. An addon
weapon that needs `onProjectileHit` must mark the projectile in
`onProjectileShot` with the CCWeapons key and its weapon ID.

Pass the main `CCWeapons` instance into the weapon constructor:

```java
CCWeapons ccWeapons = JavaPlugin.getPlugin(CCWeapons.class);
api.registerWeapon(this, new ExampleBowWeapon(ccWeapons));
```

Then mark the projectile:

```java
public final class ExampleBowWeapon extends CustomWeapon {

    private final CCWeapons ccWeapons;

    public ExampleBowWeapon(CCWeapons ccWeapons) {
        super(
                "example_bow",
                "Example Bow",
                Material.BOW,
                List.of("Creates an explosion effect on impact.")
        );
        this.ccWeapons = ccWeapons;
    }

    @Override
    public void onProjectileShot(
            Player player,
            Projectile projectile,
            EntityShootBowEvent event
    ) {
        projectile.getPersistentDataContainer().set(
                ccWeapons.getWeaponKey(),
                PersistentDataType.STRING,
                getId()
        );
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();

        projectile.getWorld().createExplosion(
                projectile.getLocation(),
                1.5F,
                false,
                false
        );
        projectile.remove();
    }
}
```

Without the persistent-data marker, `onProjectileHit` is not called because
CCWeapons cannot associate the projectile with the registered weapon.

Useful `ProjectileHitEvent` methods include:

- `getEntity()` for the projectile;
- `getHitEntity()` for an entity impact;
- `getHitBlock()` for a block impact;
- `getHitBlockFace()` for the impacted block face.

## 11. Compatible enchantments

Return the exact enchantments accepted by the weapon:

```java
private static final List<Enchantment> COMPATIBLE_ENCHANTMENTS = List.of(
        Enchantment.SHARPNESS,
        Enchantment.UNBREAKING,
        Enchantment.MENDING
);

@Override
public List<Enchantment> getCompatibleEnchantments() {
    return COMPATIBLE_ENCHANTMENTS;
}
```

This list is used by:

- the right-click enchantment page in the guide;
- enchanting-table result filtering;
- anvil result validation;
- `isEnchantmentAllowed(Enchantment)`.

Returning an empty list means the weapon accepts no enchantments. Return an
immutable list (`List.of(...)` or `List.copyOf(...)`) so it cannot accidentally
be changed at runtime.

To allow every normal enchantment compatible with a base material, inspect the
enchantment registry:

```java
private static List<Enchantment> findEnchantments(Material material) {
    ItemStack baseItem = new ItemStack(material);
    List<Enchantment> result = new ArrayList<>();

    for (Enchantment enchantment : Registry.ENCHANTMENT) {
        if (enchantment.canEnchantItem(baseItem)) {
            result.add(enchantment);
        }
    }

    return List.copyOf(result);
}
```

Optional text descriptions can be supplied separately:

```java
@Override
public List<String> getEnchantmentHints() {
    return List.of(
            "Sharpness I-V - Increases melee damage.",
            "Unbreaking I-III - Reduces durability loss.",
            "Mending I - Repairs the weapon with experience."
    );
}
```

`getEnchantmentHints()` supplies human-readable text for text/console output.
The graphical guide derives its book entries directly from
`getCompatibleEnchantments()`.

Do not override `isEnchantmentAllowed(Enchantment)`: it is final and checks
whether the enchantment exists in `getCompatibleEnchantments()`.

## 12. Configuring the generated weapon item

### `configureItem(CCWeapons, ItemMeta)`

Override this protected hook to configure permanent item meta whenever
CCWeapons creates the weapon item:

```java
@Override
protected void configureItem(CCWeapons plugin, ItemMeta meta) {
    meta.setCustomModelData(1001);
    meta.setUnbreakable(true);
    meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);

    meta.addAttributeModifier(
            Attribute.ATTACK_DAMAGE,
            new AttributeModifier(
                    plugin.getKey("example_blade_damage"),
                    8.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.MAINHAND
            )
    );
}
```

Typical uses are:

- custom model data;
- attribute modifiers;
- item flags;
- unbreakable state;
- static item enchantments;
- additional persistent data.

CCWeapons has already applied the weapon display name, description, and
internal weapon ID before this hook runs.

### `configureItemStack(CCWeapons, ItemStack)`

This hook runs after the configured `ItemMeta` has been placed back onto the
completed stack:

```java
@Override
protected void configureItemStack(CCWeapons plugin, ItemStack item) {
    // Apply Paper data components or another property that needs the final
    // ItemStack rather than only ItemMeta.
}
```

Prefer `configureItem` for normal Bukkit `ItemMeta` work. Use
`configureItemStack` only when an API specifically needs the completed stack.

## 13. Weapon constructor and read-only properties

A weapon constructor supplies its permanent identity and basic appearance:

```java
public ExampleBladeWeapon() {
    super(
            "example_blade",                  // Unique lowercase ID
            "Example Blade",                  // Display name
            Material.DIAMOND_SWORD,            // Base item material
            List.of("First lore line.",
                    "Second lore line.")       // Guide/item description
    );
}
```

The ID must be globally unique across CCWeapons and all addons. It may contain
lowercase letters, numbers, periods, underscores, and hyphens. It must match
the key used in the addon's `config.yml`.

These final getters can be used inside weapon code:

```java
String getId()
String getDisplayName()
Material getMaterial()
List<String> getDescription()
```

`createItem(CCWeapons)` creates a correctly tagged weapon item. It is final so
the internal weapon ID cannot accidentally be omitted. In most cases addons do
not need to call it directly because CCWeapons creates recipe, command, and
guide items automatically.

## 14. `CCWeaponsApi` methods

### `registerWeapon(JavaPlugin addon, CustomWeapon weapon)`

Registers a weapon, assigns it to the addon, reads its configuration, registers
its recipe, and makes it visible to commands and the guide.

```java
api.registerWeapon(this, new ExampleBladeWeapon());
```

Call it during the addon's `onEnable()`, after `saveDefaultConfig()`. Weapon IDs
must be unique.

### `unregisterWeapons(JavaPlugin addon)`

Removes all weapons and recipes owned by the addon:

```java
api.unregisterWeapons(this);
```

CCWeapons already calls this automatically when an addon is disabled. Manual
use is only necessary if an addon wants to remove all of its weapons while it
remains enabled.

### `reloadAddon(JavaPlugin addon)`

Reloads the addon's `config.yml`, enabled states, and recipes:

```java
api.reloadAddon(this);
```

This is suitable for an addon-specific reload command. It does not create new
weapon objects; register new weapon classes first with `registerWeapon`.

### `getWeapon(String id)`

Finds a registered weapon by ID:

```java
Optional<CustomWeapon> weapon = api.getWeapon("example_blade");
```

The lookup is case-insensitive and returns `Optional.empty()` for an unknown
ID.

### `getWeapons()`

Returns an unmodifiable collection of all registered main-plugin and addon
weapons:

```java
for (CustomWeapon weapon : api.getWeapons()) {
    getLogger().info(weapon.getId());
}
```

Do not attempt to add or remove entries from this collection. Use the API
registration methods instead.

## 15. Imports commonly needed by weapon classes

The examples above use classes from these packages:

```java
import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
```

Only import the classes actually used by a weapon. The IDE can normally add
the required imports automatically.