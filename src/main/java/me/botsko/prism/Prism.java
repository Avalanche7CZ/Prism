package me.botsko.prism;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import me.botsko.elixr.MaterialAliases;
import me.botsko.prism.actionlibs.*;
import me.botsko.prism.configs.MonitoredItemsConfig;
import me.botsko.prism.configs.PrismConfig;
import me.botsko.prism.parameters.PrismParameterHandler;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.bridge.PrismBlockEditSessionFactory;
import me.botsko.prism.commands.PrismCommands;
import me.botsko.prism.commands.WhatCommand;
import me.botsko.prism.database.PrismDatabaseHandler;
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
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Prism extends JavaPlugin {

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
    public static MonitoredItemsConfig monitoredItemsConfig;
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

    @SuppressWarnings("rawtypes")
    @Override
    public void onEnable() {
        plugin_name = this.getDescription().getName();
        plugin_version = this.getDescription().getVersion();
        prism = this;
        log("==================================================");
        log(" _____   _____   _____   _____  __  __ ");
        log("|  __ \\ |  __ \\ |_   _| / ____||  \\/  |");
        log("| |__) || |__) |  | |  | (___  | \\  / |");
        log("|  ___/ |  _  /   | |   \\___ \\ | |\\/| |");
        log("| |     | | \\ \\  _| |_  ____) || |  | |");
        log("|_|     |_|  \\_\\|_____||_____/ |_|  |_|");
        log(" ");
        log("Initializing " + plugin_name + " v" + plugin_version);
        log("Original by: Viveleroi");
        log("Modified version by: Avalanche7CZ, EverNife");
        log("==================================================");
        loadAllConfigs();

        if (config.getBoolean("prism.allow-metrics")) {
            try {
                final Metrics metrics = new Metrics(this);
                metrics.start();
            } catch (final IOException e) {
                log("MCStats submission failed: " + e.getMessage());
            }
        }

        PrismDatabaseHandler.initialize(this);
        final Connection test_conn = PrismDatabaseHandler.dbc();
        if (PrismDatabaseHandler.getPool() == null || test_conn == null) {
            final String[] dbDisabled = new String[3];
            dbDisabled[0] = "Prism will disable itself because it couldn't connect to a database.";
            dbDisabled[1] = "If you're using MySQL/MariaDB, check your config. Be sure your database server is running.";
            dbDisabled[2] = "For help - try https://github.com/Avalanche7CZ/Prism/wiki";
            logSection(dbDisabled);
            disablePlugin();
            return;
        }
        if (test_conn != null) {
            try {
                test_conn.close();
            } catch (final SQLException e) {
                PrismDatabaseHandler.handleDatabaseException(e);
            }
        }

        if (isEnabled()) {
            handlerRegistry = new HandlerRegistry();
            actionRegistry = new ActionRegistry();
            PrismDatabaseHandler.setupDatabase();
            PrismDatabaseHandler.cacheWorldPrimaryKeys();
            PlayerIdentification.cacheOnlinePlayerPrimaryKeys();

            for (final World w : getServer().getWorlds()) {
                if (!Prism.prismWorlds.containsKey(w.getName())) {
                    PrismDatabaseHandler.addWorldName(w.getName());
                }
            }

            final Updater up = new Updater(this);
            up.apply_updates();

            eventTimer = new TimeTaken(this);
            queueStats = new QueueStats();
            ignore = new Ignore(this);
            checkPluginDependancies();

            getServer().getPluginManager().registerEvents(new PrismBlockEvents(this), this);
            getServer().getPluginManager().registerEvents(new PrismEntityEvents(this), this);
            getServer().getPluginManager().registerEvents(new PrismWorldEvents(), this);
            getServer().getPluginManager().registerEvents(new PrismPlayerEvents(this), this);
            getServer().getPluginManager().registerEvents(new PrismInventoryEvents(this), this);
            getServer().getPluginManager().registerEvents(new PrismVehicleEvents(this), this);

            if (config.getBoolean("prism.track-hopper-item-events") && Prism.getIgnore().event("item-insert")) {
                getServer().getPluginManager().registerEvents(new PrismInventoryMoveItemEvent(), this);
            }

            if (config.getBoolean("prism.tracking.api.enabled")) {
                getServer().getPluginManager().registerEvents(new PrismCustomEvents(this), this);
            }
            getServer().getPluginManager().registerEvents(new PrismMiscEvents(), this);

            getCommand("prism").setExecutor(new PrismCommands(this));
            getCommand("prism").setTabCompleter(new PrismCommands(this));
            getCommand("what").setExecutor(new WhatCommand(this));

            registerParameter(new ActionParameter());
            registerParameter(new BeforeParameter());
            registerParameter(new BlockParameter());
            registerParameter(new EntityParameter());
            registerParameter(new FlagParameter());
            registerParameter(new IdParameter());
            registerParameter(new KeywordParameter());
            registerParameter(new PlayerParameter());
            registerParameter(new RadiusParameter());
            registerParameter(new SinceParameter());
            registerParameter(new WorldParameter());

            messenger = new Messenger(plugin_name);
            actionsQuery = new ActionsQuery(this);
            oreMonitor = new OreMonitor(this);
            useMonitor = new UseMonitor(this);

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
    public void loadAllConfigs(){
        final PrismConfig mc = new PrismConfig(this);
        config = mc.getConfig();

        monitoredItemsConfig = new MonitoredItemsConfig(this);

        illegalBlocks = (ArrayList<Integer>) config.getList("prism.appliers.never-place-block");
        illegalEntities = (ArrayList<String>) config.getList("prism.appliers.never-spawn-entity");

        final ConfigurationSection alertBlocks = config.getConfigurationSection("prism.alerts.ores.blocks");
        alertedOres.clear();
        if (alertBlocks != null) {
            for (final String key : alertBlocks.getKeys(false)) {
                alertedOres.put(key, alertBlocks.getString(key));
            }
        }
        items = new MaterialAliases();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        loadAllConfigs();

        PrismDatabaseHandler.loadDbConfigFields();
        PrismDatabaseHandler.configureDbPool();

        log("Prism configurations reloaded.");
    }

    public static MonitoredItemsConfig getMonitoredItemsConfig() {
        return monitoredItemsConfig;
    }

    public Language getLang() {
        return this.language;
    }

    public void checkPluginDependancies() {
        final Plugin we = getServer().getPluginManager().getPlugin("WorldEdit");
        if (we != null && we.isEnabled()) {
            plugin_worldEdit = (WorldEditPlugin) we;
            try {
                PrismBlockEditSessionFactory.initialize();
                log("WorldEdit found and enabled. Associated features enabled.");
                enabledPlugins.add("WorldEdit");
            } catch (Exception e) {
                log(Level.WARNING, "WorldEdit found, but failed to initialize PrismBlockEditSessionFactory: " + e.getMessage());
                plugin_worldEdit = null;
            }
        } else {
            log("WorldEdit not found or not enabled. Certain optional features of Prism disabled.");
            plugin_worldEdit = null;
        }
    }

    public boolean dependencyEnabled(String pluginName) {
        return enabledPlugins.contains(pluginName);
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
            paramHandlers.put(handler.getName().toLowerCase(), handler);
        } else {
            log(Level.WARNING, "Attempted to register a null parameter handler or handler with null name.");
        }
    }

    public static HashMap<String, PrismParameterHandler> getParameters() {
        return paramHandlers;
    }

    public static PrismParameterHandler getParameter(String name) {
        return (name != null) ? paramHandlers.get(name.toLowerCase()) : null;
    }

    public void endExpiredQueryCaches() {
        long repeatTicks = config.getLong("prism.cache.query.expire-check-ticks", 2400L);
        long expirySeconds = config.getLong("prism.cache.query.expiry-seconds", 120L);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                cachedQueries.entrySet().removeIf(entry -> {
                    final QueryResult result = entry.getValue();
                    final long diffSeconds = (currentTime - result.getQueryTime()) / 1000;
                    if (diffSeconds >= expirySeconds) {
                        debug("Expired query cache for key: " + entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks);
    }

    public void endExpiredPreviews() {
        long repeatTicks = config.getLong("prism.cache.preview.expire-check-ticks", 1200L);
        long expirySeconds = config.getLong("prism.cache.preview.expiry-seconds", 60L);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                playerActivePreviews.entrySet().removeIf(entry -> {
                    final PreviewSession session = entry.getValue();
                    final long diffSeconds = (currentTime - session.getQueryTime()) / 1000;
                    if (diffSeconds >= expirySeconds) {
                        final Player player = Bukkit.getServer().getPlayerExact(session.getPlayer().getName());
                        if (player != null && player.isOnline()) {
                            player.sendMessage(Prism.messenger.playerHeaderMsg("Canceling forgotten preview."));
                        }
                        debug("Expired preview for player: " + session.getPlayer().getName());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks);
    }

    public void removeExpiredLocations() {
        long repeatTicks = config.getLong("prism.cache.alerted-locations.expire-check-ticks", 1200L);
        long expirySeconds = config.getLong("prism.cache.alerted-locations.expiry-seconds", 300L);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                final long currentTime = System.currentTimeMillis();
                alertedBlocks.entrySet().removeIf(entry -> {
                    final long diffSeconds = (currentTime - entry.getValue()) / 1000;
                    if (diffSeconds >= expirySeconds) {
                        debug("Expired alerted location: " + entry.getKey().toString());
                        return true;
                    }
                    return false;
                });
            }
        }, repeatTicks, repeatTicks);
    }

    public void actionRecorderTask() {
        int recorder_tick_delay = config.getInt("prism.queue-empty-tick-delay", 3);
        if (recorder_tick_delay < 1) {
            recorder_tick_delay = 3;
        }
        recordingTask = getServer().getScheduler().runTaskLaterAsynchronously(this, new RecordingTask(this), recorder_tick_delay);
    }

    public void launchScheduledPurgeManager() {
        if (!config.getBoolean("prism.purge.enabled", false)) {
            log("Database purging is disabled in config.");
            return;
        }
        final List<String> purgeRules = config.getStringList("prism.db-records-purge-rules");
        if (purgeRules == null || purgeRules.isEmpty()) {
            log("Database purging enabled, but no purge rules defined. Purge manager will not run effectively.");
        }
        purgeManager = new PurgeManager(this, purgeRules);
        long initialDelayHours = config.getLong("prism.purge.initial-delay-hours", 1);
        long intervalHours = config.getLong("prism.purge.interval-hours", 12);

        if (intervalHours <= 0) {
            log(Level.WARNING, "Purge interval hours must be positive. Purge manager not scheduled.");
            return;
        }

        schedulePool.scheduleAtFixedRate(purgeManager, initialDelayHours, intervalHours, TimeUnit.HOURS);
        log("Scheduled purge manager to run every " + intervalHours + " hours, starting in " + initialDelayHours + " hour(s).");
    }

    public void launchInternalAffairs() {
        long intervalMinutes = config.getLong("prism.internal-affairs.interval-minutes", 5);
        if (intervalMinutes <= 0) {
            log(Level.WARNING, "Internal affairs task interval must be positive. Task not scheduled.");
            return;
        }
        final InternalAffairs recordingMonitor = new InternalAffairs(this);
        recordingMonitorTask.scheduleAtFixedRate(recordingMonitor, 0, intervalMinutes, TimeUnit.MINUTES);
        log("Internal affairs task (queue monitoring, etc.) scheduled every " + intervalMinutes + " minutes.");
    }

    public void alertPlayers(Player eventPlayer, String msg) {
        String formattedMsg = messenger.playerMsg(ChatColor.RED + "[!] " + msg);
        for (final Player p : getServer().getOnlinePlayers()) {
            if ((eventPlayer == null || !p.equals(eventPlayer)) || config.getBoolean("prism.alerts.alert-player-about-self", false)) {
                if (p.hasPermission("prism.alerts")) {
                    p.sendMessage(formattedMsg);
                }
            }
        }
    }

    public String msgMissingArguments() {
        return messenger.playerError("Missing arguments. Check /prism ? for help.");
    }

    public String msgInvalidArguments() {
        return messenger.playerError("Invalid arguments. Check /prism ? for help.");
    }

    public String msgInvalidSubcommand() {
        return messenger.playerError("Prism doesn't have that command. Check /prism ? for help.");
    }

    public String msgNoPermission() {
        return messenger.playerError("You don't have permission to perform this action.");
    }

    public void notifyNearby(Player centralPlayer, int radius, String msg) {
        if (!config.getBoolean("prism.appliers.notify-nearby.enabled")) {
            return;
        }
        if (centralPlayer == null || !centralPlayer.isOnline()) return;

        String formattedMsg = messenger.playerHeaderMsg(msg);
        int notifyRadius = radius + config.getInt("prism.appliers.notify-nearby.additional-radius", 0);
        if (notifyRadius <= 0 && radius <= 0) return;

        for (final Player p : centralPlayer.getWorld().getPlayers()) {
            if (!p.equals(centralPlayer)) {
                if (centralPlayer.getLocation().distanceSquared(p.getLocation()) <= ((long) notifyRadius * notifyRadius)) {
                    p.sendMessage(formattedMsg);
                }
            }
        }
    }

    public static void log(String message) {
        log(Level.INFO, message);
    }

    public static void log(Level level, String message) {
        if (plugin_name == null) plugin_name = "Prism";
        if (Prism.log != null) {
            Prism.log.log(level, "[" + plugin_name + "]: " + message);
        } else {
            System.out.println("[" + plugin_name + "] (" + level.getName() + "): " + message);
        }
    }

    public static void logSection(String[] messages) {
        if (messages != null && messages.length > 0) {
            log("--------------------- ## Important ## ---------------------");
            for (final String msg : messages) {
                if (msg != null) log(msg);
            }
            log("--------------------- ## ========= ## ---------------------");
        }
    }

    public static void debug(String message) {
        if (config != null && config.getBoolean("prism.debug", false)) {
            log(Level.INFO, "[Debug]: " + message);
        }
    }

    public static void debug(Location loc) {
        if (loc != null) {
            debug("Location: W:" + (loc.getWorld() != null ? loc.getWorld().getName() : "null") +
                    " X:" + loc.getBlockX() + " Y:" + loc.getBlockY() + " Z:" + loc.getBlockZ());
        } else {
            debug("Location: null");
        }
    }

    public void disablePlugin() {
        log(Level.WARNING, "Disabling Prism plugin...");
        this.setEnabled(false);
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

        if (config != null && config.getBoolean("prism.database.force-write-queue-on-shutdown", true)) {
            log("Attempting to force drain action queue on shutdown...");
            final QueueDrain drainer = new QueueDrain(this);
            drainer.forceDrainQueue();
        }

        if (PrismDatabaseHandler.getPool() != null) {
            log("Closing database connection pool...");
            PrismDatabaseHandler.getPool().close(true);
            log("Database connection pool closed.");
        }

        if (prismActions != null) prismActions.clear();
        if (prismWorlds != null) prismWorlds.clear();
        if (prismPlayers != null) prismPlayers.clear();
        if (playersWithActiveTools != null) playersWithActiveTools.clear();

        log("Prism plugin fully disabled.");
    }
}