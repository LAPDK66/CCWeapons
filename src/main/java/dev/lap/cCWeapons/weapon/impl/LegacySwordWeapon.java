package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Locale;

public final class LegacySwordWeapon extends CustomWeapon {

    private static final double LEGACY_SHARPNESS_DAMAGE_PER_LEVEL = 1.25;
    private static final double LEGACY_SPECIAL_DAMAGE_PER_LEVEL = 2.5;

    private static final List<Enchantment> COMPATIBLE_ENCHANTMENTS = List.of(
            Enchantment.SHARPNESS,
            Enchantment.SMITE,
            Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT,
            Enchantment.LOOTING,
            Enchantment.UNBREAKING,
            Enchantment.MENDING
    );

    private final double legacyAttackDamage;

    public LegacySwordWeapon(
            String id,
            String displayName,
            Material material,
            double legacyAttackDamage
    ) {
        super(
                id,
                displayName,
                material,
                List.of(
                        "A sword using legacy 1.8 combat mechanics.",
                        "No attack cooldown.",
                        "Hold right-click to use legacy blocking.",
                        "Legacy attack damage: "
                                + formatDamage(legacyAttackDamage)
                )
        );

        this.legacyAttackDamage = legacyAttackDamage;
    }

    @Override
    protected void configureItem(CCWeapons plugin, ItemMeta meta) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        new NamespacedKey(
                                plugin,
                                getId() + "_legacy_attack_damage"
                        ),
                        legacyAttackDamage - 1.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        new NamespacedKey(
                                plugin,
                                getId() + "_legacy_attack_speed"
                        ),
                        1020.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    protected void configureItemStack(
            CCWeapons plugin,
            ItemStack item
    ) {
        Consumable blockingComponent = Consumable.consumable()
                .consumeSeconds(999_999.0F)
                .animation(ItemUseAnimation.BLOCK)
                .hasConsumeParticles(false)
                .build();

        item.setData(
                DataComponentTypes.CONSUMABLE,
                blockingComponent
        );
    }

    @Override
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
        double damage = legacyAttackDamage;

        int sharpness = player.getInventory()
                .getItemInMainHand()
                .getEnchantmentLevel(Enchantment.SHARPNESS);

        int smite = player.getInventory()
                .getItemInMainHand()
                .getEnchantmentLevel(Enchantment.SMITE);

        int bane = player.getInventory()
                .getItemInMainHand()
                .getEnchantmentLevel(Enchantment.BANE_OF_ARTHROPODS);

        if (sharpness > 0) {
            damage += sharpness * LEGACY_SHARPNESS_DAMAGE_PER_LEVEL;
        } else if (smite > 0 && isUndead(target.getType())) {
            damage += smite * LEGACY_SPECIAL_DAMAGE_PER_LEVEL;
        } else if (bane > 0 && isArthropod(target.getType())) {
            damage += bane * LEGACY_SPECIAL_DAMAGE_PER_LEVEL;
            applyBaneSlowness(target, bane);
        }

        PotionEffect strength = player.getPotionEffect(
                PotionEffectType.STRENGTH
        );

        if (strength != null) {
            int level = strength.getAmplifier() + 1;
            damage += legacyAttackDamage * 1.3 * level;
        }

        PotionEffect weakness = player.getPotionEffect(
                PotionEffectType.WEAKNESS
        );

        if (weakness != null) {
            damage -= 0.5 * (weakness.getAmplifier() + 1);
        }

        if (isLegacyCriticalHit(player)) {
            damage *= 1.5;
        }

        event.setDamage(Math.max(0.0, damage));
    }

    private void applyBaneSlowness(
            LivingEntity target,
            int enchantmentLevel
    ) {
        int duration = 20 + (enchantmentLevel * 10);

        target.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SLOWNESS,
                        duration,
                        3,
                        false,
                        true,
                        true
                )
        );
    }

    private boolean isLegacyCriticalHit(Player player) {
        return player.getFallDistance() > 0.0F
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isInWater()
                && !player.hasPotionEffect(PotionEffectType.BLINDNESS)
                && !player.isInsideVehicle();
    }

    private boolean isUndead(EntityType type) {
        return switch (type) {
            case BOGGED,
                 DROWNED,
                 HUSK,
                 PHANTOM,
                 SKELETON,
                 SKELETON_HORSE,
                 STRAY,
                 WITHER,
                 WITHER_SKELETON,
                 ZOGLIN,
                 ZOMBIE,
                 ZOMBIE_HORSE,
                 ZOMBIE_VILLAGER,
                 ZOMBIFIED_PIGLIN -> true;
            default -> false;
        };
    }

    private boolean isArthropod(EntityType type) {
        return switch (type) {
            case BEE,
                 CAVE_SPIDER,
                 ENDERMITE,
                 SILVERFISH,
                 SPIDER -> true;
            default -> false;
        };
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return COMPATIBLE_ENCHANTMENTS;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return List.of(
                "Sharpness I-V - Adds 1.25 legacy damage per level.",
                "Smite I-V - Adds 2.5 damage per level against undead mobs.",
                "Bane of Arthropods I-V - Adds 2.5 damage per level against arthropods.",
                "Knockback I-II - Increases attack knockback.",
                "Fire Aspect I-II - Sets attacked enemies on fire.",
                "Looting I-III - Increases mob drops.",
                "Unbreaking I-III - Reduces durability loss.",
                "Mending I - Repairs the sword with experience."
        );
    }

    public double getLegacyAttackDamage() {
        return legacyAttackDamage;
    }

    private static String formatDamage(double damage) {
        if (damage == Math.rint(damage)) {
            return Integer.toString((int) damage);
        }

        return String.format(Locale.ROOT, "%.1f", damage);
    }
}