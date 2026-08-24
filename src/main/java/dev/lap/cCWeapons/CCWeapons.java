package dev.lap.cCWeapons;

import dev.lap.cCWeapons.command.WeaponCommand;
import dev.lap.cCWeapons.gui.WeaponGuideMenu;
import dev.lap.cCWeapons.listener.WeaponListener;
import dev.lap.cCWeapons.recipe.WeaponRecipeManager;
import dev.lap.cCWeapons.weapon.WeaponRegistry;
import dev.lap.cCWeapons.weapon.impl.BloodPactBladeWeapon;
import dev.lap.cCWeapons.weapon.impl.CursedDaggerWeapon;
import dev.lap.cCWeapons.weapon.impl.DashSwordWeapon;
import dev.lap.cCWeapons.weapon.impl.EvolvingSwordWeapon;
import dev.lap.cCWeapons.weapon.impl.FrostScytheWeapon;
import dev.lap.cCWeapons.weapon.impl.GravityMaceWeapon;
import dev.lap.cCWeapons.weapon.impl.LegacySwordWeapon;
import dev.lap.cCWeapons.weapon.impl.MobilityDaggerWeapon;
import dev.lap.cCWeapons.weapon.impl.SoulReaperWeapon;
import dev.lap.cCWeapons.weapon.impl.TeleportBowWeapon;
import dev.lap.cCWeapons.weapon.impl.ThunderHammerWeapon;
import dev.lap.cCWeapons.weapon.impl.VampiricWeapon;
import dev.lap.cCWeapons.weapon.impl.WindChargeAxeWeapon;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CCWeapons extends JavaPlugin {

    private NamespacedKey weaponKey;
    private WeaponRegistry weaponRegistry;
    private WeaponRecipeManager recipeManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        weaponKey = new NamespacedKey(this, "weapon_id");
        weaponRegistry = new WeaponRegistry(this);

        registerWeapons();

        recipeManager = new WeaponRecipeManager(this, weaponRegistry);
        recipeManager.reloadRecipes();

        WeaponGuideMenu weaponGuideMenu = new WeaponGuideMenu(
                this,
                weaponRegistry
        );

        getServer().getPluginManager().registerEvents(
                new WeaponListener(this, weaponRegistry),
                this
        );
        getServer().getPluginManager().registerEvents(
                weaponGuideMenu,
                this
        );

        weaponRegistry.startEffectTask();

        WeaponCommand weaponCommand = new WeaponCommand(
                this,
                weaponRegistry,
                recipeManager,
                weaponGuideMenu
        );

        PluginCommand command = getCommand("ccweapons");

        if (command == null) {
            getLogger().severe(
                    "The ccweapons command is missing from plugin.yml."
            );
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        command.setExecutor(weaponCommand);
        command.setTabCompleter(weaponCommand);

        getLogger().info(
                "CCWeapons enabled with "
                        + weaponRegistry.getWeapons().size()
                        + " registered weapon(s)."
        );
    }

    private void registerWeapons() {
        weaponRegistry.register(
                new LegacySwordWeapon(
                        "legacy_wooden_sword",
                        "Legacy Wooden Sword",
                        Material.WOODEN_SWORD,
                        4.0
                )
        );

        weaponRegistry.register(
                new LegacySwordWeapon(
                        "legacy_stone_sword",
                        "Legacy Stone Sword",
                        Material.STONE_SWORD,
                        5.0
                )
        );

        weaponRegistry.register(
                new LegacySwordWeapon(
                        "legacy_iron_sword",
                        "Legacy Iron Sword",
                        Material.IRON_SWORD,
                        6.0
                )
        );

        weaponRegistry.register(
                new LegacySwordWeapon(
                        "legacy_golden_sword",
                        "Legacy Golden Sword",
                        Material.GOLDEN_SWORD,
                        4.0
                )
        );

        weaponRegistry.register(
                new LegacySwordWeapon(
                        "legacy_diamond_sword",
                        "Legacy Diamond Sword",
                        Material.DIAMOND_SWORD,
                        7.0
                )
        );

        weaponRegistry.register(
                new VampiricWeapon(
                        "vampiric_mace",
                        "Vampiric Mace",
                        Material.MACE,
                        5.0,
                        0.6
                )
        );

        weaponRegistry.register(
                new VampiricWeapon(
                        "vampiric_netherite_sword",
                        "Vampiric Netherite Sword",
                        Material.NETHERITE_SWORD,
                        7.0,
                        1.6
                )
        );

        weaponRegistry.register(
                new VampiricWeapon(
                        "vampiric_netherite_axe",
                        "Vampiric Netherite Axe",
                        Material.NETHERITE_AXE,
                        9.0,
                        1.0
                )
        );

        weaponRegistry.register(
                new CursedDaggerWeapon()
        );

        weaponRegistry.register(
                new EvolvingSwordWeapon(this)
        );

        weaponRegistry.register(
                new MobilityDaggerWeapon()
        );

        weaponRegistry.register(
                new TeleportBowWeapon(this)
        );

        weaponRegistry.register(
                new WindChargeAxeWeapon(this)
        );

        weaponRegistry.register(
                new DashSwordWeapon(this)
        );

        weaponRegistry.register(
                new FrostScytheWeapon(this)
        );

        weaponRegistry.register(
                new ThunderHammerWeapon(this)
        );

        weaponRegistry.register(
                new GravityMaceWeapon(this)
        );

        weaponRegistry.register(
                new SoulReaperWeapon(this)
        );

        weaponRegistry.register(
                new BloodPactBladeWeapon(this)
        );
    }

    @Override
    public void onDisable() {
        if (recipeManager != null) {
            recipeManager.removeRecipes();
        }

        getServer().getScheduler().cancelTasks(this);
        getLogger().info("CCWeapons disabled.");
    }

    public NamespacedKey getWeaponKey() {
        return weaponKey;
    }

    public NamespacedKey getKey(String key) {
        return new NamespacedKey(this, key);
    }
}