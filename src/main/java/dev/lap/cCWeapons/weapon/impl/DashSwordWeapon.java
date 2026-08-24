package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class DashSwordWeapon extends CustomWeapon {

    private static final int MAXIMUM_DASHES = 3;
    private static final int INITIAL_DASHES = 3;
    private static final long RECHARGE_INTERVAL_MILLIS = 4_000L;

    /*
     * Höhere Werte erzeugen einen stärkeren und weiteren Dash.
     * 1.5 ist schnell, aber noch relativ gut kontrollierbar.
     */
    private static final double DASH_STRENGTH = 1.5;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public DashSwordWeapon(CCWeapons plugin) {
        super(
                "dash_sword",
                "Dash Sword",
                Material.DIAMOND_SWORD,
                List.of(
                        "Right-click to dash in your looking direction.",
                        "Stores up to 3 dashes.",
                        "Restores 1 dash every 4 seconds.",
                        "Dashes are stored inside the weapon.",
                        "Recharges anywhere in your inventory."
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

        if (!data.has(dashesKey(), PersistentDataType.INTEGER)) {
            data.set(
                    dashesKey(),
                    PersistentDataType.INTEGER,
                    INITIAL_DASHES
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
        ItemStack sword =
                player.getInventory().getItemInMainHand();

        long currentTime = System.currentTimeMillis();
        recharge(sword, currentTime);

        ItemMeta meta = sword.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int dashes = readDashes(data);

        if (dashes <= 0) {
            long lastRecharge = data.getOrDefault(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    currentTime
            );

            double remainingSeconds = Math.max(
                    0.0,
                    (RECHARGE_INTERVAL_MILLIS
                            - (currentTime - lastRecharge)) / 1000.0
            );

            player.sendActionBar(
                    Component.text(
                            "No dashes available",
                            NamedTextColor.RED
                    ).append(Component.text(
                            String.format(
                                    Locale.ROOT,
                                    " • Next dash in %.2f seconds",
                                    remainingSeconds
                            ),
                            NamedTextColor.GRAY
                    ))
            );

            return;
        }

        /*
         * Wenn die Waffe komplett aufgeladen war, beginnt der Timer
         * für den nächsten Dash erst beim Verbrauch des ersten Dashes.
         */
        if (dashes >= MAXIMUM_DASHES) {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    currentTime
            );
        }

        int remainingDashes = dashes - 1;

        data.set(
                dashesKey(),
                PersistentDataType.INTEGER,
                remainingDashes
        );

        updateItemMeta(meta);
        sword.setItemMeta(meta);

        Vector dashDirection = player.getEyeLocation()
                .getDirection()
                .normalize()
                .multiply(DASH_STRENGTH);

        player.setVelocity(dashDirection);
        player.setFallDistance(0.0F);

        player.getWorld().spawnParticle(
                Particle.CLOUD,
                player.getLocation().add(0.0, 0.5, 0.0),
                25,
                0.35,
                0.45,
                0.35,
                0.08
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_BREEZE_SHOOT,
                0.8F,
                1.25F
        );

        player.sendActionBar(
                Component.text(
                        "Dash used",
                        NamedTextColor.AQUA
                ).append(Component.text(
                        " • Dashes: "
                                + remainingDashes
                                + " / "
                                + MAXIMUM_DASHES,
                        remainingDashes > 0
                                ? NamedTextColor.GREEN
                                : NamedTextColor.RED
                ))
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

        int dashes = readDashes(data);

        if (dashes >= MAXIMUM_DASHES) {
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

        int restoredDashes = (int) (
                elapsedTime / RECHARGE_INTERVAL_MILLIS
        );

        if (restoredDashes <= 0) {
            return;
        }

        int newDashes = Math.min(
                MAXIMUM_DASHES,
                dashes + restoredDashes
        );

        /*
         * Es werden nur so viele Zeitintervalle verbraucht, wie zum
         * Erreichen des Maximums tatsächlich notwendig waren.
         */
        int appliedDashes = newDashes - dashes;

        data.set(
                dashesKey(),
                PersistentDataType.INTEGER,
                newDashes
        );

        if (newDashes >= MAXIMUM_DASHES) {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    currentTime
            );
        } else {
            data.set(
                    lastRechargeKey(),
                    PersistentDataType.LONG,
                    lastRecharge
                            + ((long) appliedDashes
                            * RECHARGE_INTERVAL_MILLIS)
            );
        }

        updateItemMeta(meta);
        item.setItemMeta(meta);
    }

    private void updateItemMeta(ItemMeta meta) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int dashes = readDashes(data);
        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Right-click to dash in your looking direction.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Stores up to 3 dashes.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Restores 1 dash every 4 seconds.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Dashes are stored inside the weapon.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.text(
                "Recharges anywhere in your inventory.",
                NamedTextColor.GRAY
        ));

        lore.add(Component.empty());

        lore.add(
                Component.text(
                        "Dashes: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        dashes + " / " + MAXIMUM_DASHES,
                        dashes > 0
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

    private int readDashes(PersistentDataContainer data) {
        int dashes = data.getOrDefault(
                dashesKey(),
                PersistentDataType.INTEGER,
                INITIAL_DASHES
        );

        return Math.max(
                0,
                Math.min(MAXIMUM_DASHES, dashes)
        );
    }

    private org.bukkit.NamespacedKey dashesKey() {
        return plugin.getKey("dash_sword_dashes");
    }

    private org.bukkit.NamespacedKey lastRechargeKey() {
        return plugin.getKey("dash_sword_last_recharge");
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
}