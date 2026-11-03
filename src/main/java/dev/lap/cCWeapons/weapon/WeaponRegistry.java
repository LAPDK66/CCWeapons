package dev.lap.cCWeapons.weapon;

import dev.lap.cCWeapons.CCWeapons;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WeaponRegistry {

    private final CCWeapons plugin;
    private final Map<String, CustomWeapon> weapons = new LinkedHashMap<>();
    private final Map<String, JavaPlugin> weaponOwners = new LinkedHashMap<>();

    private BukkitTask effectTask;
    private long effectTick;

    public WeaponRegistry(CCWeapons plugin) {
        this.plugin = plugin;
    }

    public void register(CustomWeapon weapon) {
        register(plugin, weapon);
    }

    public void register(JavaPlugin owner, CustomWeapon weapon) {
        if (owner == null) {
            throw new IllegalArgumentException("The weapon owner cannot be null.");
        }

        if (weapon == null) {
            throw new IllegalArgumentException("The weapon cannot be null.");
        }

        String id = weapon.getId().toLowerCase(Locale.ROOT);

        if (!id.matches("[a-z0-9._-]+")) {
            throw new IllegalArgumentException(
                    "Weapon IDs may only contain lowercase letters, numbers, "
                            + "periods, underscores and hyphens: " + id
            );
        }

        if (weapons.containsKey(id)) {
            throw new IllegalArgumentException(
                    "A weapon with the ID '" + id + "' is already registered."
            );
        }

        weapons.put(id, weapon);
        weaponOwners.put(id, owner);
        plugin.getLogger().info(
                "Registered weapon: " + id + " (owner: "
                        + owner.getName() + ")"
        );
    }

    public Collection<CustomWeapon> unregisterOwnedBy(JavaPlugin owner) {
        Collection<CustomWeapon> removedWeapons = weapons.values()
                .stream()
                .filter(weapon -> owner.equals(weaponOwners.get(weapon.getId())))
                .toList();

        for (CustomWeapon weapon : removedWeapons) {
            weapons.remove(weapon.getId());
            weaponOwners.remove(weapon.getId());
            plugin.getLogger().info(
                    "Unregistered weapon: " + weapon.getId()
            );
        }

        return removedWeapons;
    }

    public Optional<JavaPlugin> getOwner(CustomWeapon weapon) {
        if (weapon == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(weaponOwners.get(weapon.getId()));
    }

    public Optional<CustomWeapon> getWeapon(String id) {
        if (id == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                weapons.get(id.toLowerCase(Locale.ROOT))
        );
    }

    public Optional<CustomWeapon> getWeapon(ItemStack item) {
        if (item == null
                || item.getType() == Material.AIR
                || !item.hasItemMeta()) {
            return Optional.empty();
        }

        String id = item.getItemMeta()
                .getPersistentDataContainer()
                .get(plugin.getWeaponKey(), PersistentDataType.STRING);

        if (id == null) {
            return Optional.empty();
        }

        return getWeapon(id);
    }

    public Optional<CustomWeapon> getWeapon(Projectile projectile) {
        if (projectile == null) {
            return Optional.empty();
        }

        String id = projectile.getPersistentDataContainer().get(
                plugin.getWeaponKey(),
                PersistentDataType.STRING
        );

        return getWeapon(id);
    }

    public boolean isEnabled(CustomWeapon weapon) {
        JavaPlugin owner = getOwner(weapon).orElse(plugin);

        return owner.getConfig().getBoolean(
                "weapons." + weapon.getId() + ".enabled",
                true
        );
    }

    public boolean isEnabled(String id) {
        return getWeapon(id)
                .map(this::isEnabled)
                .orElse(false);
    }

    public Collection<CustomWeapon> getWeapons() {
        return Collections.unmodifiableCollection(weapons.values());
    }

    public void startEffectTask() {
        if (effectTask != null) {
            effectTask.cancel();
        }

        effectTick = 0L;

        effectTask = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::tickPlayers,
                1L,
                1L
        );
    }

    private void tickPlayers() {
        effectTick++;
        boolean runInventoryTick = effectTick % 10L == 0L;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            tickPlayer(player, runInventoryTick);
        }
    }

    private void tickPlayer(
            Player player,
            boolean runInventoryTick
    ) {
        getWeapon(player.getInventory().getItemInMainHand())
                .filter(this::isEnabled)
                .ifPresent(weapon -> weapon.onHeldTick(player));

        if (!runInventoryTick) {
            return;
        }

        Set<String> processedWeaponIds = new LinkedHashSet<>();

        for (ItemStack item : player.getInventory().getContents()) {
            getWeapon(item)
                    .filter(this::isEnabled)
                    .ifPresent(weapon -> {
                        weapon.onInventoryItemTick(player, item);

                        if (processedWeaponIds.add(weapon.getId())) {
                            weapon.onInventoryTick(player);
                        }
                    });
        }
    }
}