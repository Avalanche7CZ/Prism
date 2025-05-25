package me.botsko.elixr;

import org.bukkit.ChatColor;

public class Messenger {
   protected final String plugin_name;

   public Messenger(String plugin_name) {
      this.plugin_name = plugin_name;
   }

   public String playerHeaderMsg(String msg) {
      return msg != null ? ChatColor.GOLD + "[" + this.plugin_name + "] " + ChatColor.WHITE + msg : "";
   }

   public String playerSuccess(String msg) {
      return msg != null ? ChatColor.GOLD + "[" + this.plugin_name + "] " + ChatColor.GREEN + msg : "";
   }

   public String playerSubduedHeaderMsg(String msg) {
      return msg != null ? ChatColor.GOLD + "[" + this.plugin_name + "] " + ChatColor.GRAY + msg : "";
   }

   public String playerMsg(String msg) {
      return msg != null ? ChatColor.WHITE + msg : "";
   }

   public String playerSubduedMsg(String msg) {
      return msg != null ? ChatColor.GRAY + msg : "";
   }

   public String[] playerMsg(String[] msg) {
      if (msg != null) {
         for(int i = 0; i < msg.length; ++i) {
            msg[i] = this.playerMsg(msg[i]);
         }
      }

      return msg;
   }

   public String playerHelp(String cmd, String help) {
      return ChatColor.GRAY + "/" + cmd + ChatColor.WHITE + " - " + help;
   }

   public String playerError(String msg) {
      return msg != null ? ChatColor.GOLD + "[" + this.plugin_name + "] " + ChatColor.RED + msg : "";
   }
}
