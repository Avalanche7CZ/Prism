package me.botsko.prism.database;

import me.botsko.prism.Prism;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.logging.Level;

public class PrismDatabaseHandler {

    private static Prism pluginInstance;
    private static DataSource pool = new DataSource();

    private static String db_type;
    private static String db_hostname;
    private static String db_port_str;
    private static String db_database_name;
    private static String db_username;
    private static String db_password;
    private static String db_table_prefix;
    private static String db_file_path;

    private static int db_pool_initial_size;
    private static int db_pool_max_connections;
    private static int db_pool_max_idle_connections;
    private static int db_pool_max_wait_ms;

    public static void initialize(Prism plugin) {
        pluginInstance = plugin;
        loadDbConfigFields();
        configureDbPool();
    }

    public static void loadDbConfigFields() {
        if (Prism.config == null) {
            Prism.log(Level.SEVERE, "Prism.config is null during PrismDatabaseHandler.loadDbConfigFields. This should have been loaded by Prism's onEnable before PrismDatabaseHandler.initialize.");
            Prism tempInstance = pluginInstance;
            if (tempInstance == null) {
                Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
                if (p instanceof Prism) {
                    tempInstance = (Prism) p;
                }
            }
            if (tempInstance != null) {
                Prism.log(Level.INFO, "Attempting to load Prism config from PrismDatabaseHandler.loadDbConfigFields.");
                tempInstance.loadAllConfigs();
            }

            if (Prism.config == null) {
                Prism.log(Level.SEVERE, "Failed to load Prism.config even after attempting from PrismDatabaseHandler.loadDbConfigFields. DB operations may use defaults or fail.");
                return;
            }
        }
        db_type = Prism.config.getString("prism.database.type", "sqlite").toLowerCase();
        db_hostname = Prism.config.getString("prism.database.hostname", "127.0.0.1");
        db_port_str = Prism.config.getString("prism.database.port", getDefaultPortForDB(db_type));
        db_database_name = Prism.config.getString("prism.database.databaseName", "minecraft");
        db_username = Prism.config.getString("prism.database.username", "root");
        db_password = Prism.config.getString("prism.database.password", "");
        db_table_prefix = Prism.config.getString("prism.database.tablePrefix", "prism_");
        db_file_path = Prism.config.getString("prism.database.filePath", "prism.db");

        db_pool_initial_size = Prism.config.getInt("prism.database.pool.initial-size", 5);
        db_pool_max_connections = Prism.config.getInt("prism.database.pool.max-connections", 20);
        db_pool_max_idle_connections = Prism.config.getInt("prism.database.pool.max-idle-connections", 10);
        db_pool_max_wait_ms = Prism.config.getInt("prism.database.pool.max-wait-ms", 30000);
    }

