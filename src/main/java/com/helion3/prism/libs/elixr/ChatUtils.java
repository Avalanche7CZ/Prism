package com.helion3.prism.libs.elixr;

import java.util.Iterator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class ChatUtils {
   public static void notifyNearby(Location loc, int radius, String msg) {
      Iterator var3 = Bukkit.getServer().getOnlinePlayers().iterator();

      while(var3.hasNext()) {
         Player p = (Player)var3.next();
         if (loc.getWorld().equals(p.getWorld()) && loc.distance(p.getLocation()) <= (double)radius) {
            p.sendMessage(msg);
         }
      }

   }
}
