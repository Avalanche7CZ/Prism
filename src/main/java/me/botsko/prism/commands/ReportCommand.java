package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.RecordingManager;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.database.mysql.ActionReportQueryBuilder;
import me.botsko.prism.database.mysql.BlockReportQueryBuilder;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitScheduler;

public class ReportCommand implements SubHandler {
   private final Prism plugin;
   private final List secondaries;
   private final List sumTertiaries;

   public ReportCommand(Prism plugin) {
      this.plugin = plugin;
      this.secondaries = new ArrayList();
      this.secondaries.add("queue");
      this.secondaries.add("db");
      this.secondaries.add("sum");
      this.sumTertiaries = new ArrayList();
      this.sumTertiaries.add("blocks");
      this.sumTertiaries.add("actions");
   }

   public void handle(CallInfo call) {
      if (call.getArgs().length < 2) {
         call.getSender().sendMessage(Prism.messenger.playerError("Please specify a report. Use /prism ? for help."));
      } else {
         if (call.getArg(1).equals("queue")) {
            this.queueReport(call.getSender());
         }

         if (call.getArg(1).equals("db")) {
            this.databaseReport(call.getSender());
         }

         if (call.getArg(1).equals("sum")) {
            if (call.getArgs().length < 3) {
               call.getSender().sendMessage(Prism.messenger.playerError("Please specify a 'sum' report. Use /prism ? for help."));
               return;
            }

            if (call.getArgs().length < 4) {
               call.getSender().sendMessage(Prism.messenger.playerError("Please provide a player name. Use /prism ? for help."));
               return;
            }

            if (call.getArg(2).equals("blocks")) {
               this.blockSumReports(call);
            }

            if (call.getArg(2).equals("actions")) {
               this.actionTypeCountReport(call);
            }
         }

      }
   }

   public List handleComplete(CallInfo call) {
      if (call.getArgs().length == 2) {
         return MiscUtils.getStartingWith(call.getArg(1), this.secondaries);
      } else if (call.getArg(1).equals("sum")) {
         return call.getArgs().length == 3 ? MiscUtils.getStartingWith(call.getArg(2), this.sumTertiaries) : PreprocessArgs.complete(call.getSender(), call.getArgs());
      } else {
         return null;
      }
   }

   protected void queueReport(CommandSender sender) {
      sender.sendMessage(Prism.messenger.playerHeaderMsg("Current Stats"));
      sender.sendMessage(Prism.messenger.playerMsg("Actions in queue: " + ChatColor.WHITE + RecordingQueue.getQueueSize()));
      ConcurrentSkipListMap runs = this.plugin.queueStats.getRecentRunCounts();
      if (runs.size() > 0) {
         sender.sendMessage(Prism.messenger.playerHeaderMsg("Recent queue save stats:"));
         Iterator i$ = runs.entrySet().iterator();

         while(i$.hasNext()) {
            Map.Entry entry = (Map.Entry)i$.next();
            String time = (new SimpleDateFormat("HH:mm:ss")).format(entry.getKey());
            sender.sendMessage(Prism.messenger.playerMsg(ChatColor.GRAY + time + " " + ChatColor.WHITE + entry.getValue()));
         }
      }

   }

