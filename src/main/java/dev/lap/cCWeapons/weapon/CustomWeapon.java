package dev.lap.cCWeapons.weapon;

import dev.lap.cCWeapons.CCWeapons;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public abstract class CustomWeapon {

    private final String id;
    private final String displayName;
    private final Material material;
    private final List<String> description;

    protected CustomWeapon(
            String id,
            String displayName,
            Material material,
            List<String> description
    ) {
        this.id = Objects.requireNonNull(id).toLowerCase(Locale.ROOT);
        this.displayName = Objects.requireNonNull(displayName);
        this.material = Objects.requireNonNull(material);
        this.description = List.copyOf(description);
    }

    public final ItemStack createItem(CCWeapons plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
                Component.text(displayName, NamedTextColor.AQUA)
        );

        List<Component> lore = new ArrayList<>();
        for (String line : description) {
            lore.add(Component.text(line, NamedTextColor.GRAY));
        }

        lore.add(Component.empty());
        lore.add(Component.text("Custom Weapon", NamedTextColor.DARK_GRAY));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                plugin.getWeaponKey(),
                PersistentDataType.STRING,
                id
        );

        configureItem(plugin, meta);
        item.setItemMeta(meta);
        configureItemStack(plugin, item);

        return item;
    }

    /**
     * Use this hook for Paper data components or other properties
     * that must be applied directly to the completed ItemStack.
     */
    protected void configureItemStack(
            CCWeapons plugin,
            ItemStack item
    ) {
    }

    /**
     * Use this hook to add attributes, custom model data,
     * enchantments or other permanent item properties.
     */
    protected void configureItem(CCWeapons plugin, ItemMeta meta) {
    }

    /**
     * Returns whether this weapon has a left-click ability.
     */
    public boolean usesLeftClickAbility() {
        return false;
    }

    /**
     * Shared left-click ability hook.
     */
    public void onLeftClick(Player player) {
    }

    /**
     * Called when the player left-clicks air or a block while holding the
     * weapon.
     */
    public void onLeftClick(Player player, PlayerInteractEvent event) {
        onLeftClick(player);
    }

    /**
     * Returns whether this weapon has a right-click ability.
     */
    public boolean usesRightClickAbility() {
        return false;
    }

    /**
     * Shared right-click ability hook. This is called for air, blocks,
     * entities and players.
     */
    public void onRightClick(Player player) {
    }

    /**
     * Called when the player right-clicks while holding the weapon.
     */
    public void onRightClick(Player player, PlayerInteractEvent event) {
        onRightClick(player);
    }

    /**
     * Called when the player right-clicks an entity
     * while holding the weapon.
     */
    public void onRightClick(
            Player player,
            PlayerInteractEntityEvent event
    ) {
        onRightClick(player);
    }

    /**
     * Called when the player damages a living entity with the weapon.
     */
    public void onHit(
            Player player,
            LivingEntity target,
            EntityDamageByEntityEvent event
    ) {
    }

    public void onPlayerKill(Player killer, Player victim) {
    }

    public void onPlayerKill(
            Player killer,
            Player victim,
            ItemStack weaponItem
    ) {
        onPlayerKill(killer, victim);
    }

    public void onEntityKill(Player killer, LivingEntity victim) {
    }

    public void onEntityKill(
            Player killer,
            LivingEntity victim,
            ItemStack weaponItem
    ) {
        onEntityKill(killer, victim);
    }

    public void onHeldTick(Player player) {
    }

    public void onInventoryTick(Player player) {
    }

    /**
     * Called for every individual weapon item in a player's inventory.
     * This is useful for data stored inside a specific ItemStack.
     */
    public void onInventoryItemTick(
            Player player,
            ItemStack item
    ) {
    }

    /**
     * Called when a projectile is fired using this weapon.
     */
    public void onProjectileShot(
            Player player,
            Projectile projectile,
            EntityShootBowEvent event
    ) {
    }

    /**
     * Called when a projectile marked as belonging to this weapon hits.
     */
    public void onProjectileHit(ProjectileHitEvent event) {
    }

    public void onInventorySlotChange(
            Player player,
            PlayerInventorySlotChangeEvent event
    ) {
    }

    /**
     * Enchantments accepted by enchanting tables, books and anvils.
     */
    public List<Enchantment> getCompatibleEnchantments() {
        return List.of();
    }

    public final boolean isEnchantmentAllowed(Enchantment enchantment) {
        return getCompatibleEnchantments().contains(enchantment);
    }

    /**
     * Lines displayed in the enchantment guide.
     */
    public List<String> getEnchantmentHints() {
        return List.of("No enchantment information is available.");
    }

    public final String getId() {
        return id;
    }

    public final String getDisplayName() {
        return displayName;
    }

    public final Material getMaterial() {
        return material;
    }

    public final List<String> getDescription() {
        return description;
    }
}