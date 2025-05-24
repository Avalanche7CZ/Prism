package me.botsko.prism.commands;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class FlagsCommand implements SubHandler {
   public void handle(CallInfo call) {
      this.help(call.getSender());
   }

   public List handleComplete(CallInfo call) {
      return null;
   }

   private void help(CommandSender sender) {
      sender.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GOLD + "--- Flags Help ---"));
      sender.sendMessage(Prism.messenger.playerMsg(ChatColor.GRAY + "Flags control how Prism applies a rollback/restore, or formats lookup results."));
      sender.sendMessage(Prism.messenger.playerMsg(ChatColor.GRAY + "Use them after parameters, like /pr l p:viveleroi -extended"));
      Flag[] arr$ = Flag.values();
      int len$ = arr$.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         Flag flag = arr$[i$];
         sender.sendMessage(Prism.messenger.playerMsg(ChatColor.LIGHT_PURPLE + flag.getUsage().replace("_", "-") + ChatColor.WHITE + " " + flag.getDescription()));
      }

   }
}
