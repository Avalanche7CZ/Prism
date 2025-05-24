package me.botsko.prism.bridge;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalPlayer;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WorldEditBridge {
   public static boolean getSelectedArea(Prism plugin, Player player, QueryParameters parameters) {
      Region region;
      try {
         LocalPlayer lp = new BukkitPlayer(Prism.plugin_worldEdit, Prism.plugin_worldEdit.getWorldEdit().getServer(), player);
         World lw = lp.getWorld();
         region = Prism.plugin_worldEdit.getWorldEdit().getSession(lp).getSelection(lw);
      } catch (IncompleteRegionException var15) {
         return false;
      }

      Vector minLoc = new Vector(region.getMinimumPoint().getX(), region.getMinimumPoint().getY(), region.getMinimumPoint().getZ());
      Vector maxLoc = new Vector(region.getMaximumPoint().getX(), region.getMaximumPoint().getY(), region.getMaximumPoint().getZ());
      Selection sel = Prism.plugin_worldEdit.getSelection(player);
      double lRadius = Math.ceil((double)(sel.getLength() / 2));
      double wRadius = Math.ceil((double)(sel.getWidth() / 2));
      double hRadius = Math.ceil((double)(sel.getHeight() / 2));
      String procType = "applier";
      if (parameters.getProcessType().equals(PrismProcessType.LOOKUP)) {
         procType = "lookup";
      }

      int maxRadius = plugin.getConfig().getInt("prism.queries.max-" + procType + "-radius");
      if (maxRadius != 0 && (lRadius > (double)maxRadius || wRadius > (double)maxRadius || hRadius > (double)maxRadius) && !player.hasPermission("prism.override-max-" + procType + "-radius")) {
         return false;
      } else {
         parameters.setWorld(region.getWorld().getName());
         parameters.setMinLocation(minLoc);
         parameters.setMaxLocation(maxLoc);
         return true;
      }
   }
}
