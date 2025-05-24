package me.botsko.prism.utils;

import java.util.ArrayList;
import java.util.Arrays;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class BlockUtils extends com.helion3.prism.libs.elixr.BlockUtils {
   public static int blockIdMustRecordAs(int block_id) {
      return block_id == 62 ? 61 : block_id;
   }

   public static ArrayList removeMaterialFromRadius(Material mat, Location loc, int radius) {
      Material[] materials = new Material[]{mat};
      return removeMaterialsFromRadius(materials, loc, radius);
   }

   public static ArrayList removeMaterialsFromRadius(Material[] materials, Location loc, int radius) {
      ArrayList blockStateChanges = new ArrayList();
      if (loc != null && radius > 0 && materials != null && materials.length > 0) {
         int x1 = loc.getBlockX();
         int y1 = loc.getBlockY();
         int z1 = loc.getBlockZ();
         World world = loc.getWorld();

         for(int x = x1 - radius; x <= x1 + radius; ++x) {
            for(int y = y1 - radius; y <= y1 + radius; ++y) {
               for(int z = z1 - radius; z <= z1 + radius; ++z) {
                  loc = new Location(world, (double)x, (double)y, (double)z);
                  Block b = loc.getBlock();
                  if (!b.getType().equals(Material.AIR) && Arrays.asList(materials).contains(loc.getBlock().getType())) {
                     BlockState originalBlock = loc.getBlock().getState();
                     loc.getBlock().setType(Material.AIR);
                     BlockState newBlock = loc.getBlock().getState();
                     blockStateChanges.add(new BlockStateChange(originalBlock, newBlock));
                  }
               }
            }
         }
      }

      return blockStateChanges;
   }

   public static ArrayList extinguish(Location loc, int radius) {
      return removeMaterialFromRadius(Material.FIRE, loc, radius);
   }

   public static ArrayList drain(Location loc, int radius) {
      Material[] materials = new Material[]{Material.LAVA, Material.STATIONARY_LAVA, Material.WATER, Material.STATIONARY_WATER};
      return removeMaterialsFromRadius(materials, loc, radius);
   }

   public static ArrayList drainlava(Location loc, int radius) {
      Material[] materials = new Material[]{Material.LAVA, Material.STATIONARY_LAVA};
      return removeMaterialsFromRadius(materials, loc, radius);
   }

   public static ArrayList drainwater(Location loc, int radius) {
      Material[] materials = new Material[]{Material.WATER, Material.STATIONARY_WATER};
      return removeMaterialsFromRadius(materials, loc, radius);
   }
}
