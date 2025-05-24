package me.botsko.prism.listeners;

import com.helion3.prism.libs.elixr.BlockUtils;
import java.util.Iterator;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import org.bukkit.TreeType;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class PrismWorldEvents implements Listener {
   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onStructureGrow(StructureGrowEvent event) {
      String type = "tree-grow";
      TreeType species = event.getSpecies();
      if (species != null && species.name().toLowerCase().contains("mushroom")) {
         type = "mushroom-grow";
      }

      if (Prism.getIgnore().event(type, event.getWorld())) {
         Iterator i$ = event.getBlocks().iterator();

         while(i$.hasNext()) {
            BlockState block = (BlockState)i$.next();
            if (BlockUtils.isGrowableStructure(block.getType())) {
               String player = "Environment";
               if (event.getPlayer() != null) {
                  player = event.getPlayer().getName();
               }

               RecordingQueue.addToQueue(ActionFactory.createGrow(type, block, player));
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onWorldLoad(WorldLoadEvent event) {
      String worldName = event.getWorld().getName();
      if (!Prism.prismWorlds.containsKey(worldName)) {
         Prism.addWorldName(worldName);
      }

   }
}
