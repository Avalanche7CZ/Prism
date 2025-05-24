package me.botsko.prism.actionlibs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.actions.PrismProcessAction;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.database.mysql.DeleteQueryBuilder;
import me.botsko.prism.database.mysql.SelectQueryBuilder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ActionsQuery {
   private final Prism plugin;
   private final SelectQueryBuilder qb;
   private boolean shouldGroup = false;

   public ActionsQuery(Prism plugin) {
      this.plugin = plugin;
      this.qb = new SelectQueryBuilder(plugin);
   }

   public QueryResult lookup(QueryParameters parameters) {
      return this.lookup(parameters, (CommandSender)null);
   }

   public QueryResult lookup(QueryParameters parameters, CommandSender sender) {
      Player player = null;
      if (sender instanceof Player) {
         player = (Player)sender;
      }

      this.shouldGroup = false;
      if (parameters.getProcessType().equals(PrismProcessType.LOOKUP)) {
         this.shouldGroup = true;
         if (!this.plugin.getConfig().getBoolean("prism.queries.lookup-auto-group")) {
            this.shouldGroup = false;
         }

         if (parameters.hasFlag(Flag.NO_GROUP) || parameters.hasFlag(Flag.EXTENDED)) {
            this.shouldGroup = false;
         }
      }

      List actions = new ArrayList();
      String query = this.qb.getQuery(parameters, this.shouldGroup);
      if (query != null) {
         Connection conn = null;
         PreparedStatement s = null;
         ResultSet rs = null;

         try {
            this.plugin.eventTimer.recordTimedEvent("query started");
            conn = Prism.dbc();
            if (conn == null || conn.isClosed()) {
               if (RecordingManager.failedDbConnectionCount == 0) {
                  Prism.log("Prism database error. Connection should be there but it's not. Leaving actions to log in queue.");
               }

               ++RecordingManager.failedDbConnectionCount;
               sender.sendMessage(Prism.messenger.playerError("Database connection was closed, please wait and try again."));
               QueryResult var41 = new QueryResult(actions, parameters);
               return var41;
            }

            RecordingManager.failedDbConnectionCount = 0;
            s = conn.prepareStatement(query);
            rs = s.executeQuery();
            this.plugin.eventTimer.recordTimedEvent("query returned, building results");

            label369:
            while(true) {
               while(true) {
                  do {
                     if (!rs.next()) {
                        break label369;
                     }
                  } while(rs.getString(3) == null);

                  String actionName = "";
                  Iterator i$ = Prism.prismActions.entrySet().iterator();

                  while(i$.hasNext()) {
                     Map.Entry entry = (Map.Entry)i$.next();
                     if ((Integer)entry.getValue() == rs.getInt(3)) {
                        actionName = (String)entry.getKey();
                     }
                  }

                  if (actionName.isEmpty()) {
                     Prism.log("Record contains action ID that doesn't exist in cache: " + rs.getInt(3));
                  } else {
                     ActionType actionType = Prism.getActionRegistry().getAction(actionName);
                     if (actionType != null) {
                        try {
                           Handler baseHandler = Prism.getHandlerRegistry().getHandler(actionType.getHandler());
                           String worldName = "";
                           Iterator i$ = Prism.prismWorlds.entrySet().iterator();

                           while(i$.hasNext()) {
                              Map.Entry entry = (Map.Entry)i$.next();
                              if ((Integer)entry.getValue() == rs.getInt(5)) {
                                 worldName = (String)entry.getKey();
                              }
                           }

                           baseHandler.setPlugin(this.plugin);
                           baseHandler.setType(actionType);
                           baseHandler.setId(rs.getInt(1));
                           baseHandler.setUnixEpoch(rs.getString(2));
                           baseHandler.setPlayerName(rs.getString(4));
                           baseHandler.setWorldName(worldName);
                           baseHandler.setX((double)rs.getInt(6));
                           baseHandler.setY((double)rs.getInt(7));
                           baseHandler.setZ((double)rs.getInt(8));
                           baseHandler.setBlockId(rs.getInt(9));
                           baseHandler.setBlockSubId(rs.getInt(10));
                           baseHandler.setOldBlockId(rs.getInt(11));
                           baseHandler.setOldBlockSubId(rs.getInt(12));
                           baseHandler.setData(rs.getString(13));
                           baseHandler.setMaterialAliases(Prism.getItems());
                           int aggregated = 0;
                           if (this.shouldGroup) {
                              aggregated = rs.getInt(14);
                           }

                           baseHandler.setAggregateCount(aggregated);
                           actions.add(baseHandler);
                        } catch (Exception var35) {
                           if (!rs.isClosed()) {
                              Prism.log("Ignoring data from record #" + rs.getInt(1) + " because it caused an error:");
                           }

                           var35.printStackTrace();
                        }
                     }
                  }
               }
            }
         } catch (SQLException var36) {
            this.plugin.handleDatabaseException(var36);
         } finally {
            if (rs != null) {
               try {
                  rs.close();
               } catch (SQLException var34) {
               }
            }

            if (s != null) {
               try {
                  s.close();
               } catch (SQLException var33) {
               }
            }

            if (conn != null) {
               try {
                  conn.close();
               } catch (SQLException var32) {
               }
            }

         }
      }

      QueryResult res = new QueryResult(actions, parameters);
      res.setPerPage(parameters.getPerPage());
      if (parameters.getProcessType().equals(PrismProcessType.LOOKUP)) {
         String keyName = "console";
         if (player != null) {
            keyName = player.getName();
         }

         if (this.plugin.cachedQueries.containsKey(keyName)) {
            this.plugin.cachedQueries.remove(keyName);
         }

         this.plugin.cachedQueries.put(keyName, res);
         Iterator i$ = parameters.getSharedPlayers().iterator();

         while(i$.hasNext()) {
            CommandSender sharedPlayer = (CommandSender)i$.next();
            this.plugin.cachedQueries.put(sharedPlayer.getName(), res);
         }
      }

      this.plugin.eventTimer.recordTimedEvent("results object completed");
      return res;
   }

   public int getUsersLastPrismProcessId(String playername) {
      String prefix = this.plugin.getConfig().getString("prism.mysql.prefix");
      int id = 0;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         int action_id = (Integer)Prism.prismActions.get("prism-process");
         conn = Prism.dbc();
         if (conn != null && !conn.isClosed()) {
            s = conn.prepareStatement("SELECT id FROM " + prefix + "data JOIN " + prefix + "players p ON p.player_id = " + prefix + "data.player_id WHERE action_id = ? AND p.player = ? ORDER BY id DESC LIMIT 1");
            s.setInt(1, action_id);
            s.setString(2, playername);
            s.executeQuery();
            rs = s.getResultSet();
            if (rs.first()) {
               id = rs.getInt("id");
            }
         } else {
            Prism.log("Prism database error. getUsersLastPrismProcessId cannot continue.");
         }
      } catch (SQLException var24) {
         this.plugin.handleDatabaseException(var24);
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

      return id;
   }

   public PrismProcessAction getPrismProcessRecord(int id) {
      String prefix = this.plugin.getConfig().getString("prism.mysql.prefix");
      PrismProcessAction process = null;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         String sql = "SELECT id, action, epoch, world, player, x, y, z, data FROM " + prefix + "data d";
         sql = sql + " INNER JOIN " + prefix + "players p ON p.player_id = d.player_id ";
         sql = sql + " INNER JOIN " + prefix + "actions a ON a.action_id = d.action_id ";
         sql = sql + " INNER JOIN " + prefix + "worlds w ON w.world_id = d.world_id ";
         sql = sql + " LEFT JOIN " + prefix + "data_extra ex ON ex.data_id = d.id ";
         sql = sql + " WHERE d.id = ?";
         conn = Prism.dbc();
         if (conn != null && !conn.isClosed()) {
            s = conn.prepareStatement(sql);
            s.setInt(1, id);
            s.executeQuery();
            rs = s.getResultSet();
            if (rs.first()) {
               process = new PrismProcessAction();
               process.setId(rs.getInt("id"));
               process.setType(Prism.getActionRegistry().getAction(rs.getString("action")));
               process.setUnixEpoch(rs.getString("epoch"));
               process.setWorldName(rs.getString("world"));
               process.setPlayerName(rs.getString("player"));
               process.setX((double)rs.getInt("x"));
               process.setY((double)rs.getInt("y"));
               process.setZ((double)rs.getInt("z"));
               process.setData(rs.getString("data"));
            }
         } else {
            Prism.log("Prism database error. getPrismProcessRecord cannot continue.");
         }
      } catch (SQLException var24) {
         this.plugin.handleDatabaseException(var24);
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

      return process;
   }

   public int delete(QueryParameters parameters) {
      int total_rows_affected = 0;
      Connection conn = null;
      Statement s = null;

      try {
         DeleteQueryBuilder dqb = new DeleteQueryBuilder(this.plugin);
         String query = dqb.getQuery(parameters, this.shouldGroup);
         conn = Prism.dbc();
         if (conn != null && !conn.isClosed()) {
            s = conn.createStatement();
            int cycle_rows_affected = s.executeUpdate(query);
            total_rows_affected += cycle_rows_affected;
         } else {
            Prism.log("Prism database error. Purge cannot continue.");
         }
      } catch (SQLException var20) {
         this.plugin.handleDatabaseException(var20);
      } finally {
         if (s != null) {
            try {
               s.close();
            } catch (SQLException var19) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var18) {
            }
         }

      }

      return total_rows_affected;
   }
}
