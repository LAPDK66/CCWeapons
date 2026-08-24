package dev.lap.cCWeapons.weapon.impl;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class SoulReaperWeapon extends CustomWeapon {

    private static final int MAXIMUM_SOULS = 120;
    private static final int ENTITY_KILL_SOULS = 1;
    private static final int PLAYER_KILL_SOULS = 5;

    private static final long ABILITY_COOLDOWN_MILLIS = 1_000L;

    private static final int NORMAL_WAVE_COST = 10;
    private static final int STRONG_WAVE_COST = 20;

    private static final double BASE_ATTACK_DAMAGE = 8.0;
    private static final double ENHANCED_ATTACK_DAMAGE = 12.0;
    private static final double SLOW_ATTACK_SPEED = 0.8;
    private static final double SWORD_ATTACK_SPEED = 1.6;

    private static final double PLAYER_BASE_ATTACK_DAMAGE = 1.0;
    private static final double PLAYER_BASE_ATTACK_SPEED = 4.0;

    private static final int DAMAGE_BONUS_THRESHOLD = 25;
    private static final int HEALING_THRESHOLD = 50;
    private static final int SPEED_THRESHOLD = 100;

    private static final double HEALING_PERCENTAGE = 0.10;

    private final CCWeapons plugin;
    private final List<Enchantment> compatibleEnchantments;

    public SoulReaperWeapon(CCWeapons plugin) {
        super(
                "soul_reaper",
                "Soul Reaper",
                Material.NETHERITE_HOE,
                List.of(
                        "Creature kills grant 1 soul.",
                        "Player kills grant 5 souls.",
                        "Right-click: shockwave for 10 souls.",
                        "Sneak and right-click: strong shockwave for 20 souls.",
                        "Stores up to 120 souls."
                )
        );

        this.plugin = plugin;
        this.compatibleEnchantments =
                WeaponSupport.findCompatibleEnchantments(
                        Material.NETHERITE_HOE
                );
    }

    @Override
    protected void configureItem(
            CCWeapons plugin,
            ItemMeta meta
    ) {
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        if (!data.has(soulsKey(), PersistentDataType.INTEGER)) {
            data.set(
                    soulsKey(),
                    PersistentDataType.INTEGER,
                    0
            );
        }

        updateItemMeta(meta);
    }

    @Override
    public boolean usesRightClickAbility() {
        return true;
    }

    @Override
    public void onPlayerKill(
            Player killer,
            Player victim,
            ItemStack weaponItem
    ) {
        addSouls(killer, weaponItem, PLAYER_KILL_SOULS);
    }

    @Override
    public void onEntityKill(
            Player killer,
            LivingEntity victim,
            ItemStack weaponItem
    ) {
        addSouls(killer, weaponItem, ENTITY_KILL_SOULS);
    }

    private void addSouls(
            Player player,
            ItemStack item,
            int amount
    ) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int souls = Math.min(
                MAXIMUM_SOULS,
                readSouls(data) + amount
        );

        data.set(
                soulsKey(),
                PersistentDataType.INTEGER,
                souls
        );

        updateItemMeta(meta);
        item.setItemMeta(meta);

        player.sendActionBar(
                Component.text(
                        "Souls: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        souls + " / " + MAXIMUM_SOULS,
                        NamedTextColor.DARK_PURPLE
                ))
        );
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

        ItemStack item =
                player.getInventory().getItemInMainHand();

        int souls = readSouls(
                item.getItemMeta().getPersistentDataContainer()
        );

        if (souls < HEALING_THRESHOLD) {
            return;
        }

        AttributeInstance maximumHealth =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (maximumHealth == null) {
            return;
        }

        double availableHealth = target.getHealth()
                + target.getAbsorptionAmount();

        double dealtDamage = Math.min(
                event.getFinalDamage(),
                availableHealth
        );

        double healing = dealtDamage * HEALING_PERCENTAGE;

        player.setHealth(
                Math.min(
                        maximumHealth.getValue(),
                        player.getHealth() + healing
                )
        );
    }

    @Override
    public void onRightClick(Player player) {
        long currentTime = System.currentTimeMillis();
        PersistentDataContainer playerData =
                player.getPersistentDataContainer();

        long availableAt = playerData.getOrDefault(
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
                                    java.util.Locale.ROOT,
                                    "Soul Reaper cooldown: %.1f seconds",
                                    remainingSeconds
                            ),
                            NamedTextColor.RED
                    )
            );
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data =
                meta.getPersistentDataContainer();

        int souls = readSouls(data);
        boolean strongWave = player.isSneaking();
        int cost = strongWave
                ? STRONG_WAVE_COST
                : NORMAL_WAVE_COST;

        if (souls < cost) {
            player.sendActionBar(
                    Component.text(
                            "Not enough souls. Required: " + cost,
                            NamedTextColor.RED
                    )
            );
            return;
        }

        data.set(
                soulsKey(),
                PersistentDataType.INTEGER,
                souls - cost
        );

        playerData.set(
                cooldownKey(),
                PersistentDataType.LONG,
                currentTime + ABILITY_COOLDOWN_MILLIS
        );

        updateItemMeta(meta);
        item.setItemMeta(meta);

        releaseShockwave(player, strongWave);
    }

    private void releaseShockwave(
            Player player,
            boolean strongWave
    ) {
        Location center = player.getLocation()
                .add(0.0, 0.5, 0.0);

        double radius = strongWave ? 9.0 : 6.0;
        double damage = strongWave ? 10.0 : 6.0;
        int slownessAmplifier = strongWave ? 3 : 1;
        int slownessDuration = strongWave ? 80 : 50;

        player.getWorld().spawnParticle(
                Particle.SOUL,
                center,
                strongWave ? 220 : 120,
                radius / 2.0,
                1.2,
                radius / 2.0,
                0.12
        );

        player.getWorld().spawnParticle(
                Particle.REVERSE_PORTAL,
                center,
                strongWave ? 180 : 90,
                radius / 2.0,
                1.0,
                radius / 2.0,
                0.1
        );

        player.getWorld().playSound(
                center,
                Sound.ENTITY_WITHER_SHOOT,
                1.0F,
                strongWave ? 0.5F : 0.8F
        );

        for (LivingEntity target :
                center.getNearbyLivingEntities(radius)) {
            if (target.equals(player)) {
                continue;
            }

            target.damage(damage, player);

            target.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            slownessDuration,
                            slownessAmplifier,
                            false,
                            true,
                            true
                    )
            );

            Vector knockback = target.getLocation()
                    .toVector()
                    .subtract(center.toVector());

            if (knockback.lengthSquared() > 0.0) {
                knockback.normalize()
                        .multiply(strongWave ? 1.25 : 0.75);
                knockback.setY(strongWave ? 0.55 : 0.3);
                target.setVelocity(
                        target.getVelocity().add(knockback)
                );
            }
        }
    }

    private void updateItemMeta(ItemMeta meta) {
        int souls = readSouls(
                meta.getPersistentDataContainer()
        );

        double attackDamage = souls >= DAMAGE_BONUS_THRESHOLD
                ? ENHANCED_ATTACK_DAMAGE
                : BASE_ATTACK_DAMAGE;

        double attackSpeed = souls >= SPEED_THRESHOLD
                ? SWORD_ATTACK_SPEED
                : SLOW_ATTACK_SPEED;

        meta.removeAttributeModifier(Attribute.ATTACK_DAMAGE);
        meta.removeAttributeModifier(Attribute.ATTACK_SPEED);

        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        plugin.getKey("soul_reaper_attack_damage"),
                        attackDamage - PLAYER_BASE_ATTACK_DAMAGE,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        plugin.getKey("soul_reaper_attack_speed"),
                        attackSpeed - PLAYER_BASE_ATTACK_SPEED,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND
                )
        );

        List<Component> lore = new ArrayList<>();

        lore.add(Component.text(
                "Creature kills grant 1 soul.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Player kills grant 5 souls.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Right-click: shockwave for 10 souls.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.text(
                "Sneak + right-click: strong wave for 20 souls.",
                NamedTextColor.GRAY
        ));
        lore.add(Component.empty());
        lore.add(
                Component.text(
                        "Souls: ",
                        NamedTextColor.GRAY
                ).append(Component.text(
                        souls + " / " + MAXIMUM_SOULS,
                        NamedTextColor.DARK_PURPLE
                ))
        );
        lore.add(Component.text(
                "25 Souls: +4 attack damage",
                souls >= DAMAGE_BONUS_THRESHOLD
                        ? NamedTextColor.GREEN
                        : NamedTextColor.DARK_GRAY
        ));
        lore.add(Component.text(
                "50 Souls: 10% life steal",
                souls >= HEALING_THRESHOLD
                        ? NamedTextColor.GREEN
                        : NamedTextColor.DARK_GRAY
        ));
        lore.add(Component.text(
                "100 Souls: sword attack speed",
                souls >= SPEED_THRESHOLD
                        ? NamedTextColor.GREEN
                        : NamedTextColor.DARK_GRAY
        ));
        lore.add(Component.empty());
        lore.add(Component.text(
                "Custom Weapon",
                NamedTextColor.DARK_GRAY
        ));

        meta.lore(lore);
    }

    private int readSouls(PersistentDataContainer data) {
        return Math.max(
                0,
                Math.min(
                        MAXIMUM_SOULS,
                        data.getOrDefault(
                                soulsKey(),
                                PersistentDataType.INTEGER,
                                0
                        )
                )
        );
    }

    private org.bukkit.NamespacedKey soulsKey() {
        return plugin.getKey("soul_reaper_souls");
    }

    private org.bukkit.NamespacedKey cooldownKey() {
        return plugin.getKey("soul_reaper_cooldown");
    }

    @Override
    public List<Enchantment> getCompatibleEnchantments() {
        return compatibleEnchantments;
    }

    @Override
    public List<String> getEnchantmentHints() {
        return WeaponSupport.createEnchantmentHints(
                compatibleEnchantments,
                "hoe"
        );
    }
}