package dev.lap.cCWeapons.weapon;

import dev.lap.cCWeapons.CCWeapons;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
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

    private BukkitTask effectTask;
    private long effectTick;

    public WeaponRegistry(CCWeapons plugin) {
        this.plugin = plugin;
    }

    public void register(CustomWeapon weapon) {
        String id = weapon.getId().toLowerCase(Locale.ROOT);

        if (weapons.containsKey(id)) {
            throw new IllegalArgumentException(
                    "A weapon with the ID '" + id + "' is already registered."
            );
        }

        weapons.put(id, weapon);
        plugin.getLogger().info("Registered weapon: " + id);
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
        return plugin.getConfig().getBoolean(
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