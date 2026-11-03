package dev.lap.standartaddon.weapon.impl;

import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Example weapon demonstrating the optional left-click hook.
 */
public final class PulseBladeWeapon extends CustomWeapon {

    private static final long COOLDOWN_MILLIS = 5_000L;
    private static final int SPEED_DURATION_TICKS = 60;

    private static final List<Enchantment> COMPATIBLE_ENCHANTMENTS =
            List.of(
                    Enchantment.SHARPNESS,
                    Enchantment.UNBREAKING,
                    Enchantment.MENDING
            );

    private final JavaPlugin addon;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PulseBladeWeapon(JavaPlugin addon) {
        super(
                "pulse_blade",
                "Pulse Blade",
                Material.DIAMOND_SWORD,
                List.of(
                        "Left-click releases a small movement pulse.",
                        "The pulse grants Speed II for 3 seconds.",
                        "Ability cooldown: 5 seconds."
                )
        );
        this.addon = addon;
    }

    @Override
    public boolean usesLeftClickAbility() {
        return true;
    }

    @Override
    public void onLeftClick(Player player) {
        long currentTime = System.currentTimeMillis();
        long availableAt = cooldowns.getOrDefault(
                player.getUniqueId(),
                0L
        );

        if (availableAt > currentTime) {
            double remainingSeconds = (availableAt - currentTime) / 1_000.0;
            player.sendActionBar(Component.text(
                    String.format(
                            Locale.ROOT,
                            "Pulse cooldown: %.1f seconds",
                            remainingSeconds
                    ),
                    NamedTextColor.RED
            ));
            return;
        }

        cooldowns.put(player.getUniqueId(), currentTime + COOLDOWN_MILLIS);
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                SPEED_DURATION_TICKS,
                1,
                false,
                true,
                true
        ));

        player.getWorld().spawnParticle(
                Particle.ELECTRIC_SPARK,
                player.getLocation().add(0.0, 1.0, 0.0),
                24,
                0.6,
                0.7,
                0.6,
                0.08
        );
        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_BREEZE_WIND_BURST,
                0.7F,
                1.4F
        );
        player.sendActionBar(Component.text(
                "Movement pulse activated!",
                NamedTextColor.AQUA
        ));

        addon.getServer().getScheduler().runTaskLater(
                addon,
                () -> cooldowns.entrySet().removeIf(entry ->
                        entry.getValue() <= System.currentTimeMillis()),
                120L
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
                "Unbreaking I-III - Reduces durability loss.",
                "Mending I - Repairs the blade with experience."
        );
    }
}