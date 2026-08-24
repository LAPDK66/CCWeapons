package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class EvolvingSwordWeapon extends CustomWeapon {

    private static final int INITIAL_LEVEL = 1;
    private static final int INITIAL_XP = 0;
    private static final int MAXIMUM_LEVEL = 128;

    private static final int BASE_XP_REQUIREMENT = 50;
    private static final int ADDITIONAL_XP_PER_LEVEL = 25;
    private static final int XP_PER_ENTITY_HIT = 1;
    private static final int XP_PER_PLAYER_HIT = 5;

    private static final double INITIAL_ATTACK_DAMAGE = 2.0;
    private static final double DAMAGE_PER_LEVEL = 0.5;
    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double ATTACK_SPEED = 1.6;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public EvolvingSwordWeapon(CCWeapons plugin) {
        super(
                "evolving_sword",
                "Evolving Sword",
                Material.NETHERITE_SWORD,
                List.of(
                        "Gains 5 weapon XP by hitting players.",
                        "Gains 1 weapon XP by hitting other living entities.",
                        "Levels increase the sword's attack damage.",
                        "Progress is permanently stored inside the sword."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                findCompatibleEnchantments(Material.NETHERITE_SWORD);
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        if (!data.has(levelKey(), PersistentDataType.INTEGER)) {
            data.set(
                    levelKey(),
                    PersistentDataType.INTEGER,
                    INITIAL_LEVEL
            );
        }

        if (!data.has(xpKey(), PersistentDataType.INTEGER)) {
            data.set(
                    xpKey(),
                    PersistentDataType.INTEGER,
                    INITIAL_XP
            );
        }

        updateItemMeta(meta);
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

        ItemStack sword =
                player.getInventory().getItemInMainHand();

        ItemMeta meta = sword.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int level = readLevel(data);
        int previousLevel = level;
        int xp = readXp(data);

        if (level >= MAXIMUM_LEVEL) {
            if (xp != 0) {
                data.set(
                        xpKey(),
                        PersistentDataType.INTEGER,
                        0
                );

                updateItemMeta(meta);
                sword.setItemMeta(meta);
            }

            return;
        }

        int gainedXp = target instanceof Player
                ? XP_PER_PLAYER_HIT
                : XP_PER_ENTITY_HIT;

        xp += gainedXp;

        while (level < MAXIMUM_LEVEL) {
            int requiredXp = getRequiredXp(level);

            if (xp < requiredXp) {
                break;
            }

            xp -= requiredXp;
            level++;
        }

        if (level >= MAXIMUM_LEVEL) {
            level = MAXIMUM_LEVEL;
            xp = 0;
        }

        data.set(
                levelKey(),
                PersistentDataType.INTEGER,
                level
        );

        data.set(
                xpKey(),
                PersistentDataType.INTEGER,
                xp
        );

        updateItemMeta(meta);
        sword.setItemMeta(meta);

        if (level > previousLevel) {
            player.sendMessage(
                    Component.text(
                            "Evolving Sword Level Up! ",
                            NamedTextColor.GOLD
                    ).append(Component.text(
                            previousLevel + " → " + level,
                            NamedTextColor.AQUA
                    )).append(Component.text(
                            " | Attack Damage: "
                                    + formatDamage(getAttackDamage(level)),
                            NamedTextColor.RED
                    ))
            );
        }
    }

    private void updateItemMeta(ItemMeta meta) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int level = readLevel(data);
        int xp = readXp(data);
        double attackDamage = getAttackDamage(level);

        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey("evolving_sword_attack_damage"),
                        attackDamage - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey("evolving_sword_attack_speed"),
                        ATTACK_SPEED - PLAYER_BASE_ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Gains 5 weapon XP by hitting players.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Gains 1 weapon XP by hitting other living entities.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Levels increase the sword's attack damage.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Progress is permanently stored inside the sword.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.empty());

        lore.add(
                Component.text("Level: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                level,
                                NamedTextColor.AQUA
                        ))
        );

        if (level >= MAXIMUM_LEVEL) {
            lore.add(
                    Component.text("Weapon XP: ", NamedTextColor.GRAY)
                            .append(Component.text(
                                    "MAXIMUM LEVEL",
                                    NamedTextColor.GOLD
                            ))
            );
        } else {
            lore.add(
                    Component.text("Weapon XP: ", NamedTextColor.GRAY)
                            .append(Component.text(
                                    xp + " / " + getRequiredXp(level),
                                    NamedTextColor.GREEN
                            ))
            );
        }

        lore.add(
                Component.text("Attack Damage: ", NamedTextColor.GRAY)
                        .append(Component.text(
                                formatDamage(attackDamage),
                                NamedTextColor.RED
                        ))
        );

        lore.add(Component.empty());

        lore.add(Component.text(
                "Custom Weapon",
                NamedTextColor.DARK_GRAY
        ));

        meta.lore(lore);
    }

    private int readLevel(PersistentDataContainer data) {
        int level = data.getOrDefault(
                levelKey(),
                PersistentDataType.INTEGER,
                INITIAL_LEVEL
        );

        return Math.max(
                INITIAL_LEVEL,
                Math.min(MAXIMUM_LEVEL, level)
        );
    }

    private int readXp(PersistentDataContainer data) {
        return Math.max(
                0,
                data.getOrDefault(
                        xpKey(),
                        PersistentDataType.INTEGER,
                        INITIAL_XP
                )
        );
    }

    private int getRequiredXp(int level) {
        return BASE_XP_REQUIREMENT
                + ((level - INITIAL_LEVEL)
                * ADDITIONAL_XP_PER_LEVEL);
    }

    private double getAttackDamage(int level) {
        return INITIAL_ATTACK_DAMAGE
                + ((level - INITIAL_LEVEL) * DAMAGE_PER_LEVEL);
    }

    private org.bukkit.NamespacedKey levelKey() {
        return plugin.getKey("evolving_sword_level");
    }

    private org.bukkit.NamespacedKey xpKey() {
        return plugin.getKey("evolving_sword_xp");
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
                        formatName(enchantment.getKey().getKey())
                                + " I-"
                                + enchantment.getMaxLevel()
                                + " - Normal sword enchantment."
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

    private static String formatName(String key) {
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

    private static String formatDamage(double damage) {
        if (damage == Math.rint(damage)) {
            return Integer.toString((int) damage);
        }

        return String.format(Locale.ROOT, "%.1f", damage);
    }
}