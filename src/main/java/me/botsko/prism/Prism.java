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
import java.util.logging.Logger;

public class Prism extends JavaPlugin {

    private static DataSource pool = new DataSource();

    private static String plugin_name;
    private String plugin_version;
    private static MaterialAliases items;
    private Language language;
    private static Logger log = Logger.getLogger( "Minecraft" );
    private final ArrayList<String> enabledPlugins = new ArrayList<String>();
    private static ActionRegistry actionRegistry;
    private static HandlerRegistry<?> handlerRegistry;
    private static Ignore ignore;
    protected static ArrayList<Integer> illegalBlocks;
    protected static ArrayList<String> illegalEntities;
    protected static HashMap<String, String> alertedOres = new HashMap<String, String>();
    private static HashMap<String, PrismParameterHandler> paramHandlers = new HashMap<String, PrismParameterHandler>();
    private final ScheduledThreadPoolExecutor schedulePool = new ScheduledThreadPoolExecutor( 1 );
    private final ScheduledThreadPoolExecutor recordingMonitorTask = new ScheduledThreadPoolExecutor( 1 );
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
    public static HashMap<UUID,PrismPlayer> prismPlayers = new HashMap<UUID,PrismPlayer>();
    public static HashMap<String, Integer> prismActions = new HashMap<String, Integer>();

    public ConcurrentHashMap<String, String> preplannedBlockFalls = new ConcurrentHashMap<String, String>();
    public ConcurrentHashMap<String, String> preplannedVehiclePlacement = new ConcurrentHashMap<String, String>();

    private String db_driver_class_name;
    private String db_jdbc_url_prefix;
    private String db_hostname;
    private String db_port;
    private String db_database_name;
    private String db_username;
    private String db_password;
    private String db_table_prefix;
    private int db_pool_initial_size;
    private int db_pool_max_connections;
    private int db_pool_max_idle_connections;
    private int db_pool_max_wait_ms;


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

        db_driver_class_name = config.getString("prism.database.driverClassName", "com.mysql.jdbc.Driver");
        db_jdbc_url_prefix = config.getString("prism.database.jdbcUrlPrefix", "jdbc:mysql://");
        db_hostname = config.getString("prism.database.hostname", "127.0.0.1");
        db_port = config.getString("prism.database.port", "3306");
        db_database_name = config.getString("prism.database.databaseName", "minecraft");
        db_username = config.getString("prism.database.username", "root");
        db_password = config.getString("prism.database.password", "");
        db_table_prefix = config.getString("prism.database.tablePrefix", "prism_");