   protected void databaseReport(CommandSender sender) {
      sender.sendMessage(Prism.messenger.playerHeaderMsg("Database Connection State"));
      sender.sendMessage(Prism.messenger.playerMsg("Active Failure Count: " + ChatColor.WHITE + RecordingManager.failedDbConnectionCount));
      sender.sendMessage(Prism.messenger.playerMsg("Actions in queue: " + ChatColor.WHITE + RecordingQueue.getQueueSize()));
      sender.sendMessage(Prism.messenger.playerMsg("Pool active: " + ChatColor.WHITE + Prism.getPool().getActive()));
      sender.sendMessage(Prism.messenger.playerMsg("Pool idle: " + ChatColor.WHITE + Prism.getPool().getIdle()));
      sender.sendMessage(Prism.messenger.playerMsg("Pool active count: " + ChatColor.WHITE + Prism.getPool().getNumActive()));
      sender.sendMessage(Prism.messenger.playerMsg("Pool idle count: " + ChatColor.WHITE + Prism.getPool().getNumIdle()));
      boolean recorderActive = false;
      if (this.plugin.recordingTask != null) {
         int taskId = this.plugin.recordingTask.getTaskId();
         BukkitScheduler scheduler = Bukkit.getScheduler();
         if (scheduler.isCurrentlyRunning(taskId) || scheduler.isQueued(taskId)) {
            recorderActive = true;
         }
      }

      if (recorderActive) {
         sender.sendMessage(Prism.messenger.playerSuccess("Recorder is currently queued or running!"));
      } else {
         sender.sendMessage(Prism.messenger.playerError("Recorder stopped running! DB conn problems? Try /pr recorder start"));
      }

      sender.sendMessage(Prism.messenger.playerSubduedHeaderMsg("Attempting to check connection readiness..."));
      Connection conn = null;

      try {
         conn = Prism.dbc();
         if (conn == null) {
            sender.sendMessage(Prism.messenger.playerError("Pool returned NULL instead of a valid connection."));
         } else if (conn.isClosed()) {
            sender.sendMessage(Prism.messenger.playerError("Pool returned an already closed connection."));
         } else if (conn.isValid(5)) {
            sender.sendMessage(Prism.messenger.playerSuccess("Pool returned valid connection!"));
         }
      } catch (SQLException var13) {
         sender.sendMessage(Prism.messenger.playerError("Error: " + var13.getMessage()));
         var13.printStackTrace();
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var12) {
            }
         }

      }

   }

   protected void blockSumReports(final CallInfo call) {
      QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.LOOKUP, 3, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
      if (parameters == null) {
         call.getSender().sendMessage(Prism.messenger.playerError("You must specify parameters, at least one player."));
      } else if (!parameters.getActionTypes().isEmpty()) {
         call.getSender().sendMessage(Prism.messenger.playerError("You may not specify any action types for this report."));
      } else {
         HashMap players = parameters.getPlayerNames();
         if (players.size() != 1) {
            call.getSender().sendMessage(Prism.messenger.playerError("You must provide only a single player name."));
         } else {
            final String tempName = "";
            Iterator i$ = players.keySet().iterator();
            if (i$.hasNext()) {
               String player = (String)i$.next();
               tempName = player;
            }

            BlockReportQueryBuilder reportQuery = new BlockReportQueryBuilder(this.plugin);
            final String sql = reportQuery.getQuery(parameters, false);
            int colTextLen = true;
            int colIntLen = true;
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
               public void run() {
                  call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Crafting block change report for " + ChatColor.DARK_AQUA + tempName + "..."));
                  Connection conn = null;
                  PreparedStatement s = null;
                  ResultSet rs = null;

                  try {
                     conn = Prism.dbc();
                     s = conn.prepareStatement(sql);
                     s.executeQuery();
                     rs = s.getResultSet();
                     call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Total block changes for " + ChatColor.DARK_AQUA + tempName));
                     call.getSender().sendMessage(Prism.messenger.playerMsg(ChatColor.GRAY + TypeUtils.padStringRight("Block", 20) + TypeUtils.padStringRight("Placed", 12) + TypeUtils.padStringRight("Broken", 12)));

                     while(rs.next()) {
                        String alias = Prism.getItems().getAlias(rs.getInt(1), 0);
                        int placed = rs.getInt(2);
                        int broken = rs.getInt(3);
                        String colAlias = TypeUtils.padStringRight(alias, 20);
                        String colPlaced = TypeUtils.padStringRight("" + placed, 12);
                        String colBroken = TypeUtils.padStringRight("" + broken, 12);
                        call.getSender().sendMessage(Prism.messenger.playerMsg(ChatColor.DARK_AQUA + colAlias + ChatColor.GREEN + colPlaced + " " + ChatColor.RED + colBroken));
                     }
                  } catch (SQLException var26) {
                     var26.printStackTrace();
                  } finally {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (SQLException var25) {
                        }
                     }

                     if (s != null) {
                        try {
                           s.close();
                        } catch (SQLException var24) {
                        }
                     }

                     if (conn != null) {
                        try {
                           conn.close();
                        } catch (SQLException var23) {
                        }
                     }

                  }

               }
            });
         }
      }
   }

   protected void actionTypeCountReport(final CallInfo call) {
      QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.LOOKUP, 3, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
      if (parameters != null) {
         if (!parameters.getActionTypes().isEmpty()) {
            call.getSender().sendMessage(Prism.messenger.playerError("You may not specify any action types for this report."));
         } else {
            HashMap players = parameters.getPlayerNames();
            if (players.size() != 1) {
               call.getSender().sendMessage(Prism.messenger.playerError("You must provide only a single player name."));
            } else {
               final String tempName = "";
               Iterator i$ = players.keySet().iterator();
               if (i$.hasNext()) {
                  String player = (String)i$.next();
                  tempName = player;
               }

               ActionReportQueryBuilder reportQuery = new ActionReportQueryBuilder(this.plugin);
               final String sql = reportQuery.getQuery(parameters, false);
               int colTextLen = true;
               int colIntLen = true;
               this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
                  public void run() {
                     call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Crafting action type report for " + ChatColor.DARK_AQUA + tempName + "..."));
                     Connection conn = null;
                     PreparedStatement s = null;
                     ResultSet rs = null;

                     try {
                        conn = Prism.dbc();
                        s = conn.prepareStatement(sql);
                        s.executeQuery();
                        rs = s.getResultSet();
                        call.getSender().sendMessage(Prism.messenger.playerMsg(ChatColor.GRAY + TypeUtils.padStringRight("Action", 16) + TypeUtils.padStringRight("Count", 12)));

                        while(rs.next()) {
                           String action = rs.getString(2);
                           int count = rs.getInt(1);
                           String colAlias = TypeUtils.padStringRight(action, 16);
                           String colPlaced = TypeUtils.padStringRight("" + count, 12);
                           call.getSender().sendMessage(Prism.messenger.playerMsg(ChatColor.DARK_AQUA + colAlias + ChatColor.GREEN + colPlaced));
                        }
                     } catch (SQLException var24) {
                        var24.printStackTrace();
                     } finally {
                        if (rs != null) {
                           try {
                              rs.close();
                           } catch (SQLException var23) {
                           }
                        }

                        if (s != null) {
                           try {
                              s.close();
                           } catch (SQLException var22) {
                           }
                        }

                        if (conn != null) {
                           try {
                              conn.close();
                           } catch (SQLException var21) {
                           }
                        }

                     }

                  }
               });
            }
         }
      }
   }
}
