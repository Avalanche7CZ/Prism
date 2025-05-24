package me.botsko.prism.commands;

import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.settings.Settings;
import me.botsko.prism.wands.Wand;
import org.bukkit.ChatColor;

public class ResetmyCommand implements SubHandler {
   private final Prism plugin;

   public ResetmyCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      String setType = null;
      if (call.getArgs().length >= 2) {
         setType = call.getArg(1);
      }

      if (setType != null && !setType.equalsIgnoreCase("wand")) {
         call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid arguments. Use /prism ? for help."));
      } else {
         if (!this.plugin.getConfig().getBoolean("prism.wands.allow-user-override")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Sorry, but personalizing the wand is currently not allowed."));
         }

         if (!call.getPlayer().hasPermission("prism.rollback") && !call.getPlayer().hasPermission("prism.restore") && !call.getPlayer().hasPermission("prism.wand.*") && !call.getPlayer().hasPermission("prism.wand.inspect") && !call.getPlayer().hasPermission("prism.wand.profile") && !call.getPlayer().hasPermission("prism.wand.rollback") && !call.getPlayer().hasPermission("prism.wand.restore")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
         } else {
            if (Prism.playersWithActiveTools.containsKey(call.getPlayer().getName())) {
               Wand oldwand = (Wand)Prism.playersWithActiveTools.get(call.getPlayer().getName());
               oldwand.disable(call.getPlayer());
               Prism.playersWithActiveTools.remove(call.getPlayer().getName());
               call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Current wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
            }

            Settings.deleteSetting("wand.item", call.getPlayer());
            Settings.deleteSetting("wand.mode", call.getPlayer());
            call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Your personal wand settings have been reset to server defaults."));
         }
      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
