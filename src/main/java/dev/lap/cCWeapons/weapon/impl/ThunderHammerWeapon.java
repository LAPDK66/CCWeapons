package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ThunderHammerWeapon extends CustomWeapon {

    private static final int MAXIMUM_CHARGE = 5;
    private static final double TARGET_DISTANCE = 30.0;
    private static final double EXPLOSION_RADIUS = 5.0;
    private static final double LIGHTNING_DAMAGE = 8.0;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;
    private final Set<UUID> activeLightningAttacks = new HashSet<>();

    public ThunderHammerWeapon(CCWeapons plugin) {
        super(
                "thunder_hammer",
                "Thunder Hammer",
                Material.MACE,
                List.of(
                        "Every successful normal hit grants 1 charge.",
                        "At 5 charges, right-click to summon lightning.",
                        "Lightning damages and knocks back nearby enemies.",
                        "Charge is permanently stored inside the hammer."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                WeaponSupport.findCompatibleEnchantments(Material.MACE);
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        if (!data.has(chargeKey(), PersistentDataType.INTEGER)) {
            data.set(
                    chargeKey(),
                    PersistentDataType.INTEGER,
                    0
            );
        }

        updateLore(meta);
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
        if (event.isCancelled()
                || event.getFinalDamage() <= 0.0
                || activeLightningAttacks.contains(player.getUniqueId())) {
            return;
        }

        ItemStack hammer =
                player.getInventory().getItemInMainHand();

        ItemMeta meta = hammer.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charge = Math.min(
                MAXIMUM_CHARGE,
                readCharge(data) + 1
        );

        data.set(
                chargeKey(),
                PersistentDataType.INTEGER,
                charge
        );

        updateLore(meta);
        hammer.setItemMeta(meta);
        showCharge(player, charge);
    }

    @Override
    public void onRightClick(Player player) {
        ItemStack hammer =
                player.getInventory().getItemInMainHand();

        ItemMeta meta = hammer.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charge = readCharge(data);

        if (charge < MAXIMUM_CHARGE) {
            showCharge(player, charge);
            return;
        }

        Location targetLocation = findTargetLocation(player);

        if (targetLocation == null) {
            player.sendActionBar(
                    Component.text(
                            "No block or enemy in range.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        data.set(
                chargeKey(),
                PersistentDataType.INTEGER,
                0
        );

        updateLore(meta);
        hammer.setItemMeta(meta);

        strikeLightning(player, targetLocation);
        showCharge(player, 0);
    }

    private Location findTargetLocation(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        RayTraceResult blockResult =
                player.rayTraceBlocks(TARGET_DISTANCE);

        RayTraceResult entityResult =
                player.getWorld().rayTraceEntities(
                        eye,
                        direction,
                        TARGET_DISTANCE,
                        0.5,
                        entity -> entity instanceof LivingEntity
                                && !entity.equals(player)
                );

        Location blockLocation = blockResult == null
                || blockResult.getHitPosition() == null
                ? null
                : blockResult.getHitPosition().toLocation(
                player.getWorld()
        );

        Location entityLocation = entityResult == null
                || entityResult.getHitEntity() == null
                ? null
                : entityResult.getHitEntity().getLocation()
                .add(0.0, 0.5, 0.0);

        if (blockLocation == null) {
            return entityLocation;
        }

        if (entityLocation == null) {
            return blockLocation;
        }

        return eye.distanceSquared(entityLocation)
                < eye.distanceSquared(blockLocation)
                ? entityLocation
                : blockLocation;
    }

    private void strikeLightning(
            Player player,
            Location location
    ) {
        location.getWorld().strikeLightningEffect(location);

        location.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                location,
                100,
                1.5,
                1.5,
                1.5,
                0.2
        );

        location.getWorld().playSound(
                location,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
                1.5F,
                1.0F
        );

        activeLightningAttacks.add(player.getUniqueId());

        try {
            for (LivingEntity target :
                    location.getNearbyLivingEntities(EXPLOSION_RADIUS)) {
                if (target.equals(player)) {
                    continue;
                }

                target.damage(LIGHTNING_DAMAGE, player);

                Vector knockback = target.getLocation()
                        .toVector()
                        .subtract(location.toVector());

                if (knockback.lengthSquared() == 0.0) {
                    knockback = new Vector(0.0, 1.0, 0.0);
                } else {
                    knockback.normalize();
                }

                knockback.multiply(1.2);
                knockback.setY(0.55);

                target.setVelocity(
                        target.getVelocity().add(knockback)
                );
            }
        } finally {
            activeLightningAttacks.remove(player.getUniqueId());
        }
    }

    private void showCharge(Player player, int charge) {
        player.sendActionBar(
                Component.text(
                        "Thunder Charge: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        charge + " / " + MAXIMUM_CHARGE,
                        charge >= MAXIMUM_CHARGE
                                ? NamedTextColor.YELLOW
                                : NamedTextColor.AQUA
                ))
        );
    }

    private void updateLore(ItemMeta meta) {
        int charge = readCharge(
                meta.getPersistentDataContainer()
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Every successful normal hit grants 1 charge.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "At 5 charges, right-click to summon lightning.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Lightning damages and knocks back nearby enemies.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.empty());
        lore.add(
                Component.text(
                        "Thunder Charge: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        charge + " / " + MAXIMUM_CHARGE,
                        charge >= MAXIMUM_CHARGE
                                ? NamedTextColor.YELLOW
                                : NamedTextColor.AQUA
                ))
        );
        lore.add(Component.empty());
        lore.add(Component.text(
                "Custom Weapon",
                NamedTextColor.DARK_GRAY
        ));

        meta.lore(lore);
    }

    private int readCharge(PersistentDataContainer data) {
        return Math.max(
                0,
                Math.min(
                        MAXIMUM_CHARGE,
                        data.getOrDefault(
                                chargeKey(),
                                PersistentDataType.INTEGER,
                                0
                        )
                )
        );
    }

    private org.bukkit.NamespacedKey chargeKey() {
        return plugin.getKey("thunder_hammer_charge");
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return WeaponSupport.createEnchantmentHints(
                compatibleEnchantments,
                "mace"
        );
    }
}