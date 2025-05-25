package me.botsko.elixr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class ChunkUtils {
   public static void resetPreviewBoundaryBlocks(Player player, List blocks) {
      Iterator var2 = blocks.iterator();

      while(var2.hasNext()) {
         Block block = (Block)var2.next();
         player.sendBlockChange(block.getLocation(), block.getType(), block.getData());
      }

   }

   public static void setPreviewBoundaryBlocks(Player player, List blocks, Material m) {
      Iterator var3 = blocks.iterator();

      while(var3.hasNext()) {
         Block block = (Block)var3.next();
         player.sendBlockChange(block.getLocation(), m, (byte)0);
      }

   }

   public static Vector getChunkMinVector(Chunk chunk) {
      int blockMinX = chunk.getX() * 16;
      int blockMinZ = chunk.getZ() * 16;
      return new Vector(blockMinX, 0, blockMinZ);
   }

   public static Vector getChunkMaxVector(Chunk chunk) {
      int blockMinX = chunk.getX() * 16;
      int blockMinZ = chunk.getZ() * 16;
      int blockMaxX = blockMinX + 15;
      int blockMaxZ = blockMinZ + 15;
      return new Vector(blockMaxX, chunk.getWorld().getMaxHeight(), blockMaxZ);
   }

   public static ArrayList getBoundingBlocksAtY(Chunk chunk, int y) {
      int blockMinX = chunk.getX() * 16;
      int blockMinZ = chunk.getZ() * 16;
      int blockMaxX = blockMinX + 15;
      int blockMaxZ = blockMinZ + 15;
      ArrayList blocks = new ArrayList();

      int z;
      for(z = blockMinX; z < blockMaxX; ++z) {
         blocks.add(chunk.getWorld().getBlockAt(z, y, blockMinZ));
      }

      for(z = blockMinX; z < blockMaxX; ++z) {
         blocks.add(chunk.getWorld().getBlockAt(z, y, blockMaxZ));
      }

      for(z = blockMinZ; z < blockMaxZ; ++z) {
         blocks.add(chunk.getWorld().getBlockAt(blockMinX, y, z));
      }

      for(z = blockMinZ; z < blockMaxZ; ++z) {
         blocks.add(chunk.getWorld().getBlockAt(blockMaxX, y, z));
      }

      return blocks;
   }
}
