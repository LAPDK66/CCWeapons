package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;

import java.util.List;

public final class CursedDaggerWeapon extends CustomWeapon {

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_INTERACTION_RANGE = 3.0;

    private static final double ATTACK_DAMAGE = 4.0;
    private static final double ATTACK_SPEED = 2.4;
    private static final double MAXIMUM_REACH = 2.0;

    private static final List<Enchantment> COMPATIBLE_ENCHANTMENTS = List.of(
            Enchantment.KNOCKBACK,
            Enchantment.FIRE_ASPECT,
            Enchantment.LOOTING,
            Enchantment.UNBREAKING,
            Enchantment.MENDING
    );

    public CursedDaggerWeapon() {
        super(
                "cursed_dagger",
                "Cursed Dagger",
                Material.IRON_SWORD,
                List.of(
                        "Deals exactly 4 damage.",
                        "Maximum attack reach: 2 blocks.",
                        "Completely ignores armor and shields,",
                        "except the blocking of a legacy sword.",
                        "Ignores defensive status effects.",
                        "Damage cannot be increased or reduced."
                )
        );
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);
        meta.removeAttributeModifier(Attribute.ENTITY_INTERACTION_RANGE);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey("cursed_dagger_attack_damage"),
                        ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey("cursed_dagger_attack_speed"),
                        ATTACK_SPEED - 4.0,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ENTITY_INTERACTION_RANGE,
                new AttributeModifier(
                        plugin.getKey("cursed_dagger_reach"),
                        MAXIMUM_REACH - PLAYER_BASE_INTERACTION_RANGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
        if (!isWithinReach(player, target)) {
            event.setCancelled(true);
            return;
        }

        event.setDamage(DamageModifier.BASE, ATTACK_DAMAGE);

        for (DamageModifier modifier : DamageModifier.values()) {
            if (modifier == DamageModifier.BASE
                    || !event.isApplicable(modifier)) {
                continue;
            }

            event.setDamage(modifier, 0.0);
        }
    }

    private boolean isWithinReach(
            Player player,
            LivingEntity target
    ) {
        Location eye = player.getEyeLocation();
        BoundingBox box = target.getBoundingBox();

        double nearestX = clamp(
                eye.getX(),
                box.getMinX(),
                box.getMaxX()
        );

        double nearestY = clamp(
                eye.getY(),
                box.getMinY(),
                box.getMaxY()
        );

        double nearestZ = clamp(
                eye.getZ(),
                box.getMinZ(),
                box.getMaxZ()
        );

        double differenceX = eye.getX() - nearestX;
        double differenceY = eye.getY() - nearestY;
        double differenceZ = eye.getZ() - nearestZ;

        double distanceSquared =
                differenceX * differenceX
                        + differenceY * differenceY
                        + differenceZ * differenceZ;

        return distanceSquared <= MAXIMUM_REACH * MAXIMUM_REACH;
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return COMPATIBLE_ENCHANTMENTS;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return List.of(
                "Knockback I-II - Increases attack knockback.",
                "Fire Aspect I-II - Sets attacked enemies on fire.",
                "Looting I-III - Increases mob drops.",
                "Unbreaking I-III - Reduces durability loss.",
                "Mending I - Repairs the dagger with experience.",
                "Damage enchantments are disabled because the dagger always deals exactly 4 damage."
        );
    }
}