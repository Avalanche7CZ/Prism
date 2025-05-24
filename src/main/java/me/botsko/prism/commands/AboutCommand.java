package me.botsko.prism.commands;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.ChatColor;

public class AboutCommand implements SubHandler {
   private final Prism plugin;

   public AboutCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Prism - By " + ChatColor.GOLD + "viveleroi" + ChatColor.GRAY + " v" + this.plugin.getPrismVersion()));
      call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Help: " + ChatColor.WHITE + "/pr ?"));
      call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("IRC: " + ChatColor.WHITE + "irc.esper.net #prism"));
      call.getSender().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Wiki: " + ChatColor.WHITE + "http://discover-prism.com"));
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
