package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
//import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
//import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
//import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MobilityDaggerWeapon extends CustomWeapon {

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private static final double ATTACK_DAMAGE = 6.0;
    private static final double ATTACK_SPEED = 2.0;
    private static final double TELEPORT_DISTANCE = 4.0;

    private static final long TELEPORT_COOLDOWN_MILLIS = 6_000L;

    /*
     * Potion amplifier 3 means Speed IV because amplifiers start at zero.
     * The registry calls onHeldTick every 10 ticks, so a duration of 15
     * ticks keeps the effect active while the dagger is held and removes
     * it shortly after the player switches away.
     */
    private static final int SPEED_AMPLIFIER = 3;
    private static final int SPEED_DURATION_TICKS = 15;

    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private final List<Enchantment> compatibleEnchantments;

    public MobilityDaggerWeapon() {
        super(
                "mobility_dagger",
                "Mobility Dagger",
                Material.IRON_SWORD,
                List.of(
                        "Deals 6 attack damage.",
                        "Has a faster attack speed than a sword.",
                        "Grants Speed IV while held in the main hand.",
                        "Right-click to teleport 4 blocks forward.",
                        "Teleport cooldown: 6 seconds."
                )
        );

        this.compatibleEnchantments =
                findCompatibleEnchantments(Material.GOLDEN_SWORD);
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey("mobility_dagger_attack_damage"),
                        ATTACK_DAMAGE - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey("mobility_dagger_attack_speed"),
                        ATTACK_SPEED - PLAYER_BASE_ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onHeldTick(Player player) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        SPEED_DURATION_TICKS,
                        SPEED_AMPLIFIER,
                        false,
                        false,
                        true
                )
        );

        showCooldown(player);
    }

    private void showCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        long availableAt = teleportCooldowns.getOrDefault(
                player.getUniqueId(),
                0L
        );

        if (currentTime >= availableAt) {
            teleportCooldowns.remove(player.getUniqueId());

            player.sendActionBar(
                    Component.text(
                            "Mobility Teleport: ",
                            NamedTextColor.GRAY
                    ).append(Component.text(
                            "READY",
                            NamedTextColor.GREEN
                    )).append(Component.text(
                            " • Right-click",
                            NamedTextColor.YELLOW
                    ))
            );
            return;
        }

        double remainingSeconds =
                (availableAt - currentTime) / 1000.0;

        player.sendActionBar(
                Component.text(
                        "Mobility Teleport: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        String.format(
                                Locale.ROOT,
                                "%.2f seconds",
                                remainingSeconds
                        ),
                        NamedTextColor.AQUA
                ))
        );
    }

    @Override
    public void onRightClick(Player player) {
        long currentTime = System.currentTimeMillis();
        long availableAt = teleportCooldowns.getOrDefault(
                player.getUniqueId(),
                0L
        );

        if (currentTime < availableAt) {
            showCooldown(player);
            return;
        }

        Location destination = findDestination(player);

        if (destination == null) {
            player.sendActionBar(
                    Component.text(
                            "The teleport destination is blocked.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (!player.teleport(destination)) {
            return;
        }

        teleportCooldowns.put(
                player.getUniqueId(),
                currentTime + TELEPORT_COOLDOWN_MILLIS
        );

        player.setFallDistance(0.0F);
        showCooldown(player);
    }

    private Location findDestination(Player player) {
        Location start = player.getLocation();

        Vector movement = player.getEyeLocation()
                .getDirection()
                .normalize()
                .multiply(TELEPORT_DISTANCE);

        Location destination = start.clone().add(movement);
        destination.setYaw(start.getYaw());
        destination.setPitch(start.getPitch());

        if (!isSafeDestination(destination)) {
            return null;
        }

        return destination;
    }

    private boolean isSafeDestination(Location destination) {
        Block feetBlock = destination.getBlock();
        Block headBlock = destination.clone()
                .add(0.0, 1.0, 0.0)
                .getBlock();

        return feetBlock.isPassable()
                && headBlock.isPassable();
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