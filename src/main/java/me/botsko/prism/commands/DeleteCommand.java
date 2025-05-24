package me.botsko.prism.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.purge.PurgeChunkingUtil;
import me.botsko.prism.purge.PurgeTask;
import me.botsko.prism.purge.SenderPurgeCallback;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitTask;

public class DeleteCommand implements SubHandler {
   private final Prism plugin;
   protected BukkitTask deleteTask;
   protected int total_records_affected = 0;
   protected int cycle_rows_affected = 0;

   public DeleteCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      if (call.getArgs().length > 1 && call.getArg(1).equals("cancel")) {
         if (this.plugin.getPurgeManager().deleteTask != null) {
            this.plugin.getPurgeManager().deleteTask.cancel();
            call.getSender().sendMessage(Prism.messenger.playerMsg("Current purge tasks have been canceled."));
         } else {
            call.getSender().sendMessage(Prism.messenger.playerError("No purge task is currently running."));
         }

      } else if (call.getArgs().length > 1 && call.getArg(1).equals("queue")) {
         if (RecordingQueue.getQueue().size() > 0) {
            Prism.log("User " + call.getSender().getName() + " wiped the live queue before it could be written to the database. " + RecordingQueue.getQueue().size() + " events lost.");
            RecordingQueue.getQueue().clear();
            call.getSender().sendMessage(Prism.messenger.playerSuccess("Unwritten data in queue cleared."));
         } else {
            call.getSender().sendMessage(Prism.messenger.playerError("Event queue is empty, nothing to wipe."));
         }

      } else {
         QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.DELETE, 1, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
         if (parameters != null) {
            parameters.setStringFromRawArgs(call.getArgs(), 1);
            ArrayList defaultsUsed = parameters.getDefaultsUsed();
            String defaultsReminder = "";
            if (!defaultsUsed.isEmpty()) {
               defaultsReminder = defaultsReminder + " using defaults:";

               String d;
               for(Iterator i$ = defaultsUsed.iterator(); i$.hasNext(); defaultsReminder = defaultsReminder + " " + d) {
                  d = (String)i$.next();
               }
            }

            if (parameters.getFoundArgs().size() > 0) {
               int minId = PurgeChunkingUtil.getMinimumPrimaryKey();
               if (minId == 0) {
                  call.getSender().sendMessage(Prism.messenger.playerError("No minimum primary key could be found for purge chunking"));
                  return;
               }

               int maxId = PurgeChunkingUtil.getMaximumPrimaryKey();
               if (maxId == 0) {
                  call.getSender().sendMessage(Prism.messenger.playerError("No maximum primary key could be found for purge chunking"));
                  return;
               }

               call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Purging data..." + defaultsReminder));
               int purge_tick_delay = this.plugin.getConfig().getInt("prism.purge.batch-tick-delay");
               if (purge_tick_delay < 1) {
                  purge_tick_delay = 20;
               }

               call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Starting purge cycle." + ChatColor.GRAY + " No one will ever know..."));
               SenderPurgeCallback callback = new SenderPurgeCallback();
               callback.setSender(call.getSender());
               CopyOnWriteArrayList paramList = new CopyOnWriteArrayList();
               paramList.add(parameters);
               Prism.log("Beginning prism database purge cycle. Will be performed in batches so we don't tie up the db...");
               this.deleteTask = this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new PurgeTask(this.plugin, paramList, purge_tick_delay, minId, maxId, callback));
            } else {
               call.getSender().sendMessage(Prism.messenger.playerError("You must supply at least one parameter."));
            }

         }
      }
   }

   public List handleComplete(CallInfo call) {
      return PreprocessArgs.complete(call.getSender(), call.getArgs());
   }
}
