package dev.lap.cCWeapons.listener;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import dev.lap.cCWeapons.weapon.impl.CursedDaggerWeapon;
import dev.lap.cCWeapons.weapon.impl.LegacySwordWeapon;
import dev.lap.cCWeapons.weapon.impl.VampiricWeapon;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WeaponListener implements Listener {

    private static final long KILL_RECORD_MAX_AGE = 5_000L;

    private final WeaponRegistry weaponRegistry;
    private final Map<UUID, KillRecord> pendingKills = new HashMap<>();

    public WeaponListener(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry
    ) {
        this.weaponRegistry = weaponRegistry;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(event.getItem())
                .filter(weaponRegistry::isEnabled)
                .orElse(null);

        if (weapon == null) {
            return;
        }

        if (weapon.usesRightClickAbility()) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setUseInteractedBlock(Event.Result.DENY);
            }

            event.setUseItemInHand(Event.Result.ALLOW);
        }

        if (weapon instanceof LegacySwordWeapon) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setUseInteractedBlock(Event.Result.DENY);
            }

            event.setUseItemInHand(Event.Result.ALLOW);
        }

        weapon.onRightClick(event.getPlayer(), event);
    }

    @EventHandler
    public void onPlayerInteractEntity(
            PlayerInteractEntityEvent event
    ) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();

        CustomWeapon weapon = weaponRegistry.getWeapon(
                        player.getInventory().getItemInMainHand()
                )
                .filter(weaponRegistry::isEnabled)
                .orElse(null);

        if (weapon == null) {
            return;
        }

        weapon.onRightClick(player, event);

        if (weapon.usesRightClickAbility()) {
            event.setCancelled(true);
        }

        if (weapon instanceof LegacySwordWeapon) {
            event.setCancelled(true);
            player.startUsingItem(EquipmentSlot.HAND);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileShot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getProjectile()
                instanceof Projectile projectile)) {
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(event.getBow())
                .filter(weaponRegistry::isEnabled)
                .orElse(null);

        if (weapon == null) {
            return;
        }

        weapon.onProjectileShot(
                player,
                projectile,
                event
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        weaponRegistry.getWeapon(event.getEntity())
                .filter(weaponRegistry::isEnabled)
                .ifPresent(weapon ->
                        weapon.onProjectileHit(event)
                );
    }

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = true
    )
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getCause()
                == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            if (event.getDamager() instanceof Player player
                    && isHoldingLegacySword(player)) {
                event.setCancelled(true);
            }

            return;
        }

        if (event.getDamager() instanceof Player player
                && event.getEntity() instanceof LivingEntity target) {
            ItemStack item = player.getInventory().getItemInMainHand();

            weaponRegistry.getWeapon(item)
                    .filter(weaponRegistry::isEnabled)
                    .ifPresent(weapon -> {
                        weapon.onHit(player, target, event);

                        if (!event.isCancelled() && event.getFinalDamage() >= target.getHealth()) {
                            pendingKills.put(
                                    target.getUniqueId(),
                                    new KillRecord(
                                            player.getUniqueId(),
                                            weapon,
                                            player.getInventory()
                                                    .getHeldItemSlot(),
                                            System.currentTimeMillis()
                                    )
                            );
                        }
                    });
        }

        if (event.getEntity() instanceof Player target
                && isBlocking(target)
                && isBlockableDamage(event.getCause())) {
            event.setDamage(event.getDamage() * 0.5);
        }
    }

    private boolean isCursedDaggerAttack(
            EntityDamageByEntityEvent event
    ) {
        if (!(event.getDamager() instanceof Player player)) {
            return false;
        }

        return weaponRegistry.getWeapon(
                        player.getInventory().getItemInMainHand()
                )
                .filter(weaponRegistry::isEnabled)
                .filter(CursedDaggerWeapon.class::isInstance)
                .isPresent();
    }

    private boolean isBlocking(Player player) {
        return player.isHandRaised() && isHoldingLegacySword(player);
    }

    private boolean isHoldingLegacySword(Player player) {
        return weaponRegistry.getWeapon(
                        player.getInventory().getItemInMainHand()
                )
                .filter(weaponRegistry::isEnabled)
                .filter(LegacySwordWeapon.class::isInstance)
                .isPresent();
    }

    private boolean isBlockableDamage(
            EntityDamageEvent.DamageCause cause
    ) {
        return switch (cause) {
            case ENTITY_ATTACK,
                 ENTITY_SWEEP_ATTACK,
                 PROJECTILE,
                 BLOCK_EXPLOSION,
                 ENTITY_EXPLOSION -> true;
            default -> false;
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        CustomWeapon weapon = weaponRegistry.getWeapon(event.getItem())
                .orElse(null);

        if (weapon == null) {
            return;
        }

        event.getEnchantsToAdd()
                .entrySet()
                .removeIf(entry ->
                        !weapon.isEnchantmentAllowed(entry.getKey())
                );

        if (event.getEnchantsToAdd().isEmpty()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();

        CustomWeapon weapon = weaponRegistry.getWeapon(result)
                .orElse(null);

        if (weapon == null || result == null) {
            return;
        }

        for (Enchantment enchantment :
                result.getEnchantments().keySet()) {
            if (!weapon.isEnchantmentAllowed(enchantment)) {
                event.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        ItemStack equipment =
                event.getInventory().getInputEquipment();

        if (weaponRegistry.getWeapon(equipment).isPresent()) {
            event.setResult(null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        KillRecord record = pendingKills.remove(
                event.getEntity().getUniqueId()
        );

        if (record == null) {
            return;
        }

        if (System.currentTimeMillis() - record.createdAt()
                > KILL_RECORD_MAX_AGE) {
            return;
        }

        Player killer = event.getEntity()
                .getServer()
                .getPlayer(record.killerId());

        if (killer == null || !killer.isOnline()) {
            return;
        }

        ItemStack weaponItem = killer.getInventory().getItem(
                record.inventorySlot()
        );

        boolean sameWeapon = weaponRegistry.getWeapon(weaponItem)
                .filter(weaponRegistry::isEnabled)
                .filter(weapon ->
                        weapon.getId().equals(
                                record.weapon().getId()
                        )
                )
                .isPresent();

        if (!sameWeapon) {
            return;
        }

        if (event.getEntity() instanceof Player victim) {
            record.weapon().onPlayerKill(killer, victim, weaponItem);
        } else {
            record.weapon().onEntityKill(killer, event.getEntity(), weaponItem);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        VampiricWeapon.resetBonusHealth(
                event.getEntity()
        );
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventorySlotChange(
            PlayerInventorySlotChangeEvent event
    ) {
        weaponRegistry.getWeapon(event.getNewItemStack())
                .filter(weaponRegistry::isEnabled)
                .ifPresent(weapon ->
                        weapon.onInventorySlotChange(
                                event.getPlayer(),
                                event
                        )
                );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();

        pendingKills.entrySet().removeIf(entry ->
                entry.getValue().killerId().equals(playerId)
        );
    }

    private record KillRecord(
            UUID killerId,
            CustomWeapon weapon,
            int inventorySlot,
            long createdAt
    ) {
    }
}