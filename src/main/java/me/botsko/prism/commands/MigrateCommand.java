package me.botsko.prism.commands;

import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.database.PrismDatabaseHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MigrateCommand implements SubHandler {

    private final Prism plugin;
    private String migrationTargetDbType = null;
    private String migrationSourceDbType = null;

    private String targetHost, targetPort, targetDbName, targetUser, targetPass, targetPrefix, targetFilePath;
    private String sourcePrefix;

    private Map<Integer, Integer> actionIdMap = new HashMap<>();
    private Map<Integer, Integer> worldIdMap = new HashMap<>();
    private Map<Integer, Integer> playerIdMap = new HashMap<>();
    private Map<Integer, Integer> dataIdMap = new HashMap<>();

    private static boolean migrationInProgress = false;
    private FileConfiguration migrationConfig = null;
    private File migrationConfigFile = null;

    public MigrateCommand(Prism plugin) {
        this.plugin = plugin;
        this.migrationConfigFile = new File(plugin.getDataFolder(), "migrate.yml");
        loadMigrationConfig();
    }

    private void loadMigrationConfig() {
        if (!migrationConfigFile.exists()) {
            plugin.saveResource("migrate.yml", false);
            Prism.log(Level.INFO, "[Prism Migration] Default migrate.yml created. Please configure it for migration.");
        }
        migrationConfig = YamlConfiguration.loadConfiguration(migrationConfigFile);

        InputStream defaultConfigStream = plugin.getResource("migrate.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            migrationConfig.setDefaults(defaultConfig);
            migrationConfig.options().copyDefaults(true);
            try {
                migrationConfig.save(migrationConfigFile);
            } catch (IOException e) {
                Prism.log(Level.SEVERE, "[Prism Migration] Could not save migrate.yml with defaults: " + e.getMessage());
            }
        }
    }

    @Override
    public void handle(CallInfo call) {
        CommandSender sender = call.getSender();
        String[] receivedArgs = call.getArgs();

        if (Prism.config.getBoolean("prism.debug", false)) {
            String debugMsg = "[Prism Migrate DEBUG] handle() received raw args: " + Arrays.toString(receivedArgs);
            if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GRAY + debugMsg);
            Prism.log(Level.INFO, debugMsg);
        }


        if (receivedArgs.length < 1 || !receivedArgs[0].equalsIgnoreCase("migrate")) {
            sender.sendMessage(ChatColor.RED + "Error: Migrate command handler invoked incorrectly.");
            Prism.log(Level.WARNING, "[Prism Migrate DEBUG] Handler invoked with unexpected initial args: " + Arrays.toString(receivedArgs));
            return;
        }

        String[] migrateArgs = new String[receivedArgs.length - 1];
        System.arraycopy(receivedArgs, 1, migrateArgs, 0, receivedArgs.length - 1);

        if (Prism.config.getBoolean("prism.debug", false)) {
            String debugMsg = "[Prism Migrate DEBUG] Effective args for migrate: " + Arrays.toString(migrateArgs);
            if (!(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GRAY + debugMsg);
            Prism.log(Level.INFO, debugMsg);
        }


        if (migrateArgs.length == 0) {
            sendUsage(sender);
            return;
        }

        String subCommand = migrateArgs[0].toLowerCase();

        if (MigrateCommand.migrationInProgress && !subCommand.equals("status")) {
            sender.sendMessage(ChatColor.RED + "A migration is already in progress. Please wait for it to complete or check status with /prism migrate status.");
            return;
        }

        String[] subCommandSpecificArgs = new String[migrateArgs.length - 1];
        if (migrateArgs.length > 1) {
            System.arraycopy(migrateArgs, 1, subCommandSpecificArgs, 0, migrateArgs.length - 1);
        }


        switch (subCommand) {
            case "start":
                if (subCommandSpecificArgs.length < 1) {
                    sender.sendMessage(ChatColor.RED + "Usage: /prism migrate start <target_type (mysql|mariadb|sqlite)>");
                    return;
                }
                this.migrationTargetDbType = subCommandSpecificArgs[0].toLowerCase();
                if (!this.migrationTargetDbType.equals("mysql") && !this.migrationTargetDbType.equals("mariadb") && !this.migrationTargetDbType.equals("sqlite")) {
                    sender.sendMessage(ChatColor.RED + "Invalid target type. Supported: mysql, mariadb, sqlite.");
                    return;
                }
                this.migrationSourceDbType = PrismDatabaseHandler.getDbType();
                if (this.migrationSourceDbType == null || this.migrationSourceDbType.isEmpty()){
                    sender.sendMessage(ChatColor.RED + "Could not determine source database type. Please check Prism's main configuration.");
                    Prism.log(Level.SEVERE, "[Prism Migration] Source database type is null or empty. Main config issue?");
                    return;
                }
                if (this.migrationSourceDbType.equalsIgnoreCase(this.migrationTargetDbType)) {
                    sender.sendMessage(ChatColor.RED + "Source and target database types are the same. No migration needed.");
                    return;
                }
                sender.sendMessage(ChatColor.YELLOW + "Migration initiated: " + migrationSourceDbType + " -> " + migrationTargetDbType);
                sender.sendMessage(ChatColor.YELLOW + "Please ensure your 'migrate.yml' file is correctly set for the " + migrationTargetDbType + " database target.");
                sender.sendMessage(ChatColor.YELLOW + "Also, BACK UP YOUR CURRENT '" + migrationSourceDbType + "' DATABASE before proceeding!");
                sender.sendMessage(ChatColor.YELLOW + "Type '" + ChatColor.GOLD + "/prism migrate confirm" + ChatColor.YELLOW + "' to start the process once configured and backed up.");
                break;
            case "confirm":
                if (this.migrationTargetDbType == null || this.migrationSourceDbType == null) {
                    sender.sendMessage(ChatColor.RED + "Please use '/prism migrate start <target_type>' first.");
                    return;
                }
                if (migrationConfig == null) {
                    loadMigrationConfig();
                    if (migrationConfig == null) {
                        sender.sendMessage(ChatColor.RED + "Critical error: migrate.yml could not be loaded. Aborting.");
                        return;
                    }
                }
                if (!loadTargetConfigFromMigrationFile(sender)) {
                    return;
                }
                this.sourcePrefix = PrismDatabaseHandler.getTablePrefix();
                if (this.sourcePrefix == null){
                    sender.sendMessage(ChatColor.RED + "Could not determine source database table prefix. Please check Prism's main configuration.");
                    Prism.log(Level.SEVERE, "[Prism Migration] Source table prefix is null. Main config issue?");
                    return;
                }
                sender.sendMessage(ChatColor.GREEN + "Confirmation received. Starting migration from " + migrationSourceDbType + " to " + migrationTargetDbType + ".");
                sender.sendMessage(ChatColor.GOLD + "This will run in the background and may take a long time. Do NOT restart the server or Prism plugin.");
                sender.sendMessage(ChatColor.GOLD + "Check server console and use '/prism migrate status' for updates.");
                MigrateCommand.migrationInProgress = true;
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> performMigration(sender));
                break;
            case "status":
                if (MigrateCommand.migrationInProgress) {
                    sender.sendMessage(ChatColor.YELLOW + "Migration is currently in progress from " + (this.migrationSourceDbType != null ? this.migrationSourceDbType : "N/A") + " to " + (this.migrationTargetDbType != null ? this.migrationTargetDbType : "N/A") + ".");
                } else {
                    sender.sendMessage(ChatColor.GREEN + "No migration is currently in progress.");
                }
                break;
            default:
                sendUsage(sender);
                break;
        }
    }

    @Override
    public List<String> handleComplete(CallInfo call) {
        String[] receivedArgs = call.getArgs();
        if (receivedArgs.length == 2) {
            String currentSubCmdArg = receivedArgs[1].toLowerCase();
            List<String> mainSubCommands = Arrays.asList("start", "confirm", "status");
            return mainSubCommands.stream()
                    .filter(s -> s.startsWith(currentSubCmdArg))
                    .collect(Collectors.toList());
        }
        if (receivedArgs.length == 3 && receivedArgs[1].equalsIgnoreCase("start")) {
            String currentTargetTypeArg = receivedArgs[2].toLowerCase();
            List<String> targetTypes = Arrays.asList("mysql", "mariadb", "sqlite");
            return targetTypes.stream()
                    .filter(s -> s.startsWith(currentTargetTypeArg))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.AQUA + "Prism Data Migration Tool:"));
        sender.sendMessage(ChatColor.GOLD + "/prism migrate start <mysql|mariadb|sqlite>" + ChatColor.GRAY + " - Prepares for migration.");
        sender.sendMessage(ChatColor.GOLD + "/prism migrate confirm" + ChatColor.GRAY + " - Starts migration after 'start' & migrate.yml setup.");
        sender.sendMessage(ChatColor.GOLD + "/prism migrate status" + ChatColor.GRAY + " - Checks migration status.");
    }

    private boolean loadTargetConfigFromMigrationFile(CommandSender sender) {
        if (migrationConfig == null) {
            sender.sendMessage(ChatColor.RED + "migrate.yml is not loaded. Please try the command again or check server logs.");
            Prism.log(Level.SEVERE, "[Prism Migration] migrate.yml was null during loadTargetConfigFromMigrationFile.");
            return false;
        }

        String basePath = "target.";

        if (!migrationConfig.contains(basePath + "type") || !migrationConfig.getString(basePath + "type", "").equalsIgnoreCase(this.migrationTargetDbType)) {
            sender.sendMessage(ChatColor.RED + "Migration target type in migrate.yml (" + basePath + "type) does not match '" + this.migrationTargetDbType + "' or is not set.");
            Prism.log(Level.SEVERE, "Migration target type in migrate.yml (" + basePath + "type) mismatch or not set. Expected: " + this.migrationTargetDbType + ", Found: " + migrationConfig.getString(basePath + "type"));
            return false;
        }

        this.targetHost = migrationConfig.getString(basePath + "hostname", "127.0.0.1");
        this.targetPort = migrationConfig.getString(basePath + "port", (this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) ? "3306" : "");
        this.targetDbName = migrationConfig.getString(basePath + "databaseName", "prism_migrated");
        this.targetUser = migrationConfig.getString(basePath + "username", "root");
        this.targetPass = migrationConfig.getString(basePath + "password", "");
        this.targetPrefix = migrationConfig.getString(basePath + "tablePrefix", "prism_");
        this.targetFilePath = migrationConfig.getString(basePath + "filePath", "prism_migrated.db");

        if ((this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) && (this.targetHost.isEmpty() || this.targetDbName.isEmpty() || this.targetUser.isEmpty())) {
            sender.sendMessage(ChatColor.RED + "Missing essential MySQL/MariaDB target configuration under 'target.*' in migrate.yml");
            Prism.log(Level.SEVERE, "Missing essential MySQL/MariaDB target configuration in migrate.yml.");
            return false;
        }
        if (this.migrationTargetDbType.equals("sqlite") && this.targetFilePath.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Missing essential SQLite target file path configuration under 'target.filePath' in migrate.yml");
            Prism.log(Level.SEVERE, "Missing essential SQLite target file path configuration in migrate.yml.");
            return false;
        }
        sender.sendMessage(ChatColor.GREEN + "Target database configuration loaded successfully from migrate.yml.");
        return true;
    }

    private void performMigration(CommandSender sender) {
        Connection targetDbConn = null;
        long startTime = System.currentTimeMillis();
        String initialMsg = "[Prism Migration] Starting process from " + migrationSourceDbType + " to " + migrationTargetDbType + "...";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + initialMsg);
        Prism.log(Level.INFO, initialMsg);


        try (Connection sourceDbConn = PrismDatabaseHandler.dbc()) {
            if (sourceDbConn == null) {
                finishMigration(sender, false, "Failed to connect to source " + migrationSourceDbType + " database.", startTime);
                return;
            }
            Prism.log(Level.INFO, "[Prism Migration] Connected to source database (" + migrationSourceDbType + ").");

            targetDbConn = connectToTargetDatabase();
            if (targetDbConn == null) {
                finishMigration(sender, false, "Failed to connect to target " + migrationTargetDbType + " database.", startTime);
                return;
            }
            targetDbConn.setAutoCommit(false);
            Prism.log(Level.INFO, "[Prism Migration] Connected to target database (" + migrationTargetDbType + ").");

            createSchemaInTarget(targetDbConn, sender);

            migrateSimpleTable(sender, sourceDbConn, targetDbConn, "actions", "action_id", "action", actionIdMap, (ps, rs) -> ps.setString(1, rs.getString("action")));
            migrateSimpleTable(sender, sourceDbConn, targetDbConn, "worlds", "world_id", "world", worldIdMap, (ps, rs) -> ps.setString(1, rs.getString("world")));
            migratePlayersTable(sender, sourceDbConn, targetDbConn);
            migrateSimpleTable(sender, sourceDbConn, targetDbConn, "meta", null, null, null, (ps, rs) -> {
                ps.setString(1, rs.getString("k"));
                ps.setString(2, rs.getString("v"));
            });
            migrateDataTable(sender, sourceDbConn, targetDbConn);
            migrateDataExtraTable(sender, sourceDbConn, targetDbConn);

            targetDbConn.commit();
            finishMigration(sender, true, "Migration successfully completed!", startTime);

        } catch (SQLException e) {
            Prism.log(Level.SEVERE, "[Prism Migration] SQL error: " + e.getMessage());
            e.printStackTrace();
            if (targetDbConn != null) {
                try { if(!targetDbConn.isClosed()) targetDbConn.rollback(); } catch (SQLException ex) { Prism.log(Level.SEVERE, "[Prism Migration] Rollback failed: " + ex.getMessage()); }
            }
            finishMigration(sender, false, "Migration failed due to SQL error: " + e.getMessage(), startTime);
        } catch (Exception e) {
            Prism.log(Level.SEVERE, "[Prism Migration] General error: " + e.getMessage());
            e.printStackTrace();
            if (targetDbConn != null) {
                try { if(!targetDbConn.isClosed()) targetDbConn.rollback(); } catch (SQLException ex) { Prism.log(Level.SEVERE, "[Prism Migration] Rollback failed during general error: " + ex.getMessage()); }
            }
            finishMigration(sender, false, "Migration failed due to an unexpected error: " + e.getMessage(), startTime);
        }
        finally {
            if (targetDbConn != null) {
                try { if(!targetDbConn.isClosed()) targetDbConn.close(); } catch (SQLException e) { Prism.log(Level.WARNING, "[Prism Migration] Error closing target connection: " + e.getMessage()); }
            }
            clearMappings();
            MigrateCommand.migrationInProgress = false;
            this.migrationTargetDbType = null;
            this.migrationSourceDbType = null;
        }
    }

    private void finishMigration(CommandSender sender, boolean success, String message, long startTime) {
        long endTime = System.currentTimeMillis();
        long durationSeconds = (endTime - startTime) / 1000;
        String fullMessage = "[Prism Migration] " + message + " (Duration: " + durationSeconds + "s)";

        if (success) {
            if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
                sender.sendMessage(ChatColor.GREEN + fullMessage);
                sender.sendMessage(ChatColor.GREEN + "[Prism Migration] IMPORTANT: Update your main 'prism.database.*' settings in config.yml to point to the new " + this.migrationTargetDbType + " database and then RESTART your server.");
            }
            Prism.log(Level.INFO, fullMessage);
            Prism.log(Level.INFO, "[Prism Migration] IMPORTANT: User advised to update main config to " + this.migrationTargetDbType + " and restart.");
        } else {
            if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.RED + fullMessage);
            Prism.log(Level.SEVERE, fullMessage);
        }
        MigrateCommand.migrationInProgress = false;
    }


    private Connection connectToTargetDatabase() throws SQLException {
        String jdbcUrl;
        String driverClass = "";
        Properties props = new Properties();

        if (migrationConfig == null) {
            throw new SQLException("Migration configuration (migrate.yml) not loaded.");
        }

        String driverConfigPath = "target.driverClassName." + this.migrationTargetDbType;
        String urlPrefixConfigPath = "target.jdbcUrlPrefix." + this.migrationTargetDbType;


        switch (this.migrationTargetDbType) {
            case "mysql":
                driverClass = migrationConfig.getString(driverConfigPath, "com.mysql.cj.jdbc.Driver");
                jdbcUrl = migrationConfig.getString(urlPrefixConfigPath, "jdbc:mysql://")
                        + this.targetHost + ":" + this.targetPort + "/" + this.targetDbName
                        + "?rewriteBatchedStatements=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
                props.setProperty("user", this.targetUser);
                props.setProperty("password", this.targetPass);
                break;
            case "mariadb":
                driverClass = migrationConfig.getString(driverConfigPath, "org.mariadb.jdbc.Driver");
                jdbcUrl = migrationConfig.getString(urlPrefixConfigPath, "jdbc:mariadb://")
                        + this.targetHost + ":" + this.targetPort + "/" + this.targetDbName
                        + "?rewriteBatchedStatements=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
                props.setProperty("user", this.targetUser);
                props.setProperty("password", this.targetPass);
                break;
            case "sqlite":
                driverClass = migrationConfig.getString(driverConfigPath, "org.sqlite.JDBC");
                File dbFile = new File(plugin.getDataFolder(), this.targetFilePath);
                if (!dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }
                jdbcUrl = migrationConfig.getString(urlPrefixConfigPath, "jdbc:sqlite:") + dbFile.getAbsolutePath();
                break;
            default:
                throw new SQLException("Unsupported target database type for migration: " + this.migrationTargetDbType);
        }

        try {
            Class.forName(driverClass);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC Driver not found: " + driverClass, e);
        }
        return DriverManager.getConnection(jdbcUrl, props);
    }

    private void createSchemaInTarget(Connection targetDbConn, CommandSender sender) throws SQLException {
        String msg = "[Prism Migration] Creating/Verifying schema in target " + this.migrationTargetDbType + " database...";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + msg);
        Prism.log(Level.INFO, msg + " with prefix '" + this.targetPrefix + "'...");


        try (Statement stmt = targetDbConn.createStatement()) {
            String autoIncrementType, uuidType, textType, engineClause, intType, blockIdDataType;
            intType = "INT";
            blockIdDataType = "INT";
            textType = "TEXT";


            switch (this.migrationTargetDbType) {
                case "mysql":
                case "mariadb":
                    autoIncrementType = intType + " NOT NULL AUTO_INCREMENT";
                    uuidType = "BINARY(16) NOT NULL";
                    engineClause = " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
                    break;
                case "sqlite":
                    autoIncrementType = "INTEGER PRIMARY KEY AUTOINCREMENT";
                    uuidType = "BLOB NOT NULL";
                    engineClause = "";
                    break;
                default:
                    throw new SQLException("Unsupported DB type for schema creation: " + this.migrationTargetDbType);
            }

            List<String> queries = new ArrayList<>();

            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "actions` ("
                    + "`action_id` " + autoIncrementType + ","
                    + "`action` VARCHAR(25) NOT NULL"
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`action_id`)" : "")
                    + ((this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) ? ", UNIQUE KEY `" + this.targetPrefix + "action_idx` (`action`)" : ", CONSTRAINT `" + this.targetPrefix + "uq_action` UNIQUE (`action`)")
                    + ")" + engineClause + ";");

            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "data` ("
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
                    + "`old_block_subid` " + blockIdDataType + " DEFAULT NULL"
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`id`)" : "")
                    + ")" + engineClause + ";");

            if (this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) {
                queries.add("ALTER TABLE `" + this.targetPrefix + "data` ADD KEY `" + this.targetPrefix + "epoch_idx` (`epoch`), ADD KEY `" + this.targetPrefix + "location_idx` (`world_id`, `x`, `z`, `y`, `action_id`);");
            } else if (this.migrationTargetDbType.equals("sqlite")) {
                queries.add("CREATE INDEX IF NOT EXISTS `" + this.targetPrefix + "data_epoch_idx` ON `" + this.targetPrefix + "data` (`epoch`);");
                queries.add("CREATE INDEX IF NOT EXISTS `" + this.targetPrefix + "data_location_idx` ON `" + this.targetPrefix + "data` (`world_id`, `x`, `z`, `y`, `action_id`);");
                queries.add("CREATE INDEX IF NOT EXISTS `" + this.targetPrefix + "data_action_player_idx` ON `" + this.targetPrefix + "data` (`action_id`, `player_id`);");
            }


            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "data_extra` ("
                    + "`extra_id` " + autoIncrementType + ","
                    + "`data_id` " + intType + " NOT NULL,"
                    + "`data` " + textType + " NULL,"
                    + "`te_data` " + textType + " NULL"
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`extra_id`)" : "")
                    + ")" + engineClause + ";");

            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "meta` ("
                    + "`id` " + autoIncrementType + ","
                    + "`k` VARCHAR(50) NOT NULL,"
                    + "`v` VARCHAR(255) NOT NULL"
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`id`)" : "")
                    + ", CONSTRAINT `" + this.targetPrefix + "uq_meta_k` UNIQUE (`k`)"
                    + ")" + engineClause + ";");

            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "players` ("
                    + "`player_id` " + autoIncrementType + ","
                    + "`player` VARCHAR(255) NOT NULL,"
                    + "`player_uuid` " + uuidType
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`player_id`)" : "")
                    + ((this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) ?
                    ", UNIQUE KEY `" + this.targetPrefix + "player_name_idx` (`player`), UNIQUE KEY `" + this.targetPrefix + "player_uuid_idx` (`player_uuid`)" :
                    ", CONSTRAINT `" + this.targetPrefix + "uq_player_player` UNIQUE (`player`), CONSTRAINT `" + this.targetPrefix + "uq_player_uuid` UNIQUE (`player_uuid`)")
                    + ")" + engineClause + ";");

            queries.add("CREATE TABLE IF NOT EXISTS `" + this.targetPrefix + "worlds` ("
                    + "`world_id` " + autoIncrementType + ","
                    + "`world` VARCHAR(255) NOT NULL"
                    + (!this.migrationTargetDbType.equals("sqlite") ? ", PRIMARY KEY (`world_id`)" : "")
                    + ((this.migrationTargetDbType.equals("mysql") || this.migrationTargetDbType.equals("mariadb")) ? ", UNIQUE KEY `" + this.targetPrefix + "world_name_idx` (`world`)" : ", CONSTRAINT `" + this.targetPrefix + "uq_world_world` UNIQUE (`world`)")
                    + ")" + engineClause + ";");

            for (String query : queries) {
                stmt.executeUpdate(query);
            }
        }
        String successMsg = "[Prism Migration] Schema setup in target database completed.";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + successMsg);
        Prism.log(Level.INFO,successMsg);
    }

    @FunctionalInterface
    interface PsFiller {
        void fill(PreparedStatement ps, ResultSet rs) throws SQLException;
    }

    private void migrateSimpleTable(CommandSender sender, Connection sourceConn, Connection targetConn,
                                    String tableName, String sourceIdColumn, String sourceValueColumn,
                                    Map<Integer, Integer> idMap, PsFiller filler) throws SQLException {
        String logPrefix = "[Prism Migration][" + tableName + "] ";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + logPrefix + "Starting migration...");
        Prism.log(Level.INFO, logPrefix + "Starting migration...");


        String sourceFullTableName = this.sourcePrefix + tableName;
        String targetFullTableName = this.targetPrefix + tableName;

        String selectSql = "SELECT * FROM `" + sourceFullTableName + "`";
        String insertSql;

        if (tableName.equals("meta")) {
            insertSql = "INSERT INTO `" + targetFullTableName + "` (k, v) VALUES (?, ?)";
        } else {
            insertSql = "INSERT INTO `" + targetFullTableName + "` (" + sourceValueColumn + ") VALUES (?)";
        }

        long rowCount = 0;
        final int BATCH_SIZE = 1000;
        List<Integer> currentBatchSourceIds = mapIdColumn(idMap) ? new ArrayList<>() : null;


        try (PreparedStatement sourcePs = sourceConn.prepareStatement(selectSql);
             ResultSet rs = sourcePs.executeQuery();
             PreparedStatement targetPs = targetConn.prepareStatement(insertSql, mapIdColumn(idMap) ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {

            while (rs.next()) {
                if (mapIdColumn(idMap) && currentBatchSourceIds != null) {
                    currentBatchSourceIds.add(rs.getInt(sourceIdColumn));
                }
                filler.fill(targetPs, rs);
                targetPs.addBatch();
                rowCount++;

                if (rowCount % BATCH_SIZE == 0) {
                    targetPs.executeBatch();
                    if (mapIdColumn(idMap) && currentBatchSourceIds != null) {
                        mapGeneratedKeys(targetPs, currentBatchSourceIds, idMap);
                        currentBatchSourceIds.clear();
                    }
                    targetConn.commit();
                    Prism.log(Level.INFO, logPrefix + "Migrated " + rowCount + " rows...");
                }
            }
            if (rowCount % BATCH_SIZE != 0 && rowCount > 0) {
                targetPs.executeBatch();
                if (mapIdColumn(idMap) && currentBatchSourceIds != null && !currentBatchSourceIds.isEmpty()) {
                    mapGeneratedKeys(targetPs, currentBatchSourceIds, idMap);
                }
            }
            targetConn.commit();
        }
        String successMsg = logPrefix + "Migration completed. Total rows: " + rowCount;
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + successMsg);
        Prism.log(Level.INFO, successMsg + (mapIdColumn(idMap) ? ". Mapped IDs: " + idMap.size() : ""));
    }

    private boolean mapIdColumn(Map<Integer, Integer> idMap){
        return idMap != null && (idMap == actionIdMap || idMap == worldIdMap || idMap == playerIdMap);
    }

    private void mapGeneratedKeys(PreparedStatement targetPs, List<Integer> sourceIds, Map<Integer, Integer> idMap) throws SQLException {
        if (sourceIds == null || sourceIds.isEmpty()) return;
        try (ResultSet generatedKeys = targetPs.getGeneratedKeys()) {
            int i = 0;
            while (generatedKeys.next() && i < sourceIds.size()) {
                idMap.put(sourceIds.get(i), generatedKeys.getInt(1));
                i++;
            }
        }
    }

    private void migratePlayersTable(CommandSender sender, Connection sourceConn, Connection targetConn) throws SQLException {
        String logPrefix = "[Prism Migration][players] ";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + logPrefix + "Starting migration...");
        Prism.log(Level.INFO, logPrefix + "Starting migration...");


        String sourceFullTableName = this.sourcePrefix + "players";
        String targetFullTableName = this.targetPrefix + "players";

        String selectSql = "SELECT player_id, player, player_uuid FROM `" + sourceFullTableName + "`";
        String insertSql = "INSERT INTO `" + targetFullTableName + "` (player, player_uuid) VALUES (?, ?)";

        long rowCount = 0;
        final int BATCH_SIZE = 1000;
        List<Integer> currentBatchSourceIds = new ArrayList<>();

        try (PreparedStatement sourcePs = sourceConn.prepareStatement(selectSql);
             ResultSet rs = sourcePs.executeQuery();
             PreparedStatement targetPs = targetConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

            while (rs.next()) {
                currentBatchSourceIds.add(rs.getInt("player_id"));
                targetPs.setString(1, rs.getString("player"));

                byte[] uuidBytes = rs.getBytes("player_uuid");
                targetPs.setBytes(2, uuidBytes);

                targetPs.addBatch();
                rowCount++;

                if (rowCount % BATCH_SIZE == 0) {
                    targetPs.executeBatch();
                    mapGeneratedKeys(targetPs, currentBatchSourceIds, playerIdMap);
                    currentBatchSourceIds.clear();
                    targetConn.commit();
                    Prism.log(Level.INFO, logPrefix + "Migrated " + rowCount + " rows...");
                }
            }
            if (rowCount % BATCH_SIZE != 0 && rowCount > 0) {
                targetPs.executeBatch();
                mapGeneratedKeys(targetPs, currentBatchSourceIds, playerIdMap);
            }
            targetConn.commit();
        }
        rebuildPlayerIdMapByUUID(sourceConn, targetConn, sourceFullTableName, targetFullTableName);
        String successMsg = logPrefix + "Migration completed. Total rows: " + rowCount + ". Mapped IDs: " + playerIdMap.size();
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + successMsg);
        Prism.log(Level.INFO,successMsg);
    }

    private void rebuildPlayerIdMapByUUID(Connection sourceConn, Connection targetConn, String sourceTable, String targetTable) throws SQLException {
        Map<ByteBuffer, Integer> uuidToSourcePlayerId = new HashMap<>();
        try (Statement stmt = sourceConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_id, player_uuid FROM `" + sourceTable + "`")) {
            while (rs.next()) {
                byte[] uuid = rs.getBytes("player_uuid");
                if (uuid != null) {
                    uuidToSourcePlayerId.put(ByteBuffer.wrap(uuid), rs.getInt("player_id"));
                }
            }
        }

        playerIdMap.clear();
        try (Statement stmt = targetConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT player_id, player_uuid FROM `" + targetTable + "`")) {
            while (rs.next()) {
                byte[] uuid = rs.getBytes("player_uuid");
                if (uuid != null) {
                    Integer sourcePlayerId = uuidToSourcePlayerId.get(ByteBuffer.wrap(uuid));
                    if (sourcePlayerId != null) {
                        playerIdMap.put(sourcePlayerId, rs.getInt("player_id"));
                    }
                }
            }
        }
        Prism.log(Level.INFO, "[Prism Migration][players] Player ID map rebuilt by UUID. Final mapped count: " + playerIdMap.size());
    }


    private void migrateDataTable(CommandSender sender, Connection sourceConn, Connection targetConn) throws SQLException {
        String logPrefix = "[Prism Migration][data] ";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + logPrefix + "Starting migration... This may take a very long time.");
        Prism.log(Level.INFO, logPrefix + "Starting migration...");


        String sourceFullTableName = this.sourcePrefix + "data";
        String targetFullTableName = this.targetPrefix + "data";

        long totalRows = 0;
        try (Statement stmt = sourceConn.createStatement(); ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM `" + sourceFullTableName + "`")) {
            if (rsCount.next()) totalRows = rsCount.getLong(1);
        }
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.YELLOW + logPrefix + "Total rows to migrate: " + totalRows);
        Prism.log(Level.INFO, logPrefix + "Total rows to migrate: " + totalRows);

        if (totalRows == 0) {
            String noDataMsg = logPrefix + "No data to migrate.";
            if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + noDataMsg);
            Prism.log(Level.INFO, noDataMsg);
            return;
        }

        String selectSqlBase = "SELECT id, epoch, action_id, player_id, world_id, x, y, z, block_id, block_subid, old_block_id, old_block_subid FROM `" + sourceFullTableName + "`";
        String insertSql = "INSERT INTO `" + targetFullTableName + "` (epoch, action_id, player_id, world_id, x, y, z, block_id, block_subid, old_block_id, old_block_subid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        long migratedRowCount = 0;
        final int BATCH_SIZE = 1000;
        int lastProcessedSourceId = 0;
        boolean moreData = true;

        while(moreData) {
            List<Integer> currentBatchSourceDataIds = new ArrayList<>();
            String currentSelectSql = selectSqlBase + " WHERE id > " + lastProcessedSourceId + " ORDER BY id LIMIT " + BATCH_SIZE;

            try (PreparedStatement sourcePs = sourceConn.prepareStatement(currentSelectSql);
                 ResultSet rs = sourcePs.executeQuery();
                 PreparedStatement targetPs = targetConn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {

                int currentBatchActualSize = 0;
                while (rs.next()) {
                    currentBatchActualSize++;
                    int sourceDataId = rs.getInt("id");
                    lastProcessedSourceId = sourceDataId;
                    currentBatchSourceDataIds.add(sourceDataId);

                    targetPs.setLong(1, rs.getLong("epoch"));
                    targetPs.setInt(2, actionIdMap.getOrDefault(rs.getInt("action_id"), 0));
                    targetPs.setInt(3, playerIdMap.getOrDefault(rs.getInt("player_id"), 0));
                    targetPs.setInt(4, worldIdMap.getOrDefault(rs.getInt("world_id"), 0));
                    targetPs.setInt(5, rs.getInt("x"));
                    targetPs.setInt(6, rs.getInt("y"));
                    targetPs.setInt(7, rs.getInt("z"));
                    targetPs.setInt(8, rs.getInt("block_id"));
                    targetPs.setInt(9, rs.getInt("block_subid"));
                    targetPs.setInt(10, rs.getInt("old_block_id"));
                    targetPs.setInt(11, rs.getInt("old_block_subid"));
                    targetPs.addBatch();
                }

                if (currentBatchActualSize > 0) {
                    targetPs.executeBatch();
                    mapGeneratedKeys(targetPs, currentBatchSourceDataIds, dataIdMap);
                    targetConn.commit();
                    migratedRowCount += currentBatchActualSize;
                    Prism.log(Level.INFO, logPrefix + "Migrated " + migratedRowCount + "/" + totalRows + " rows (" + String.format("%.2f", (double)migratedRowCount/totalRows*100) + "%)...");
                } else {
                    moreData = false;
                }
            }
        }
        String successMsg = logPrefix + "Migration completed. Total rows: " + migratedRowCount;
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + successMsg);
        Prism.log(Level.INFO, successMsg + ". Mapped data IDs: " + dataIdMap.size());
    }

    private void migrateDataExtraTable(CommandSender sender, Connection sourceConn, Connection targetConn) throws SQLException {
        String logPrefix = "[Prism Migration][data_extra] ";
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.AQUA + logPrefix + "Starting migration...");
        Prism.log(Level.INFO, logPrefix + "Starting migration...");


        String sourceFullTableName = this.sourcePrefix + "data_extra";
        String targetFullTableName = this.targetPrefix + "data_extra";

        long totalRows = 0;
        try (Statement stmt = sourceConn.createStatement(); ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM `" + sourceFullTableName + "`")) {
            if (rsCount.next()) totalRows = rsCount.getLong(1);
        }
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.YELLOW + logPrefix + "Total rows to migrate: " + totalRows);
        Prism.log(Level.INFO, logPrefix + "Total rows to migrate: " + totalRows);

        if (totalRows == 0) {
            String noDataMsg = logPrefix + "No data to migrate.";
            if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + noDataMsg);
            Prism.log(Level.INFO, noDataMsg);
            return;
        }


        String selectSqlBase = "SELECT extra_id, data_id, data, te_data FROM `" + sourceFullTableName + "`";
        String insertSql = "INSERT INTO `" + targetFullTableName + "` (data_id, data, te_data) VALUES (?, ?, ?)";

        long migratedRowCount = 0;
        final int BATCH_SIZE = 1000;
        int lastProcessedSourceExtraId = 0;
        boolean moreData = true;

        while(moreData){
            String currentSelectSql = selectSqlBase + " WHERE extra_id > " + lastProcessedSourceExtraId + " ORDER BY extra_id LIMIT " + BATCH_SIZE;

            try (PreparedStatement sourcePs = sourceConn.prepareStatement(currentSelectSql);
                 ResultSet rs = sourcePs.executeQuery();
                 PreparedStatement targetPs = targetConn.prepareStatement(insertSql)) {

                int currentBatchActualSize = 0;
                while (rs.next()) {
                    currentBatchActualSize++;
                    lastProcessedSourceExtraId = rs.getInt("extra_id");
                    int sourceDataId = rs.getInt("data_id");
                    Integer targetDataId = dataIdMap.get(sourceDataId);

                    if (targetDataId == null) {
                        Prism.log(Level.WARNING, logPrefix + "Skipping data_extra record for source data_id " + sourceDataId + " as it has no mapping in target DB (source extra_id: " + lastProcessedSourceExtraId +").");
                        continue;
                    }

                    targetPs.setInt(1, targetDataId);
                    targetPs.setString(2, rs.getString("data"));
                    targetPs.setString(3, rs.getString("te_data"));
                    targetPs.addBatch();
                }

                if (currentBatchActualSize > 0) {
                    targetPs.executeBatch();
                    targetConn.commit();
                    migratedRowCount += currentBatchActualSize;
                    Prism.log(Level.INFO, logPrefix + "Migrated " + migratedRowCount + "/" + totalRows + " rows (" + String.format("%.2f", (double)migratedRowCount/totalRows*100) + "%)...");
                } else {
                    moreData = false;
                }
            }
        }
        String successMsg = logPrefix + "Migration completed. Total rows: " + migratedRowCount;
        if(sender != null && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) sender.sendMessage(ChatColor.GREEN + successMsg);
        Prism.log(Level.INFO, successMsg);
    }


    private void clearMappings() {
        actionIdMap.clear();
        worldIdMap.clear();
        playerIdMap.clear();
        dataIdMap.clear();
        Prism.log(Level.INFO, "[Prism Migration] ID maps cleared.");
    }
}
