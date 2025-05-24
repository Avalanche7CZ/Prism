package me.botsko.prism.purge;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.PreprocessArgs;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

public final class PurgeManager implements Runnable {
   private final List purgeRules;
   private final Prism plugin;
   public BukkitTask deleteTask;

   public PurgeManager(Prism plugin, List purgeRules) {
      this.plugin = plugin;
      this.purgeRules = purgeRules;
   }

   public void run() {
      Prism.log("Scheduled purge executor beginning new run...");
      if (!this.purgeRules.isEmpty()) {
         CopyOnWriteArrayList paramList = new CopyOnWriteArrayList();
         Iterator i$ = this.purgeRules.iterator();

         while(i$.hasNext()) {
            String purgeArgs = (String)i$.next();
            QueryParameters parameters = PreprocessArgs.process(this.plugin, (CommandSender)null, purgeArgs.split(" "), PrismProcessType.DELETE, 0, false);
            if (parameters == null) {
               Prism.log("Invalid parameters for database purge: " + purgeArgs);
            } else if (parameters.getFoundArgs().size() > 0) {
               parameters.setStringFromRawArgs(purgeArgs.split(" "), 0);
               paramList.add(parameters);
            }
         }

         if (paramList.size() > 0) {
            int minId = PurgeChunkingUtil.getMinimumPrimaryKey();
            if (minId == 0) {
               Prism.log("No minimum primary key could be found for purge chunking.");
               return;
            }

            int maxId = PurgeChunkingUtil.getMaximumPrimaryKey();
            if (maxId == 0) {
               Prism.log("No maximum primary key could be found for purge chunking.");
               return;
            }

            int purge_tick_delay = this.plugin.getConfig().getInt("prism.purge.batch-tick-delay");
            if (purge_tick_delay < 1) {
               purge_tick_delay = 20;
            }

            Prism.log("Beginning prism database purge cycle. Will be performed in batches so we don't tie up the db...");
            this.deleteTask = Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, new PurgeTask(this.plugin, paramList, purge_tick_delay, minId, maxId, new LogPurgeCallback()), (long)purge_tick_delay);
         }
      } else {
         Prism.log("Purge rules are empty, not purging anything.");
      }

   }
}
