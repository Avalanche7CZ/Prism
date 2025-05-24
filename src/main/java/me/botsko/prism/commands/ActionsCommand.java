package me.botsko.prism.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ActionsCommand implements SubHandler {
   public void handle(CallInfo call) {
      this.help(call.getSender());
   }

   public List handleComplete(CallInfo call) {
      return null;
   }

   private void help(CommandSender sender) {
      sender.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GOLD + "--- Actions List ---"));
      ArrayList shortNames = new ArrayList();
      TreeMap actions = Prism.getActionRegistry().getRegisteredAction();
      Iterator i$ = actions.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         if (!((String)entry.getKey()).contains("prism") && !shortNames.contains(((ActionType)entry.getValue()).getShortName())) {
            shortNames.add(((ActionType)entry.getValue()).getShortName());
         }
      }

      Collections.sort(shortNames);
      String actionList = "";
      int i = 1;

      Iterator i$;
      for(i$ = shortNames.iterator(); i$.hasNext(); ++i) {
         String shortName = (String)i$.next();
         actionList = actionList + shortName + (i < shortNames.size() ? ", " : "");
      }

      sender.sendMessage(Prism.messenger.playerMsg(ChatColor.LIGHT_PURPLE + "Action Aliases:" + ChatColor.WHITE + " " + actionList));
      actionList = "";
      i = 1;
      i$ = actions.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         if (!((String)entry.getKey()).contains("prism")) {
            actionList = actionList + (String)entry.getKey() + (i < actions.size() ? ", " : "");
            ++i;
         }
      }

      sender.sendMessage(Prism.messenger.playerMsg(ChatColor.LIGHT_PURPLE + "Full Actions:" + ChatColor.GRAY + " " + actionList));
   }
}
