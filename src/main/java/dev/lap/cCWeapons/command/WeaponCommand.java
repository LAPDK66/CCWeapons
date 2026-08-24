package dev.lap.cCWeapons.command;

import dev.lap.cCWeapons.CCWeapons;
import dev.lap.cCWeapons.gui.WeaponGuideMenu;
import dev.lap.cCWeapons.recipe.WeaponRecipeManager;
import dev.lap.cCWeapons.weapon.CustomWeapon;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WeaponCommand
        implements CommandExecutor, TabCompleter {

    private final CCWeapons plugin;
    private final WeaponRegistry weaponRegistry;
    private final WeaponRecipeManager recipeManager;
    private final WeaponGuideMenu weaponGuideMenu;

    public WeaponCommand(
            CCWeapons plugin,
            WeaponRegistry weaponRegistry,
            WeaponRecipeManager recipeManager,
            WeaponGuideMenu weaponGuideMenu
    ) {
        this.plugin = plugin;
        this.weaponRegistry = weaponRegistry;
        this.recipeManager = recipeManager;
        this.weaponGuideMenu = weaponGuideMenu;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "guide" -> showGuide(sender);
            case "enchantments" -> showEnchantments(sender, args);
            case "give" -> giveWeapon(sender, args);
            case "reload" -> reloadPlugin(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(
                Component.text("CCWeapons Commands", NamedTextColor.AQUA)
        );
        sender.sendMessage(
                Component.text(
                        "/ccweapons guide",
                        NamedTextColor.YELLOW
                ).append(Component.text(
                        " - Open the weapons guide.",
                        NamedTextColor.GRAY
                ))
        );
        sender.sendMessage(
                Component.text(
                        "/ccweapons enchantments <weapon>",
                        NamedTextColor.YELLOW
                ).append(Component.text(
                        " - View compatible enchantments.",
                        NamedTextColor.GRAY
                ))
        );

        if (sender.hasPermission("ccweapons.admin")) {
            sender.sendMessage(
                    Component.text(
                            "/ccweapons give <player> <weapon> [amount]",
                            NamedTextColor.YELLOW
                    )
            );
            sender.sendMessage(
                    Component.text(
                            "/ccweapons reload",
                            NamedTextColor.YELLOW
                    )
            );
        }
    }

    private void showGuide(CommandSender sender) {
        if (sender instanceof Player player) {
            weaponGuideMenu.openGuide(player);
            return;
        }

        sender.sendMessage(
                Component.text(
                        "Enabled Custom Weapons",
                        NamedTextColor.AQUA
                )
        );

        weaponRegistry.getWeapons()
                .stream()
                .filter(weaponRegistry::isEnabled)
                .forEach(weapon ->
                        sender.sendMessage(
                                Component.text(
                                        "- " + weapon.getDisplayName()
                                                + " [" + weapon.getId() + "]",
                                        NamedTextColor.GOLD
                                )
                        )
                );
    }

    private void showEnchantments(
            CommandSender sender,
            String[] args
    ) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.text(
                            "Usage: /ccweapons enchantments <weapon>",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(args[1])
                .filter(weaponRegistry::isEnabled)
                .orElse(null);

        if (weapon == null) {
            sender.sendMessage(
                    Component.text(
                            "Unknown or disabled weapon: " + args[1],
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (sender instanceof Player player) {
            weaponGuideMenu.openEnchantments(player, weapon);
            return;
        }

        sender.sendMessage(
                Component.text(
                        weapon.getDisplayName() + " Enchantments",
                        NamedTextColor.AQUA
                )
        );

        for (String line : weapon.getEnchantmentHints()) {
            sender.sendMessage(
                    Component.text("• " + line, NamedTextColor.GRAY)
            );
        }
    }

    private void giveWeapon(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ccweapons.admin.give")) {
            sender.sendMessage(
                    Component.text(
                            "You do not have permission to use this command.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(
                    Component.text(
                            "Usage: /ccweapons give <player> <weapon> [amount]",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        Player target = plugin.getServer().getPlayer(args[1]);

        if (target == null) {
            sender.sendMessage(
                    Component.text(
                            "Player not found: " + args[1],
                            NamedTextColor.RED
                    )
            );
            return;
        }

        CustomWeapon weapon = weaponRegistry.getWeapon(args[2])
                .orElse(null);

        if (weapon == null) {
            sender.sendMessage(
                    Component.text(
                            "Unknown weapon: " + args[2],
                            NamedTextColor.RED
                    )
            );
            return;
        }

        if (!weaponRegistry.isEnabled(weapon)) {
            sender.sendMessage(
                    Component.text(
                            "That weapon is disabled in the configuration.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        int amount = args.length >= 4
                ? Math.min(parsePositiveInteger(args[3], 1), 64)
                : 1;

        for (int index = 0; index < amount; index++) {
            ItemStack item = weapon.createItem(plugin);

            target.getInventory()
                    .addItem(item)
                    .values()
                    .forEach(leftover ->
                            target.getWorld().dropItemNaturally(
                                    target.getLocation(),
                                    leftover
                            )
                    );
        }

        sender.sendMessage(
                Component.text(
                        "Gave " + amount + "x "
                                + weapon.getDisplayName()
                                + " to " + target.getName() + ".",
                        NamedTextColor.GREEN
                )
        );

        target.sendMessage(
                Component.text(
                        "You received " + amount + "x "
                                + weapon.getDisplayName() + ".",
                        NamedTextColor.GREEN
                )
        );
    }

    private void reloadPlugin(CommandSender sender) {
        if (!sender.hasPermission("ccweapons.admin.reload")) {
            sender.sendMessage(
                    Component.text(
                            "You do not have permission to use this command.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        plugin.reloadConfig();
        recipeManager.reloadRecipes();

        sender.sendMessage(
                Component.text(
                        "CCWeapons configuration and recipes reloaded.",
                        NamedTextColor.GREEN
                )
        );
    }

    private int parsePositiveInteger(
            String input,
            int fallback
    ) {
        try {
            return Math.max(1, Integer.parseInt(input));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            commands.add("guide");
            commands.add("enchantments");

            if (sender.hasPermission("ccweapons.admin")) {
                commands.add("give");
                commands.add("reload");
            }

            return filter(commands, args[0]);
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("enchantments")) {
            return filter(enabledWeaponIds(), args[1]);
        }

        if (args.length == 2
                && args[0].equalsIgnoreCase("give")
                && sender.hasPermission("ccweapons.admin.give")) {
            return filter(
                    plugin.getServer().getOnlinePlayers()
                            .stream()
                            .map(Player::getName)
                            .toList(),
                    args[1]
            );
        }

        if (args.length == 3
                && args[0].equalsIgnoreCase("give")
                && sender.hasPermission("ccweapons.admin.give")) {
            return filter(enabledWeaponIds(), args[2]);
        }

        if (args.length == 4
                && args[0].equalsIgnoreCase("give")
                && sender.hasPermission("ccweapons.admin.give")) {
            return filter(
                    List.of("1", "2", "4", "8", "16"),
                    args[3]
            );
        }

        return List.of();
    }

    private List<String> enabledWeaponIds() {
        return weaponRegistry.getWeapons()
                .stream()
                .filter(weaponRegistry::isEnabled)
                .map(CustomWeapon::getId)
                .sorted()
                .toList();
    }

    private List<String> filter(
            List<String> suggestions,
            String input
    ) {
        String lowerInput = input.toLowerCase(Locale.ROOT);

        return suggestions.stream()
                .filter(value ->
                        value.toLowerCase(Locale.ROOT)
                                .startsWith(lowerInput)
                )
                .sorted()
                .toList();
    }
}