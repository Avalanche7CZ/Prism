package me.botsko.prism.actionlibs;

import java.sql.Connection;
import java.sql.SQLException;
import me.botsko.prism.Prism;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class InternalAffairs implements Runnable {
   private final Prism plugin;

   public InternalAffairs(Prism plugin) {
      Prism.debug("[InternalAffairs] Keeping watch over the watchers.");
      this.plugin = plugin;
   }

   public void run() {
      if (this.plugin.recordingTask != null) {
         int taskId = this.plugin.recordingTask.getTaskId();
         BukkitScheduler scheduler = Bukkit.getScheduler();
         if (scheduler.isCurrentlyRunning(taskId) || scheduler.isQueued(taskId)) {
            Prism.debug("[InternalAffairs] Recorder is currently active. All is good.");
            return;
         }
      }

      Prism.log("[InternalAffairs] Recorder is NOT active... checking database");
      Connection conn = null;

      try {
         conn = Prism.dbc();
         if (conn == null) {
            Prism.log("[InternalAffairs] Pool returned NULL instead of a valid connection.");
         } else if (conn.isClosed()) {
            Prism.log("[InternalAffairs] Pool returned an already closed connection.");
         } else if (conn.isValid(5)) {
            Prism.log("[InternalAffairs] Pool returned valid connection!");
            Prism.log("[InternalAffairs] Restarting scheduled recorder tasks");
            this.plugin.actionRecorderTask();
         }
      } catch (SQLException var11) {
         Prism.debug("[InternalAffairs] Error: " + var11.getMessage());
         var11.printStackTrace();
      } finally {
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var10) {
            }
         }

      }

   }
}
