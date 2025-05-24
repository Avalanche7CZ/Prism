package me.botsko.prism.purge;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class SenderPurgeCallback implements PurgeCallback {
   private CommandSender sender;

   public void cycle(QueryParameters param, int cycle_rows_affected, int total_records_affected, boolean cycle_complete) {
      if (this.sender != null) {
         this.sender.sendMessage(Prism.messenger.playerSubduedHeaderMsg("Purge cycle cleared " + cycle_rows_affected + " records."));
         if (cycle_complete) {
            this.sender.sendMessage(Prism.messenger.playerHeaderMsg(total_records_affected + " records have been purged."));
         }

      }
   }

   public void setSender(CommandSender sender) {
      this.sender = sender;
   }
}
