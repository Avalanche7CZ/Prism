package me.botsko.elixr;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@SuppressWarnings("deprecation")
public class ChatUtils {
   public static void notifyNearby(Location loc, int radius, String msg) {

      Player[] onlinePlayers = Bukkit.getServer().getOnlinePlayers().toArray(new Player[0]);
      for (Player p : onlinePlayers) {
         if (loc.getWorld().equals(p.getWorld()) && loc.distance(p.getLocation()) <= (double)radius) {
            p.sendMessage(msg);
         }
      }
   }
}