        db_pool_initial_size = config.getInt("prism.database.pool.initial-size", 10);
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

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadConfig();
        configureDbPool();
    }

    private void configureDbPool() {
        final String dns = db_jdbc_url_prefix + db_hostname + ":" + db_port + "/" + db_database_name + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&characterEncoding=UTF-8";

        pool.setDriverClassName(db_driver_class_name);
        pool.setUrl(dns);
        pool.setUsername(db_username);
        pool.setPassword(db_password);
        pool.setInitialSize(db_pool_initial_size);
        pool.setMaxActive(db_pool_max_connections);
        pool.setMaxIdle(db_pool_max_idle_connections);
        pool.setMaxWait(db_pool_max_wait_ms);
        pool.setRemoveAbandoned(true);
        pool.setRemoveAbandonedTimeout(60);
        pool.setTestOnBorrow(true);
        pool.setValidationQuery("/* ping */SELECT 1");
        pool.setValidationInterval(30000);
    }

    public DataSource initDbPool() {
        configureDbPool();
        return pool;
    }

    public void rebuildPool() {
        if( pool != null ) {
            pool.close();
        }
        pool = new DataSource();
        configureDbPool();
    }

    public static DataSource getPool() {
        return Prism.pool;
    }

    public static Connection dbc() {
        Connection con = null;
        try {
            if (pool == null) {
                Prism instance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
                if (instance != null) {
                    instance.configureDbPool();
                } else {
                    return null;
                }
            }
            con = pool.getConnection();
        } catch ( final SQLException e ) {
            log( "Database connection failed. " + e.getMessage() );
            if( !e.getMessage().contains( "Pool empty" ) ) {
                e.printStackTrace();
            }
        }
        return con;
    }

    protected boolean attemptToRescueConnection(SQLException e) throws SQLException {
        if( e.getMessage().contains( "connection closed" ) || e.getMessage().contains("Communications link failure") ) {
            log("Attempting to rescue database connection...");
            rebuildPool();
            if( pool != null ) {
                try (Connection conn = dbc()){
                    if( conn != null && !conn.isClosed() ) {
                        log("Database connection rescued successfully.");
                        return true;
                    }
                } catch (SQLException rescueEx){
                    log("Failed to rescue database connection: " + rescueEx.getMessage());
                }
            }
        }
        return false;
    }

    public void handleDatabaseException(SQLException e) {
        try {
            if( attemptToRescueConnection( e ) ) { return; }
        } catch ( final SQLException e1 ) {}
        log( "Database connection error: " + e.getMessage() );
        if( e.getMessage().contains( "marked as crashed" ) ) {
            final String[] msg = new String[2];
            msg[0] = "If your database crashes during write it may corrupt it's indexes.";
            msg[1] = "Try running `CHECK TABLE " + db_table_prefix + "data` and then `REPAIR TABLE " + db_table_prefix + "data`.";
            logSection( msg );
        }
        e.printStackTrace();
    }

    protected void setupDatabase() {
        Connection conn = null;
        Statement st = null;
        try {
            conn = dbc();
            if( conn == null ){
                return;
            }

            String query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "actions` ("
                    + "`action_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`action` varchar(25) NOT NULL,"
                    + "PRIMARY KEY (`action_id`)," + "UNIQUE KEY `action` (`action`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st = conn.createStatement();
            st.executeUpdate( query );

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "data` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                    + "`epoch` int(10) unsigned NOT NULL," + "`action_id` int(10) unsigned NOT NULL,"
                    + "`player_id` int(10) unsigned NOT NULL," + "`world_id` int(10) unsigned NOT NULL,"
                    + "`x` int(11) NOT NULL," + "`y` int(11) NOT NULL," + "`z` int(11) NOT NULL,"
                    + "`block_id` mediumint(5) DEFAULT NULL," + "`block_subid` mediumint(5) DEFAULT NULL,"
                    + "`old_block_id` mediumint(5) DEFAULT NULL," + "`old_block_subid` mediumint(5) DEFAULT NULL,"
                    + "PRIMARY KEY (`id`)," + "KEY `epoch` (`epoch`),"
                    + "KEY  `location` (`world_id`, `x`, `z`, `y`, `action_id`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            final DatabaseMetaData metadata = conn.getMetaData();
            ResultSet resultSet;
            resultSet = metadata.getTables( null, null, "" + db_table_prefix + "data_extra", null );
            if( !resultSet.next() ) {
                query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "data_extra` ("
                        + "`extra_id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                        + "`data_id` int(10) unsigned NOT NULL," + "`data` text NULL," + "`te_data` text NULL,"
                        + "PRIMARY KEY (`extra_id`)," + "KEY `data_id` (`data_id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
                st.executeUpdate( query );
                query = "ALTER TABLE `" + db_table_prefix + "data_extra` ADD CONSTRAINT `" + db_table_prefix + "data_extra_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `" + db_table_prefix + "data` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;";
                st.executeUpdate( query );
            }
            if(resultSet != null) resultSet.close();

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "meta` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT,"
                    + "`k` varchar(25) NOT NULL," + "`v` varchar(255) NOT NULL," + "PRIMARY KEY (`id`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "players` ("
                    + "`player_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`player` varchar(255) NOT NULL,"
                    + "`player_uuid` binary(16) NOT NULL,"
                    + "PRIMARY KEY (`player_id`)," + "UNIQUE KEY `player` (`player`),"
                    + "UNIQUE KEY `player_uuid` (`player_uuid`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            query = "CREATE TABLE IF NOT EXISTS `" + db_table_prefix + "worlds` ("
                    + "`world_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`world` varchar(255) NOT NULL,"
                    + "PRIMARY KEY (`world_id`)," + "UNIQUE KEY `world` (`world`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
            st.executeUpdate( query );

            cacheActionPrimaryKeys();
            final String[] actions = actionRegistry.listAll();
            for ( final String a : actions ) {
                addActionName( a );
            }
        } catch ( final SQLException e ) {
            log( "Database setup error: " + e.getMessage() );
            e.printStackTrace();
        } finally {
            if( st != null ) try { st.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    protected void cacheActionPrimaryKeys() {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null) return;
            s = conn.prepareStatement( "SELECT action_id, action FROM " + db_table_prefix + "actions" );
            rs = s.executeQuery();
            while ( rs.next() ) {
                prismActions.put( rs.getString( 2 ), rs.getInt( 1 ) );
            }
        } catch ( final SQLException e ) {
            handleDatabaseException( e );
        } finally {
            if( rs != null ) try { rs.close(); } catch ( final SQLException e ) {}
            if( s != null ) try { s.close(); } catch ( final SQLException e ) {}
            if( conn != null ) try { conn.close(); } catch ( final SQLException e ) {}
        }
    }

    public static void addActionName(String actionName) {
        Prism instance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if( instance == null || prismActions.containsKey( actionName ) ) return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null) return;
            s = conn.prepareStatement( "INSERT INTO " + instance.db_table_prefix + "actions (action) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, actionName );
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                prismActions.put( actionName, rs.getInt( 1 ) );
            } else {
                throw new SQLException( "Insert statement failed - no generated key obtained." );
            }
        } catch ( final SQLException e ) {
            instance.handleDatabaseException(e);
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
            if(conn == null) return;
            s = conn.prepareStatement( "SELECT world_id, world FROM " + db_table_prefix + "worlds" );
            rs = s.executeQuery();
            while ( rs.next() ) {
                prismWorlds.put( rs.getString( 2 ), rs.getInt( 1 ) );
            }
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
        if( instance == null || prismWorlds.containsKey( worldName ) ) return;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if(conn == null) return;
            s = conn.prepareStatement( "INSERT INTO " + instance.db_table_prefix + "worlds (world) VALUES (?)", Statement.RETURN_GENERATED_KEYS );
            s.setString( 1, worldName );
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if( rs.next() ) {
                prismWorlds.put( worldName, rs.getInt( 1 ) );
            } else {
                throw new SQLException( "Insert statement failed - no generated key obtained." );
            }
        } catch ( final SQLException e ) {
            instance.handleDatabaseException(e);
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
        if( we != null ) {
            plugin_worldEdit = (WorldEditPlugin) we;
            PrismBlockEditSessionFactory.initialize();
            log( "WorldEdit found. Associated features enabled." );
        } else {
            log( "WorldEdit not found. Certain optional features of Prism disabled." );
        }
    }

    public boolean dependencyEnabled(String pluginName) {
        return enabledPlugins.contains( pluginName );
    }

    public static ArrayList<Integer> getIllegalBlocks() {
        return illegalBlocks;
    }

    public static ArrayList<String> getIllegalEntities() {
        return illegalEntities;
    }

    public static HashMap<String, String> getAlertedOres() {
        return alertedOres;
    }

    public static MaterialAliases getItems() {
        return items;
    }

    public static ActionRegistry getActionRegistry() {
        return actionRegistry;
    }

    public static HandlerRegistry<?> getHandlerRegistry() {
        return handlerRegistry;
    }

    public static Ignore getIgnore() {
        return ignore;
    }

    public PurgeManager getPurgeManager() {
        return purgeManager;
    }

    public static void registerParameter(PrismParameterHandler handler) {
        paramHandlers.put( handler.getName().toLowerCase(), handler );
    }

    public static HashMap<String, PrismParameterHandler> getParameters() {
        return paramHandlers;
    }

    public static PrismParameterHandler getParameter(String name) {
        return paramHandlers.get( name );
    }

    public void endExpiredQueryCaches() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                for ( final Map.Entry<String, QueryResult> query : cachedQueries.entrySet() ) {
                    final QueryResult result = query.getValue();
                    final long diff = ( date.getTime() - result.getQueryTime() ) / 1000;
                    if( diff >= 120 ) {
                        cachedQueries.remove( query.getKey() );
                    }
                }
            }
        }, 2400L, 2400L );
    }

    public void endExpiredPreviews() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                for ( final Map.Entry<String, PreviewSession> query : playerActivePreviews.entrySet() ) {
                    final PreviewSession result = query.getValue();
                    final long diff = ( date.getTime() - result.getQueryTime() ) / 1000;
                    if( diff >= 60 ) {
                        final Player player = Bukkit.getServer().getPlayerExact( result.getPlayer().getName() );
                        if( player != null ) {
                            player.sendMessage( Prism.messenger.playerHeaderMsg( "Canceling forgotten preview." ) );
                        }
                        playerActivePreviews.remove( query.getKey() );
                    }
                }
            }
        }, 1200L, 1200L );
    }

    public void removeExpiredLocations() {
        getServer().getScheduler().scheduleSyncRepeatingTask( this, new Runnable() {
            @Override
            public void run() {
                final java.util.Date date = new java.util.Date();
                for ( final Entry<Location, Long> entry : alertedBlocks.entrySet() ) {
                    final long diff = ( date.getTime() - entry.getValue() ) / 1000;
                    if( diff >= 300 ) {
                        alertedBlocks.remove( entry.getKey() );
                    }
                }
            }
        }, 1200L, 1200L );
    }

    public void actionRecorderTask() {
        int recorder_tick_delay = config.getInt( "prism.queue-empty-tick-delay", 3 );
        if( recorder_tick_delay < 1 ) {
            recorder_tick_delay = 3;
        }
        recordingTask = getServer().getScheduler().runTaskLaterAsynchronously( this, new RecordingTask( this ), recorder_tick_delay );
    }

    public void launchScheduledPurgeManager() {
        final List<String> purgeRules = config.getStringList( "prism.db-records-purge-rules" );
        purgeManager = new PurgeManager( this, purgeRules );
        schedulePool.scheduleAtFixedRate( purgeManager, 0, 12, TimeUnit.HOURS );
    }

    public void launchInternalAffairs() {
        final InternalAffairs recordingMonitor = new InternalAffairs( this );
        recordingMonitorTask.scheduleAtFixedRate( recordingMonitor, 0, 5, TimeUnit.MINUTES );
    }

    public void alertPlayers(Player player, String msg) {
        for ( final Player p : getServer().getOnlinePlayers() ) {
            if( !p.equals( player ) || config.getBoolean( "prism.alerts.alert-player-about-self" ) ) {
                if( p.hasPermission( "prism.alerts" ) ) {
                    p.sendMessage( messenger.playerMsg( ChatColor.RED + "[!] " + msg ) );
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

    public void notifyNearby(Player player, int radius, String msg) {
        if( !config.getBoolean( "prism.appliers.notify-nearby.enabled" ) ) { return; }
        for ( final Player p : player.getServer().getOnlinePlayers() ) {
            if( !p.equals( player ) ) {
                if( player.getWorld().equals( p.getWorld() ) ) {
                    if( player.getLocation().distance( p.getLocation() ) <= ( radius + config.getInt( "prism.appliers.notify-nearby.additional-radius" ) ) ) {
                        p.sendMessage( messenger.playerHeaderMsg( msg ) );
                    }
                }
            }
        }
    }

    public static void log(String message) {
        if(plugin_name == null) plugin_name = "Prism";
        log.info( "[" + plugin_name + "]: " + message );
    }

    public static void logSection(String[] messages) {
        if( messages.length > 0 ) {
            log( "--------------------- ## Important ## ---------------------" );
            for ( final String msg : messages ) {
                log( msg );
            }
            log( "--------------------- ## ========= ## ---------------------" );
        }
    }

    public static void debug(String message) {
        if(config != null && config.getBoolean( "prism.debug" ) ) {
            if(plugin_name == null) plugin_name = "Prism";
            log.info( "[" + plugin_name + "]: " + message );
        }
    }

    public static void debug(Location loc) {
        debug( "Location: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() );
    }

    public void disablePlugin() {
        this.setEnabled( false );
    }

    @Override
    public void onDisable() {
        if( config != null && config.getBoolean( "prism.database.force-write-queue-on-shutdown" ) ) {
            final QueueDrain drainer = new QueueDrain( this );
            drainer.forceDrainQueue();
        }
        if( pool != null ) {
            pool.close();
        }
        if(schedulePool != null) schedulePool.shutdownNow();
        if(recordingMonitorTask != null) recordingMonitorTask.shutdownNow();
        log( "Closing plugin." );
    }
}
