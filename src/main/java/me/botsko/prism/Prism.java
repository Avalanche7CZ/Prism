package me.botsko.prism;

import com.helion3.prism.libs.elixr.MaterialAliases;
import com.helion3.prism.libs.org.apache.tomcat.jdbc.pool.DataSource;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import me.botsko.prism.actionlibs.ActionRegistry;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.HandlerRegistry;
import me.botsko.prism.actionlibs.Ignore;
import me.botsko.prism.actionlibs.InternalAffairs;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actionlibs.QueueDrain;
import me.botsko.prism.actionlibs.RecordingTask;
import me.botsko.prism.appliers.PreviewSession;
import me.botsko.prism.bridge.PrismBlockEditHandler;
import me.botsko.prism.commands.PrismCommands;
import me.botsko.prism.commands.WhatCommand;
import me.botsko.prism.listeners.PrismBlockEvents;
import me.botsko.prism.listeners.PrismCustomEvents;
import me.botsko.prism.listeners.PrismEntityEvents;
import me.botsko.prism.listeners.PrismInventoryEvents;
import me.botsko.prism.listeners.PrismInventoryMoveItemEvent;
import me.botsko.prism.listeners.PrismPlayerEvents;
import me.botsko.prism.listeners.PrismVehicleEvents;
import me.botsko.prism.listeners.PrismWorldEvents;
import me.botsko.prism.listeners.self.PrismMiscEvents;
import me.botsko.prism.measurement.Metrics;
import me.botsko.prism.measurement.QueueStats;
import me.botsko.prism.measurement.TimeTaken;
import me.botsko.prism.monitors.OreMonitor;
import me.botsko.prism.monitors.UseMonitor;
import me.botsko.prism.parameters.ActionParameter;
import me.botsko.prism.parameters.BeforeParameter;
import me.botsko.prism.parameters.BlockParameter;
import me.botsko.prism.parameters.EntityParameter;
import me.botsko.prism.parameters.FlagParameter;
import me.botsko.prism.parameters.IdParameter;
import me.botsko.prism.parameters.KeywordParameter;
import me.botsko.prism.parameters.PlayerParameter;
import me.botsko.prism.parameters.PrismParameterHandler;
import me.botsko.prism.parameters.RadiusParameter;
import me.botsko.prism.parameters.SinceParameter;
import me.botsko.prism.parameters.WorldParameter;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.purge.PurgeManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Prism extends JavaPlugin {
   private static DataSource pool = new DataSource();
   private static String plugin_name;
   private String plugin_version;
   private static MaterialAliases items;
   private Language language;
   private static Logger log = Logger.getLogger("Minecraft");
   private final ArrayList enabledPlugins = new ArrayList();
   private static ActionRegistry actionRegistry;
   private static HandlerRegistry handlerRegistry;
   private static Ignore ignore;
   protected static ArrayList illegalBlocks;
   protected static ArrayList illegalEntities;
   protected static HashMap alertedOres = new HashMap();
   private static HashMap paramHandlers = new HashMap();
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
   public static ConcurrentHashMap playersWithActiveTools = new ConcurrentHashMap();
   public ConcurrentHashMap playerActivePreviews = new ConcurrentHashMap();
   public ConcurrentHashMap playerActiveViews = new ConcurrentHashMap();
   public ConcurrentHashMap cachedQueries = new ConcurrentHashMap();
   public ConcurrentHashMap alertedBlocks = new ConcurrentHashMap();
   public TimeTaken eventTimer;
   public QueueStats queueStats;
   public BukkitTask recordingTask;
   public int total_records_affected = 0;
   public static HashMap prismWorlds = new HashMap();
   public static HashMap prismPlayers = new HashMap();
   public static HashMap prismActions = new HashMap();
   public ConcurrentHashMap preplannedBlockFalls = new ConcurrentHashMap();
   public ConcurrentHashMap preplannedVehiclePlacement = new ConcurrentHashMap();

   public void onEnable() {
      plugin_name = this.getDescription().getName();
      this.plugin_version = this.getDescription().getVersion();
      this.prism = this;
      log("Initializing Prism " + this.plugin_version + ". By Viveleroi.");
      this.loadConfig();
      if (this.getConfig().getBoolean("prism.allow-metrics")) {
         try {
            Metrics metrics = new Metrics(this);
            metrics.start();
         } catch (IOException var5) {
            log("MCStats submission failed.");
         }
      }

      pool = this.initDbPool();
      Connection test_conn = dbc();
      if (pool == null || test_conn == null) {
         String[] dbDisabled = new String[]{"Prism will disable itself because it couldn't connect to a database.", "If you're using MySQL, check your config. Be sure MySQL is running.", "For help - try http://discover-prism.com/wiki/view/troubleshooting/"};
         logSection(dbDisabled);
         this.disablePlugin();
      }

      if (test_conn != null) {
         try {
            test_conn.close();
         } catch (SQLException var4) {
            this.handleDatabaseException(var4);
         }
      }

      if (this.isEnabled()) {
         handlerRegistry = new HandlerRegistry();
         actionRegistry = new ActionRegistry();
         this.setupDatabase();
         this.cacheWorldPrimaryKeys();
         PlayerIdentification.cacheOnlinePlayerPrimaryKeys();
         Iterator i$ = this.getServer().getWorlds().iterator();

         while(i$.hasNext()) {
            World w = (World)i$.next();
            if (!prismWorlds.containsKey(w.getName())) {
               addWorldName(w.getName());
            }
         }

         Updater up = new Updater(this);
         up.apply_updates();
         this.eventTimer = new TimeTaken(this);
         this.queueStats = new QueueStats();
         ignore = new Ignore(this);
         this.checkPluginDependancies();
         this.getServer().getPluginManager().registerEvents(new PrismBlockEvents(this), this);
         this.getServer().getPluginManager().registerEvents(new PrismEntityEvents(this), this);
         this.getServer().getPluginManager().registerEvents(new PrismWorldEvents(), this);
         this.getServer().getPluginManager().registerEvents(new PrismPlayerEvents(this), this);
         this.getServer().getPluginManager().registerEvents(new PrismInventoryEvents(this), this);
         this.getServer().getPluginManager().registerEvents(new PrismVehicleEvents(this), this);
         if (this.getConfig().getBoolean("prism.track-hopper-item-events") && getIgnore().event("item-insert")) {
            this.getServer().getPluginManager().registerEvents(new PrismInventoryMoveItemEvent(), this);
         }

         if (this.getConfig().getBoolean("prism.tracking.api.enabled")) {
            this.getServer().getPluginManager().registerEvents(new PrismCustomEvents(this), this);
         }

         this.getServer().getPluginManager().registerEvents(new PrismMiscEvents(), this);
         this.getCommand("prism").setExecutor(new PrismCommands(this));
         this.getCommand("prism").setTabCompleter(new PrismCommands(this));
         this.getCommand("what").setExecutor(new WhatCommand(this));
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
         this.actionsQuery = new ActionsQuery(this);
         this.oreMonitor = new OreMonitor(this);
         this.useMonitor = new UseMonitor(this);
         this.actionRecorderTask();
         this.endExpiredQueryCaches();
         this.endExpiredPreviews();
         this.removeExpiredLocations();
         this.launchScheduledPurgeManager();
         this.launchInternalAffairs();
      }

   }

   public static String getPrismName() {
      return plugin_name;
   }

   public String getPrismVersion() {
      return this.plugin_version;
   }

   public void loadConfig() {
      PrismConfig mc = new PrismConfig(this);
      config = mc.getConfig();
      illegalBlocks = (ArrayList)this.getConfig().getList("prism.appliers.never-place-block");
      illegalEntities = (ArrayList)this.getConfig().getList("prism.appliers.never-spawn-entity");
      ConfigurationSection alertBlocks = this.getConfig().getConfigurationSection("prism.alerts.ores.blocks");
      alertedOres.clear();
      if (alertBlocks != null) {
         Iterator i$ = alertBlocks.getKeys(false).iterator();

         while(i$.hasNext()) {
            String key = (String)i$.next();
            alertedOres.put(key, alertBlocks.getString(key));
         }
      }

      items = new MaterialAliases();
   }

   public void reloadConfig() {
      super.reloadConfig();
      this.loadConfig();
   }

   public DataSource initDbPool() {
      DataSource pool = null;
      String dns = "jdbc:mysql://" + config.getString("prism.mysql.hostname") + ":" + config.getString("prism.mysql.port") + "/" + config.getString("prism.mysql.database");
      pool = new DataSource();
      pool.setDriverClassName("com.mysql.jdbc.Driver");
      pool.setUrl(dns);
      pool.setUsername(config.getString("prism.mysql.username"));
      pool.setPassword(config.getString("prism.mysql.password"));
      pool.setInitialSize(config.getInt("prism.database.pool-initial-size"));
      pool.setMaxActive(config.getInt("prism.database.max-pool-connections"));
      pool.setMaxIdle(config.getInt("prism.database.max-idle-connections"));
      pool.setMaxWait(config.getInt("prism.database.max-wait"));
      pool.setRemoveAbandoned(true);
      pool.setRemoveAbandonedTimeout(60);
      pool.setTestOnBorrow(true);
      pool.setValidationQuery("/* ping */SELECT 1");
      pool.setValidationInterval(30000L);
      return pool;
   }

   public void rebuildPool() {
      if (pool != null) {
         pool.close();
      }

      pool = this.initDbPool();
   }

   public static DataSource getPool() {
      return pool;
   }

   public static Connection dbc() {
      Connection con = null;

      try {
         con = pool.getConnection();
      } catch (SQLException var2) {
         log("Database connection failed. " + var2.getMessage());
         if (!var2.getMessage().contains("Pool empty")) {
            var2.printStackTrace();
         }
      }

      return con;
   }

   protected boolean attemptToRescueConnection(SQLException e) throws SQLException {
      if (e.getMessage().contains("connection closed")) {
         this.rebuildPool();
         if (pool != null) {
            Connection conn = dbc();
            if (conn != null && !conn.isClosed()) {
               return true;
            }
         }
      }

      return false;
   }

   public void handleDatabaseException(SQLException e) {
      String prefix = config.getString("prism.mysql.prefix");

      try {
         if (this.attemptToRescueConnection(e)) {
            return;
         }
      } catch (SQLException var4) {
      }

      log("Database connection error: " + e.getMessage());
      if (e.getMessage().contains("marked as crashed")) {
         String[] msg = new String[]{"If MySQL crashes during write it may corrupt it's indexes.", "Try running `CHECK TABLE " + prefix + "data` and then `REPAIR TABLE " + prefix + "data`."};
         logSection(msg);
      }

      e.printStackTrace();
   }

   protected void setupDatabase() {
      String prefix = config.getString("prism.mysql.prefix");
      Connection conn = null;
      Statement st = null;

      try {
         conn = dbc();
         if (conn == null) {
            return;
         }

         String query = "CREATE TABLE IF NOT EXISTS `" + prefix + "actions` (" + "`action_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`action` varchar(25) NOT NULL," + "PRIMARY KEY (`action_id`)," + "UNIQUE KEY `action` (`action`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
         st = conn.createStatement();
         st.executeUpdate(query);
         query = "CREATE TABLE IF NOT EXISTS `" + prefix + "data` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`epoch` int(10) unsigned NOT NULL," + "`action_id` int(10) unsigned NOT NULL," + "`player_id` int(10) unsigned NOT NULL," + "`world_id` int(10) unsigned NOT NULL," + "`x` int(11) NOT NULL," + "`y` int(11) NOT NULL," + "`z` int(11) NOT NULL," + "`block_id` mediumint(5) DEFAULT NULL," + "`block_subid` mediumint(5) DEFAULT NULL," + "`old_block_id` mediumint(5) DEFAULT NULL," + "`old_block_subid` mediumint(5) DEFAULT NULL," + "PRIMARY KEY (`id`)," + "KEY `epoch` (`epoch`)," + "KEY  `location` (`world_id`, `x`, `z`, `y`, `action_id`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
         st.executeUpdate(query);
         DatabaseMetaData metadata = conn.getMetaData();
         ResultSet resultSet = metadata.getTables((String)null, (String)null, "" + prefix + "data_extra", (String[])null);
         if (!resultSet.next()) {
            query = "CREATE TABLE IF NOT EXISTS `" + prefix + "data_extra` (" + "`extra_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`data_id` int(10) unsigned NOT NULL," + "`data` text NULL," + "`te_data` text NULL," + "PRIMARY KEY (`extra_id`)," + "KEY `data_id` (`data_id`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
            st.executeUpdate(query);
            query = "ALTER TABLE `" + prefix + "data_extra` ADD CONSTRAINT `" + prefix + "data_extra_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `" + prefix + "data` (`id`) ON DELETE CASCADE ON UPDATE NO ACTION;";
            st.executeUpdate(query);
         }

         query = "CREATE TABLE IF NOT EXISTS `" + prefix + "meta` (" + "`id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`k` varchar(25) NOT NULL," + "`v` varchar(255) NOT NULL," + "PRIMARY KEY (`id`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
         st.executeUpdate(query);
         query = "CREATE TABLE IF NOT EXISTS `" + prefix + "players` (" + "`player_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`player` varchar(255) NOT NULL," + "`player_uuid` binary(16) NOT NULL," + "PRIMARY KEY (`player_id`)," + "UNIQUE KEY `player` (`player`)," + "UNIQUE KEY `player_uuid` (`player_uuid`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
         st.executeUpdate(query);
         query = "CREATE TABLE IF NOT EXISTS `" + prefix + "worlds` (" + "`world_id` int(10) unsigned NOT NULL AUTO_INCREMENT," + "`world` varchar(255) NOT NULL," + "PRIMARY KEY (`world_id`)," + "UNIQUE KEY `world` (`world`)" + ") ENGINE=InnoDB  DEFAULT CHARSET=utf8;";
         st.executeUpdate(query);
         this.cacheActionPrimaryKeys();
         String[] actions = actionRegistry.listAll();
         String[] arr$ = actions;
         int len$ = actions.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String a = arr$[i$];
            addActionName(a);
         }
      } catch (SQLException var26) {
         log("Database connection error: " + var26.getMessage());
         var26.printStackTrace();
      } finally {
         if (st != null) {
            try {
               st.close();
            } catch (SQLException var25) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var24) {
            }
         }

      }

   }

   protected void cacheActionPrimaryKeys() {
      String prefix = config.getString("prism.mysql.prefix");
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = dbc();
         s = conn.prepareStatement("SELECT action_id, action FROM " + prefix + "actions");
         rs = s.executeQuery();

         while(rs.next()) {
            debug("Loaded " + rs.getString(2) + ", id:" + rs.getInt(1));
            prismActions.put(rs.getString(2), rs.getInt(1));
         }

         debug("Loaded " + prismActions.size() + " actions into the cache.");
      } catch (SQLException var22) {
         this.handleDatabaseException(var22);
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var21) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var20) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var19) {
            }
         }

      }

   }

   public static void addActionName(String actionName) {
      String prefix = config.getString("prism.mysql.prefix");
      if (!prismActions.containsKey(actionName)) {
         Connection conn = null;
         PreparedStatement s = null;
         ResultSet rs = null;

         try {
            conn = dbc();
            s = conn.prepareStatement("INSERT INTO " + prefix + "actions (action) VALUES (?)", 1);
            s.setString(1, actionName);
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (!rs.next()) {
               throw new SQLException("Insert statement failed - no generated key obtained.");
            }

            log("Registering new action type to the database/cache: " + actionName + " " + rs.getInt(1));
            prismActions.put(actionName, rs.getInt(1));
         } catch (SQLException var22) {
         } finally {
            if (rs != null) {
               try {
                  rs.close();
               } catch (SQLException var21) {
               }
            }

            if (s != null) {
               try {
                  s.close();
               } catch (SQLException var20) {
               }
            }

            if (conn != null) {
               try {
                  conn.close();
               } catch (SQLException var19) {
               }
            }

         }

      }
   }

   protected void cacheWorldPrimaryKeys() {
      String prefix = config.getString("prism.mysql.prefix");
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = dbc();
         s = conn.prepareStatement("SELECT world_id, world FROM " + prefix + "worlds");
         rs = s.executeQuery();

         while(rs.next()) {
            prismWorlds.put(rs.getString(2), rs.getInt(1));
         }

         debug("Loaded " + prismWorlds.size() + " worlds into the cache.");
      } catch (SQLException var22) {
         this.handleDatabaseException(var22);
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var21) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var20) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var19) {
            }
         }

      }

   }

   public static void addWorldName(String worldName) {
      String prefix = config.getString("prism.mysql.prefix");
      if (!prismWorlds.containsKey(worldName)) {
         Connection conn = null;
         PreparedStatement s = null;
         ResultSet rs = null;

         try {
            conn = dbc();
            s = conn.prepareStatement("INSERT INTO " + prefix + "worlds (world) VALUES (?)", 1);
            s.setString(1, worldName);
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (!rs.next()) {
               throw new SQLException("Insert statement failed - no generated key obtained.");
            }

            log("Registering new world to the database/cache: " + worldName + " " + rs.getInt(1));
            prismWorlds.put(worldName, rs.getInt(1));
         } catch (SQLException var22) {
         } finally {
            if (rs != null) {
               try {
                  rs.close();
               } catch (SQLException var21) {
               }
            }

            if (s != null) {
               try {
                  s.close();
               } catch (SQLException var20) {
               }
            }

            if (conn != null) {
               try {
                  conn.close();
               } catch (SQLException var19) {
               }
            }

         }

      }
   }

   public Language getLang() {
      return this.language;
   }

   public void checkPluginDependancies() {
      Plugin we = this.getServer().getPluginManager().getPlugin("WorldEdit");
      if (we != null) {
         plugin_worldEdit = (WorldEditPlugin)we;

         try {
            WorldEdit.getInstance().getEventBus().register(new PrismBlockEditHandler());
            log("WorldEdit found. Associated features enabled.");
         } catch (Throwable var3) {
            log("Required WorldEdit version is 6.0.0 or greater! Certain optional features of Prism disabled.");
         }
      } else {
         log("WorldEdit not found. Certain optional features of Prism disabled.");
      }

   }

   public boolean dependencyEnabled(String pluginName) {
      return this.enabledPlugins.contains(pluginName);
   }

   public static ArrayList getIllegalBlocks() {
      return illegalBlocks;
   }

   public static ArrayList getIllegalEntities() {
      return illegalEntities;
   }

   public static HashMap getAlertedOres() {
      return alertedOres;
   }

   public static MaterialAliases getItems() {
      return items;
   }

   public static ActionRegistry getActionRegistry() {
      return actionRegistry;
   }

   public static HandlerRegistry getHandlerRegistry() {
      return handlerRegistry;
   }

   public static Ignore getIgnore() {
      return ignore;
   }

   public PurgeManager getPurgeManager() {
      return this.purgeManager;
   }

   public static void registerParameter(PrismParameterHandler handler) {
      paramHandlers.put(handler.getName().toLowerCase(), handler);
   }

   public static HashMap getParameters() {
      return paramHandlers;
   }

   public static PrismParameterHandler getParameter(String name) {
      return (PrismParameterHandler)paramHandlers.get(name);
   }

   public void endExpiredQueryCaches() {
      this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
         public void run() {
            Date date = new Date();
            Iterator i$ = Prism.this.cachedQueries.entrySet().iterator();

            while(i$.hasNext()) {
               Map.Entry query = (Map.Entry)i$.next();
               QueryResult result = (QueryResult)query.getValue();
               long diff = (date.getTime() - result.getQueryTime()) / 1000L;
               if (diff >= 120L) {
                  Prism.this.cachedQueries.remove(query.getKey());
               }
            }

         }
      }, 2400L, 2400L);
   }

   public void endExpiredPreviews() {
      this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
         public void run() {
            Date date = new Date();
            Iterator i$ = Prism.this.playerActivePreviews.entrySet().iterator();

            while(i$.hasNext()) {
               Map.Entry query = (Map.Entry)i$.next();
               PreviewSession result = (PreviewSession)query.getValue();
               long diff = (date.getTime() - result.getQueryTime()) / 1000L;
               if (diff >= 60L) {
                  Player player = Prism.this.prism.getServer().getPlayer(result.getPlayer().getName());
                  if (player != null) {
                     player.sendMessage(Prism.messenger.playerHeaderMsg("Canceling forgotten preview."));
                  }

                  Prism.this.playerActivePreviews.remove(query.getKey());
               }
            }

         }
      }, 1200L, 1200L);
   }

   public void removeExpiredLocations() {
      this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
         public void run() {
            Date date = new Date();
            Iterator i$ = Prism.this.alertedBlocks.entrySet().iterator();

            while(i$.hasNext()) {
               Map.Entry entry = (Map.Entry)i$.next();
               long diff = (date.getTime() - (Long)entry.getValue()) / 1000L;
               if (diff >= 300L) {
                  Prism.this.alertedBlocks.remove(entry.getKey());
               }
            }

         }
      }, 1200L, 1200L);
   }

   public void actionRecorderTask() {
      int recorder_tick_delay = this.getConfig().getInt("prism.queue-empty-tick-delay");
      if (recorder_tick_delay < 1) {
         recorder_tick_delay = 3;
      }

      this.recordingTask = this.getServer().getScheduler().runTaskLaterAsynchronously(this, new RecordingTask(this.prism), (long)recorder_tick_delay);
   }

   public void launchScheduledPurgeManager() {
      List purgeRules = this.getConfig().getStringList("prism.db-records-purge-rules");
      this.purgeManager = new PurgeManager(this, purgeRules);
      this.schedulePool.scheduleAtFixedRate(this.purgeManager, 0L, 12L, TimeUnit.HOURS);
   }

   public void launchInternalAffairs() {
      InternalAffairs recordingMonitor = new InternalAffairs(this);
      this.recordingMonitorTask.scheduleAtFixedRate(recordingMonitor, 0L, 5L, TimeUnit.MINUTES);
   }

   public void alertPlayers(Player player, String msg) {
      Iterator i$ = this.getServer().getOnlinePlayers().iterator();

      while(true) {
         Player p;
         do {
            if (!i$.hasNext()) {
               return;
            }

            p = (Player)i$.next();
         } while(p.equals(player) && !this.getConfig().getBoolean("prism.alerts.alert-player-about-self"));

         if (p.hasPermission("prism.alerts")) {
            p.sendMessage(messenger.playerMsg(ChatColor.RED + "[!] " + msg));
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

   public void notifyNearby(Player player, int radius, String msg) {
      if (this.getConfig().getBoolean("prism.appliers.notify-nearby.enabled")) {
         Iterator i$ = player.getServer().getOnlinePlayers().iterator();

         while(i$.hasNext()) {
            Player p = (Player)i$.next();
            if (!p.equals(player) && player.getWorld().equals(p.getWorld()) && player.getLocation().distance(p.getLocation()) <= (double)(radius + config.getInt("prism.appliers.notify-nearby.additional-radius"))) {
               p.sendMessage(messenger.playerHeaderMsg(msg));
            }
         }

      }
   }

   public static void log(String message) {
      log.info("[" + getPrismName() + "]: " + message);
   }

   public static void logSection(String[] messages) {
      if (messages.length > 0) {
         log("--------------------- ## Important ## ---------------------");
         String[] arr$ = messages;
         int len$ = messages.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String msg = arr$[i$];
            log(msg);
         }

         log("--------------------- ## ========= ## ---------------------");
      }

   }

   public static void debug(String message) {
      if (config.getBoolean("prism.debug")) {
         log.info("[" + plugin_name + "]: " + message);
      }

   }

   public static void debug(Location loc) {
      debug("Location: " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
   }

   public void disablePlugin() {
      this.setEnabled(false);
   }

   public void onDisable() {
      if (this.getConfig().getBoolean("prism.database.force-write-queue-on-shutdown")) {
         QueueDrain drainer = new QueueDrain(this);
         drainer.forceDrainQueue();
      }

      if (pool != null) {
         pool.close();
      }

      log("Closing plugin.");
   }
}
