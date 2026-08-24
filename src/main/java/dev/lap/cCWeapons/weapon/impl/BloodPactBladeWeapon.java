package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public final class BloodPactBladeWeapon extends CustomWeapon {

    private static final double SACRIFICED_HEALTH = 16.0;

    private static final int STRENGTH_DURATION_TICKS = 600;
    private static final int STRENGTH_AMPLIFIER = 5;

    private static final long ACTIVE_DURATION_MILLIS = 30_000L;
    private static final long COOLDOWN_MILLIS = 60_000L;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public BloodPactBladeWeapon(CCWeapons plugin) {
        super(
                "blood_pact_blade",
                "Blood Pact Blade",
                Material.NETHERITE_SWORD,
                List.of(
                        "Right-click to sacrifice 8 hearts.",
                        "Grants Strength VI for 30 seconds.",
                        "The ability cannot kill its user.",
                        "Ability cooldown: 60 seconds."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                WeaponSupport.findCompatibleEnchantments(
                        Material.NETHERITE_SWORD
                );
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onRightClick(Player player) {
        long currentTime = System.currentTimeMillis();

        PersistentDataContainer data =
                player.getPersistentDataContainer();

        long availableAt = data.getOrDefault(
                cooldownKey(),
                PersistentDataType.LONG,
                0L
        );

        if (currentTime < availableAt) {
            double remainingSeconds =
                    (availableAt - currentTime) / 1000.0;

            player.sendActionBar(
                    Component.text(
                            String.format(
                                    Locale.ROOT,
                                    "Blood Pact cooldown: %.1f seconds",
                                    remainingSeconds
                            ),
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (player.getHealth() <= SACRIFICED_HEALTH) {
            player.sendActionBar(
                    Component.text(
                            "You need more than 8 hearts to use Blood Pact.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        player.setHealth(
                player.getHealth() - SACRIFICED_HEALTH
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        STRENGTH_DURATION_TICKS,
                        STRENGTH_AMPLIFIER,
                        false,
                        false,
                        true
                )
        );

        long activeUntil =
                currentTime + ACTIVE_DURATION_MILLIS;

        data.set(
                cooldownKey(),
                PersistentDataType.LONG,
                currentTime + COOLDOWN_MILLIS
        );

        data.set(
                activeUntilKey(),
                PersistentDataType.LONG,
                activeUntil
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_HURT,
                1.0F,
                0.7F
        );

        startBloodParticles(player, activeUntil);

        player.sendActionBar(
                Component.text(
                        "Blood Pact activated: Strength VI for 30 seconds",
                        NamedTextColor.DARK_RED
                )
        );
    }

    private void startBloodParticles(
            Player player,
            long activeUntil
    ) {
        AtomicReference<BukkitTask> taskReference =
                new AtomicReference<>();

        BukkitTask task = plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        () -> {
                            if (!player.isOnline()
                                    || player.isDead()
                                    || System.currentTimeMillis()
                                    >= activeUntil) {
                                BukkitTask runningTask =
                                        taskReference.get();

                                if (runningTask != null) {
                                    runningTask.cancel();
                                }

                                return;
                            }

                            player.getWorld().spawnParticle(
                                    Particle.DUST,
                                    player.getLocation()
                                            .add(0.0, 1.0, 0.0),
                                    18,
                                    0.5,
                                    0.8,
                                    0.5,
                                    0.0,
                                    new Particle.DustOptions(
                                            Color.fromRGB(160, 0, 0),
                                            1.25F
                                    )
                            );
                        },
                        0L,
                        5L
                );

        taskReference.set(task);
    }

    @Override
    public void onHeldTick(Player player) {
        long currentTime = System.currentTimeMillis();

        long availableAt = player.getPersistentDataContainer()
                .getOrDefault(
                        cooldownKey(),
                        PersistentDataType.LONG,
                        0L
                );

        if (currentTime >= availableAt) {
            player.sendActionBar(
                    Component.text(
                            "Blood Pact: READY",
                            NamedTextColor.GREEN
                    )
            );
            return;
        }

        double remainingSeconds =
                (availableAt - currentTime) / 1000.0;

        player.sendActionBar(
                Component.text(
                        String.format(
                                Locale.ROOT,
                                "Blood Pact cooldown: %.1f seconds",
                                remainingSeconds
                        ),
                        NamedTextColor.RED
                )
        );
    }

    private org.bukkit.NamespacedKey cooldownKey() {
        return plugin.getKey("blood_pact_cooldown");
    }

    private org.bukkit.NamespacedKey activeUntilKey() {
        return plugin.getKey("blood_pact_active_until");
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return WeaponSupport.createEnchantmentHints(
                compatibleEnchantments,
                "sword"
        );
    }
}