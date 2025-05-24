package me.botsko.prism.commandlibs;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Executor implements CommandExecutor, TabCompleter {
   public final Plugin plugin;
   public String mode = "command";
   public String defaultSubcommand = "default";
   public final Map subcommands = new LinkedHashMap();

   public Executor(Plugin plugin, String mode, String perm_base) {
      this.mode = mode == null ? "command" : mode;
      this.plugin = plugin;
   }

   public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
      Player player = null;
      if (sender instanceof Player) {
         player = (Player)sender;
      }

      String subcommandName;
      if (this.mode.equals("subcommand") && args.length > 0) {
         subcommandName = args[0].toLowerCase();
      } else {
         subcommandName = cmd.getName();
      }

      String currentMode = this.mode;
      SubCommand sub = (SubCommand)this.subcommands.get(subcommandName);
      if (sub == null) {
         sub = (SubCommand)this.subcommands.get(this.defaultSubcommand);
         if (sub == null) {
            sender.sendMessage("Invalid command");
            return true;
         }

         currentMode = "command";
      }

      if (player != null && !sub.playerHasPermission(player)) {
         sender.sendMessage("You do not have permission to use this command");
         return true;
      } else if ((!currentMode.equals("subcommand") || args.length - 1 >= sub.getMinArgs()) && (!currentMode.equals("command") || args.length >= sub.getMinArgs())) {
         if (!(sender instanceof Player) && !sub.isConsoleAllowed()) {
            sender.sendMessage("You must be in-game to use this command");
            return true;
         } else {
            CallInfo call = new CallInfo(sender, player, args);
            sub.getHandler().handle(call);
            return true;
         }
      } else {
         sender.sendMessage("You're missing arguments for this command");
         return true;
      }
   }

   protected SubCommand addSub(String[] commandAliases, String[] permissionNodes, SubHandler handler) {
      SubCommand cmd = new SubCommand(commandAliases, permissionNodes, handler);
      String[] arr$ = commandAliases;
      int len$ = commandAliases.length;

      for(int i$ = 0; i$ < len$; ++i$) {
         String alias = arr$[i$];
         this.subcommands.put(alias, cmd);
      }

      return cmd;
   }

   protected SubCommand addSub(String[] commandAliases, String[] permissionNodes) {
      return this.addSub(commandAliases, permissionNodes, (SubHandler)null);
   }

   protected SubCommand addSub(String[] commandAliases, String permissionNode) {
      return this.addSub(commandAliases, new String[]{permissionNode}, (SubHandler)null);
   }

   protected SubCommand addSub(String commandAlias, String[] permissionNodes) {
      return this.addSub(new String[]{commandAlias}, permissionNodes, (SubHandler)null);
   }

   protected SubCommand addSub(String commandAlias, String permissionNode) {
      return this.addSub(new String[]{commandAlias}, new String[]{permissionNode}, (SubHandler)null);
   }

   public List onTabComplete(CommandSender sender, Command cmd, String s, String[] args) {
      Player player = null;
      if (sender instanceof Player) {
         player = (Player)sender;
      }

      String subcommandName;
      if (this.mode.equals("subcommand") && args.length > 0) {
         subcommandName = args[0].toLowerCase();
         if (args.length == 1) {
            return MiscUtils.getStartingWith(subcommandName, this.subcommands.keySet());
         }
      } else {
         subcommandName = cmd.getName();
      }

      String currentMode = this.mode;
      SubCommand sub = (SubCommand)this.subcommands.get(subcommandName);
      if (sub == null) {
         sub = (SubCommand)this.subcommands.get(this.defaultSubcommand);
         if (sub == null) {
            sender.sendMessage("Invalid command");
            return null;
         }

         currentMode = "command";
      }

      if (player != null && !sub.playerHasPermission(player)) {
         sender.sendMessage("You do not have permission to use this command");
         return null;
      } else if ((!currentMode.equals("subcommand") || args.length - 1 >= sub.getMinArgs()) && (!currentMode.equals("command") || args.length >= sub.getMinArgs())) {
         if (!(sender instanceof Player) && !sub.isConsoleAllowed()) {
            sender.sendMessage("You must be in-game to use this command");
            return null;
         } else {
            CallInfo call = new CallInfo(sender, player, args);
            return sub.getHandler().handleComplete(call);
         }
      } else {
         sender.sendMessage("You're missing arguments for this command");
         return null;
      }
   }
}
