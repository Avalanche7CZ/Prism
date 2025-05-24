package me.botsko.prism.commands;

import java.sql.Connection;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class RecorderCommand implements SubHandler {
   private final Prism plugin;

   public RecorderCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      if (call.getArgs().length <= 1) {
         call.getSender().sendMessage(Prism.messenger.playerError("Invalid command. Use /pr ? for help"));
      } else {
         boolean recorderActive = false;
         if (this.plugin.recordingTask != null) {
            int taskId = this.plugin.recordingTask.getTaskId();
            BukkitScheduler scheduler = Bukkit.getScheduler();
            if (scheduler.isCurrentlyRunning(taskId) || scheduler.isQueued(taskId)) {
               recorderActive = true;
            }
         }

         if (call.getArg(1).equals("cancel")) {
            if (recorderActive) {
               this.plugin.recordingTask.cancel();
               this.plugin.recordingTask = null;
               call.getSender().sendMessage(Prism.messenger.playerMsg("Current recording task has been canceled."));
               call.getSender().sendMessage(Prism.messenger.playerError("WARNING: Actions will collect until queue until recorder restarted manually."));
            } else {
               call.getSender().sendMessage(Prism.messenger.playerError("No recording task is currently running."));
            }

         } else if (call.getArg(1).equals("start")) {
            if (recorderActive) {
               call.getSender().sendMessage(Prism.messenger.playerError("Recording tasks are currently running. Cannot start."));
            } else {
               call.getSender().sendMessage(Prism.messenger.playerMsg("Validating database connections..."));
               Connection conn = null;

               try {
                  conn = Prism.dbc();
                  if (conn != null && !conn.isClosed()) {
                     call.getSender().sendMessage(Prism.messenger.playerSuccess("Valid connection found. Yay!"));
                     call.getSender().sendMessage(Prism.messenger.playerMsg("Restarting recordering tasks..."));
                     this.plugin.actionRecorderTask();
                     return;
                  }

                  call.getSender().sendMessage(Prism.messenger.playerError("Valid database connection could not be found. Check the db/console and try again."));
               } catch (Exception var14) {
                  var14.printStackTrace();
                  return;
               } finally {
                  if (conn != null) {
                     try {
                        conn.close();
                     } catch (Exception var13) {
                     }
                  }

               }

            }
         }
      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
