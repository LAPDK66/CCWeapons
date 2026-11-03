package dev.lap.standartaddon.weapon.impl;

import dev.lap.cCWeapons.weapon.CustomWeapon;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.List;

/**
 * Example weapon demonstrating the normal on-hit hook.
 */
public final class EmberAxeWeapon extends CustomWeapon {

    private static final int FIRE_DURATION_TICKS = 60;

    private static final List<Enchantment> COMPATIBLE_ENCHANTMENTS =
            List.of(
                    Enchantment.SHARPNESS,
                    Enchantment.EFFICIENCY,
                    Enchantment.UNBREAKING,
                    Enchantment.MENDING
            );

    public EmberAxeWeapon() {
        super(
                "ember_axe",
                "Ember Axe",
                Material.DIAMOND_AXE,
                List.of(
                        "Melee hits ignite the target for 3 seconds.",
                        "Demonstrates an addon's on-hit weapon hook."
                )
        );
    }

    @Override
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
        if (event.isCancelled()) {
            return;
        }

        target.setFireTicks(Math.max(
                target.getFireTicks(),
                FIRE_DURATION_TICKS
        ));
        target.getWorld().spawnParticle(
                Particle.FLAME,
                target.getLocation().add(0.0, 1.0, 0.0),
                12,
                0.35,
                0.5,
                0.35,
                0.02
        );
        target.getWorld().playSound(
                target.getLocation(),
                Sound.ITEM_FIRECHARGE_USE,
                0.6F,
                1.2F
        );
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return COMPATIBLE_ENCHANTMENTS;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return List.of(
                "Sharpness I-V - Increases melee damage.",
                "Efficiency I-V - Increases mining speed.",
                "Unbreaking I-III - Reduces durability loss.",
                "Mending I - Repairs the axe with experience."
        );
    }
}