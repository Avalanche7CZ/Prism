package me.botsko.prism;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class PrismConfig extends ConfigBase {

    /**
     * * @param plugin
     */
    public PrismConfig(Plugin plugin) {
        super( plugin );
    }

    /**
     *
     */
    @Override
    public FileConfiguration getConfig() {

        config = plugin.getConfig();

        config.addDefault( "prism.debug", false );
        config.addDefault( "prism.allow-metrics", true );
        // --- Database Settings ---
        config.addDefault("prism.database.type", "sqlite"); // Changed default to sqlite for easy setup
        // Options: mysql, mariadb, postgresql, h2, sqlite

        // --- Settings for server-based databases (MySQL, MariaDB, PostgreSQL) ---
        config.addDefault("prism.database.hostname", "127.0.0.1");
        config.addDefault("prism.database.port", "3306"); // Default for MySQL/MariaDB, PostgreSQL is 5432
        config.addDefault("prism.database.databaseName", "minecraft");
        config.addDefault("prism.database.username", "root");
        config.addDefault("prism.database.password", "");
        config.addDefault("prism.database.tablePrefix", "prism_");

        // --- Settings for file-based databases (H2, SQLite) ---
        config.addDefault("prism.database.filePath", "prism.db"); // Filename, relative to plugin data folder

        // --- H2 Specific Settings ---
        config.addDefault("prism.database.h2.mysqlMode", true); // For better compatibility if migrating or using MySQL-like queries
        config.addDefault("prism.database.h2.autoServer", false); // Allows multiple connections to the same H2 file db

        // --- JDBC Driver and URL (can be overridden per type if needed, but usually auto-detected) ---
        config.addDefault("prism.database.driverClassName", "org.sqlite.JDBC"); // Example for SQLite
        config.addDefault("prism.database.jdbcUrlPrefix", "jdbc:sqlite:"); // Example for SQLite

        // --- Connection Pool settings (Tomcat JDBC Pool) ---
        config.addDefault("prism.database.pool.max-connections", 20);
        config.addDefault("prism.database.pool.initial-size", 5); // Reduced default initial size
        config.addDefault("prism.database.pool.max-idle-connections", 10);
        config.addDefault("prism.database.pool.max-wait-ms", 30000);
        config.addDefault("prism.database.max-failures-before-wait", 5);
        config.addDefault("prism.database.actions-per-insert-batch", 1000);
        config.addDefault("prism.database.force-write-queue-on-shutdown", true);

        // pste.me sharing.
        config.addDefault( "prism.paste.enable", false );
        config.addDefault( "prism.paste.username", "Username on http://pste.me/#/signup" );
        config.addDefault( "prism.paste.api-key", "API key from http://pste.me/#/account" );

        // Wands
        config.addDefault( "prism.wands.default-mode", "hand" ); // hand, item, or block
        config.addDefault( "prism.wands.default-item-mode-id", "280:0" );
        config.addDefault( "prism.wands.default-block-mode-id", "17:1" );
        config.addDefault( "prism.wands.auto-equip", true );
        config.addDefault( "prism.wands.allow-user-override", true );
        final List<String> ignoreActionsForInspect = new ArrayList<String>();
        ignoreActionsForInspect.add( "player-chat" );
        ignoreActionsForInspect.add( "player-command" );
        ignoreActionsForInspect.add( "player-join" );
        ignoreActionsForInspect.add( "player-quit" );
        config.addDefault( "prism.wands.inspect.ignore-actions", ignoreActionsForInspect );

        // Queries
        config.addDefault( "prism.queries.default-radius", 5 );
        config.addDefault( "prism.queries.default-time-since", "3d" );
        config.addDefault( "prism.queries.max-lookup-radius", 100 );
        config.addDefault( "prism.queries.max-applier-radius", 100 );
        config.addDefault( "prism.queries.never-use-defaults", false );
        config.addDefault( "prism.queries.lookup-max-results", 1000 );
        config.addDefault( "prism.queries.default-results-per-page", 5 );
        config.addDefault( "prism.queries.lookup-auto-group", true );

        // Messenger
        config.addDefault( "prism.messenger.always-show-extended", false );

        // Near
        config.addDefault( "prism.near.default-radius", 5 );
        config.addDefault( "prism.near.max-results", 100 );

        // Drain
        config.addDefault( "prism.drain.max-radius", 10 );
        config.addDefault( "prism.drain.default-radius", 5 );

        // Ex
        config.addDefault( "prism.ex.max-radius", 100 );
        config.addDefault( "prism.ex.default-radius", 10 );

        // Ignore
        config.addDefault( "prism.ignore.enable-perm-nodes", false );
        config.addDefault( "prism.ignore.players-in-creative", false );
        config.addDefault( "prism.ignore.players", new ArrayList<String>() );
        config.addDefault( "prism.ignore.worlds", new ArrayList<String>() );

        // Purge
        final List<String> purgeRules = new ArrayList<String>();
        purgeRules.add( "before:8w" );
        purgeRules.add( "a:water-flow before:4w" );
        config.addDefault( "prism.db-records-purge-rules", purgeRules );
        config.addDefault( "prism.purge.batch-tick-delay", 60 );
        config.addDefault( "prism.purge.records-per-batch", 500000 );

        // Appliers
        config.addDefault( "prism.appliers.allow-rollback-items-removed-from-container", true );
        config.addDefault( "prism.appliers.notify-nearby.enabled", true );
        config.addDefault( "prism.appliers.notify-nearby.additional-radius", 20 );
        config.addDefault( "prism.appliers.remove-fire-on-burn-rollback", true );
        config.addDefault( "prism.appliers.remove-drops-on-explode-rollback", true );

        // Illegal Entity Rollbacks
        final List<String> illegalEntities = new ArrayList<String>();
        illegalEntities.add( "creeper" );
        config.addDefault( "prism.appliers.never-spawn-entity", illegalEntities );

        // Illegal Block Rollbacks
        final List<Integer> illegalBlocks = new ArrayList<Integer>();
        illegalBlocks.add( 10 ); // Flowing Lava
        illegalBlocks.add( 11 ); // Still Lava
        illegalBlocks.add( 46 ); // TNT
        illegalBlocks.add( 51 ); // Fire
        config.addDefault( "prism.appliers.never-place-block", illegalBlocks );

        // Tracking (Defaults seem reasonable, ensure they match desired behavior)
        config.addDefault( "prism.tracking.block-break", true );
        config.addDefault( "prism.tracking.block-burn", true );
        config.addDefault( "prism.tracking.block-dispense", true );
        config.addDefault( "prism.tracking.block-fade", true );
        config.addDefault( "prism.tracking.block-fall", true );
        config.addDefault( "prism.tracking.block-form", true );
        config.addDefault( "prism.tracking.block-place", true );
        config.addDefault( "prism.tracking.block-shift", true );
        config.addDefault( "prism.tracking.block-spread", true );
        config.addDefault( "prism.tracking.block-use", true );
        config.addDefault( "prism.tracking.bucket-fill", true );
        config.addDefault( "prism.tracking.bonemeal-use", true );
        config.addDefault( "prism.tracking.container-access", true );
        config.addDefault( "prism.tracking.cake-eat", true );
        config.addDefault( "prism.tracking.craft-item", false );
        config.addDefault( "prism.tracking.creeper-explode", true );
        config.addDefault( "prism.tracking.crop-trample", true );
        config.addDefault( "prism.tracking.dragon-eat", true );
        config.addDefault( "prism.tracking.enchant-item", false );
        config.addDefault( "prism.tracking.enderman-pickup", true );
        config.addDefault( "prism.tracking.enderman-place", true );
        config.addDefault( "prism.tracking.entity-break", true );
        config.addDefault( "prism.tracking.entity-dye", false );
        config.addDefault( "prism.tracking.entity-explode", true );
        config.addDefault( "prism.tracking.entity-follow", true ); // Can be very spammy
        config.addDefault( "prism.tracking.entity-form", true );
        config.addDefault( "prism.tracking.entity-kill", true );
        config.addDefault( "prism.tracking.entity-leash", true );
        config.addDefault( "prism.tracking.entity-shear", true );
        config.addDefault( "prism.tracking.entity-spawn", true );
        config.addDefault( "prism.tracking.entity-unleash", true );
        config.addDefault( "prism.tracking.fireball", true );
        config.addDefault( "prism.tracking.fire-spread", false );
        config.addDefault( "prism.tracking.firework-launch", true );
        config.addDefault( "prism.tracking.hangingitem-break", true );
        config.addDefault( "prism.tracking.hangingitem-place", true );
        config.addDefault( "prism.tracking.item-drop", true );
        config.addDefault( "prism.tracking.item-insert", true );
        config.addDefault( "prism.tracking.item-pickup", true );
        config.addDefault( "prism.tracking.item-remove", true );
        config.addDefault( "prism.tracking.item-rotate", true );
        config.addDefault( "prism.tracking.lava-break", true ); // Breaking obsidian from lava
        config.addDefault( "prism.tracking.lava-bucket", true );
        config.addDefault( "prism.tracking.lava-flow", false ); // Can be extremely spammy
        config.addDefault( "prism.tracking.lava-ignite", true );
        config.addDefault( "prism.tracking.leaf-decay", true );
        config.addDefault( "prism.tracking.lighter", true );
        config.addDefault( "prism.tracking.lightning", true );
        config.addDefault( "prism.tracking.mushroom-grow", true );
        config.addDefault( "prism.tracking.player-chat", false );
        config.addDefault( "prism.tracking.player-command", false );
        config.addDefault( "prism.tracking.player-death", true );
        config.addDefault( "prism.tracking.player-join", false );
        config.addDefault( "prism.tracking.player-kill", true );
        config.addDefault( "prism.tracking.player-quit", false );
        config.addDefault( "prism.tracking.player-teleport", false );
        config.addDefault( "prism.tracking.potion-splash", true );
        config.addDefault( "prism.tracking.sheep-eat", true );
        config.addDefault( "prism.tracking.sign-change", true );
        config.addDefault( "prism.tracking.spawnegg-use", true );
        config.addDefault( "prism.tracking.tnt-explode", true );
        config.addDefault( "prism.tracking.tnt-prime", true );
        config.addDefault( "prism.tracking.tree-grow", true );
        config.addDefault( "prism.tracking.vehicle-break", true );
        config.addDefault( "prism.tracking.vehicle-enter", true );
        config.addDefault( "prism.tracking.vehicle-exit", true );
        config.addDefault( "prism.tracking.vehicle-place", true );
        config.addDefault( "prism.tracking.water-break", true ); // Breaking ice from water
        config.addDefault( "prism.tracking.water-bucket", true );
        config.addDefault( "prism.tracking.water-flow", false ); // Can be extremely spammy
        config.addDefault( "prism.tracking.world-edit", false ); // Enable if you want to log WorldEdit actions through Prism
        config.addDefault( "prism.tracking.xp-pickup", false );

        // Tracker configs
        config.addDefault( "prism.track-player-ip-on-join", false );
        config.addDefault( "prism.track-hopper-item-events", false ); // Set to true to enable hopper tracking via PrismInventoryMoveItemEvent

        final List<String> doNotTrackCommand = new ArrayList<String>();
        doNotTrackCommand.add( "vanish" );
        doNotTrackCommand.add( "v" );
        doNotTrackCommand.add( "login" );
        doNotTrackCommand.add( "changepassword" );
        doNotTrackCommand.add( "register" );
        doNotTrackCommand.add( "unregister" );
        config.addDefault( "prism.do-not-track.commands", doNotTrackCommand );

        config.addDefault( "prism.tracking.api.enabled", true );
        final List<String> allowedApiPlugins = new ArrayList<String>();
        allowedApiPlugins.add( "DarkMythos" );
        allowedApiPlugins.add( "PrismApiDemo" );
        config.addDefault( "prism.tracking.api.allowed-plugins", allowedApiPlugins );

        // Ore Alerts
        config.addDefault( "prism.alerts.alert-staff-to-applied-process", true );
        config.addDefault( "prism.alerts.alert-player-about-self", true );
        config.addDefault( "prism.alerts.ores.enabled", true );
        config.addDefault( "prism.alerts.ores.log-to-console", true );
        config.addDefault( "prism.alerts.ores.log-commands", Arrays.asList("examplecommand <alert>") );
        // Ore blocks (IDs for 1.7.10)
        final HashMap<String, String> oreBlocks = new HashMap<String, String>();
        oreBlocks.put( "14", "&e" ); // Gold Ore (Original was Iron, 14 is Gold Ore ID)
        oreBlocks.put( "15", "&7" ); // Iron Ore (Original was Gold, 15 is Iron Ore ID)
        oreBlocks.put( "21", "&9" ); // Lapis Lazuli Ore
        oreBlocks.put( "56", "&b" ); // Diamond Ore
        oreBlocks.put( "73", "&c" ); // Redstone Ore (Glowing or not, 73 is base)
        oreBlocks.put( "129", "&a" ); // Emerald Ore
        oreBlocks.put( "16", "&8"); // Coal Ore
        config.addDefault( "prism.alerts.ores.blocks", oreBlocks );

        // Illegal Command Alerts
        config.addDefault( "prism.alerts.illegal-commands.enabled", false );
        config.addDefault( "prism.alerts.illegal-commands.log-to-console", true );
        config.addDefault( "prism.alerts.illegal-commands.log-commands", Arrays.asList("examplecommand <alert>") );
        final List<String> illegal_commands = new ArrayList<String>();
        illegal_commands.add( "op" );
        illegal_commands.add( "deop" );
        illegal_commands.add( "stop" );
        illegal_commands.add( "reload" );
        illegal_commands.add( "bukkit:op" );
        illegal_commands.add( "bukkit:deop" );
        illegal_commands.add( "bukkit:stop" );
        illegal_commands.add( "bukkit:reload" );
        illegal_commands.add( "minecraft:op" );
        illegal_commands.add( "minecraft:deop" );
        illegal_commands.add( "minecraft:stop" );
        illegal_commands.add( "minecraft:reload" );
        config.addDefault( "prism.alerts.illegal-commands.commands", illegal_commands );

        // Use Alerts
        config.addDefault( "prism.alerts.uses.enabled", true );
        config.addDefault( "prism.alerts.uses.log-to-console", true );
        config.addDefault( "prism.alerts.uses.log-commands", Arrays.asList("examplecommand <alert>") );
        config.addDefault( "prism.alerts.uses.lighter", true ); // Flint and Steel
        config.addDefault( "prism.alerts.uses.lava", true );    // Lava bucket placement

        List<String> monitorItemsPlacement = new ArrayList<String>();
        monitorItemsPlacement.add( "7" );  // Bedrock (Admin abuse?)
        monitorItemsPlacement.add( "46" ); // TNT
        monitorItemsPlacement.add( "10" ); // Flowing Lava
        monitorItemsPlacement.add( "11" ); // Still Lava
        config.addDefault( "prism.alerts.uses.item-placement", monitorItemsPlacement );
        List<String> monitorItemsBreak = new ArrayList<String>();
        config.addDefault( "prism.alerts.uses.item-break", monitorItemsBreak );

        config.addDefault( "prism.alerts.vanilla-xray.enabled", true ); // This likely refers to ore mining alerts

        config.addDefault( "prism.queue-empty-tick-delay", 3 ); // How often to check the recording queue when empty

        config.options().copyDefaults( true );

        plugin.saveConfig();

        return config;
    }
}