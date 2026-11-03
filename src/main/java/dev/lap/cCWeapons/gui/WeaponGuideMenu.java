package dev.lap.cCWeapons.gui;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.recipe.WeaponRecipeDefinition;
import dev.lap.cCWeapons.recipe.WeaponRecipeManager;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class WeaponGuideMenu implements Listener {

    private static final int MENU_SIZE = 54;
    private static final int BACK_SLOT = 49;
    private static final int PREVIOUS_SLOT = 45;
    private static final int NEXT_SLOT = 53;
    private static final int RECIPE_RESULT_SLOT = 25;

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] RECIPE_SLOTS = {
            11, 12, 13,
            20, 21, 22,
            29, 30, 31
    };

    private final CCWeapons plugin;
    private final WeaponRegistry weaponRegistry;
    private final WeaponRecipeManager recipeManager;

    public WeaponGuideMenu(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry,
            WeaponRecipeManager recipeManager
    ) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
        this.recipeManager = recipeManager;
    }

    public void openGuide(Player player) {
        openGuide(player, 0);
    }

    public void openGuide(Player player, int requestedPage) {
        List<CustomWeapon> weapons = enabledWeapons();
        int pageCount = pageCount(weapons.size(), CONTENT_SLOTS.length);
        int page = clampPage(requestedPage, pageCount);

        GuideHolder holder = new GuideHolder(page);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(
                        "Custom Weapons Guide (" + (page + 1)
                                + "/" + pageCount + ")"
                )
        );
        holder.setInventory(inventory);

        int startIndex = page * CONTENT_SLOTS.length;
        int endIndex = Math.min(
                weapons.size(),
                startIndex + CONTENT_SLOTS.length
        );

        for (int index = startIndex; index < endIndex; index++) {
            CustomWeapon weapon = weapons.get(index);
            inventory.setItem(
                    CONTENT_SLOTS[index - startIndex],
                    createWeaponIcon(weapon)
            );
        }

        addNavigation(inventory, page, pageCount, true);
        player.openInventory(inventory);
    }

    public void openEnchantments(
            Player player,
            CustomWeapon weapon
    ) {
        openEnchantments(player, weapon, 0, 0);
    }

    private void openEnchantments(
            Player player,
            CustomWeapon weapon,
            int guidePage,
            int requestedPage
    ) {
        List<Enchantment> enchantments = weapon.getCompatibleEnchantments()
                .stream()
                .sorted(Comparator.comparing(enchantment ->
                        enchantment.getKey().toString()))
                .toList();
        int pageCount = pageCount(enchantments.size(), CONTENT_SLOTS.length);
        int page = clampPage(requestedPage, pageCount);

        EnchantmentHolder holder = new EnchantmentHolder(
                weapon.getId(),
                guidePage,
                page
        );
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(
                        weapon.getDisplayName() + " Enchantments ("
                                + (page + 1) + "/" + pageCount + ")"
                )
        );
        holder.setInventory(inventory);

        int startIndex = page * CONTENT_SLOTS.length;
        int endIndex = Math.min(
                enchantments.size(),
                startIndex + CONTENT_SLOTS.length
        );

        for (int index = startIndex; index < endIndex; index++) {
            inventory.setItem(
                    CONTENT_SLOTS[index - startIndex],
                    createEnchantmentIcon(enchantments.get(index))
            );
        }

        if (enchantments.isEmpty()) {
            inventory.setItem(
                    22,
                    createInfoItem(
                            Material.BARRIER,
                            "No compatible enchantments",
                            NamedTextColor.RED
                    )
            );
        }

        addNavigation(inventory, page, pageCount, false);
        player.openInventory(inventory);
    }

    private void openRecipe(
            Player player,
            CustomWeapon weapon,
            int guidePage
    ) {
        RecipeHolder holder = new RecipeHolder(weapon.getId(), guidePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(weapon.getDisplayName() + " Recipe")
        );
        holder.setInventory(inventory);

        WeaponRecipeDefinition recipe = recipeManager.getRecipe(weapon)
                .orElse(null);

        if (recipe == null) {
            inventory.setItem(
                    21,
                    createInfoItem(
                            Material.BARRIER,
                            "No recipe configured",
                            NamedTextColor.RED
                    )
            );
        } else {
            populateRecipe(inventory, recipe);

            ItemStack result = weapon.createItem(plugin);
            result.setAmount(Math.min(
                    result.getMaxStackSize(),
                    recipe.amount()
            ));
            inventory.setItem(RECIPE_RESULT_SLOT, result);
        }

        inventory.setItem(
                BACK_SLOT,
                createNavigationItem(
                        Material.ARROW,
                        "Back to guide",
                        NamedTextColor.YELLOW
                )
        );
        player.openInventory(inventory);
    }

    private void populateRecipe(
            Inventory inventory,
            WeaponRecipeDefinition recipe
    ) {
        for (int row = 0; row < recipe.shape().size(); row++) {
            String shapeRow = recipe.shape().get(row);

            for (int column = 0; column < shapeRow.length(); column++) {
                char ingredientKey = shapeRow.charAt(column);
                Material material = recipe.ingredients().get(ingredientKey);

                if (material != null) {
                    inventory.setItem(
                            RECIPE_SLOTS[row * 3 + column],
                            new ItemStack(material)
                    );
                }
            }
        }

        inventory.setItem(
                23,
                createInfoItem(
                        Material.CRAFTING_TABLE,
                        "Crafting Result",
                        NamedTextColor.GOLD
                )
        );
    }

    private ItemStack createWeaponIcon(CustomWeapon weapon) {
        ItemStack icon = weapon.createItem(plugin);
        ItemMeta meta = icon.getItemMeta();
        List<Component> lore = meta.lore() == null
                ? new ArrayList<>()
                : new ArrayList<>(meta.lore());

        lore.add(Component.empty());
        lore.add(Component.text(
                "Left-click: View recipe",
                NamedTextColor.GREEN
        ));
        lore.add(Component.text(
                "Right-click: View compatible enchantments",
                NamedTextColor.LIGHT_PURPLE
        ));

        meta.lore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    private ItemStack createEnchantmentIcon(Enchantment enchantment) {
        ItemStack icon = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text(
                formatName(enchantment.getKey().getKey()),
                NamedTextColor.LIGHT_PURPLE
        ));
        meta.lore(List.of(
                Component.text(
                        "Maximum level: " + enchantment.getMaxLevel(),
                        NamedTextColor.GRAY
                ),
                Component.text(
                        "Compatible with this weapon.",
                        NamedTextColor.GREEN
                )
        ));
        icon.setItemMeta(meta);
        return icon;
    }

    private void addNavigation(
            Inventory inventory,
            int page,
            int pageCount,
            boolean closeOnBack
    ) {
        if (page > 0) {
            inventory.setItem(
                    PREVIOUS_SLOT,
                    createNavigationItem(
                            Material.ARROW,
                            "Previous page",
                            NamedTextColor.YELLOW
                    )
            );
        }

        if (page + 1 < pageCount) {
            inventory.setItem(
                    NEXT_SLOT,
                    createNavigationItem(
                            Material.ARROW,
                            "Next page",
                            NamedTextColor.YELLOW
                    )
            );
        }

        inventory.setItem(
                BACK_SLOT,
                createNavigationItem(
                        Material.BARRIER,
                        closeOnBack ? "Back / Close" : "Back to guide",
                        NamedTextColor.RED
                )
        );
    }

    private ItemStack createNavigationItem(
            Material material,
            String name,
            NamedTextColor color
    ) {
        return createInfoItem(material, name, color);
    }

    private ItemStack createInfoItem(
            Material material,
            String name,
            NamedTextColor color
    ) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name, color));
        item.setItemMeta(meta);
        return item;
    }

    private List<CustomWeapon> enabledWeapons() {
        return weaponRegistry.getWeapons().stream()
                .filter(weaponRegistry::isEnabled)
                .sorted(Comparator.comparing(CustomWeapon::getId))
                .toList();
    }

    private int pageCount(int itemCount, int pageSize) {
        return Math.max(1, (itemCount + pageSize - 1) / pageSize);
    }

    private int clampPage(int requestedPage, int pageCount) {
        return Math.max(0, Math.min(requestedPage, pageCount - 1));
    }

    private String formatName(String key) {
        String[] words = key.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    word.substring(0, 1).toUpperCase(Locale.ROOT)
            );

            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }

        return result.toString();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        InventoryHolder holder = event.getView()
                .getTopInventory()
                .getHolder();

        if (!(holder instanceof MenuHolder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getClickedInventory() == null
                || !event.getClickedInventory().equals(
                event.getView().getTopInventory())) {
            return;
        }

        if (holder instanceof GuideHolder guideHolder) {
            handleGuideClick(event, player, guideHolder);
        } else if (holder instanceof EnchantmentHolder enchantmentHolder) {
            handleEnchantmentClick(event, player, enchantmentHolder);
        } else if (holder instanceof RecipeHolder recipeHolder
                && event.getRawSlot() == BACK_SLOT) {
            openGuide(player, recipeHolder.guidePage());
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder()
                instanceof MenuHolder)) {
            return;
        }

        int topInventorySize = event.getView().getTopInventory().getSize();
        if (event.getRawSlots().stream().anyMatch(slot ->
                slot < topInventorySize)) {
            event.setCancelled(true);
        }
    }

    private void handleGuideClick(
            InventoryClickEvent event,
            Player player,
            GuideHolder holder
    ) {
        if (event.getRawSlot() == BACK_SLOT) {
            player.closeInventory();
            return;
        }

        if (event.getRawSlot() == PREVIOUS_SLOT) {
            openGuide(player, holder.page() - 1);
            return;
        }

        if (event.getRawSlot() == NEXT_SLOT) {
            openGuide(player, holder.page() + 1);
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(event.getCurrentItem())
                .filter(weaponRegistry::isEnabled)
                .orElse(null);
        if (weapon == null) {
            return;
        }

        if (event.isRightClick()) {
            openEnchantments(player, weapon, holder.page(), 0);
        } else if (event.isLeftClick()) {
            openRecipe(player, weapon, holder.page());
        }
    }

    private void handleEnchantmentClick(
            InventoryClickEvent event,
            Player player,
            EnchantmentHolder holder
    ) {
        if (event.getRawSlot() == BACK_SLOT) {
            openGuide(player, holder.guidePage());
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(holder.weaponId())
                .filter(weaponRegistry::isEnabled)
                .orElse(null);
        if (weapon == null) {
            openGuide(player, holder.guidePage());
            return;
        }

        if (event.getRawSlot() == PREVIOUS_SLOT) {
            openEnchantments(
                    player,
                    weapon,
                    holder.guidePage(),
                    holder.page() - 1
            );
        } else if (event.getRawSlot() == NEXT_SLOT) {
            openEnchantments(
                    player,
                    weapon,
                    holder.guidePage(),
                    holder.page() + 1
            );
        }
    }

    private interface MenuHolder extends InventoryHolder {
    }

    private abstract static class AbstractMenuHolder implements MenuHolder {

        private Inventory inventory;

        protected final void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final class GuideHolder extends AbstractMenuHolder {

        private final int page;

        private GuideHolder(int page) {
            this.page = page;
        }

        private int page() {
            return page;
        }
    }

    private static final class EnchantmentHolder
            extends AbstractMenuHolder {

        private final String weaponId;
        private final int guidePage;
        private final int page;

        private EnchantmentHolder(
                String weaponId,
                int guidePage,
                int page
        ) {
            this.weaponId = weaponId;
            this.guidePage = guidePage;
            this.page = page;
        }

        private String weaponId() {
            return weaponId;
        }

        private int guidePage() {
            return guidePage;
        }

        private int page() {
            return page;
        }
    }

    private static final class RecipeHolder extends AbstractMenuHolder {

        private final String weaponId;
        private final int guidePage;

        private RecipeHolder(String weaponId, int guidePage) {
            this.weaponId = weaponId;
            this.guidePage = guidePage;
        }

        @SuppressWarnings("unused")
        private String weaponId() {
            return weaponId;
        }

        private int guidePage() {
            return guidePage;
        }
    }
}