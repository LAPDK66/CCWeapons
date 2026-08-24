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
import org.bukkit.entity.Player;
import org.bukkit.entity.WindCharge;
//import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WindChargeAxeWeapon extends CustomWeapon {

    private static final int MAXIMUM_CHARGES = 64;
    private static final int INITIAL_CHARGES = 64;
    private static final long RECHARGE_INTERVAL_MILLIS = 1_000L;

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    /*
     * A normal diamond axe deals 9 total attack damage.
     * This axe deals two less, resulting in 7 attack damage.
     */
    private static final double ATTACK_DAMAGE = 7.0;
    private static final double ATTACK_SPEED = 1.0;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public WindChargeAxeWeapon(CCWeapons plugin) {
        super(
                "wind_charge_axe",
                "Wind Charge Axe",
                Material.DIAMOND_AXE,
                List.of(
                        "Deals 2 less damage than a diamond axe.",
                        "Right-click to fire a wind charge.",
                        "Stores up to 64 wind charges.",
                        "Restores 1 charge every second.",
                        "Recharges anywhere in your inventory."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                findCompatibleEnchantments(Material.DIAMOND_AXE);
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
                    INITIAL_CHARGES
            );
        }

        if (!data.has(lastRechargeKey(), PersistentDataType.LONG)) {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    System.currentTimeMillis()
            );
        }

        updateItemMeta(meta);
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onRightClick(Player player) {
        ItemStack axe =
                player.getInventory().getItemInMainHand();

        long currentTime = System.currentTimeMillis();
        recharge(axe, currentTime);

        ItemMeta meta = axe.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charges = readCharges(data);

        if (charges <= 0) {
            player.sendActionBar(
                    Component.text(
                            "No wind charges available.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        /*
         * When firing from a completely full axe, reset the recharge
         * timestamp. This prevents time spent at 64 charges from being
         * converted into immediate free charges after firing.
         */
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

        updateItemMeta(meta);
        axe.setItemMeta(meta);

        WindCharge windCharge =
                player.launchProjectile(WindCharge.class);

        windCharge.setVelocity(
                player.getEyeLocation()
                        .getDirection()
                        .normalize()
                        .multiply(1.5)
        );
    }

    @Override
    public void onInventoryItemTick(
            Player player,
            ItemStack item
    ) {
        recharge(item, System.currentTimeMillis());
    }

    private void recharge(
            ItemStack item,
            long currentTime
    ) {
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

        long elapsedTime = Math.max(
                0L,
                currentTime - lastRecharge
        );

        int restoredCharges = (int) (
                elapsedTime / RECHARGE_INTERVAL_MILLIS
        );

        if (restoredCharges <= 0) {
            return;
        }

        int newCharges = Math.min(
                MAXIMUM_CHARGES,
                charges + restoredCharges
        );

        long consumedIntervals =
                (long) restoredCharges
                        * RECHARGE_INTERVAL_MILLIS;

        data.set(
                chargesKey(),
                PersistentDataType.INTEGER,
                newCharges
        );

        data.set(
                lastRechargeKey(),
                PersistentDataType.LONG,
                lastRecharge + consumedIntervals
        );

        updateItemMeta(meta);
        item.setItemMeta(meta);
    }

    private void updateItemMeta(ItemMeta meta) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int charges = readCharges(data);

        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey(
                                "wind_charge_axe_attack_damage"
                        ),
                        ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey(
                                "wind_charge_axe_attack_speed"
                        ),
                        ATTACK_SPEED - PLAYER_BASE_ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Deals 2 less damage than a diamond axe.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Right-click to fire a wind charge.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Restores 1 charge every second.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Recharges anywhere in your inventory.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.empty());

        lore.add(
                Component.text(
                        "Wind Charges: ",
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
        int charges = data.getOrDefault(
                chargesKey(),
                PersistentDataType.INTEGER,
                INITIAL_CHARGES
        );

        return Math.max(
                0,
                Math.min(MAXIMUM_CHARGES, charges)
        );
    }

    private org.bukkit.NamespacedKey chargesKey() {
        return plugin.getKey("wind_charge_axe_charges");
    }

    private org.bukkit.NamespacedKey lastRechargeKey() {
        return plugin.getKey("wind_charge_axe_last_recharge");
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return compatibleEnchantments.stream()
                .sorted(Comparator.comparing(
                        enchantment -> enchantment.getKey().getKey()
                ))
                .map(enchantment ->
                        formatName(enchantment.getKey().getKey())
                                + " I-"
                                + enchantment.getMaxLevel()
                                + " - Normal axe enchantment."
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
}