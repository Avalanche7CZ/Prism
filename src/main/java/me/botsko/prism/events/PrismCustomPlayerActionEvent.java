package me.botsko.prism.events;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

public class PrismCustomPlayerActionEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final String plugin_name;
   private final String action_type_name;
   private final Player player;
   private final String message;

   public PrismCustomPlayerActionEvent(Plugin plugin, String action_type_name, Player player, String message) {
      this.plugin_name = plugin.getName();
      this.action_type_name = action_type_name;
      this.player = player;
      this.message = message + ChatColor.GOLD + " [" + this.plugin_name + "]" + ChatColor.DARK_AQUA;
   }

   public String getPluginName() {
      return this.plugin_name;
   }

   public String getActionTypeName() {
      return this.action_type_name;
   }

   public Player getPlayer() {
      return this.player;
   }

   public String getMessage() {
      return this.message;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }
}
