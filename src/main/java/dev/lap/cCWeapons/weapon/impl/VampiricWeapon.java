package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class VampiricWeapon extends CustomWeapon {

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;
    private static final double NORMAL_PLAYER_MAX_HEALTH = 20.0;
    private static final double MAXIMUM_PLAYER_MAX_HEALTH = 1024.0;

    private static final double HEALING_PERCENTAGE = 0.20;
    private static final double HEALTH_PER_PLAYER_KILL = 2.0;

    private final double attackDamage;
    private final double attackSpeed;
    private final List<Enchantment> compatibleEnchantments;

    public VampiricWeapon(
            String id,
            String displayName,
            Material material,
            double attackDamage,
            double attackSpeed
    ) {
        super(
                id,
                displayName,
                material,
                List.of(
                        "Deals one less damage than the normal weapon.",
                        "Heals 20% of damage dealt.",
                        "Player kills grant one additional heart.",
                        "Additional hearts are lost on death."
                )
        );

        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.compatibleEnchantments =
                findCompatibleEnchantments(material);
    }

    @Override
    protected void configureItem(CCWeapons plugin, ItemMeta meta) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey(
                                getId() + "_attack_damage"
                        ),
                        attackDamage - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey(
                                getId() + "_attack_speed"
                        ),
                        attackSpeed - PLAYER_BASE_ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
    }

    @Override
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
        double availableHealth = target.getHealth()
                + target.getAbsorptionAmount();

        double dealtDamage = Math.min(
                Math.max(0.0, event.getFinalDamage()),
                availableHealth
        );

        if (dealtDamage <= 0.0) {
            return;
        }

        AttributeInstance maximumHealth =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (maximumHealth == null) {
            return;
        }

        double healing = dealtDamage * HEALING_PERCENTAGE;

        player.setHealth(
                Math.min(
                        maximumHealth.getValue(),
                        player.getHealth() + healing
                )
        );
    }

    @Override
    public void onPlayerKill(Player killer, Player victim) {
        AttributeInstance maximumHealth =
                killer.getAttribute(Attribute.MAX_HEALTH);

        if (maximumHealth == null) {
            return;
        }

        double previousMaximumHealth = maximumHealth.getValue();
        double newBaseValue = Math.min(
                MAXIMUM_PLAYER_MAX_HEALTH,
                maximumHealth.getBaseValue()
                        + HEALTH_PER_PLAYER_KILL
        );

        maximumHealth.setBaseValue(newBaseValue);

        double receivedHealth = Math.max(
                0.0,
                maximumHealth.getValue() - previousMaximumHealth
        );

        if (receivedHealth > 0.0) {
            killer.setHealth(
                    Math.min(
                            maximumHealth.getValue(),
                            killer.getHealth() + receivedHealth
                    )
            );
        }
    }

    public static void resetBonusHealth(Player player) {
        AttributeInstance maximumHealth =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (maximumHealth == null) {
            return;
        }

        maximumHealth.setBaseValue(NORMAL_PLAYER_MAX_HEALTH);
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return compatibleEnchantments.stream()
                .sorted(Comparator.comparing(
                        enchantment ->
                                enchantment.getKey().getKey()
                ))
                .map(enchantment ->
                        formatEnchantmentName(
                                enchantment.getKey().getKey()
                        )
                                + " I-"
                                + enchantment.getMaxLevel()
                                + " - Normal "
                                + formatMaterialName(getMaterial())
                                + " enchantment."
                )
                .toList();
    }

    private static List<Enchantment> findCompatibleEnchantments(
            Material material
    ) {
        ItemStack vanillaItem = new ItemStack(material);
        List<Enchantment> enchantments = new ArrayList<>();

        for (Enchantment enchantment : Registry.ENCHANTMENT) {
            if (enchantment.canEnchantItem(vanillaItem)) {
                enchantments.add(enchantment);
            }
        }

        return List.copyOf(enchantments);
    }

    private static String formatEnchantmentName(String key) {
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    word.substring(0, 1)
                            .toUpperCase(Locale.ROOT)
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    private static String formatMaterialName(Material material) {
        return formatEnchantmentName(
                material.name().toLowerCase(Locale.ROOT)
        );
    }
}