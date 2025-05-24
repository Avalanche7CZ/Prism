package me.botsko.prism.appliers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.botsko.prism.Prism;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrismApplierCallback implements ApplierCallback {
   public void handle(CommandSender sender, ApplierResult result) {
      HashMap entitiesMoved = result.getEntitiesMoved();
      Iterator i$;
      if (!entitiesMoved.isEmpty()) {
         i$ = entitiesMoved.entrySet().iterator();

         while(i$.hasNext()) {
            Map.Entry entry = (Map.Entry)i$.next();
            if (entry.getKey() instanceof Player) {
               ((Player)entry.getKey()).sendMessage(Prism.messenger.playerSubduedHeaderMsg("Moved you " + entry.getValue() + " blocks to safety due to a rollback."));
            }
         }
      }

      String msg;
      if (result.getProcessType().equals(PrismProcessType.ROLLBACK)) {
         if (!result.isPreview()) {
            msg = result.getChangesApplied() + " reversals.";
            if (result.getChangesSkipped() > 0) {
               msg = msg + " " + result.getChangesSkipped() + " skipped.";
            }

            if (result.getChangesApplied() > 0) {
               msg = msg + ChatColor.GRAY + " It's like it never happened.";
            }

            sender.sendMessage(Prism.messenger.playerHeaderMsg(msg));
         } else {
            msg = "At least " + result.getChangesPlanned() + " planned reversals.";
            if (result.getChangesSkipped() > 0) {
               msg = msg + " " + result.getChangesSkipped() + " skipped.";
            }

            if (result.getChangesPlanned() > 0) {
               msg = msg + ChatColor.GRAY + " Use /prism preview apply to confirm.";
            }

            sender.sendMessage(Prism.messenger.playerHeaderMsg(msg));
            if (result.getChangesPlanned() == 0) {
               sender.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GRAY + "Nothing to rollback, preview canceled for you."));
            }
         }
      }

      if (result.getProcessType().equals(PrismProcessType.RESTORE)) {
         if (!result.isPreview()) {
            msg = result.getChangesApplied() + " events restored.";
            if (result.getChangesSkipped() > 0) {
               msg = msg + " " + result.getChangesSkipped() + " skipped.";
            }

            if (result.getChangesApplied() > 0) {
               msg = msg + ChatColor.GRAY + " It's like it was always there.";
            }

            sender.sendMessage(Prism.messenger.playerHeaderMsg(msg));
         } else {
            msg = result.getChangesPlanned() + " planned restorations.";
            if (result.getChangesSkipped() > 0) {
               msg = msg + " " + result.getChangesSkipped() + " skipped.";
            }

            if (result.getChangesPlanned() > 0) {
               msg = msg + ChatColor.GRAY + " Use /prism preview apply to confirm.";
            }

            sender.sendMessage(Prism.messenger.playerHeaderMsg(msg));
            if (result.getChangesPlanned() == 0) {
               sender.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GRAY + "Nothing to restore, preview canceled for you."));
            }
         }
      }

      if (result.getProcessType().equals(PrismProcessType.UNDO)) {
         msg = result.getChangesApplied() + " changes undone.";
         if (result.getChangesSkipped() > 0) {
            msg = msg + " " + result.getChangesSkipped() + " skipped.";
         }

         if (result.getChangesApplied() > 0) {
            msg = msg + ChatColor.GRAY + " If anyone asks, you never did that.";
         }

         sender.sendMessage(Prism.messenger.playerHeaderMsg(msg));
      }

      i$ = result.getParameters().getSharedPlayers().iterator();

      while(i$.hasNext()) {
         CommandSender sharedPlayer = (CommandSender)i$.next();
         sharedPlayer.sendMessage(Prism.messenger.playerHeaderMsg("A preview is being shared with you: " + result.getParameters().getOriginalCommand()));
      }

   }
}
