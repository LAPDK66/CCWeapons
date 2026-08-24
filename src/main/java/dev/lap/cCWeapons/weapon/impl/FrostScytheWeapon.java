package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FrostScytheWeapon extends CustomWeapon {

    private static final int MAXIMUM_CHARGES = 2;
    private static final long RECHARGE_INTERVAL_MILLIS = 8_000L;

    private static final int EFFECT_DURATION_TICKS = 40;
    private static final long EFFECT_DURATION_MILLIS =
            EFFECT_DURATION_TICKS * 50L;
    private static final int WAVE_SLOWNESS_AMPLIFIER = 3;
    private static final double WAVE_DISTANCE = 10.0;
    private static final double WAVE_WIDTH = 1.4;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;
    private final Map<UUID, Long> frozenUntil = new HashMap<>();

    public FrostScytheWeapon(CCWeapons plugin) {
        super(
                "frost_scythe",
                "Frost Scythe",
                Material.NETHERITE_HOE,
                List.of(
                        "Hits slow enemies for 2 seconds.",
                        "Right-click to release a frost wave.",
                        "The wave applies Slowness IV and prevents jumping.",
                        "Stores 2 frost charges.",
                        "Restores 1 charge every 8 seconds."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                WeaponSupport.findCompatibleEnchantments(
                        Material.NETHERITE_HOE
                );
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        if (!data.has(chargesKey(), PersistentDataType.INTEGER)) {
            data.set(
                    chargesKey(),
                    PersistentDataType.INTEGER,
                    MAXIMUM_CHARGES
            );
        }

        if (!data.has(lastRechargeKey(), PersistentDataType.LONG)) {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    System.currentTimeMillis()
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
        if (event.isCancelled() || event.getFinalDamage() <= 0.0) {
            return;
        }

        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        0,
                        false,
                        true,
                        true
                )
        );
    }

    @Override
    public void onRightClick(Player player) {
        ItemStack scythe =
                player.getInventory().getItemInMainHand();

        long currentTime = System.currentTimeMillis();
        recharge(scythe, currentTime);

        ItemMeta meta = scythe.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charges = readCharges(data);

        if (charges <= 0) {
            player.sendActionBar(
                    Component.text(
                            "No frost charges available.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (charges >= MAXIMUM_CHARGES) {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    currentTime
            );
        }

        data.set(
                chargesKey(),
                PersistentDataType.INTEGER,
                charges - 1
        );

        updateLore(meta);
        scythe.setItemMeta(meta);

        releaseFrostWave(player);

        player.sendActionBar(
                Component.text(
                        "Frost Charges: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        (charges - 1) + " / " + MAXIMUM_CHARGES,
                        charges - 1 > 0
                                ? NamedTextColor.AQUA
                                : NamedTextColor.RED
                ))
        );
    }

    private void releaseFrostWave(Player player) {
        Location start = player.getEyeLocation()
                .subtract(0.0, 0.4, 0.0);

        Vector direction = start.getDirection().normalize();
        Set<UUID> affectedEntities = new HashSet<>();

        for (double distance = 1.0;
             distance <= WAVE_DISTANCE;
             distance += 0.5) {
            Location point = start.clone().add(
                    direction.clone().multiply(distance)
            );

            point.getWorld().spawnParticle(
                    Particle.SNOWFLAKE,
                    point,
                    8,
                    0.35,
                    0.25,
                    0.35,
                    0.02
            );

            point.getWorld().spawnParticle(
                    Particle.BLOCK,
                    point,
                    3,
                    0.25,
                    0.2,
                    0.25,
                    Material.PACKED_ICE.createBlockData()
            );

            for (LivingEntity target :
                    point.getNearbyLivingEntities(WAVE_WIDTH)) {
                if (target.equals(player)
                        || !affectedEntities.add(target.getUniqueId())) {
                    continue;
                }

                freezeTarget(target);
            }
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_GLASS_BREAK,
                1.0F,
                0.7F
        );
    }

    private void freezeTarget(LivingEntity target) {
        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        EFFECT_DURATION_TICKS,
                        WAVE_SLOWNESS_AMPLIFIER,
                        false,
                        true,
                        true
                )
        );

        AttributeInstance jumpStrength =
                target.getAttribute(Attribute.JUMP_STRENGTH);

        if (jumpStrength == null) {
            return;
        }

        UUID targetId = target.getUniqueId();

        org.bukkit.NamespacedKey modifierKey = plugin.getKey(
                "frost_scythe_jump_"
                        + targetId.toString().replace("-", "")
        );

        AttributeModifier existingModifier =
                jumpStrength.getModifier(modifierKey);

        if (existingModifier != null) {
            jumpStrength.removeModifier(existingModifier);
        }

        AttributeModifier modifier = new AttributeModifier(
                modifierKey,
                -1.0,
                AttributeModifier.Operation.ADD_SCALAR
        );

        jumpStrength.addTransientModifier(modifier);

        long effectEndsAt =
                System.currentTimeMillis() + EFFECT_DURATION_MILLIS;

        frozenUntil.put(targetId, effectEndsAt);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    long currentEffectEndsAt = frozenUntil.getOrDefault(
                            targetId,
                            0L
                    );

                    if (currentEffectEndsAt != effectEndsAt) {
                        return;
                    }

                    frozenUntil.remove(targetId);

                    AttributeInstance currentAttribute =
                            target.getAttribute(Attribute.JUMP_STRENGTH);

                    if (currentAttribute == null) {
                        return;
                    }

                    AttributeModifier currentModifier =
                            currentAttribute.getModifier(modifierKey);

                    if (currentModifier != null) {
                        currentAttribute.removeModifier(currentModifier);
                    }
                },
                EFFECT_DURATION_TICKS
        );
    }

    @Override
    public void onInventoryItemTick(
            Player player,
            ItemStack item
    ) {
        recharge(item, System.currentTimeMillis());
    }

    private void recharge(ItemStack item, long currentTime) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charges = readCharges(data);

        if (charges >= MAXIMUM_CHARGES) {
            return;
        }

        long lastRecharge = data.getOrDefault(
                lastRechargeKey(),
                PersistentDataType.LONG,
                currentTime
        );

        int restoredCharges = (int) (
                Math.max(0L, currentTime - lastRecharge)
                        / RECHARGE_INTERVAL_MILLIS
        );

        if (restoredCharges <= 0) {
            return;
        }

        int newCharges = Math.min(
                MAXIMUM_CHARGES,
                charges + restoredCharges
        );

        data.set(
                chargesKey(),
                PersistentDataType.INTEGER,
                newCharges
        );

        data.set(
                lastRechargeKey(),
                PersistentDataType.LONG,
                lastRecharge
                        + ((long) restoredCharges
                        * RECHARGE_INTERVAL_MILLIS)
        );

        updateLore(meta);
        item.setItemMeta(meta);
    }

    private void updateLore(ItemMeta meta) {
        int charges = readCharges(
                meta.getPersistentDataContainer()
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Hits slow enemies for 2 seconds.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Right-click to release a frost wave.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "The wave applies Slowness IV and prevents jumping.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Restores 1 charge every 8 seconds.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.empty());
        lore.add(
                Component.text(
                        "Frost Charges: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        charges + " / " + MAXIMUM_CHARGES,
                        charges > 0
                                ? NamedTextColor.AQUA
                                : NamedTextColor.RED
                ))
        );
        lore.add(Component.empty());
        lore.add(Component.text(
                "Custom Weapon",
                NamedTextColor.DARK_GRAY
        ));

        meta.lore(lore);
    }

    private int readCharges(PersistentDataContainer data) {
        return Math.max(
                0,
                Math.min(
                        MAXIMUM_CHARGES,
                        data.getOrDefault(
                                chargesKey(),
                                PersistentDataType.INTEGER,
                                MAXIMUM_CHARGES
                        )
                )
        );
    }

    private org.bukkit.NamespacedKey chargesKey() {
        return plugin.getKey("frost_scythe_charges");
    }

    private org.bukkit.NamespacedKey lastRechargeKey() {
        return plugin.getKey("frost_scythe_last_recharge");
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return WeaponSupport.createEnchantmentHints(
                compatibleEnchantments,
                "hoe"
        );
    }
}