package me.botsko.prism.purge;

import java.util.concurrent.CopyOnWriteArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;

public class PurgeTask implements Runnable {
   private final Prism plugin;
   private final CopyOnWriteArrayList paramList;
   private int cycle_rows_affected = 0;
   private final int purge_tick_delay;
   private int minId = 0;
   private int maxId = 0;
   private final PurgeCallback callback;

   public PurgeTask(Prism plugin, CopyOnWriteArrayList paramList, int purge_tick_delay, int minId, int maxId, PurgeCallback callback) {
      this.plugin = plugin;
      this.paramList = paramList;
      this.purge_tick_delay = purge_tick_delay;
      this.minId = minId;
      this.maxId = maxId;
      this.callback = callback;
   }

   public void run() {
      if (!this.paramList.isEmpty()) {
         ActionsQuery aq = new ActionsQuery(this.plugin);
         QueryParameters param = (QueryParameters)this.paramList.get(0);
         boolean cycle_complete = false;
         int spread = this.plugin.getConfig().getInt("prism.purge.records-per-batch");
         if (spread <= 1) {
            spread = 10000;
         }

         int newMinId = this.minId + spread;
         param.setMinPrimaryKey(this.minId);
         param.setMaxPrimaryKey(newMinId);
         this.cycle_rows_affected = aq.delete(param);
         Prism var10000 = this.plugin;
         var10000.total_records_affected += this.cycle_rows_affected;
         if (newMinId > this.maxId) {
            this.paramList.remove(param);
            cycle_complete = true;
         }

         Prism.debug("------------------- " + param.getOriginalCommand());
         Prism.debug("minId: " + this.minId);
         Prism.debug("maxId: " + this.maxId);
         Prism.debug("newMinId: " + newMinId);
         Prism.debug("cycle_rows_affected: " + this.cycle_rows_affected);
         Prism.debug("cycle_complete: " + cycle_complete);
         Prism.debug("plugin.total_records_affected: " + this.plugin.total_records_affected);
         Prism.debug("-------------------");
         this.callback.cycle(param, this.cycle_rows_affected, this.plugin.total_records_affected, cycle_complete);
         if (!this.plugin.isEnabled()) {
            Prism.log("Can't schedule new purge tasks as plugin is now disabled. If you're shutting down the server, ignore me.");
         } else {
            if (!cycle_complete) {
               this.plugin.getPurgeManager().deleteTask = this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, new PurgeTask(this.plugin, this.paramList, this.purge_tick_delay, newMinId, this.maxId, this.callback), (long)this.purge_tick_delay);
            } else {
               this.plugin.total_records_affected = 0;
               if (this.paramList.isEmpty()) {
                  return;
               }

               Prism.log("Moving on to next purge rule...");
               newMinId = PurgeChunkingUtil.getMinimumPrimaryKey();
               if (newMinId == 0) {
                  Prism.log("No minimum primary key could be found for purge chunking.");
                  return;
               }

               this.plugin.getPurgeManager().deleteTask = this.plugin.getServer().getScheduler().runTaskLaterAsynchronously(this.plugin, new PurgeTask(this.plugin, this.paramList, this.purge_tick_delay, newMinId, this.maxId, this.callback), (long)this.purge_tick_delay);
            }

         }
      }
   }
}
