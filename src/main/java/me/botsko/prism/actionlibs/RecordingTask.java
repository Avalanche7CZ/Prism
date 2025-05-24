package me.botsko.prism.actionlibs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.players.PrismPlayer;

public class RecordingTask implements Runnable {
   private final Prism plugin;

   public RecordingTask(Prism plugin) {
      this.plugin = plugin;
   }

   public void save() {
      if (!RecordingQueue.getQueue().isEmpty()) {
         this.insertActionsIntoDatabase();
      }

   }

   public static int insertActionIntoDatabase(Handler a) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      int id = 0;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet generatedKeys = null;

      int world_id;
      try {
         a.save();
         conn = Prism.dbc();
         if (conn != null) {
            world_id = 0;
            if (Prism.prismWorlds.containsKey(a.getWorldName())) {
               world_id = (Integer)Prism.prismWorlds.get(a.getWorldName());
            }

            int action_id = 0;
            if (Prism.prismActions.containsKey(a.getType().getName())) {
               action_id = (Integer)Prism.prismActions.get(a.getType().getName());
            }

            int player_id = 0;
            PrismPlayer prismPlayer = PlayerIdentification.cachePrismPlayer(a.getPlayerName());
            if (prismPlayer != null) {
               player_id = prismPlayer.getId();
            }

            if (world_id != 0 && action_id != 0 && player_id == 0) {
            }

            s = conn.prepareStatement("INSERT INTO " + prefix + "data (epoch,action_id,player_id,world_id,block_id,block_subid,old_block_id,old_block_subid,x,y,z) VALUES (?,?,?,?,?,?,?,?,?,?,?)", 1);
            s.setLong(1, System.currentTimeMillis() / 1000L);
            s.setInt(2, action_id);
            s.setInt(3, player_id);
            s.setInt(4, world_id);
            s.setInt(5, a.getBlockId());
            s.setInt(6, a.getBlockSubId());
            s.setInt(7, a.getOldBlockId());
            s.setInt(8, a.getOldBlockSubId());
            s.setInt(9, (int)a.getX());
            s.setInt(10, (int)a.getY());
            s.setInt(11, (int)a.getZ());
            s.executeUpdate();
            generatedKeys = s.getGeneratedKeys();
            if (generatedKeys.next()) {
               id = generatedKeys.getInt(1);
            }

            if (a.getData() != null && !a.getData().isEmpty()) {
               s = conn.prepareStatement("INSERT INTO " + prefix + "data_extra (data_id,data) VALUES (?,?)");
               s.setInt(1, id);
               s.setString(2, a.getData());
               s.executeUpdate();
            }

            return id;
         }

         Prism.log("Prism database error. Connection should be there but it's not. This action wasn't logged.");
         world_id = 0;
      } catch (SQLException var29) {
         return id;
      } finally {
         if (generatedKeys != null) {
            try {
               generatedKeys.close();
            } catch (SQLException var28) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var27) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var26) {
            }
         }

      }

      return world_id;
   }

   public void insertActionsIntoDatabase() {
      String prefix = this.plugin.getConfig().getString("prism.mysql.prefix");
      PreparedStatement s = null;
      Connection conn = null;
      int actionsRecorded = 0;

      try {
         int perBatch = this.plugin.getConfig().getInt("prism.database.actions-per-insert-batch");
         if (perBatch < 1) {
            perBatch = 1000;
         }

         if (RecordingQueue.getQueue().isEmpty()) {
            return;
         }

         Prism.debug("Beginning batch insert from queue. " + System.currentTimeMillis());
         ArrayList extraDataQueue = new ArrayList();
         conn = Prism.dbc();
         if (conn != null && !conn.isClosed()) {
            RecordingManager.failedDbConnectionCount = 0;
            conn.setAutoCommit(false);
            s = conn.prepareStatement("INSERT INTO " + prefix + "data (epoch,action_id,player_id,world_id,block_id,block_subid,old_block_id,old_block_subid,x,y,z) VALUES (?,?,?,?,?,?,?,?,?,?,?)", 1);
            int i = 0;

            label258:
            while(true) {
               while(true) {
                  if (RecordingQueue.getQueue().isEmpty()) {
                     break label258;
                  }

                  if (conn.isClosed()) {
                     Prism.log("Prism database error. We have to bail in the middle of building primary bulk insert query.");
                     break label258;
                  }

                  Handler a = (Handler)RecordingQueue.getQueue().poll();
                  if (a == null) {
                     break label258;
                  }

                  int world_id = 0;
                  if (Prism.prismWorlds.containsKey(a.getWorldName())) {
                     world_id = (Integer)Prism.prismWorlds.get(a.getWorldName());
                  }

                  int action_id = 0;
                  if (Prism.prismActions.containsKey(a.getType().getName())) {
                     action_id = (Integer)Prism.prismActions.get(a.getType().getName());
                  }

                  int player_id = 0;
                  PrismPlayer prismPlayer = PlayerIdentification.cachePrismPlayer(a.getPlayerName());
                  if (prismPlayer != null) {
                     player_id = prismPlayer.getId();
                  }

                  if (world_id != 0 && action_id != 0 && player_id != 0) {
                     if (!a.isCanceled()) {
                        ++actionsRecorded;
                        s.setLong(1, System.currentTimeMillis() / 1000L);
                        s.setInt(2, action_id);
                        s.setInt(3, player_id);
                        s.setInt(4, world_id);
                        s.setInt(5, a.getBlockId());
                        s.setInt(6, a.getBlockSubId());
                        s.setInt(7, a.getOldBlockId());
                        s.setInt(8, a.getOldBlockSubId());
                        s.setInt(9, (int)a.getX());
                        s.setInt(10, (int)a.getY());
                        s.setInt(11, (int)a.getZ());
                        s.addBatch();
                        extraDataQueue.add(a);
                        if (i >= perBatch) {
                           Prism.debug("Recorder: Batch max exceeded, running insert. Queue remaining: " + RecordingQueue.getQueue().size());
                           break label258;
                        }

                        ++i;
                     }
                  } else {
                     Prism.log("Cache data was empty. Please report to developer: world_id:" + world_id + "/" + a.getWorldName() + " action_id:" + action_id + "/" + a.getType().getName() + " player_id:" + player_id + "/" + a.getPlayerName());
                     Prism.log("HOWEVER, this likely means you have a broken prism database installation.");
                  }
               }
            }

            s.executeBatch();
            if (conn.isClosed()) {
               Prism.log("Prism database error. We have to bail in the middle of building primary bulk insert query.");
            } else {
               conn.commit();
               Prism.debug("Batch insert was commit: " + System.currentTimeMillis());
            }

            this.plugin.queueStats.addRunCount(actionsRecorded);
            this.insertExtraData(extraDataQueue, s.getGeneratedKeys());
            return;
         }

         if (RecordingManager.failedDbConnectionCount == 0) {
            Prism.log("Prism database error. Connection should be there but it's not. Leaving actions to log in queue.");
         }

         ++RecordingManager.failedDbConnectionCount;
         if (RecordingManager.failedDbConnectionCount > this.plugin.getConfig().getInt("prism.database.max-failures-before-wait")) {
            Prism.log("Too many problems connecting. Giving up for a bit.");
            this.scheduleNextRecording();
         }

         Prism.debug("Database connection still missing, incrementing count.");
      } catch (SQLException var27) {
         var27.printStackTrace();
         this.plugin.handleDatabaseException(var27);
         return;
      } finally {
         if (s != null) {
            try {
               s.close();
            } catch (SQLException var26) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var25) {
            }
         }

      }

   }

   protected void insertExtraData(ArrayList extraDataQueue, ResultSet keys) throws SQLException {
      String prefix = this.plugin.getConfig().getString("prism.mysql.prefix");
      if (!extraDataQueue.isEmpty()) {
         PreparedStatement s = null;
         Connection conn = Prism.dbc();
         if (conn != null && !conn.isClosed()) {
            try {
               conn.setAutoCommit(false);
               s = conn.prepareStatement("INSERT INTO " + prefix + "data_extra (data_id,data) VALUES (?,?)");
               int i = 0;

               while(keys.next()) {
                  if (conn.isClosed()) {
                     Prism.log("Prism database error. We have to bail in the middle of building bulk insert extra data query.");
                     break;
                  }

                  if (i >= extraDataQueue.size()) {
                     Prism.log("Skipping extra data for " + prefix + "data.id " + keys.getInt(1) + " because the queue doesn't have data for it.");
                  } else {
                     Handler a = (Handler)extraDataQueue.get(i);
                     if (a.getData() != null && !a.getData().isEmpty()) {
                        s.setInt(1, keys.getInt(1));
                        s.setString(2, a.getData());
                        s.addBatch();
                     }

                     ++i;
                  }
               }

               s.executeBatch();
               if (conn.isClosed()) {
                  Prism.log("Prism database error. We have to bail in the middle of building extra data bulk insert query.");
               } else {
                  conn.commit();
               }
            } catch (SQLException var20) {
               var20.printStackTrace();
               this.plugin.handleDatabaseException(var20);
            } finally {
               if (s != null) {
                  try {
                     s.close();
                  } catch (SQLException var19) {
                  }
               }

               try {
                  conn.close();
               } catch (SQLException var18) {
               }

            }

         } else {
            Prism.log("Prism database error. Skipping extra data queue insertion.");
         }
      }
   }

   public void run() {
      if (RecordingManager.failedDbConnectionCount > 5) {
         this.plugin.rebuildPool();
      }

      this.save();
      this.scheduleNextRecording();
   }

   protected int getTickDelayForNextBatch() {
      if (RecordingManager.failedDbConnectionCount > this.plugin.getConfig().getInt("prism.database.max-failures-before-wait")) {
         return RecordingManager.failedDbConnectionCount * 20;
      } else {
         int recorder_tick_delay = this.plugin.getConfig().getInt("prism.queue-empty-tick-delay");
         if (recorder_tick_delay < 1) {
            recorder_tick_delay = 3;
         }

         return recorder_tick_delay;
      }
   }

   protected void scheduleNextRecording() {
      if (!this.plugin.isEnabled()) {
         Prism.log("Can't schedule new recording tasks as plugin is now disabled. If you're shutting down the server, ignore me.");
      } else {
         this.plugin.recordingTask = this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, new RecordingTask(this.plugin), (long)this.getTickDelayForNextBatch());
      }
   }
}
