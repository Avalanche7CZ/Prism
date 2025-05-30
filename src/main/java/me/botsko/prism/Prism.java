package me.botsko.prism;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;

import me.botsko.elixr.MaterialAliases;
import me.botsko.prism.actionlibs.*;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.bridge.PrismBlockEditSessionFactory;
import me.botsko.prism.commands.PrismCommands;
import me.botsko.prism.commands.WhatCommand;
import me.botsko.prism.listeners.*;
import me.botsko.prism.listeners.self.PrismMiscEvents;
import me.botsko.prism.measurement.Metrics;
import me.botsko.prism.measurement.QueueStats;
import me.botsko.prism.measurement.TimeTaken;
import me.botsko.prism.monitors.OreMonitor;
import me.botsko.prism.monitors.UseMonitor;
import me.botsko.prism.parameters.*;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.players.PrismPlayer;
import me.botsko.prism.purge.PurgeManager;
import me.botsko.prism.wands.Wand;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Prism extends JavaPlugin {

    private static DataSource pool = new DataSource();


    private static String plugin_name;
    private String plugin_version;
    private static MaterialAliases items;
    private Language language;
    private static Logger log = Logger.getLogger("Minecraft");
    private final ArrayList<String> enabledPlugins = new ArrayList<String>();
    private static ActionRegistry actionRegistry;
    private static HandlerRegistry<?> handlerRegistry;
    private static Ignore ignore;
    protected static ArrayList<Integer> illegalBlocks;
    protected static ArrayList<String> illegalEntities;
    protected static HashMap<String, String> alertedOres = new HashMap<String, String>();
    private static HashMap<String, PrismParameterHandler> paramHandlers = new HashMap<String, PrismParameterHandler>();
    private final ScheduledThreadPoolExecutor schedulePool = new ScheduledThreadPoolExecutor(1);
    private final ScheduledThreadPoolExecutor recordingMonitorTask = new ScheduledThreadPoolExecutor(1);
    private PurgeManager purgeManager;

    public Prism prism;
    public static Messenger messenger;
    public static FileConfiguration config;
    public static WorldEditPlugin plugin_worldEdit = null;
    public ActionsQuery actionsQuery;
    public OreMonitor oreMonitor;
    public UseMonitor useMonitor;
    public static ConcurrentHashMap<String, Wand> playersWithActiveTools = new ConcurrentHashMap<String, Wand>();
    public ConcurrentHashMap<String, PreviewSession> playerActivePreviews = new ConcurrentHashMap<String, PreviewSession>();
    public ConcurrentHashMap<String, ArrayList<Block>> playerActiveViews = new ConcurrentHashMap<String, ArrayList<Block>>();
    public ConcurrentHashMap<String, QueryResult> cachedQueries = new ConcurrentHashMap<String, QueryResult>();
    public ConcurrentHashMap<Location, Long> alertedBlocks = new ConcurrentHashMap<Location, Long>();
    public TimeTaken eventTimer;
    public QueueStats queueStats;
    public BukkitTask recordingTask;
    public int total_records_affected = 0;

    public static HashMap<String, Integer> prismWorlds = new HashMap<String, Integer>();
    public static HashMap<UUID, PrismPlayer> prismPlayers = new HashMap<UUID, PrismPlayer>();
    public static HashMap<String, Integer> prismActions = new HashMap<String, Integer>();

    public ConcurrentHashMap<String, String> preplannedBlockFalls = new ConcurrentHashMap<String, String>();
    public ConcurrentHashMap<String, String> preplannedVehiclePlacement = new ConcurrentHashMap<String, String>();

    private String db_type;
    private String db_driver_class_name_cfg;
    private String db_jdbc_url_prefix_cfg;
    private String db_hostname;
    private String db_port_str;
    private String db_database_name;
    private String db_username;
    private String db_password;
    private String db_table_prefix;
    private String db_file_path;

    private int db_pool_initial_size;
    private int db_pool_max_connections;
    private int db_pool_max_idle_connections;
    private int db_pool_max_wait_ms;

    public String getDbType() {
        return db_type;
    }

    public String getTablePrefix() {
        return db_table_prefix;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void onEnable() {

        plugin_name = this.getDescription().getName();
        plugin_version = this.getDescription().getVersion();
        prism = this;
        log( "Initializing Prism " + plugin_version + ". By Viveleroi." );
        loadConfig();

        if( config.getBoolean( "prism.allow-metrics" ) ) {
            try {
                final Metrics metrics = new Metrics( this );
                metrics.start();
            } catch ( final IOException e ) {
                log( "MCStats submission failed." );
            }
        }

        configureDbPool();
        final Connection test_conn = dbc();
        if( pool == null || test_conn == null ) {
            final String[] dbDisabled = new String[3];
            dbDisabled[0] = "Prism will disable itself because it couldn't connect to a database.";
            dbDisabled[1] = "If you're using MySQL/MariaDB, check your config. Be sure your database server is running.";
            dbDisabled[2] = "For help - try http://discover-prism.com/wiki/view/troubleshooting/";
            logSection( dbDisabled );
            disablePlugin();
            return;
        }
        if( test_conn != null ) {
            try {
                test_conn.close();
            } catch ( final SQLException e ) {
                handleDatabaseException( e );
            }
        }

        if( isEnabled() ) {
            handlerRegistry = new HandlerRegistry();
            actionRegistry = new ActionRegistry();
            setupDatabase();
            cacheWorldPrimaryKeys();
            PlayerIdentification.cacheOnlinePlayerPrimaryKeys();

            for ( final World w : getServer().getWorlds() ) {
                if( !Prism.prismWorlds.containsKey( w.getName() ) ) {
                    Prism.addWorldName( w.getName() );
                }
            }

            final Updater up = new Updater( this );
            up.apply_updates();

            eventTimer = new TimeTaken( this );
            queueStats = new QueueStats();
            ignore = new Ignore( this );
            checkPluginDependancies();

            getServer().getPluginManager().registerEvents( new PrismBlockEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismEntityEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismWorldEvents(), this );
            getServer().getPluginManager().registerEvents( new PrismPlayerEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismInventoryEvents( this ), this );
            getServer().getPluginManager().registerEvents( new PrismVehicleEvents( this ), this );

            if( config.getBoolean( "prism.track-hopper-item-events" ) && Prism.getIgnore().event( "item-insert" ) ) {
                getServer().getPluginManager().registerEvents( new PrismInventoryMoveItemEvent(), this );
            }

            if( config.getBoolean( "prism.tracking.api.enabled" ) ) {
                getServer().getPluginManager().registerEvents( new PrismCustomEvents( this ), this );
            }
            getServer().getPluginManager().registerEvents( new PrismMiscEvents(), this );

            getCommand( "prism" ).setExecutor( new PrismCommands( this ) );
            getCommand( "prism" ).setTabCompleter( new PrismCommands( this ) );
            getCommand( "what" ).setExecutor( new WhatCommand( this ) );

            registerParameter( new ActionParameter() );
            registerParameter( new BeforeParameter() );
            registerParameter( new BlockParameter() );
            registerParameter( new EntityParameter() );
            registerParameter( new FlagParameter() );
            registerParameter( new IdParameter() );
            registerParameter( new KeywordParameter() );
            registerParameter( new PlayerParameter() );
            registerParameter( new RadiusParameter() );
            registerParameter( new SinceParameter() );
            registerParameter( new WorldParameter() );

            messenger = new Messenger( plugin_name );
            actionsQuery = new ActionsQuery( this );
            oreMonitor = new OreMonitor( this );
            useMonitor = new UseMonitor( this );

            actionRecorderTask();
            endExpiredQueryCaches();
            endExpiredPreviews();
            removeExpiredLocations();
            launchScheduledPurgeManager();
            launchInternalAffairs();
        }
    }

    public static String getPrismName() {
        return plugin_name;
    }

    public String getPrismVersion() {
        return this.plugin_version;
    }

    @SuppressWarnings("unchecked")
    public void loadConfig() {

        final PrismConfig mc = new PrismConfig( this );
        config = mc.getConfig();

        db_type = config.getString("prism.database.type", "sqlite").toLowerCase();

        db_driver_class_name_cfg = config.getString("prism.database.driverClassName");
        db_jdbc_url_prefix_cfg = config.getString("prism.database.jdbcUrlPrefix");

        db_hostname = config.getString("prism.database.hostname", "127.0.0.1");
        db_port_str = config.getString("prism.database.port", getDefaultPortForDB(db_type));
        db_database_name = config.getString("prism.database.databaseName", "minecraft");
        db_username = config.getString("prism.database.username", "root");
        db_password = config.getString("prism.database.password", "");
        db_table_prefix = config.getString("prism.database.tablePrefix", "prism_");
        db_file_path = config.getString("prism.database.filePath", "prism.db");


        db_pool_initial_size = config.getInt("prism.database.pool.initial-size", 5);
        db_pool_max_connections = config.getInt("prism.database.pool.max-connections", 20);
        db_pool_max_idle_connections = config.getInt("prism.database.pool.max-idle-connections", 10);
        db_pool_max_wait_ms = config.getInt("prism.database.pool.max-wait-ms", 30000);

        illegalBlocks = (ArrayList<Integer>) config.getList( "prism.appliers.never-place-block" );
        illegalEntities = (ArrayList<String>) config.getList( "prism.appliers.never-spawn-entity" );

        final ConfigurationSection alertBlocks = config.getConfigurationSection( "prism.alerts.ores.blocks" );
        alertedOres.clear();
        if( alertBlocks != null ) {
            for ( final String key : alertBlocks.getKeys( false ) ) {
                alertedOres.put( key, alertBlocks.getString( key ) );
            }
        }
        items = new MaterialAliases();
    }

    private String getDefaultPortForDB(String dbType) {
        switch (dbType) {
            case "mysql":
            case "mariadb":
                return "3306";
            case "postgresql":
                return "5432";
            default:
                return "3306";
        }
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
        configureDbPool();
    }

    private void configureDbPool() {
        if (pool == null) {
            log("DataSource pool is null during configureDbPool, re-initializing. This indicates a prior issue.");
            pool = new DataSource();
        }
        if (config == null) {
            log("Configuration is null during configureDbPool. Attempting to load it now.");
            loadConfig();
            if (config == null) {
                log(Level.SEVERE, "Failed to load configuration. Aborting database pool configuration. Prism may not function.");
                return;
            }
        }

        String jdbcUrl;
        String driverClass;

        log("Configuring database connection for type: " + db_type);

        switch (db_type) {
            case "mysql":
                driverClass = config.getString("prism.database.driverClassName.mysql", "com.mysql.jdbc.Driver");
                jdbcUrl = config.getString("prism.database.jdbcUrlPrefix.mysql", "jdbc:mysql://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name
                        + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
                break;
            case "mariadb":
                driverClass = config.getString("prism.database.driverClassName.mariadb", "org.mariadb.jdbc.Driver");
                jdbcUrl = config.getString("prism.database.jdbcUrlPrefix.mariadb", "jdbc:mariadb://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name
                        + "?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
                break;
            case "postgresql":
                driverClass = config.getString("prism.database.driverClassName.postgresql", "org.postgresql.Driver");
                jdbcUrl = config.getString("prism.database.jdbcUrlPrefix.postgresql", "jdbc:postgresql://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name;
                break;
            case "h2":
                driverClass = config.getString("prism.database.driverClassName.h2", "org.h2.Driver");
                File h2DbFile = new File(getDataFolder(), db_file_path);
                getDataFolder().mkdirs();
                jdbcUrl = config.getString("prism.database.jdbcUrlPrefix.h2", "jdbc:h2:") + h2DbFile.getAbsolutePath();
                if (config.getBoolean("prism.database.h2.mysqlMode", true)) {
                    jdbcUrl += ";MODE=MySQL;DATABASE_TO_UPPER=FALSE";
                }
                if (config.getBoolean("prism.database.h2.autoServer", false)) {
                    jdbcUrl += ";AUTO_SERVER=TRUE";
                }
                log("H2 JDBC URL: " + jdbcUrl);
                break;
            case "sqlite":
                driverClass = config.getString("prism.database.driverClassName.sqlite", "org.sqlite.JDBC");
                File sqliteDbFile = new File(getDataFolder(), db_file_path);
                getDataFolder().mkdirs();
                jdbcUrl = config.getString("prism.database.jdbcUrlPrefix.sqlite", "jdbc:sqlite:") + sqliteDbFile.getAbsolutePath();
                log("SQLite JDBC URL: " + jdbcUrl);
                break;
            default:
                log(Level.WARNING, "Unsupported database type: " + db_type + ". Defaulting to SQLite for safety.");
                db_type = "sqlite";
                driverClass = "org.sqlite.JDBC";
                File fallbackSqliteFile = new File(getDataFolder(), "prism_fallback.db");
                getDataFolder().mkdirs();
                jdbcUrl = "jdbc:sqlite:" + fallbackSqliteFile.getAbsolutePath();
                log("Fallback SQLite JDBC URL: " + jdbcUrl);
                break;
        }

        pool.setDriverClassName(driverClass);
        pool.setUrl(jdbcUrl);

        if (!db_type.equals("sqlite") && !db_type.equals("h2")) {
            pool.setUsername(db_username);
            pool.setPassword(db_password);
        }

        pool.setInitialSize(db_pool_initial_size);
        pool.setMaxActive(db_pool_max_connections);
        pool.setMaxIdle(db_pool_max_idle_connections);
        pool.setMaxWait(db_pool_max_wait_ms);
        pool.setRemoveAbandoned(config.getBoolean("prism.database.pool.remove-abandoned.enabled", true));
        pool.setRemoveAbandonedTimeout(config.getInt("prism.database.pool.remove-abandoned.timeout-seconds", 60));
        pool.setTestOnBorrow(config.getBoolean("prism.database.pool.test-on-borrow", true));
        pool.setValidationQuery(getValidationQueryForDB(db_type));
        pool.setValidationInterval(config.getInt("prism.database.pool.validation-interval-ms", 30000));
        pool.setLogAbandoned(config.getBoolean("prism.debug", false));

        log("Database pool configured for " + db_type + " with driver " + driverClass);
    }

    private String getValidationQueryForDB(String dbType) {
        String queryFromConfig = config.getString("prism.database.pool.validation-query." + dbType.toLowerCase());
        if (queryFromConfig != null && !queryFromConfig.isEmpty()) {
            return queryFromConfig;
        }
        switch (dbType.toLowerCase()) {
            case "postgresql":
                return "SELECT 1";
            case "h2":
            case "sqlite":
                return "SELECT 1";
            case "mysql":
            case "mariadb":
            default:
                return "/* ping */ SELECT 1";
        }
    }

    public static DataSource getPool() {
        return Prism.pool;
    }

    public static Connection dbc() {
        Connection con = null;
        try {
            if (pool == null || pool.getPoolProperties().getDriverClassName() == null) {
                log(Level.WARNING, "Database pool is not initialized or configured. Attempting to reconfigure.");
                Prism instance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
                if (instance != null) {
                    if (instance.config == null) instance.loadConfig();
                    instance.configureDbPool();
                    if (pool == null || pool.getPoolProperties().getDriverClassName() == null) {
                        log(Level.SEVERE, "Failed to reconfigure database pool. No connection can be established.");
                        return null;
                    }
                } else {
                    log(Level.SEVERE, "Prism plugin instance not found. Cannot establish database connection.");
                    return null;
                }
            }
            con = pool.getConnection();
        } catch ( final SQLException e ) {
            log(Level.SEVERE, "Database connection failed: " + e.getMessage() );
            if(config == null || config.getBoolean("prism.debug", false) || !e.getMessage().contains( "Pool empty" ) ){
                e.printStackTrace();
            }
        }
        return con;
    }

    public void rebuildPool() {
        log("Rebuilding database connection pool...");
        if( pool != null ) {
            pool.close(true);
        }
        pool = new DataSource();
        if (config == null) loadConfig();
        configureDbPool();
        log("Database connection pool rebuilt.");
    }

    protected boolean attemptToRescueConnection(SQLException e) {
        String msg = e.getMessage().toLowerCase();
        if( msg.contains( "connection closed" ) || msg.contains("communications link failure") || msg.contains("broken pipe") || msg.contains("connection reset") || msg.contains("no operations allowed after connection closed")) {
            log(Level.WARNING, "Attempting to rescue database connection due to: " + e.getMessage());
            rebuildPool();
            try (Connection conn = dbc()){
                if( conn != null && !conn.isClosed() ) {
                    log("Database connection rescued successfully.");
                    return true;
                } else {
                    log(Level.SEVERE, "Failed to rescue database connection: New connection is null or closed.");
                }
            } catch (SQLException rescueEx){
                log(Level.SEVERE, "SQLException during database connection rescue attempt: " + rescueEx.getMessage());
                if(config != null && config.getBoolean("prism.debug",false)) rescueEx.printStackTrace();
            }
        }
        return false;
    }

    public void handleDatabaseException(SQLException e) {
        if( attemptToRescueConnection( e ) ) { return; }

        log(Level.SEVERE, "Database connection error: " + e.getMessage() );
        if( e.getMessage().contains( "marked as crashed" ) ) {
            final String[] crashMsg = new String[2];
            crashMsg[0] = "If your database tables are marked as crashed, they may be corrupted.";
            crashMsg[1] = "For MySQL/MariaDB, try running `CHECK TABLE " + db_table_prefix + "your_table_name` and then `REPAIR TABLE " + db_table_prefix + "your_table_name`.";
            logSection( crashMsg );
        }
        if(config == null || config.getBoolean("prism.debug", false)){
            e.printStackTrace();
        }
    }

    protected void setupDatabase() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = dbc();
            if (conn == null) {
                log(Level.SEVERE, "Cannot setup database, no connection available. Prism will be disabled.");
                disablePlugin();
                return;
            }

            String currentDbSchemaType = db_type;
            log("Setting up database schema for type: " + currentDbSchemaType);

            String autoIncrementType;
            String uuidType;
            String textType = "TEXT";
            String engineClause = "";
            String intType = "INT";
            String blockIdDataType = "INT";

            switch (currentDbSchemaType) {
                case "mysql":
                case "mariadb":
                    autoIncrementType = intType + " NOT NULL AUTO_INCREMENT";
                    uuidType = "BINARY(16) NOT NULL";
                    engineClause = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                    break;
                case "postgresql":
                    autoIncrementType = "SERIAL";
                    uuidType = "UUID NOT NULL";
                    textType = "TEXT";
                    engineClause = "";
                    break;
                case "h2":
                    autoIncrementType = intType + " AUTO_INCREMENT";
                    uuidType = "UUID NOT NULL";
                    textType = "CLOB";
                    engineClause = "";
                    break;
                case "sqlite":
                    autoIncrementType = "INTEGER PRIMARY KEY AUTOINCREMENT";
                    uuidType = "BLOB NOT NULL";
                    textType = "TEXT";
                    engineClause = "";
                    break;
                default:
                    log(Level.WARNING, "Unknown database type in setupDatabase: " + currentDbSchemaType + ". Using SQLite defaults for schema types.");
                    autoIncrementType = "INTEGER PRIMARY KEY AUTOINCREMENT";
                    uuidType = "BLOB NOT NULL";
                    engineClause = "";
                    break;
            }

            st = conn.createStatement();
            String query;

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "actions` ("
                    + "`action_id` " + autoIncrementType + ","
                    + "`action` VARCHAR(25) NOT NULL";
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`action_id`)";
            }
            if (currentDbSchemaType.equals("mysql") || currentDbSchemaType.equals("mariadb")) {
                query += ", UNIQUE KEY `" + db_table_prefix + "action_idx` (`action`)";
            } else {
                query += ", CONSTRAINT `" + db_table_prefix + "uq_action` UNIQUE (`action`)";
            }
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "data` ("
                    + "`id` " + autoIncrementType + ","
                    + "`epoch` BIGINT NOT NULL,"
                    + "`action_id` " + intType + " NOT NULL,"
                    + "`player_id` " + intType + " NOT NULL,"
                    + "`world_id` " + intType + " NOT NULL,"
                    + "`x` " + intType + " NOT NULL,"
                    + "`y` " + intType + " NOT NULL,"
                    + "`z` " + intType + " NOT NULL,"
                    + "`block_id` " + blockIdDataType + " DEFAULT NULL,"
                    + "`block_subid` " + blockIdDataType + " DEFAULT NULL,"
                    + "`old_block_id` " + blockIdDataType + " DEFAULT NULL,"
                    + "`old_block_subid` " + blockIdDataType + " DEFAULT NULL";
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`id`)";
            }
            if (currentDbSchemaType.equals("mysql") || currentDbSchemaType.equals("mariadb")) {
                query += ", KEY `" + db_table_prefix + "epoch_idx` (`epoch`),"
                        + " KEY `" + db_table_prefix + "location_idx` (`world_id`, `x`, `z`, `y`, `action_id`)";
            }
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            if (!currentDbSchemaType.equals("mysql") && !currentDbSchemaType.equals("mariadb")) {
                tryExecuteUpdate(st, "CREATE INDEX IF NOT EXISTS `" + db_table_prefix + "data_epoch_idx` ON `" + db_table_prefix + "data` (`epoch`);");
                tryExecuteUpdate(st, "CREATE INDEX IF NOT EXISTS `" + db_table_prefix + "data_location_idx` ON `" + db_table_prefix + "data` (`world_id`, `x`, `z`, `y`, `action_id`);");
                tryExecuteUpdate(st, "CREATE INDEX IF NOT EXISTS `" + db_table_prefix + "data_action_player_idx` ON `" + db_table_prefix + "data` (`action_id`, `player_id`);");
            }

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "data_extra` ("
                    + "`extra_id` " + autoIncrementType + ","
                    + "`data_id` " + intType + " NOT NULL,"
                    + "`data` " + textType + " NULL,"
                    + "`te_data` " + textType + " NULL";
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`extra_id`)";
            }
            query += ", CONSTRAINT `" + db_table_prefix + "fk_data_extra_data_id` FOREIGN KEY (`data_id`) REFERENCES `" + db_table_prefix + "data` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION";

            if (currentDbSchemaType.equals("mysql") || currentDbSchemaType.equals("mariadb")) {
                query += ", KEY `" + db_table_prefix + "data_extra_data_id_idx` (`data_id`)";
            }
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            if (!currentDbSchemaType.equals("mysql") && !currentDbSchemaType.equals("mariadb")) {
                tryExecuteUpdate(st, "CREATE INDEX IF NOT EXISTS `" + db_table_prefix + "data_extra_data_id_idx` ON `" + db_table_prefix + "data_extra` (`data_id`);");
            }

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "meta` ("
                    + "`id` " + autoIncrementType + ","
                    + "`k` VARCHAR(50) NOT NULL,"
                    + "`v` VARCHAR(255) NOT NULL";
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`id`)";
            }
            query += ", CONSTRAINT `" + db_table_prefix + "uq_meta_k` UNIQUE (`k`)";
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "players` ("
                    + "`player_id` " + autoIncrementType + ","
                    + "`player` VARCHAR(255) NOT NULL,"
                    + "`player_uuid` " + uuidType;
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`player_id`)";
            }
            if (currentDbSchemaType.equals("mysql") || currentDbSchemaType.equals("mariadb")) {
                query += ", UNIQUE KEY `" + db_table_prefix + "player_name_idx` (`player`),"
                        + " UNIQUE KEY `" + db_table_prefix + "player_uuid_idx` (`player_uuid`)";
            } else {
                query += ", CONSTRAINT `" + db_table_prefix + "uq_player_player` UNIQUE (`player`),"
                        + " CONSTRAINT `" + db_table_prefix + "uq_player_uuid` UNIQUE (`player_uuid`)";
            }
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "worlds` ("
                    + "`world_id` " + autoIncrementType + ","
                    + "`world` VARCHAR(255) NOT NULL";
            if (!currentDbSchemaType.equals("sqlite")) {
                query += ", PRIMARY KEY (`world_id`)";
            }
            if (currentDbSchemaType.equals("mysql") || currentDbSchemaType.equals("mariadb")) {
                query += ", UNIQUE KEY `" + db_table_prefix + "world_name_idx` (`world`)";
            } else {
                query += ", CONSTRAINT `" + db_table_prefix + "uq_world_world` UNIQUE (`world`)";
            }
            query += ")" + engineClause + ";";
            st.executeUpdate(query);

            log("Database schema setup/verification complete.");
            cacheActionPrimaryKeys();
            if (actionRegistry != null) {
                final String[] actions = actionRegistry.listAll();
                for (final String a : actions) {
                    addActionName(a);
                }
            } else {
                log(Level.WARNING, "ActionRegistry not initialized during setupDatabase, cannot add default actions to DB.");
            }

        } catch (final SQLException e) {
            log(Level.SEVERE, "Database setup error: " + e.getMessage());
            e.printStackTrace();
            disablePlugin();
        } finally {
            if (st != null) try { st.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    protected void cacheActionPrimaryKeys() {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null){
                log(Level.WARNING, "Cannot cache action primary keys, no DB connection.");
                return;
            }
            s = conn.prepareStatement( "SELECT action_id, action FROM `" + db_table_prefix + "actions`" );
            rs = s.executeQuery();
            HashMap<String, Integer> newActions = new HashMap<>();
            while ( rs.next() ) {
                newActions.put( rs.getString( "action" ), rs.getInt( "action_id" ) );
            }
            prismActions.clear();
            prismActions.putAll(newActions);
            debug("Cached " + prismActions.size() + " action primary keys.");
        } catch ( final SQLException e ) {
            handleDatabaseException( e );
        } finally {
            if( rs != null ) try { rs.close(); } catch ( final SQLException e ) {}
            if( s != null ) try { s.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    private void tryExecuteUpdate(Statement st, String query) {
        try {
            st.executeUpdate(query);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            boolean alreadyExistsError = (sqlState != null && (sqlState.equals("42S11") || sqlState.equals("42P07") || sqlState.equals("42S01") || sqlState.equals("X0Y32"))) ||
                    (e.getMessage() != null && (e.getMessage().toLowerCase().contains("already exist") || e.getMessage().toLowerCase().contains("duplicate")));

            if (alreadyExistsError) {
                debug("Attempted to create index/table that already exists (expected for IF NOT EXISTS or similar logic): " + query + " - SQLState: " + sqlState + ", Error: " + e.getMessage());
            } else {
                log(Level.WARNING, "SQLException during tryExecuteUpdate for query [" + query + "]: SQLState: " + sqlState + ", Error: " + e.getMessage());
                if (config != null && config.getBoolean("prism.debug", false)) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void addActionName(String actionName) {
        Prism instance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if( instance == null ) {
            log(Level.WARNING, "Prism instance is null in addActionName for: " + actionName);
            return;
        }
        if( prismActions.containsKey( actionName ) ) return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null){
                log(Level.WARNING, "Cannot add action name '" + actionName + "', no DB connection.");
                return;
            }
            s = conn.prepareStatement( "INSERT INTO `" + instance.db_table_prefix + "actions` (action) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, actionName );
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                int newId = rs.getInt( 1 );
                prismActions.put( actionName, newId );
                debug("Added action '" + actionName + "' with ID " + newId + " to database and cache.");
            } else {
                log(Level.WARNING, "Insert statement for action '" + actionName + "' failed to return generated key. Re-querying.");
                instance.cacheActionPrimaryKeys();
                if(!prismActions.containsKey(actionName)){
                    log(Level.SEVERE, "Failed to add or find action '" + actionName + "' in database after insert attempt.");
                }
            }
        } catch ( final SQLException e ) {
            if (e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("duplicate entry")) {
                debug("Action '" + actionName + "' likely already exists (unique constraint violation). Recaching actions.");
                instance.cacheActionPrimaryKeys();
            } else {
                instance.handleDatabaseException(e);
            }
        } finally {
            if( rs != null ) try { rs.close(); } catch ( final SQLException e ) {}
            if( s != null ) try { s.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    protected void cacheWorldPrimaryKeys() {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null){
                log(Level.WARNING, "Cannot cache world primary keys, no DB connection.");
                return;
            }
            s = conn.prepareStatement( "SELECT world_id, world FROM `" + db_table_prefix + "worlds`" );
            rs = s.executeQuery();
            HashMap<String, Integer> newWorlds = new HashMap<>();
            while ( rs.next() ) {
                newWorlds.put( rs.getString( "world" ), rs.getInt( "world_id" ) );
            }
            prismWorlds.clear();
            prismWorlds.putAll(newWorlds);
            debug("Cached " + prismWorlds.size() + " world primary keys.");
        } catch ( final SQLException e ) {
            handleDatabaseException( e );
        } finally {
            if( rs != null ) try { rs.close(); } catch ( final SQLException e ) {}
            if( s != null ) try { s.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    public static void addWorldName(String worldName) {
        Prism instance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if( instance == null ) {
            log(Level.WARNING, "Prism instance is null in addWorldName for: " + worldName);
            return;
        }
        if( prismWorlds.containsKey( worldName ) ) return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null){
                log(Level.WARNING, "Cannot add world name '" + worldName + "', no DB connection.");
                return;
            }
            s = conn.prepareStatement( "INSERT INTO `" + instance.db_table_prefix + "worlds` (world) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, worldName );
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                int newId = rs.getInt( 1 );
                prismWorlds.put( worldName, newId );
                debug("Added world '" + worldName + "' with ID " + newId + " to database and cache.");
            } else {
                log(Level.WARNING, "Insert statement for world '" + worldName + "' failed to return generated key. Re-querying.");
                instance.cacheWorldPrimaryKeys();
                if(!prismWorlds.containsKey(worldName)){
                    log(Level.SEVERE, "Failed to add or find world '" + worldName + "' in database after insert attempt.");
                }
            }
        } catch ( final SQLException e ) {
            if (e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("duplicate entry")) {
                debug("World '" + worldName + "' likely already exists (unique constraint violation). Recaching worlds.");
                instance.cacheWorldPrimaryKeys();
            } else {
                instance.handleDatabaseException(e);
            }
        } finally {
            if( rs != null ) try { rs.close(); } catch ( final SQLException e ) {}
            if( s != null ) try { s.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    public Language getLang() {
        return this.language;
    }

    public void checkPluginDependancies() {
        final Plugin we = getServer().getPluginManager().getPlugin( "WorldEdit" );
        if( we != null && we.isEnabled()) {
            plugin_worldEdit = (WorldEditPlugin) we;
            try {
                PrismBlockEditSessionFactory.initialize();
                log( "WorldEdit found and enabled. Associated features enabled." );
                enabledPlugins.add("WorldEdit");
            } catch (Exception e) {
                log(Level.WARNING, "WorldEdit found, but failed to initialize PrismBlockEditSessionFactory: " + e.getMessage());
                plugin_worldEdit = null;
            }
        } else {
            log( "WorldEdit not found or not enabled. Certain optional features of Prism disabled." );
            plugin_worldEdit = null;
        }
    }

    public boolean dependencyEnabled(String pluginName) {
        return enabledPlugins.contains( pluginName );
    }

    public static ArrayList<Integer> getIllegalBlocks() {
        return (illegalBlocks != null) ? illegalBlocks : new ArrayList<Integer>();
    }

    public static ArrayList<String> getIllegalEntities() {
        return (illegalEntities != null) ? illegalEntities : new ArrayList<String>();
    }

    public static HashMap<String, String> getAlertedOres() {
        return alertedOres;
    }

    public static MaterialAliases getItems() {
        if (items == null) {
            items = new MaterialAliases();
        }
        return items;
    }

    public static ActionRegistry getActionRegistry() {
        if (actionRegistry == null) {
            actionRegistry = new ActionRegistry();
        }
        return actionRegistry;
    }

    @SuppressWarnings("rawtypes")
    public static HandlerRegistry<?> getHandlerRegistry() {
        if (handlerRegistry == null) {
            handlerRegistry = new HandlerRegistry();
        }
        return handlerRegistry;
    }

    public static Ignore getIgnore() {
        return ignore;
    }

    public PurgeManager getPurgeManager() {
        return purgeManager;
    }

    public static void registerParameter(PrismParameterHandler handler) {
        if (handler != null && handler.getName() != null) {
            paramHandlers.put( handler.getName().toLowerCase(), handler );
        } else {
            log(Level.WARNING, "Attempted to register a null parameter handler or handler with null name.");
        }
    }

    public static HashMap<String, PrismParameterHandler> getParameters() {
        return paramHandlers;
    }

    public static PrismParameterHandler getParameter(String name) {
        return (name != null) ? paramHandlers.get( name.toLowerCase() ) : null;
    }

    public void endExpiredQueryCaches() {
        long repeatTicks = config.getLong("prism.cache.query.expire-check-ticks", 2400L);
        long expirySeconds = config.getLong("prism.cache.query.expiry-seconds", 120L);

        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                cachedQueries.entrySet().removeIf(entry -> {
                    final QueryResult result = entry.getValue();
                    final long diffSeconds = ( currentTime - result.getQueryTime() ) / 1000;
                    if( diffSeconds >= expirySeconds ) {
                        debug("Expired query cache for key: " + entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks );
    }

    public void endExpiredPreviews() {
        long repeatTicks = config.getLong("prism.cache.preview.expire-check-ticks", 1200L);
        long expirySeconds = config.getLong("prism.cache.preview.expiry-seconds", 60L);

        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                playerActivePreviews.entrySet().removeIf(entry -> {
                    final PreviewSession session = entry.getValue();
                    final long diffSeconds = ( currentTime - session.getQueryTime() ) / 1000;
                    if( diffSeconds >= expirySeconds ) {
                        final Player player = Bukkit.getServer().getPlayerExact( session.getPlayer().getName() );
                        if( player != null && player.isOnline() ) {
                            player.sendMessage( Prism.messenger.playerHeaderMsg( "Canceling forgotten preview." ) );
                        }
                        debug("Expired preview for player: " + session.getPlayer().getName());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks );
    }

    public void removeExpiredLocations() {
        long repeatTicks = config.getLong("prism.cache.alerted-locations.expire-check-ticks", 1200L);
        long expirySeconds = config.getLong("prism.cache.alerted-locations.expiry-seconds", 300L);

        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                alertedBlocks.entrySet().removeIf(entry -> {
                    final long diffSeconds = ( currentTime - entry.getValue() ) / 1000;
                    if( diffSeconds >= expirySeconds ) {
                        debug("Expired alerted location: " + entry.getKey().toString());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks );
    }

    public void actionRecorderTask() {
        int recorder_tick_delay = config.getInt( "prism.queue-empty-tick-delay", 3 );
        if( recorder_tick_delay < 1 ) {
            recorder_tick_delay = 3;
        }
        recordingTask = getServer().getScheduler().runTaskLaterAsynchronously( this, new RecordingTask( this ), recorder_tick_delay );
    }

    public void launchScheduledPurgeManager() {
        if (!config.getBoolean("prism.purge.enabled", false)) {
            log("Database purging is disabled in config.");
            return;
        }
        final List<String> purgeRules = config.getStringList( "prism.db-records-purge-rules" );
        if (purgeRules == null || purgeRules.isEmpty()) {
            log("Database purging enabled, but no purge rules defined. Purge manager will not run effectively.");
        }
        purgeManager = new PurgeManager( this, purgeRules );
        long initialDelayHours = config.getLong("prism.purge.initial-delay-hours", 1);
        long intervalHours = config.getLong("prism.purge.interval-hours", 12);

        if (intervalHours <= 0) {
            log(Level.WARNING, "Purge interval hours must be positive. Purge manager not scheduled.");
            return;
        }

        schedulePool.scheduleAtFixedRate( purgeManager, initialDelayHours, intervalHours, TimeUnit.HOURS );
        log("Scheduled purge manager to run every " + intervalHours + " hours, starting in " + initialDelayHours + " hour(s).");
    }

    public void launchInternalAffairs() {
        long intervalMinutes = config.getLong("prism.internal-affairs.interval-minutes", 5);
        if (intervalMinutes <= 0) {
            log(Level.WARNING, "Internal affairs task interval must be positive. Task not scheduled.");
            return;
        }
        final InternalAffairs recordingMonitor = new InternalAffairs( this );
        recordingMonitorTask.scheduleAtFixedRate( recordingMonitor, 0, intervalMinutes, TimeUnit.MINUTES );
        log("Internal affairs task (queue monitoring, etc.) scheduled every " + intervalMinutes + " minutes.");
    }

    public void alertPlayers(Player eventPlayer, String msg) {
        String formattedMsg = messenger.playerMsg( ChatColor.RED + "[!] " + msg );
        for ( final Player p : getServer().getOnlinePlayers() ) {
            if( (eventPlayer == null || !p.equals( eventPlayer )) || config.getBoolean( "prism.alerts.alert-player-about-self", false ) ) {
                if( p.hasPermission( "prism.alerts" ) ) {
                    p.sendMessage( formattedMsg );
                }
            }
        }
    }

    public String msgMissingArguments() {
        return messenger.playerError( "Missing arguments. Check /prism ? for help." );
    }

    public String msgInvalidArguments() {
        return messenger.playerError( "Invalid arguments. Check /prism ? for help." );
    }

    public String msgInvalidSubcommand() {
        return messenger.playerError( "Prism doesn't have that command. Check /prism ? for help." );
    }

    public String msgNoPermission() {
        return messenger.playerError( "You don't have permission to perform this action." );
    }

    public void notifyNearby(Player centralPlayer, int radius, String msg) {
        if( !config.getBoolean( "prism.appliers.notify-nearby.enabled" ) ) { return; }
        if (centralPlayer == null || !centralPlayer.isOnline()) return;

        String formattedMsg = messenger.playerHeaderMsg( msg );
        int notifyRadius = radius + config.getInt( "prism.appliers.notify-nearby.additional-radius", 0 );
        if (notifyRadius <= 0 && radius <= 0) return;

        for ( final Player p : centralPlayer.getWorld().getPlayers() ) {
            if( !p.equals( centralPlayer ) ) {
                if( centralPlayer.getLocation().distanceSquared( p.getLocation() ) <= ( (long)notifyRadius * notifyRadius ) ) {
                    p.sendMessage( formattedMsg );
                }
            }
        }
    }

    public static void log(String message) {
        log(Level.INFO, message);
    }

    public static void log(Level level, String message){
        if(plugin_name == null) plugin_name = "Prism";
        if (Prism.log != null) {
            Prism.log.log(level, "[" + plugin_name + "]: " + message);
        } else {
            System.out.println("[" + plugin_name + "] (" + level.getName() + "): " + message);
        }
    }


    public static void logSection(String[] messages) {
        if( messages != null && messages.length > 0 ) {
            log( "--------------------- ## Important ## ---------------------" );
            for ( final String msg : messages ) {
                if (msg != null) log( msg );
            }
            log( "--------------------- ## ========= ## ---------------------" );
        }
    }

    public static void debug(String message) {
        if(config != null && config.getBoolean( "prism.debug", false ) ) {
            log(Level.INFO, "[Debug]: " + message );
        }
    }

    public static void debug(Location loc) {
        if (loc != null) {
            debug( "Location: W:" + (loc.getWorld() != null ? loc.getWorld().getName() : "null") +
                    " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ() );
        } else {
            debug("Location: null");
        }
    }

    public void disablePlugin() {
        log(Level.WARNING, "Disabling Prism plugin...");
        this.setEnabled( false );
    }

    @Override
    public void onDisable() {
        log("Prism disabling...");
        if (schedulePool != null && !schedulePool.isShutdown()) {
            schedulePool.shutdown();
            try {
                if (!schedulePool.awaitTermination(5, TimeUnit.SECONDS)) {
                    schedulePool.shutdownNow();
                    log(Level.WARNING, "Scheduled tasks did not terminate gracefully, forcing shutdown.");
                } else {
                    log("Scheduled tasks shut down gracefully.");
                }
            } catch (InterruptedException ie) {
                schedulePool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        if (recordingMonitorTask != null && !recordingMonitorTask.isShutdown()) {
            recordingMonitorTask.shutdown();
            try {
                if (!recordingMonitorTask.awaitTermination(5, TimeUnit.SECONDS)) {
                    recordingMonitorTask.shutdownNow();
                    log(Level.WARNING, "Recording monitor task did not terminate gracefully, forcing shutdown.");
                } else {
                    log("Recording monitor task shut down gracefully.");
                }
            } catch (InterruptedException ie) {
                recordingMonitorTask.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }


        if( config != null && config.getBoolean( "prism.database.force-write-queue-on-shutdown", true ) ) {
            log("Attempting to force drain action queue on shutdown...");
            final QueueDrain drainer = new QueueDrain( this );
            drainer.forceDrainQueue();
        }

        if( pool != null ) {
            log("Closing database connection pool...");
            pool.close(true);
            pool = null;
            log("Database connection pool closed.");
        }

        if (prismActions != null) prismActions.clear();
        if (prismWorlds != null) prismWorlds.clear();
        if (prismPlayers != null) prismPlayers.clear();
        if (playersWithActiveTools != null) playersWithActiveTools.clear();

        log( "Prism plugin fully disabled." );
    }
}
