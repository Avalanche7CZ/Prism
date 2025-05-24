package me.botsko.prism.commandlibs;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CallInfo {
   private final CommandSender sender;
   private final Player player;
   private final String[] args;

   public CallInfo(CommandSender sender, Player player, String[] args) {
      this.sender = sender;
      this.player = player;
      this.args = args;
   }

   public Player getPlayer() {
      return this.player;
   }

   public CommandSender getSender() {
      return this.sender;
   }

   public String getArg(int n) {
      return this.args[n];
   }

   public String[] getArgs() {
      return this.args;
   }
}
