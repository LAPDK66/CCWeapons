package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class GravityMaceWeapon extends CustomWeapon {

    private static final double RADIUS = 7.0;
    private static final long COOLDOWN_MILLIS = 24_000L;

    private static final int RESISTANCE_DURATION_TICKS = 40;
    private static final int RESISTANCE_AMPLIFIER = 1;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public GravityMaceWeapon(CCWeapons plugin) {
        super(
                "gravity_mace",
                "Gravity Mace",
                Material.MACE,
                List.of(
                        "Right-click to pull nearby enemies toward you.",
                        "Pulled enemies are launched into the air.",
                        "Sneak and right-click to push enemies away.",
                        "Grants brief Resistance II.",
                        "Ability cooldown: 24 seconds."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                WeaponSupport.findCompatibleEnchantments(Material.MACE);
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onRightClick(Player player) {
        long currentTime = System.currentTimeMillis();
        long availableAt = cooldowns.getOrDefault(
                player.getUniqueId(),
                0L
        );

        if (currentTime < availableAt) {
            double remaining = (availableAt - currentTime) / 1000.0;

            player.sendActionBar(
                    Component.text(
                            String.format(
                                    Locale.ROOT,
                                    "Gravity cooldown: %.1f seconds",
                                    remaining
                            ),
                            NamedTextColor.RED
                    )
            );
            return;
        }

        List<LivingEntity> targets = new ArrayList<>();

        for (LivingEntity target :
                player.getLocation().getNearbyLivingEntities(RADIUS)) {
            if (!target.equals(player) && !target.isDead()) {
                targets.add(target);
            }
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        RESISTANCE_DURATION_TICKS,
                        RESISTANCE_AMPLIFIER,
                        false,
                        true,
                        true
                )
        );

        if (player.isSneaking()) {
            pushTargets(player, targets);
        } else {
            pullTargets(player, targets);
        }

        cooldowns.put(
                player.getUniqueId(),
                currentTime + COOLDOWN_MILLIS
        );
    }

    private void pullTargets(
            Player player,
            List<LivingEntity> targets
    ) {
        Location center = player.getLocation()
                .add(0.0, 0.6, 0.0);

        for (LivingEntity target : targets) {
            Vector movement = center.toVector()
                    .subtract(target.getLocation().toVector());

            if (movement.lengthSquared() > 0.0) {
                movement.normalize().multiply(1.25);
                movement.setY(0.25);
                target.setVelocity(movement);
            }
        }

        spawnGravityParticles(player, Particle.PORTAL);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    for (LivingEntity target : targets) {
                        if (!target.isValid() || target.isDead()) {
                            continue;
                        }

                        Vector velocity = target.getVelocity();
                        velocity.setY(0.8);
                        target.setVelocity(velocity);
                    }
                },
                10L
        );
    }

    private void pushTargets(
            Player player,
            List<LivingEntity> targets
    ) {
        Location center = player.getLocation();

        for (LivingEntity target : targets) {
            Vector movement = target.getLocation()
                    .toVector()
                    .subtract(center.toVector());

            if (movement.lengthSquared() == 0.0) {
                movement = new Vector(0.0, 1.0, 0.0);
            } else {
                movement.normalize();
            }

            movement.multiply(1.7);
            movement.setY(0.55);
            target.setVelocity(movement);
        }

        spawnGravityParticles(player, Particle.REVERSE_PORTAL);
    }

    private void spawnGravityParticles(
            Player player,
            Particle particle
    ) {
        player.getWorld().spawnParticle(
                particle,
                player.getLocation().add(0.0, 1.0, 0.0),
                150,
                RADIUS / 2.0,
                1.5,
                RADIUS / 2.0,
                0.15
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.BLOCK_RESPAWN_ANCHOR_CHARGE,
                1.0F,
                0.7F
        );
    }

    @Override
    public void onHeldTick(Player player) {
        long currentTime = System.currentTimeMillis();
        long availableAt = cooldowns.getOrDefault(
                player.getUniqueId(),
                0L
        );

        if (currentTime >= availableAt) {
            cooldowns.remove(player.getUniqueId());

            player.sendActionBar(
                    Component.text(
                            "Gravity Ability: READY",
                            NamedTextColor.GREEN
                    )
            );
        }
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return WeaponSupport.createEnchantmentHints(
                compatibleEnchantments,
                "mace"
        );
    }
}