    private static String getDefaultPortForDB(String dbType) {
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

    public static void configureDbPool() {
        if (pool == null) {
            Prism.log("DataSource pool is null during configureDbPool, re-initializing. This indicates a prior issue.");
            pool = new DataSource();
        }

        if (Prism.config == null) {
            Prism.log("Configuration is null during configureDbPool. Attempting to load it now.");
            Prism tempInstanceForConfigLoad = pluginInstance;
            if (tempInstanceForConfigLoad == null) {
                Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
                if (p instanceof Prism) {
                    tempInstanceForConfigLoad = (Prism) p;
                }
            }

            if (tempInstanceForConfigLoad != null) {
                tempInstanceForConfigLoad.loadAllConfigs();
                loadDbConfigFields();
            }

            if (Prism.config == null) {
                Prism.log(Level.SEVERE, "Failed to load configuration. Aborting database pool configuration. Prism may not function.");
                return;
            }
        }

        if (db_type == null) {
            loadDbConfigFields();
            if (db_type == null && (Prism.config == null || Prism.config.getString("prism.database.type") == null) ) {
                Prism.log(Level.SEVERE, "Database type (db_type) is null after attempting to load config. Aborting pool configuration.");
                return;
            }
        }


        Prism localPluginInstance = pluginInstance;
        if (localPluginInstance == null) {
            Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
            if (p instanceof Prism) {
                localPluginInstance = (Prism) p;
                PrismDatabaseHandler.pluginInstance = localPluginInstance;
            } else {
                if ("h2".equals(db_type) || "sqlite".equals(db_type)) {
                    Prism.log(Level.SEVERE, "Prism plugin instance is null and DB type is " + db_type + ". Cannot get data folder. Aborting pool configuration.");
                    return;
                }
                Prism.log(Level.WARNING, "Prism plugin instance is null in configureDbPool. File-based DBs (H2, SQLite) might fail if attempted.");
            }
        }

        String jdbcUrl;
        String driverClass;

        Prism.log("Configuring database connection for type: " + db_type);

        switch (db_type) {
            case "mysql":
                driverClass = Prism.config.getString("prism.database.driverClassName.mysql", "com.mysql.jdbc.Driver");
                jdbcUrl = Prism.config.getString("prism.database.jdbcUrlPrefix.mysql", "jdbc:mysql://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name
                        + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
                break;
            case "mariadb":
                driverClass = Prism.config.getString("prism.database.driverClassName.mariadb", "org.mariadb.jdbc.Driver");
                jdbcUrl = Prism.config.getString("prism.database.jdbcUrlPrefix.mariadb", "jdbc:mariadb://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name
                        + "?autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
                break;
            case "postgresql":
                driverClass = Prism.config.getString("prism.database.driverClassName.postgresql", "org.postgresql.Driver");
                jdbcUrl = Prism.config.getString("prism.database.jdbcUrlPrefix.postgresql", "jdbc:postgresql://")
                        + db_hostname + ":" + db_port_str + "/" + db_database_name;
                break;
            case "h2":
                driverClass = Prism.config.getString("prism.database.driverClassName.h2", "org.h2.Driver");
                if (localPluginInstance == null) {
                    Prism.log(Level.SEVERE, "Cannot configure H2: Prism plugin instance is null.");
                    return;
                }
                File h2DbFile = new File(localPluginInstance.getDataFolder(), db_file_path);
                localPluginInstance.getDataFolder().mkdirs();
                jdbcUrl = Prism.config.getString("prism.database.jdbcUrlPrefix.h2", "jdbc:h2:") + h2DbFile.getAbsolutePath();
                if (Prism.config.getBoolean("prism.database.h2.mysqlMode", true)) {
                    jdbcUrl += ";MODE=MySQL;DATABASE_TO_UPPER=FALSE";
                }
                if (Prism.config.getBoolean("prism.database.h2.autoServer", false)) {
                    jdbcUrl += ";AUTO_SERVER=TRUE";
                }
                Prism.log("H2 JDBC URL: " + jdbcUrl);
                break;
            case "sqlite":
                driverClass = Prism.config.getString("prism.database.driverClassName.sqlite", "org.sqlite.JDBC");
                if (localPluginInstance == null) {
                    Prism.log(Level.SEVERE, "Cannot configure SQLite: Prism plugin instance is null.");
                    return;
                }
                File sqliteDbFile = new File(localPluginInstance.getDataFolder(), db_file_path);
                localPluginInstance.getDataFolder().mkdirs();
                jdbcUrl = Prism.config.getString("prism.database.jdbcUrlPrefix.sqlite", "jdbc:sqlite:") + sqliteDbFile.getAbsolutePath();
                Prism.log("SQLite JDBC URL: " + jdbcUrl);
                break;
            default:
                Prism.log(Level.WARNING, "Unsupported database type: " + db_type + ". Defaulting to SQLite for safety.");
                db_type = "sqlite";
                driverClass = "org.sqlite.JDBC";
                if (localPluginInstance == null) {
                    Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
                    if (p instanceof Prism) localPluginInstance = (Prism) p;
                    else {
                        Prism.log(Level.SEVERE, "Cannot configure fallback SQLite: Prism plugin instance is null.");
                        return;
                    }
                }
                File fallbackSqliteFile = new File(localPluginInstance.getDataFolder(), "prism_fallback.db");
                localPluginInstance.getDataFolder().mkdirs();
                jdbcUrl = "jdbc:sqlite:" + fallbackSqliteFile.getAbsolutePath();
                Prism.log("Fallback SQLite JDBC URL: " + jdbcUrl);
                db_file_path = "prism_fallback.db";
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
        pool.setRemoveAbandoned(Prism.config.getBoolean("prism.database.pool.remove-abandoned.enabled", true));
        pool.setRemoveAbandonedTimeout(Prism.config.getInt("prism.database.pool.remove-abandoned.timeout-seconds", 60));
        pool.setTestOnBorrow(Prism.config.getBoolean("prism.database.pool.test-on-borrow", true));
        pool.setValidationQuery(getValidationQueryForDB(db_type));
        pool.setValidationInterval(Prism.config.getInt("prism.database.pool.validation-interval-ms", 30000));
        pool.setLogAbandoned(Prism.config.getBoolean("prism.debug", false));

        Prism.log("Database pool configured for " + db_type + " with driver " + driverClass);
    }

    private static String getValidationQueryForDB(String dbType) {
        if (Prism.config == null) {
            Prism.log(Level.WARNING, "Prism.config is null in getValidationQueryForDB. Validation query may be incorrect.");
            return "SELECT 1";
        }
        String queryFromConfig = Prism.config.getString("prism.database.pool.validation-query." + dbType.toLowerCase());
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
        return pool;
    }

    public static Connection dbc() {
        Connection con = null;
        try {
            if (pool == null || pool.getPoolProperties().getDriverClassName() == null) {
                Prism.log(Level.WARNING, "Database pool is not initialized or configured. Attempting to reconfigure.");
                Prism localPluginInstance = pluginInstance;
                if (localPluginInstance == null) {
                    Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
                    if (p instanceof Prism) {
                        localPluginInstance = (Prism) p;
                        PrismDatabaseHandler.pluginInstance = localPluginInstance;
                    }
                }
                if (localPluginInstance != null) {
                    if (Prism.config == null) localPluginInstance.loadAllConfigs();
                    loadDbConfigFields();
                    configureDbPool();
                    if (pool == null || pool.getPoolProperties().getDriverClassName() == null) {
                        Prism.log(Level.SEVERE, "Failed to reconfigure database pool. No connection can be established.");
                        return null;
                    }
                } else {
                    Prism.log(Level.SEVERE, "Prism plugin instance not found. Cannot establish database connection.");
                    return null;
                }
            }
            con = pool.getConnection();
        } catch (final SQLException e) {
            Prism.log(Level.SEVERE, "Database connection failed: " + e.getMessage());
            FileConfiguration currentConfig = Prism.config;
            if (currentConfig == null || currentConfig.getBoolean("prism.debug", false) || !e.getMessage().contains("Pool empty")) {
                e.printStackTrace();
            }
        }
        return con;
    }

    public static void rebuildPool() {
        Prism.log("Rebuilding database connection pool...");
        if (pool != null) {
            pool.close(true);
        }
        pool = new DataSource();
        Prism localPluginInstance = pluginInstance;
        if (localPluginInstance == null) {
            Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
            if (p instanceof Prism) {
                localPluginInstance = (Prism) p;
                PrismDatabaseHandler.pluginInstance = localPluginInstance;
            }
        }
        if (Prism.config == null && localPluginInstance != null) localPluginInstance.loadAllConfigs();
        loadDbConfigFields();
        configureDbPool();
        Prism.log("Database connection pool rebuilt.");
    }

    protected static boolean attemptToRescueConnection(SQLException e) {
        String msg = e.getMessage().toLowerCase();
        if (msg.contains("connection closed") || msg.contains("communications link failure") || msg.contains("broken pipe") || msg.contains("connection reset") || msg.contains("no operations allowed after connection closed")) {
            Prism.log(Level.WARNING, "Attempting to rescue database connection due to: " + e.getMessage());
            rebuildPool();
            try (Connection conn = dbc()) {
                if (conn != null && !conn.isClosed()) {
                    Prism.log("Database connection rescued successfully.");
                    return true;
                } else {
                    Prism.log(Level.SEVERE, "Failed to rescue database connection: New connection is null or closed.");
                }
            } catch (SQLException rescueEx) {
                Prism.log(Level.SEVERE, "SQLException during database connection rescue attempt: " + rescueEx.getMessage());
                FileConfiguration currentConfig = Prism.config;
                if (currentConfig != null && currentConfig.getBoolean("prism.debug", false)) rescueEx.printStackTrace();
            }
        }
        return false;
    }

    public static void handleDatabaseException(SQLException e) {
        if (attemptToRescueConnection(e)) {
            return;
        }
        Prism.log(Level.SEVERE, "Database connection error: " + e.getMessage());
        if (e.getMessage().contains("marked as crashed")) {
            final String[] crashMsg = new String[2];
            crashMsg[0] = "If your database tables are marked as crashed, they may be corrupted.";
            crashMsg[1] = "For MySQL/MariaDB, try running `CHECK TABLE " + db_table_prefix + "your_table_name` and then `REPAIR TABLE " + db_table_prefix + "your_table_name`.";
            Prism.logSection(crashMsg);
        }
        FileConfiguration currentConfig = Prism.config;
        if (currentConfig == null || currentConfig.getBoolean("prism.debug", false)) {
            e.printStackTrace();
        }
    }

    public static void setupDatabase() {
        Prism localPluginInstance = pluginInstance;
        if (localPluginInstance == null) {
            Plugin p = Bukkit.getPluginManager().getPlugin(Prism.getPrismName());
            if (p instanceof Prism) {
                localPluginInstance = (Prism) p;
                PrismDatabaseHandler.pluginInstance = localPluginInstance;
            }
        }

        Connection conn = null;
        Statement st = null;
        try {
            conn = dbc();
            if (conn == null) {
                Prism.log(Level.SEVERE, "Cannot setup database, no connection available. Prism will be disabled.");
                if (localPluginInstance != null) {
                    localPluginInstance.disablePlugin();
                } else {
                    Prism.log(Level.SEVERE, "Cannot disable Prism plugin as instance is null during setupDatabase.");
                }
                return;
            }

            if (db_type == null) {
                loadDbConfigFields();
                if (db_type == null && (Prism.config == null || Prism.config.getString("prism.database.type") == null)) {
                    Prism.log(Level.SEVERE, "Database type (db_type) is null in setupDatabase. Aborting.");
                    return;
                }
            }


            String currentDbSchemaType = db_type;
            Prism.log("Setting up database schema for type: " + currentDbSchemaType);

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
                    Prism.log(Level.WARNING, "Unknown database type in setupDatabase: " + currentDbSchemaType + ". Using SQLite defaults for schema types.");
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

            Prism.log("Database schema setup/verification complete.");
            cacheActionPrimaryKeys();
            if (Prism.getActionRegistry() != null) {
                final String[] actions = Prism.getActionRegistry().listAll();
                for (final String a : actions) {
                    addActionName(a);
                }
            } else {
                Prism.log(Level.WARNING, "ActionRegistry not initialized during setupDatabase, cannot add default actions to DB.");
            }

        } catch (final SQLException e) {
            Prism.log(Level.SEVERE, "Database setup error: " + e.getMessage());
            e.printStackTrace();
            if (localPluginInstance != null) localPluginInstance.disablePlugin();
        } finally {
            if (st != null) try { st.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    private static void tryExecuteUpdate(Statement st, String query) {
        try {
            st.executeUpdate(query);
        } catch (SQLException e) {
            String sqlState = e.getSQLState();
            boolean alreadyExistsError = (sqlState != null && (sqlState.equals("42S11") || sqlState.equals("42P07") || sqlState.equals("42S01") || sqlState.equals("X0Y32"))) ||
                    (e.getMessage() != null && (e.getMessage().toLowerCase().contains("already exist") || e.getMessage().toLowerCase().contains("duplicate")));

            if (alreadyExistsError) {
                Prism.debug("Attempted to create index/table that already exists (expected for IF NOT EXISTS or similar logic): " + query + " - SQLState: " + sqlState + ", Error: " + e.getMessage());
            } else {
                Prism.log(Level.WARNING, "SQLException during tryExecuteUpdate for query [" + query + "]: SQLState: " + sqlState + ", Error: " + e.getMessage());
                FileConfiguration currentConfig = Prism.config;
                if (currentConfig != null && currentConfig.getBoolean("prism.debug", false)) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void cacheActionPrimaryKeys() {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if (conn == null) {
                Prism.log(Level.WARNING, "Cannot cache action primary keys, no DB connection.");
                return;
            }
            s = conn.prepareStatement("SELECT action_id, action FROM `" + db_table_prefix + "actions`");
            rs = s.executeQuery();
            HashMap<String, Integer> newActions = new HashMap<>();
            while (rs.next()) {
                newActions.put(rs.getString("action"), rs.getInt("action_id"));
            }
            Prism.prismActions.clear();
            Prism.prismActions.putAll(newActions);
            Prism.debug("Cached " + Prism.prismActions.size() + " action primary keys.");
        } catch (final SQLException e) {
            handleDatabaseException(e);
        } finally {
            if (rs != null) try { rs.close(); } catch (final SQLException e) { }
            if (s != null) try { s.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    public static void addActionName(String actionName) {
        if (Prism.prismActions.containsKey(actionName)) return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if (conn == null) {
                Prism.log(Level.WARNING, "Cannot add action name '" + actionName + "', no DB connection.");
                return;
            }
            s = conn.prepareStatement("INSERT INTO `" + db_table_prefix + "actions` (action) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            s.setString(1, actionName);
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                Prism.prismActions.put(actionName, newId);
                Prism.debug("Added action '" + actionName + "' with ID " + newId + " to database and cache.");
            } else {
                Prism.log(Level.WARNING, "Insert statement for action '" + actionName + "' failed to return generated key. Re-querying.");
                cacheActionPrimaryKeys();
                if (!Prism.prismActions.containsKey(actionName)) {
                    Prism.log(Level.SEVERE, "Failed to add or find action '" + actionName + "' in database after insert attempt.");
                }
            }
        } catch (final SQLException e) {
            if (e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("duplicate entry")) {
                Prism.debug("Action '" + actionName + "' likely already exists (unique constraint violation). Recaching actions.");
                cacheActionPrimaryKeys();
            } else {
                handleDatabaseException(e);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (final SQLException e) { }
            if (s != null) try { s.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    public static void cacheWorldPrimaryKeys() {
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if (conn == null) {
                Prism.log(Level.WARNING, "Cannot cache world primary keys, no DB connection.");
                return;
            }
            s = conn.prepareStatement("SELECT world_id, world FROM `" + db_table_prefix + "worlds`");
            rs = s.executeQuery();
            HashMap<String, Integer> newWorlds = new HashMap<>();
            while (rs.next()) {
                newWorlds.put(rs.getString("world"), rs.getInt("world_id"));
            }
            Prism.prismWorlds.clear();
            Prism.prismWorlds.putAll(newWorlds);
            Prism.debug("Cached " + Prism.prismWorlds.size() + " world primary keys.");
        } catch (final SQLException e) {
            handleDatabaseException(e);
        } finally {
            if (rs != null) try { rs.close(); } catch (final SQLException e) { }
            if (s != null) try { s.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    public static void addWorldName(String worldName) {
        if (Prism.prismWorlds.containsKey(worldName)) return;

        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = dbc();
            if (conn == null) {
                Prism.log(Level.WARNING, "Cannot add world name '" + worldName + "', no DB connection.");
                return;
            }
            s = conn.prepareStatement("INSERT INTO `" + db_table_prefix + "worlds` (world) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
            s.setString(1, worldName);
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (rs.next()) {
                int newId = rs.getInt(1);
                Prism.prismWorlds.put(worldName, newId);
                Prism.debug("Added world '" + worldName + "' with ID " + newId + " to database and cache.");
            } else {
                Prism.log(Level.WARNING, "Insert statement for world '" + worldName + "' failed to return generated key. Re-querying.");
                cacheWorldPrimaryKeys();
                if (!Prism.prismWorlds.containsKey(worldName)) {
                    Prism.log(Level.SEVERE, "Failed to add or find world '" + worldName + "' in database after insert attempt.");
                }
            }
        } catch (final SQLException e) {
            if (e.getMessage().toLowerCase().contains("unique constraint") || e.getMessage().toLowerCase().contains("duplicate entry")) {
                Prism.debug("World '" + worldName + "' likely already exists (unique constraint violation). Recaching worlds.");
                cacheWorldPrimaryKeys();
            } else {
                handleDatabaseException(e);
            }
        } finally {
            if (rs != null) try { rs.close(); } catch (final SQLException e) { }
            if (s != null) try { s.close(); } catch (final SQLException e) { }
            if (conn != null) try { conn.close(); } catch (final SQLException e) { }
        }
    }

    public static String getDbType() {
        if (db_type == null) {
            loadDbConfigFields();
        }
        return db_type;
    }

    public static String getTablePrefix() {
        if (db_table_prefix == null) {
            loadDbConfigFields();
        }
        return db_table_prefix;
    }
}
