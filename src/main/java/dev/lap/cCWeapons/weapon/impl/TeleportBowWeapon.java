package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class TeleportBowWeapon extends CustomWeapon {

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public TeleportBowWeapon(CCWeapons plugin) {
        super(
                "teleport_bow",
                "Teleport Bow",
                Material.BOW,
                List.of(
                        "Teleports the shooter when an arrow hits.",
                        "Works against blocks and entities.",
                        "The destination must have enough free space."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                findCompatibleEnchantments(Material.BOW);
    }

    @Override
    public void onProjectileShot(
            Player player,
            Projectile projectile,
            EntityShootBowEvent event
    ) {
        projectile.getPersistentDataContainer().set(
                plugin.getWeaponKey(),
                PersistentDataType.STRING,
                getId()
        );
    }

    @Override
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        ProjectileSource source = projectile.getShooter();

        if (!(source instanceof Player player)
                || !player.isOnline()
                || player.isDead()) {
            projectile.remove();
            return;
        }

        Location destination = findDestination(event, player);

        if (destination == null) {
            player.sendActionBar(
                    Component.text(
                            "The teleport destination is blocked.",
                            NamedTextColor.RED
                    )
            );

            projectile.remove();
            return;
        }

        Location impactLocation =
                projectile.getLocation().clone();

        if (player.teleport(destination)) {
            player.setFallDistance(0.0F);

            impactLocation.getWorld().spawnParticle(
                    Particle.PORTAL,
                    impactLocation,
                    100,
                    0.6,
                    0.9,
                    0.6,
                    0.25
            );
        }

        projectile.remove();
    }

    private Location findDestination(
            ProjectileHitEvent event,
            Player player
    ) {
        Location destination;

        if (event.getHitBlock() != null) {
            Block hitBlock = event.getHitBlock();
            BlockFace hitFace = event.getHitBlockFace();

            if (hitFace == null) {
                return null;
            }

            Block adjacentBlock = hitBlock.getRelative(hitFace);

            destination = adjacentBlock.getLocation().add(
                    0.5,
                    0.0,
                    0.5
            );
        } else if (event.getHitEntity() != null) {
            destination = event.getEntity().getLocation().clone();
        } else {
            return null;
        }

        destination.setYaw(player.getLocation().getYaw());
        destination.setPitch(player.getLocation().getPitch());

        return findNearestSafeLocation(destination);
    }

    private Location findNearestSafeLocation(Location origin) {
        /*
         * First try the exact impact location. If that is obstructed,
         * try a small number of positions immediately above it.
         */
        for (int verticalOffset = 0;
             verticalOffset <= 2;
             verticalOffset++) {
            Location candidate = origin.clone().add(
                    0.0,
                    verticalOffset,
                    0.0
            );

            if (isSafeDestination(candidate)) {
                return candidate;
            }
        }

        return null;
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
                                + " - Normal bow enchantment."
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