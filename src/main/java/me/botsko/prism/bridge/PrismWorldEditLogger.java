package me.botsko.prism.bridge;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.logging.AbstractLoggingExtent;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import org.bukkit.Location;
import org.bukkit.World;

public class PrismWorldEditLogger extends AbstractLoggingExtent {
   private final Actor player;
   private final World world;

   public PrismWorldEditLogger(Actor player, Extent extent, World world) {
      super(extent);
      this.player = player;
      this.world = world;
   }

   protected void onBlockChange(Vector pt, BaseBlock newBlock) {
      if (Prism.config.getBoolean("prism.tracking.world-edit")) {
         BaseBlock oldBlock = this.getBlock(pt);
         Location loc = new Location(this.world, (double)pt.getBlockX(), (double)pt.getBlockY(), (double)pt.getBlockZ());
         RecordingQueue.addToQueue(ActionFactory.createBlockChange("world-edit", loc, oldBlock.getId(), (byte)oldBlock.getData(), newBlock.getId(), (byte)newBlock.getData(), this.player.getName()));
      }
   }
}
