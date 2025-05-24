package com.helion3.prism.libs.elixr;

import org.bukkit.Location;
import org.bukkit.World.Environment;

public class WorldUtils {
   public static void thunder(Location loc) {
      loc.setY(350.0);
      if (loc.getWorld().getEnvironment() == Environment.NORMAL) {
         loc.getWorld().strikeLightning(loc);
      }

   }
}
