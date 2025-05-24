package me.botsko.prism.parameters;

import java.util.regex.Pattern;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WorldParameter extends SimplePrismParameterHandler {
   public WorldParameter() {
      super("World", Pattern.compile("[^\\s]+"), "w");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      String worldName = input;
      if (input.equalsIgnoreCase("current")) {
         if (sender instanceof Player) {
            worldName = ((Player)sender).getWorld().getName();
         } else {
            sender.sendMessage(Prism.messenger.playerError("Can't use the current world since you're not a player. Using default world."));
            worldName = ((World)Bukkit.getServer().getWorlds().get(0)).getName();
         }
      }

      query.setWorld(worldName);
   }

   public void defaultTo(QueryParameters query, CommandSender sender) {
      if (!query.getProcessType().equals(PrismProcessType.DELETE)) {
         if (sender instanceof Player && !query.allowsNoRadius()) {
            query.setWorld(((Player)sender).getWorld().getName());
         }

      }
   }
}
