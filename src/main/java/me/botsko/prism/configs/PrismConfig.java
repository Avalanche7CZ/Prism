package me.botsko.prism.configs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import me.botsko.prism.ConfigBase;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

public class PrismConfig extends ConfigBase {

    public PrismConfig(Plugin plugin) {
        super( plugin );
    }

    @Override
    public FileConfiguration getConfig() {

        config = plugin.getConfig();

        String header = "Prism Plugin Configuration\n\n"
                + "Database Settings:\n"
                + "  type: Specify the database system Prism should use.\n"
                + "    Supported options: mysql, mariadb, postgresql, h2, sqlite (default).\n"
                + "    SQLite is recommended for smaller servers or easy setup as it's file-based.\n"
                + "    For larger servers, MySQL, MariaDB, or PostgreSQL are recommended for performance.\n\n"
                + "  hostname, port, databaseName, username, password: Standard credentials for MySQL, MariaDB, PostgreSQL.\n"
                + "    Default MySQL/MariaDB port: 3306\n"
                + "    Default PostgreSQL port: 5432\n\n"
                + "  filePath: For SQLite and H2, this is the name of the database file (e.g., prism.db) relative to the plugin's data folder.\n\n"
                + "  h2.mysqlMode: If using H2, set to true for better compatibility if you plan to migrate from/to MySQL.\n"
                + "  h2.autoServer: If using H2, set to true to allow multiple applications/plugins to access the same H2 database file (not commonly needed for Prism alone).\n\n"
                + "  driverClassName: The Java class name for the JDBC driver. Prism attempts to auto-detect this based on 'type'.\n"
                + "    Examples (usually not needed to change):\n"
                + "      MySQL (old): com.mysql.jdbc.Driver\n"
                + "      MySQL (new): com.mysql.cj.jdbc.Driver\n"
                + "      MariaDB: org.mariadb.jdbc.Driver\n"
                + "      PostgreSQL: org.postgresql.Driver\n"
                + "      SQLite: org.sqlite.JDBC\n"
                + "      H2: org.h2.Driver\n\n"
                + "  jdbcUrlPrefix: The prefix for the JDBC connection URL. Prism also attempts to auto-detect this.\n"
                + "    Examples (usually not needed to change):\n"
                + "      MySQL: jdbc:mysql://\n"
                + "      MariaDB: jdbc:mariadb://\n"
                + "      PostgreSQL: jdbc:postgresql://\n"
                + "      SQLite: jdbc:sqlite:\n"
                + "      H2: jdbc:h2:\n";
        config.options().header(header);
        config.options().copyHeader(true);


        config.addDefault( "prism.debug", false );
        config.addDefault( "prism.allow-metrics", true );

        config.addDefault("prism.database.type", "sqlite");
        config.addDefault("prism.database.hostname", "127.0.0.1");
        config.addDefault("prism.database.port", "3306");
        config.addDefault("prism.database.databaseName", "minecraft");
        config.addDefault("prism.database.username", "root");
        config.addDefault("prism.database.password", "");
        config.addDefault("prism.database.tablePrefix", "prism_");
        config.addDefault("prism.database.filePath", "prism.db");
        config.addDefault("prism.database.h2.mysqlMode", true);
        config.addDefault("prism.database.h2.autoServer", false);
        config.addDefault("prism.database.driverClassName", "");
        config.addDefault("prism.database.jdbcUrlPrefix", "");
        config.addDefault("prism.database.pool.max-connections", 20);
        config.addDefault("prism.database.pool.initial-size", 5);
        config.addDefault("prism.database.pool.max-idle-connections", 10);
        config.addDefault("prism.database.pool.max-wait-ms", 30000);
        config.addDefault("prism.database.max-failures-before-wait", 5);
        config.addDefault("prism.database.min-actions-per-insert-batch", 1);
        config.addDefault("prism.database.actions-per-insert-batch", 1000);
        config.addDefault("prism.database.force-write-queue-on-shutdown", true);

        config.addDefault( "prism.paste.enable", false );
        config.addDefault( "prism.paste.username", "Username on http://pste.me/#/signup" );
        config.addDefault( "prism.paste.api-key", "API key from http://pste.me/#/account" );

        config.addDefault( "prism.wands.default-mode", "hand" );
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

        config.addDefault( "prism.queries.default-radius", 5 );
        config.addDefault( "prism.queries.default-time-since", "3d" );
        config.addDefault( "prism.queries.max-lookup-radius", 100 );
        config.addDefault( "prism.queries.max-applier-radius", 100 );
        config.addDefault( "prism.queries.never-use-defaults", false );
        config.addDefault( "prism.queries.lookup-max-results", 1000 );
        config.addDefault( "prism.queries.default-results-per-page", 5 );
        config.addDefault( "prism.queries.lookup-auto-group", true );

        config.addDefault( "prism.messenger.always-show-extended", false );

        config.addDefault( "prism.near.default-radius", 5 );
        config.addDefault( "prism.near.max-results", 100 );

        config.addDefault( "prism.drain.max-radius", 10 );
        config.addDefault( "prism.drain.default-radius", 5 );

        config.addDefault( "prism.ex.max-radius", 100 );
        config.addDefault( "prism.ex.default-radius", 10 );

        config.addDefault( "prism.ignore.enable-perm-nodes", false );
        config.addDefault( "prism.ignore.players-in-creative", false );
        config.addDefault( "prism.ignore.players", new ArrayList<String>() );
        config.addDefault( "prism.ignore.worlds", new ArrayList<String>() );

        final List<String> purgeRules = new ArrayList<String>();
        purgeRules.add( "before:8w" );
        purgeRules.add( "a:water-flow before:4w" );
        config.addDefault( "prism.db-records-purge-rules", purgeRules );
        config.addDefault( "prism.purge.batch-tick-delay", 60 );
        config.addDefault( "prism.purge.records-per-batch", 500000 );

        config.addDefault( "prism.appliers.allow-rollback-items-removed-from-container", true );
        config.addDefault( "prism.appliers.notify-nearby.enabled", true );
        config.addDefault( "prism.appliers.notify-nearby.additional-radius", 20 );
        config.addDefault( "prism.appliers.remove-fire-on-burn-rollback", true );
        config.addDefault( "prism.appliers.remove-drops-on-explode-rollback", true );

        final List<String> illegalEntities = new ArrayList<String>();
        illegalEntities.add( "creeper" );
        config.addDefault( "prism.appliers.never-spawn-entity", illegalEntities );

        final List<Integer> illegalBlocks = new ArrayList<Integer>();
        illegalBlocks.add( 10 );
        illegalBlocks.add( 11 );
        illegalBlocks.add( 46 );
        illegalBlocks.add( 51 );
        config.addDefault( "prism.appliers.never-place-block", illegalBlocks );

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
        config.addDefault( "prism.tracking.entity-follow", true );
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
        config.addDefault( "prism.tracking.lava-break", true );
        config.addDefault( "prism.tracking.lava-bucket", true );
        config.addDefault( "prism.tracking.lava-flow", false );
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
        config.addDefault( "prism.tracking.water-break", true );
        config.addDefault( "prism.tracking.water-bucket", true );
        config.addDefault( "prism.tracking.water-flow", false );
        config.addDefault( "prism.tracking.world-edit", false );
        config.addDefault( "prism.tracking.xp-pickup", false );

        config.addDefault( "prism.track-player-ip-on-join", false );
        config.addDefault( "prism.track-hopper-item-events", false );

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

        config.addDefault( "prism.alerts.alert-staff-to-applied-process", true );
        config.addDefault( "prism.alerts.alert-player-about-self", true );
        config.addDefault( "prism.alerts.ores.enabled", true );
        config.addDefault( "prism.alerts.ores.log-to-console", true );
        config.addDefault( "prism.alerts.ores.log-commands", Arrays.asList("examplecommand <alert>") );
        final HashMap<String, String> oreBlocks = new HashMap<String, String>();
        oreBlocks.put( "14", "&e" );
        oreBlocks.put( "15", "&7" );
        oreBlocks.put( "21", "&9" );
        oreBlocks.put( "56", "&b" );
        oreBlocks.put( "73", "&c" );
        oreBlocks.put( "129", "&a" );
        oreBlocks.put( "16", "&8");
        config.addDefault( "prism.alerts.ores.blocks", oreBlocks );

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

        config.addDefault( "prism.alerts.uses.enabled", true );
        config.addDefault( "prism.alerts.uses.log-to-console", true );
        config.addDefault( "prism.alerts.uses.log-commands", Arrays.asList("examplecommand <alert>") );
        config.addDefault( "prism.alerts.uses.lighter", true );
        config.addDefault( "prism.alerts.uses.lava", true );

        config.addDefault( "prism.alerts.vanilla-xray.enabled", true );

        config.addDefault( "prism.queue-empty-tick-delay", 3 );

        config.options().copyDefaults( true );

        plugin.saveConfig();

        return config;
    }
}
