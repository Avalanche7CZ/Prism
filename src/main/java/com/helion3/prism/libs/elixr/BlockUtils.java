package com.helion3.prism.libs.elixr;

import java.util.ArrayList;
import java.util.Iterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.material.Bed;

public class BlockUtils {
   // $FF: synthetic field
   private static int[] $SWITCH_TABLE$org$bukkit$Material;

   public static boolean isAcceptableForBlockPlace(Material m) {
      switch (m) {
         case AIR:
         case WATER:
         case STATIONARY_WATER:
         case LAVA:
         case STATIONARY_LAVA:
         case SAND:
         case GRAVEL:
         case LONG_GRASS:
         case FIRE:
         case SNOW:
         case SNOW_BLOCK:
            return true;
         default:
            return false;
      }
   }

   public static ArrayList findFallingBlocksAboveBlock(Block block) {
      ArrayList falling_blocks = new ArrayList();
      Block above = block.getRelative(BlockFace.UP);
      if (isFallingBlock(above)) {
         falling_blocks.add(above);
         ArrayList fallingBlocksAbove = findFallingBlocksAboveBlock(above);
         if (fallingBlocksAbove.size() > 0) {
            Iterator var4 = fallingBlocksAbove.iterator();

            while(var4.hasNext()) {
               Block _temp = (Block)var4.next();
               falling_blocks.add(_temp);
            }
         }
      }

      return falling_blocks;
   }

   public static boolean isFallingBlock(Block block) {
      Material m = block.getType();
      return m.equals(Material.SAND) || m.equals(Material.GRAVEL) || m.equals(Material.ANVIL);
   }

   public static ArrayList findSideFaceAttachedBlocks(Block block) {
      ArrayList detaching_blocks = new ArrayList();
      Block blockToCheck = block.getRelative(BlockFace.EAST);
      if (isSideFaceDetachableMaterial(blockToCheck.getType())) {
         detaching_blocks.add(blockToCheck);
      }

      blockToCheck = block.getRelative(BlockFace.WEST);
      if (isSideFaceDetachableMaterial(blockToCheck.getType())) {
         detaching_blocks.add(blockToCheck);
      }

      blockToCheck = block.getRelative(BlockFace.NORTH);
      if (isSideFaceDetachableMaterial(blockToCheck.getType())) {
         detaching_blocks.add(blockToCheck);
      }

      blockToCheck = block.getRelative(BlockFace.SOUTH);
      if (isSideFaceDetachableMaterial(blockToCheck.getType())) {
         detaching_blocks.add(blockToCheck);
      }

      return detaching_blocks;
   }

   public static Block findFirstSurroundingBlockOfType(Block source, Material surrounding) {
      Block blockToCheck = source.getRelative(BlockFace.EAST);
      if (blockToCheck.getType().equals(surrounding)) {
         return blockToCheck;
      } else {
         blockToCheck = source.getRelative(BlockFace.WEST);
         if (blockToCheck.getType().equals(surrounding)) {
            return blockToCheck;
         } else {
            blockToCheck = source.getRelative(BlockFace.NORTH);
            if (blockToCheck.getType().equals(surrounding)) {
               return blockToCheck;
            } else {
               blockToCheck = source.getRelative(BlockFace.SOUTH);
               return blockToCheck.getType().equals(surrounding) ? blockToCheck : null;
            }
         }
      }
   }

   public static boolean isSideFaceDetachableMaterial(Material m) {
      return m.equals(Material.WALL_SIGN) || m.equals(Material.TORCH) || m.equals(Material.LEVER) || m.equals(Material.WOOD_BUTTON) || m.equals(Material.STONE_BUTTON) || m.equals(Material.LADDER) || m.equals(Material.VINE) || m.equals(Material.COCOA) || m.equals(Material.PORTAL) || m.equals(Material.PISTON_EXTENSION) || m.equals(Material.PISTON_MOVING_PIECE) || m.equals(Material.PISTON_BASE) || m.equals(Material.PISTON_STICKY_BASE) || m.equals(Material.REDSTONE_TORCH_OFF) || m.equals(Material.REDSTONE_TORCH_ON) || m.equals(Material.TRIPWIRE_HOOK) || m.equals(Material.TRAP_DOOR);
   }

   public static ArrayList findTopFaceAttachedBlocks(Block block) {
      ArrayList detaching_blocks = new ArrayList();
      Block blockToCheck = block.getRelative(BlockFace.UP);
      if (isTopFaceDetachableMaterial(blockToCheck.getType())) {
         detaching_blocks.add(blockToCheck);
         if (blockToCheck.getType().equals(Material.CACTUS) || blockToCheck.getType().equals(Material.SUGAR_CANE_BLOCK)) {
            ArrayList additionalBlocks = findTopFaceAttachedBlocks(blockToCheck);
            if (!additionalBlocks.isEmpty()) {
               Iterator var4 = additionalBlocks.iterator();

               while(var4.hasNext()) {
                  Block _temp = (Block)var4.next();
                  detaching_blocks.add(_temp);
               }
            }
         }
      }

      return detaching_blocks;
   }

   public static boolean isTopFaceDetachableMaterial(Material m) {
      switch (m) {
         case SAPLING:
         case POWERED_RAIL:
         case DETECTOR_RAIL:
         case LONG_GRASS:
         case DEAD_BUSH:
         case YELLOW_FLOWER:
         case RED_ROSE:
         case BROWN_MUSHROOM:
         case RED_MUSHROOM:
         case TORCH:
         case REDSTONE_WIRE:
         case CROPS:
         case SIGN_POST:
         case WOODEN_DOOR:
         case RAILS:
         case LEVER:
         case STONE_PLATE:
         case IRON_DOOR_BLOCK:
         case WOOD_PLATE:
         case REDSTONE_TORCH_OFF:
         case REDSTONE_TORCH_ON:
         case SNOW:
         case CACTUS:
         case SUGAR_CANE_BLOCK:
         case PORTAL:
         case DIODE_BLOCK_OFF:
         case DIODE_BLOCK_ON:
         case PUMPKIN_STEM:
         case MELON_STEM:
         case WATER_LILY:
         case NETHER_WARTS:
         case TRIPWIRE:
         case FLOWER_POT:
         case CARROT:
         case POTATO:
         case SKULL:
         case GOLD_PLATE:
         case IRON_PLATE:
         case REDSTONE_COMPARATOR_OFF:
         case REDSTONE_COMPARATOR_ON:
         case ACTIVATOR_RAIL:
         case DOUBLE_PLANT:
         case WHEAT:
         case SIGN:
         case WOOD_DOOR:
         case IRON_DOOR:
         case REDSTONE:
         case DIODE:
            return true;
         default:
            return false;
      }
   }

   public static boolean materialMeansBlockDetachment(Material m) {
      switch (m) {
         case AIR:
         case WATER:
         case STATIONARY_WATER:
         case LAVA:
         case STATIONARY_LAVA:
         case FIRE:
            return true;
         default:
            return false;
      }
   }

   public static ArrayList findHangingEntities(Block block) {
      ArrayList entities = new ArrayList();
      Entity[] foundEntities = block.getChunk().getEntities();
      if (foundEntities.length > 0) {
         Entity[] var3 = foundEntities;
         int var4 = foundEntities.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            Entity e = var3[var5];
            if (block.getWorld().equals(e.getWorld()) && block.getLocation().distance(e.getLocation()) < 2.0 && isHangingEntity(e)) {
               entities.add(e);
            }
         }
      }

      return entities;
   }

   public static boolean isHangingEntity(Entity entity) {
      EntityType e = entity.getType();
      return e.equals(EntityType.ITEM_FRAME) || e.equals(EntityType.PAINTING);
   }

   public static Block getSiblingForDoubleLengthBlock(Block block) {
      if (!block.getType().equals(Material.WOODEN_DOOR) && !block.getType().equals(Material.IRON_DOOR_BLOCK) || block.getData() != 8 && block.getData() != 9) {
         if (block.getType().equals(Material.BED_BLOCK)) {
            Bed b = (Bed)block.getState().getData();
            if (b.isHeadOfBed()) {
               return block.getRelative(b.getFacing().getOppositeFace());
            }
         }

         return !block.getType().equals(Material.CHEST) && !block.getType().equals(Material.TRAPPED_CHEST) ? null : findFirstSurroundingBlockOfType(block, block.getType());
      } else {
         return block.getRelative(BlockFace.DOWN);
      }
   }

   public static void properlySetDoor(Block originalBlock, int typeid, byte subid) {
      Block aboveOrBelow;
      if (subid != 8 && subid != 9) {
         aboveOrBelow = originalBlock.getRelative(BlockFace.UP);
         aboveOrBelow.setTypeId(typeid);
         Block left = null;
         switch (subid) {
            case 0:
               left = originalBlock.getRelative(BlockFace.NORTH);
               break;
            case 1:
               left = originalBlock.getRelative(BlockFace.EAST);
               break;
            case 2:
               left = originalBlock.getRelative(BlockFace.SOUTH);
               break;
            case 3:
               left = originalBlock.getRelative(BlockFace.WEST);
         }

         if (aboveOrBelow != null) {
            if (left != null && isDoor(left.getType())) {
               aboveOrBelow.setData((byte)9);
            } else {
               aboveOrBelow.setData((byte)8);
            }
         }
      } else {
         aboveOrBelow = originalBlock.getRelative(BlockFace.DOWN);
         aboveOrBelow.setTypeId(typeid);
         aboveOrBelow.setData((byte)0);
      }

   }

   public static boolean isDoor(Material m) {
      switch (m) {
         case WOODEN_DOOR:
         case IRON_DOOR_BLOCK:
         case WOOD_DOOR:
         case IRON_DOOR:
            return true;
         default:
            return false;
      }
   }

   public static void properlySetBed(Block originalBlock, int typeid, byte subid) {
      Block top = null;
      int new_subid = 0;
      switch (subid) {
         case 0:
            top = originalBlock.getRelative(BlockFace.SOUTH);
            new_subid = 8;
            break;
         case 1:
            top = originalBlock.getRelative(BlockFace.WEST);
            new_subid = 9;
            break;
         case 2:
            top = originalBlock.getRelative(BlockFace.NORTH);
            new_subid = 10;
            break;
         case 3:
            top = originalBlock.getRelative(BlockFace.EAST);
            new_subid = 11;
      }

      if (top != null) {
         top.setTypeId(typeid);
         top.setData((byte)new_subid);
      } else {
         System.out.println("Error setting bed: block top location was illegal. Data value: " + subid + " New data value: " + new_subid);
      }

   }

   public static void properlySetDoublePlant(Block originalBlock, int typeid, byte subid) {
      if (originalBlock.getType().equals(Material.DOUBLE_PLANT)) {
         Block above = originalBlock.getRelative(BlockFace.UP);
         if (isAcceptableForBlockPlace(above.getType())) {
            if (typeid == 175 && subid < 8) {
               subid = 8;
            }

            above.setTypeId(typeid);
            above.setData(subid);
         }
      }
   }

   public static boolean canFlowBreakMaterial(Material m) {
      switch (m) {
         case SAPLING:
         case POWERED_RAIL:
         case DETECTOR_RAIL:
         case LONG_GRASS:
         case DEAD_BUSH:
         case YELLOW_FLOWER:
         case RED_ROSE:
         case BROWN_MUSHROOM:
         case RED_MUSHROOM:
         case TORCH:
         case REDSTONE_WIRE:
         case CROPS:
         case SIGN_POST:
         case WOODEN_DOOR:
         case LADDER:
         case RAILS:
         case LEVER:
         case STONE_PLATE:
         case IRON_DOOR_BLOCK:
         case WOOD_PLATE:
         case REDSTONE_TORCH_OFF:
         case REDSTONE_TORCH_ON:
         case CACTUS:
         case SUGAR_CANE_BLOCK:
         case DIODE_BLOCK_OFF:
         case DIODE_BLOCK_ON:
         case PUMPKIN_STEM:
         case MELON_STEM:
         case VINE:
         case WATER_LILY:
         case NETHER_WARTS:
         case COCOA:
         case TRIPWIRE_HOOK:
         case TRIPWIRE:
         case FLOWER_POT:
         case CARROT:
         case POTATO:
         case SKULL:
         case REDSTONE_COMPARATOR_OFF:
         case REDSTONE_COMPARATOR_ON:
         case ACTIVATOR_RAIL:
         case DOUBLE_PLANT:
         case WHEAT:
         case SIGN:
         case WOOD_DOOR:
         case IRON_DOOR:
         case REDSTONE:
         case DIODE:
            return true;
         default:
            return false;
      }
   }

   public static boolean materialRequiresSoil(Material m) {
      switch (m) {
         case CROPS:
         case PUMPKIN_STEM:
         case MELON_STEM:
         case CARROT:
         case POTATO:
         case WHEAT:
            return true;
         default:
            return false;
      }
   }

   public static ArrayList findConnectedBlocksOfType(Material type, Block currBlock, ArrayList foundLocations) {
      ArrayList foundBlocks = new ArrayList();
      if (foundLocations == null) {
         foundLocations = new ArrayList();
      }

      foundLocations.add(currBlock.getLocation());

      for(int x = -1; x <= 1; ++x) {
         for(int z = -1; z <= 1; ++z) {
            for(int y = -1; y <= 1; ++y) {
               Block newblock = currBlock.getRelative(x, y, z);
               if (newblock.getType() == type && !foundLocations.contains(newblock.getLocation())) {
                  foundBlocks.add(newblock);
                  ArrayList additionalBlocks = findConnectedBlocksOfType(type, newblock, foundLocations);
                  if (additionalBlocks.size() > 0) {
                     foundBlocks.addAll(additionalBlocks);
                  }
               }
            }
         }
      }

      return foundBlocks;
   }

   public static Block getFirstBlockOfMaterialBelow(Material m, Location loc) {
      for(int y = (int)loc.getY(); y > 0; --y) {
         loc.setY((double)y);
         if (loc.getBlock().getType().equals(m)) {
            return loc.getBlock();
         }
      }

      return null;
   }

   public static boolean isGrowableStructure(Material m) {
      switch (m) {
         case LOG:
         case LEAVES:
         case HUGE_MUSHROOM_1:
         case HUGE_MUSHROOM_2:
            return true;
         default:
            return false;
      }
   }

   public static boolean areBlockIdsSameCoreItem(int id1, int id2) {
      if (id1 == id2) {
         return true;
      } else if (id1 != 2 && id1 != 3 || id2 != 2 && id2 != 3) {
         if (id1 != 110 && id1 != 3 || id2 != 110 && id2 != 3) {
            if (id1 != 8 && id1 != 9 || id2 != 8 && id2 != 9) {
               if (id1 != 10 && id1 != 11 || id2 != 10 && id2 != 11) {
                  if (id1 != 75 && id1 != 76 || id2 != 75 && id2 != 76) {
                     if (id1 != 93 && id1 != 94 || id2 != 93 && id2 != 94) {
                        if (id1 != 123 && id1 != 124 || id2 != 123 && id2 != 124) {
                           if (id1 != 61 && id1 != 62 || id2 != 61 && id2 != 62) {
                              return (id1 == 149 || id1 == 150) && (id2 == 149 || id2 == 150);
                           } else {
                              return true;
                           }
                        } else {
                           return true;
                        }
                     } else {
                        return true;
                     }
                  } else {
                     return true;
                  }
               } else {
                  return true;
               }
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   // $FF: synthetic method
   static int[] $SWITCH_TABLE$org$bukkit$Material() {
      int[] var10000 = $SWITCH_TABLE$org$bukkit$Material;
      if (var10000 != null) {
         return var10000;
      } else {
         int[] var0 = new int[Material.values().length];

         try {
            var0[Material.ACACIA_STAIRS.ordinal()] = 165;
         } catch (NoSuchFieldError var343) {
         }

         try {
            var0[Material.ACTIVATOR_RAIL.ordinal()] = 159;
         } catch (NoSuchFieldError var342) {
         }

         try {
            var0[Material.AIR.ordinal()] = 1;
         } catch (NoSuchFieldError var341) {
         }

         try {
            var0[Material.ANVIL.ordinal()] = 147;
         } catch (NoSuchFieldError var340) {
         }

         try {
            var0[Material.APPLE.ordinal()] = 177;
         } catch (NoSuchFieldError var339) {
         }

         try {
            var0[Material.ARROW.ordinal()] = 179;
         } catch (NoSuchFieldError var338) {
         }

         try {
            var0[Material.BAKED_POTATO.ordinal()] = 310;
         } catch (NoSuchFieldError var337) {
         }

         try {
            var0[Material.BEACON.ordinal()] = 140;
         } catch (NoSuchFieldError var336) {
         }

         try {
            var0[Material.BED.ordinal()] = 272;
         } catch (NoSuchFieldError var335) {
         }

         try {
            var0[Material.BEDROCK.ordinal()] = 8;
         } catch (NoSuchFieldError var334) {
         }

         try {
            var0[Material.BED_BLOCK.ordinal()] = 27;
         } catch (NoSuchFieldError var333) {
         }

         try {
            var0[Material.BIRCH_WOOD_STAIRS.ordinal()] = 137;
         } catch (NoSuchFieldError var332) {
         }

         try {
            var0[Material.BLAZE_POWDER.ordinal()] = 294;
         } catch (NoSuchFieldError var331) {
         }

         try {
            var0[Material.BLAZE_ROD.ordinal()] = 286;
         } catch (NoSuchFieldError var330) {
         }

         try {
            var0[Material.BOAT.ordinal()] = 250;
         } catch (NoSuchFieldError var329) {
         }

         try {
            var0[Material.BONE.ordinal()] = 269;
         } catch (NoSuchFieldError var328) {
         }

         try {
            var0[Material.BOOK.ordinal()] = 257;
         } catch (NoSuchFieldError var327) {
         }

         try {
            var0[Material.BOOKSHELF.ordinal()] = 48;
         } catch (NoSuchFieldError var326) {
         }

         try {
            var0[Material.BOOK_AND_QUILL.ordinal()] = 303;
         } catch (NoSuchFieldError var325) {
         }

         try {
            var0[Material.BOW.ordinal()] = 178;
         } catch (NoSuchFieldError var324) {
         }

         try {
            var0[Material.BOWL.ordinal()] = 198;
         } catch (NoSuchFieldError var323) {
         }

         try {
            var0[Material.BREAD.ordinal()] = 214;
         } catch (NoSuchFieldError var322) {
         }

         try {
            var0[Material.BREWING_STAND.ordinal()] = 119;
         } catch (NoSuchFieldError var321) {
         }

         try {
            var0[Material.BREWING_STAND_ITEM.ordinal()] = 296;
         } catch (NoSuchFieldError var320) {
         }

         try {
            var0[Material.BRICK.ordinal()] = 46;
         } catch (NoSuchFieldError var319) {
         }

         try {
            var0[Material.BRICK_STAIRS.ordinal()] = 110;
         } catch (NoSuchFieldError var318) {
         }

         try {
            var0[Material.BROWN_MUSHROOM.ordinal()] = 40;
         } catch (NoSuchFieldError var317) {
         }

         try {
            var0[Material.BUCKET.ordinal()] = 242;
         } catch (NoSuchFieldError var316) {
         }

         try {
            var0[Material.BURNING_FURNACE.ordinal()] = 63;
         } catch (NoSuchFieldError var315) {
         }

         try {
            var0[Material.CACTUS.ordinal()] = 82;
         } catch (NoSuchFieldError var314) {
         }

         try {
            var0[Material.CAKE.ordinal()] = 271;
         } catch (NoSuchFieldError var313) {
         }

         try {
            var0[Material.CAKE_BLOCK.ordinal()] = 93;
         } catch (NoSuchFieldError var312) {
         }

         try {
            var0[Material.CARPET.ordinal()] = 168;
         } catch (NoSuchFieldError var311) {
         }

         try {
            var0[Material.CARROT.ordinal()] = 143;
         } catch (NoSuchFieldError var310) {
         }

         try {
            var0[Material.CARROT_ITEM.ordinal()] = 308;
         } catch (NoSuchFieldError var309) {
         }

         try {
            var0[Material.CARROT_STICK.ordinal()] = 315;
         } catch (NoSuchFieldError var308) {
         }

         try {
            var0[Material.CAULDRON.ordinal()] = 120;
         } catch (NoSuchFieldError var307) {
         }

         try {
            var0[Material.CAULDRON_ITEM.ordinal()] = 297;
         } catch (NoSuchFieldError var306) {
         }

         try {
            var0[Material.CHAINMAIL_BOOTS.ordinal()] = 222;
         } catch (NoSuchFieldError var305) {
         }

         try {
            var0[Material.CHAINMAIL_CHESTPLATE.ordinal()] = 220;
         } catch (NoSuchFieldError var304) {
         }

         try {
            var0[Material.CHAINMAIL_HELMET.ordinal()] = 219;
         } catch (NoSuchFieldError var303) {
         }

         try {
            var0[Material.CHAINMAIL_LEGGINGS.ordinal()] = 221;
         } catch (NoSuchFieldError var302) {
         }

         try {
            var0[Material.CHEST.ordinal()] = 55;
         } catch (NoSuchFieldError var301) {
         }

         try {
            var0[Material.CLAY.ordinal()] = 83;
         } catch (NoSuchFieldError var300) {
         }

         try {
            var0[Material.CLAY_BALL.ordinal()] = 254;
         } catch (NoSuchFieldError var299) {
         }

         try {
            var0[Material.CLAY_BRICK.ordinal()] = 253;
         } catch (NoSuchFieldError var298) {
         }

         try {
            var0[Material.COAL.ordinal()] = 180;
         } catch (NoSuchFieldError var297) {
         }

         try {
            var0[Material.COAL_BLOCK.ordinal()] = 170;
         } catch (NoSuchFieldError var296) {
         }

         try {
            var0[Material.COAL_ORE.ordinal()] = 17;
         } catch (NoSuchFieldError var295) {
         }

         try {
            var0[Material.COBBLESTONE.ordinal()] = 5;
         } catch (NoSuchFieldError var294) {
         }

         try {
            var0[Material.COBBLESTONE_STAIRS.ordinal()] = 68;
         } catch (NoSuchFieldError var293) {
         }

         try {
            var0[Material.COBBLE_WALL.ordinal()] = 141;
         } catch (NoSuchFieldError var292) {
         }

         try {
            var0[Material.COCOA.ordinal()] = 129;
         } catch (NoSuchFieldError var291) {
         }

         try {
            var0[Material.COMMAND.ordinal()] = 139;
         } catch (NoSuchFieldError var290) {
         }

         try {
            var0[Material.COMMAND_MINECART.ordinal()] = 331;
         } catch (NoSuchFieldError var289) {
         }

         try {
            var0[Material.COMPASS.ordinal()] = 262;
         } catch (NoSuchFieldError var288) {
         }

         try {
            var0[Material.COOKED_BEEF.ordinal()] = 281;
         } catch (NoSuchFieldError var287) {
         }

         try {
            var0[Material.COOKED_CHICKEN.ordinal()] = 283;
         } catch (NoSuchFieldError var286) {
         }

         try {
            var0[Material.COOKED_FISH.ordinal()] = 267;
         } catch (NoSuchFieldError var285) {
         }

         try {
            var0[Material.COOKIE.ordinal()] = 274;
         } catch (NoSuchFieldError var284) {
         }

         try {
            var0[Material.CROPS.ordinal()] = 60;
         } catch (NoSuchFieldError var283) {
         }

         try {
            var0[Material.DARK_OAK_STAIRS.ordinal()] = 166;
         } catch (NoSuchFieldError var282) {
         }

         try {
            var0[Material.DAYLIGHT_DETECTOR.ordinal()] = 153;
         } catch (NoSuchFieldError var281) {
         }

         try {
            var0[Material.DEAD_BUSH.ordinal()] = 33;
         } catch (NoSuchFieldError var280) {
         }

         try {
            var0[Material.DETECTOR_RAIL.ordinal()] = 29;
         } catch (NoSuchFieldError var279) {
         }

         try {
            var0[Material.DIAMOND.ordinal()] = 181;
         } catch (NoSuchFieldError var278) {
         }

         try {
            var0[Material.DIAMOND_AXE.ordinal()] = 196;
         } catch (NoSuchFieldError var277) {
         }

         try {
            var0[Material.DIAMOND_BARDING.ordinal()] = 328;
         } catch (NoSuchFieldError var276) {
         }

         try {
            var0[Material.DIAMOND_BLOCK.ordinal()] = 58;
         } catch (NoSuchFieldError var275) {
         }

         try {
            var0[Material.DIAMOND_BOOTS.ordinal()] = 230;
         } catch (NoSuchFieldError var274) {
         }

         try {
            var0[Material.DIAMOND_CHESTPLATE.ordinal()] = 228;
         } catch (NoSuchFieldError var273) {
         }

         try {
            var0[Material.DIAMOND_HELMET.ordinal()] = 227;
         } catch (NoSuchFieldError var272) {
         }

         try {
            var0[Material.DIAMOND_HOE.ordinal()] = 210;
         } catch (NoSuchFieldError var271) {
         }

         try {
            var0[Material.DIAMOND_LEGGINGS.ordinal()] = 229;
         } catch (NoSuchFieldError var270) {
         }

         try {
            var0[Material.DIAMOND_ORE.ordinal()] = 57;
         } catch (NoSuchFieldError var269) {
         }

         try {
            var0[Material.DIAMOND_PICKAXE.ordinal()] = 195;
         } catch (NoSuchFieldError var268) {
         }

         try {
            var0[Material.DIAMOND_SPADE.ordinal()] = 194;
         } catch (NoSuchFieldError var267) {
         }

         try {
            var0[Material.DIAMOND_SWORD.ordinal()] = 193;
         } catch (NoSuchFieldError var266) {
         }

         try {
            var0[Material.DIODE.ordinal()] = 273;
         } catch (NoSuchFieldError var265) {
         }

         try {
            var0[Material.DIODE_BLOCK_OFF.ordinal()] = 94;
         } catch (NoSuchFieldError var264) {
         }

         try {
            var0[Material.DIODE_BLOCK_ON.ordinal()] = 95;
         } catch (NoSuchFieldError var263) {
         }

         try {
            var0[Material.DIRT.ordinal()] = 4;
         } catch (NoSuchFieldError var262) {
         }

         try {
            var0[Material.DISPENSER.ordinal()] = 24;
         } catch (NoSuchFieldError var261) {
         }

         try {
            var0[Material.DOUBLE_PLANT.ordinal()] = 172;
         } catch (NoSuchFieldError var260) {
         }

         try {
            var0[Material.DOUBLE_STEP.ordinal()] = 44;
         } catch (NoSuchFieldError var259) {
         }

         try {
            var0[Material.DRAGON_EGG.ordinal()] = 124;
         } catch (NoSuchFieldError var258) {
         }

         try {
            var0[Material.DROPPER.ordinal()] = 160;
         } catch (NoSuchFieldError var257) {
         }

         try {
            var0[Material.EGG.ordinal()] = 261;
         } catch (NoSuchFieldError var256) {
         }

         try {
            var0[Material.EMERALD.ordinal()] = 305;
         } catch (NoSuchFieldError var255) {
         }

         try {
            var0[Material.EMERALD_BLOCK.ordinal()] = 135;
         } catch (NoSuchFieldError var254) {
         }

         try {
            var0[Material.EMERALD_ORE.ordinal()] = 131;
         } catch (NoSuchFieldError var253) {
         }

         try {
            var0[Material.EMPTY_MAP.ordinal()] = 312;
         } catch (NoSuchFieldError var252) {
         }

         try {
            var0[Material.ENCHANTED_BOOK.ordinal()] = 320;
         } catch (NoSuchFieldError var251) {
         }

         try {
            var0[Material.ENCHANTMENT_TABLE.ordinal()] = 118;
         } catch (NoSuchFieldError var250) {
         }

         try {
            var0[Material.ENDER_CHEST.ordinal()] = 132;
         } catch (NoSuchFieldError var249) {
         }

         try {
            var0[Material.ENDER_PEARL.ordinal()] = 285;
         } catch (NoSuchFieldError var248) {
         }

         try {
            var0[Material.ENDER_PORTAL.ordinal()] = 121;
         } catch (NoSuchFieldError var247) {
         }

         try {
            var0[Material.ENDER_PORTAL_FRAME.ordinal()] = 122;
         } catch (NoSuchFieldError var246) {
         }

         try {
            var0[Material.ENDER_STONE.ordinal()] = 123;
         } catch (NoSuchFieldError var245) {
         }

         try {
            var0[Material.EXPLOSIVE_MINECART.ordinal()] = 324;
         } catch (NoSuchFieldError var244) {
         }

         try {
            var0[Material.EXP_BOTTLE.ordinal()] = 301;
         } catch (NoSuchFieldError var243) {
         }

         try {
            var0[Material.EYE_OF_ENDER.ordinal()] = 298;
         } catch (NoSuchFieldError var242) {
         }

         try {
            var0[Material.FEATHER.ordinal()] = 205;
         } catch (NoSuchFieldError var241) {
         }

         try {
            var0[Material.FENCE.ordinal()] = 86;
         } catch (NoSuchFieldError var240) {
         }

         try {
            var0[Material.FENCE_GATE.ordinal()] = 109;
         } catch (NoSuchFieldError var239) {
         }

         try {
            var0[Material.FERMENTED_SPIDER_EYE.ordinal()] = 293;
         } catch (NoSuchFieldError var238) {
         }

         try {
            var0[Material.FIRE.ordinal()] = 52;
         } catch (NoSuchFieldError var237) {
         }

         try {
            var0[Material.FIREBALL.ordinal()] = 302;
         } catch (NoSuchFieldError var236) {
         }

         try {
            var0[Material.FIREWORK.ordinal()] = 318;
         } catch (NoSuchFieldError var235) {
         }

         try {
            var0[Material.FIREWORK_CHARGE.ordinal()] = 319;
         } catch (NoSuchFieldError var234) {
         }

         try {
            var0[Material.FISHING_ROD.ordinal()] = 263;
         } catch (NoSuchFieldError var233) {
         }

         try {
            var0[Material.FLINT.ordinal()] = 235;
         } catch (NoSuchFieldError var232) {
         }

         try {
            var0[Material.FLINT_AND_STEEL.ordinal()] = 176;
         } catch (NoSuchFieldError var231) {
         }

         try {
            var0[Material.FLOWER_POT.ordinal()] = 142;
         } catch (NoSuchFieldError var230) {
         }

         try {
            var0[Material.FLOWER_POT_ITEM.ordinal()] = 307;
         } catch (NoSuchFieldError var229) {
         }

         try {
            var0[Material.FURNACE.ordinal()] = 62;
         } catch (NoSuchFieldError var228) {
         }

         try {
            var0[Material.GHAST_TEAR.ordinal()] = 287;
         } catch (NoSuchFieldError var227) {
         }

         try {
            var0[Material.GLASS.ordinal()] = 21;
         } catch (NoSuchFieldError var226) {
         }

         try {
            var0[Material.GLASS_BOTTLE.ordinal()] = 291;
         } catch (NoSuchFieldError var225) {
         }

         try {
            var0[Material.GLOWING_REDSTONE_ORE.ordinal()] = 75;
         } catch (NoSuchFieldError var224) {
         }

         try {
            var0[Material.GLOWSTONE.ordinal()] = 90;
         } catch (NoSuchFieldError var223) {
         }

         try {
            var0[Material.GLOWSTONE_DUST.ordinal()] = 265;
         } catch (NoSuchFieldError var222) {
         }

         try {
            var0[Material.GOLDEN_APPLE.ordinal()] = 239;
         } catch (NoSuchFieldError var221) {
         }

         try {
            var0[Material.GOLDEN_CARROT.ordinal()] = 313;
         } catch (NoSuchFieldError var220) {
         }

         try {
            var0[Material.GOLD_AXE.ordinal()] = 203;
         } catch (NoSuchFieldError var219) {
         }

         try {
            var0[Material.GOLD_BARDING.ordinal()] = 327;
         } catch (NoSuchFieldError var218) {
         }

         try {
            var0[Material.GOLD_BLOCK.ordinal()] = 42;
         } catch (NoSuchFieldError var217) {
         }

         try {
            var0[Material.GOLD_BOOTS.ordinal()] = 234;
         } catch (NoSuchFieldError var216) {
         }

         try {
            var0[Material.GOLD_CHESTPLATE.ordinal()] = 232;
         } catch (NoSuchFieldError var215) {
         }

         try {
            var0[Material.GOLD_HELMET.ordinal()] = 231;
         } catch (NoSuchFieldError var214) {
         }

         try {
            var0[Material.GOLD_HOE.ordinal()] = 211;
         } catch (NoSuchFieldError var213) {
         }

         try {
            var0[Material.GOLD_INGOT.ordinal()] = 183;
         } catch (NoSuchFieldError var212) {
         }

         try {
            var0[Material.GOLD_LEGGINGS.ordinal()] = 233;
         } catch (NoSuchFieldError var211) {
         }

         try {
            var0[Material.GOLD_NUGGET.ordinal()] = 288;
         } catch (NoSuchFieldError var210) {
         }

         try {
            var0[Material.GOLD_ORE.ordinal()] = 15;
         } catch (NoSuchFieldError var209) {
         }

         try {
            var0[Material.GOLD_PICKAXE.ordinal()] = 202;
         } catch (NoSuchFieldError var208) {
         }

         try {
            var0[Material.GOLD_PLATE.ordinal()] = 149;
         } catch (NoSuchFieldError var207) {
         }

         try {
            var0[Material.GOLD_RECORD.ordinal()] = 332;
         } catch (NoSuchFieldError var206) {
         }

         try {
            var0[Material.GOLD_SPADE.ordinal()] = 201;
         } catch (NoSuchFieldError var205) {
         }

         try {
            var0[Material.GOLD_SWORD.ordinal()] = 200;
         } catch (NoSuchFieldError var204) {
         }

         try {
            var0[Material.GRASS.ordinal()] = 3;
         } catch (NoSuchFieldError var203) {
         }

         try {
            var0[Material.GRAVEL.ordinal()] = 14;
         } catch (NoSuchFieldError var202) {
         }

         try {
            var0[Material.GREEN_RECORD.ordinal()] = 333;
         } catch (NoSuchFieldError var201) {
         }

         try {
            var0[Material.GRILLED_PORK.ordinal()] = 237;
         } catch (NoSuchFieldError var200) {
         }

         try {
            var0[Material.HARD_CLAY.ordinal()] = 169;
         } catch (NoSuchFieldError var199) {
         }

         try {
            var0[Material.HAY_BLOCK.ordinal()] = 167;
         } catch (NoSuchFieldError var198) {
         }

         try {
            var0[Material.HOPPER.ordinal()] = 156;
         } catch (NoSuchFieldError var197) {
         }

         try {
            var0[Material.HOPPER_MINECART.ordinal()] = 325;
         } catch (NoSuchFieldError var196) {
         }

         try {
            var0[Material.HUGE_MUSHROOM_1.ordinal()] = 101;
         } catch (NoSuchFieldError var195) {
         }

         try {
            var0[Material.HUGE_MUSHROOM_2.ordinal()] = 102;
         } catch (NoSuchFieldError var194) {
         }

         try {
            var0[Material.ICE.ordinal()] = 80;
         } catch (NoSuchFieldError var193) {
         }

         try {
            var0[Material.INK_SACK.ordinal()] = 268;
         } catch (NoSuchFieldError var192) {
         }

         try {
            var0[Material.IRON_AXE.ordinal()] = 175;
         } catch (NoSuchFieldError var191) {
         }

         try {
            var0[Material.IRON_BARDING.ordinal()] = 326;
         } catch (NoSuchFieldError var190) {
         }

         try {
            var0[Material.IRON_BLOCK.ordinal()] = 43;
         } catch (NoSuchFieldError var189) {
         }

         try {
            var0[Material.IRON_BOOTS.ordinal()] = 226;
         } catch (NoSuchFieldError var188) {
         }

         try {
            var0[Material.IRON_CHESTPLATE.ordinal()] = 224;
         } catch (NoSuchFieldError var187) {
         }

         try {
            var0[Material.IRON_DOOR.ordinal()] = 247;
         } catch (NoSuchFieldError var186) {
         }

         try {
            var0[Material.IRON_DOOR_BLOCK.ordinal()] = 72;
         } catch (NoSuchFieldError var185) {
         }

         try {
            var0[Material.IRON_FENCE.ordinal()] = 103;
         } catch (NoSuchFieldError var184) {
         }

         try {
            var0[Material.IRON_HELMET.ordinal()] = 223;
         } catch (NoSuchFieldError var183) {
         }

         try {
            var0[Material.IRON_HOE.ordinal()] = 209;
         } catch (NoSuchFieldError var182) {
         }

         try {
            var0[Material.IRON_INGOT.ordinal()] = 182;
         } catch (NoSuchFieldError var181) {
         }

         try {
            var0[Material.IRON_LEGGINGS.ordinal()] = 225;
         } catch (NoSuchFieldError var180) {
         }

         try {
            var0[Material.IRON_ORE.ordinal()] = 16;
         } catch (NoSuchFieldError var179) {
         }

         try {
            var0[Material.IRON_PICKAXE.ordinal()] = 174;
         } catch (NoSuchFieldError var178) {
         }

         try {
            var0[Material.IRON_PLATE.ordinal()] = 150;
         } catch (NoSuchFieldError var177) {
         }

         try {
            var0[Material.IRON_SPADE.ordinal()] = 173;
         } catch (NoSuchFieldError var176) {
         }

         try {
            var0[Material.IRON_SWORD.ordinal()] = 184;
         } catch (NoSuchFieldError var175) {
         }

         try {
            var0[Material.ITEM_FRAME.ordinal()] = 306;
         } catch (NoSuchFieldError var174) {
         }

         try {
            var0[Material.JACK_O_LANTERN.ordinal()] = 92;
         } catch (NoSuchFieldError var173) {
         }

         try {
            var0[Material.JUKEBOX.ordinal()] = 85;
         } catch (NoSuchFieldError var172) {
         }

         try {
            var0[Material.JUNGLE_WOOD_STAIRS.ordinal()] = 138;
         } catch (NoSuchFieldError var171) {
         }

         try {
            var0[Material.LADDER.ordinal()] = 66;
         } catch (NoSuchFieldError var170) {
         }

         try {
            var0[Material.LAPIS_BLOCK.ordinal()] = 23;
         } catch (NoSuchFieldError var169) {
         }

         try {
            var0[Material.LAPIS_ORE.ordinal()] = 22;
         } catch (NoSuchFieldError var168) {
         }

         try {
            var0[Material.LAVA.ordinal()] = 11;
         } catch (NoSuchFieldError var167) {
         }

         try {
            var0[Material.LAVA_BUCKET.ordinal()] = 244;
         } catch (NoSuchFieldError var166) {
         }

         try {
            var0[Material.LEASH.ordinal()] = 329;
         } catch (NoSuchFieldError var165) {
         }

         try {
            var0[Material.LEATHER.ordinal()] = 251;
         } catch (NoSuchFieldError var164) {
         }

         try {
            var0[Material.LEATHER_BOOTS.ordinal()] = 218;
         } catch (NoSuchFieldError var163) {
         }

         try {
            var0[Material.LEATHER_CHESTPLATE.ordinal()] = 216;
         } catch (NoSuchFieldError var162) {
         }

         try {
            var0[Material.LEATHER_HELMET.ordinal()] = 215;
         } catch (NoSuchFieldError var161) {
         }

         try {
            var0[Material.LEATHER_LEGGINGS.ordinal()] = 217;
         } catch (NoSuchFieldError var160) {
         }

         try {
            var0[Material.LEAVES.ordinal()] = 19;
         } catch (NoSuchFieldError var159) {
         }

         try {
            var0[Material.LEAVES_2.ordinal()] = 163;
         } catch (NoSuchFieldError var158) {
         }

         try {
            var0[Material.LEVER.ordinal()] = 70;
         } catch (NoSuchFieldError var157) {
         }

         try {
            var0[Material.LOCKED_CHEST.ordinal()] = 96;
         } catch (NoSuchFieldError var156) {
         }

         try {
            var0[Material.LOG.ordinal()] = 18;
         } catch (NoSuchFieldError var155) {
         }

         try {
            var0[Material.LOG_2.ordinal()] = 164;
         } catch (NoSuchFieldError var154) {
         }

         try {
            var0[Material.LONG_GRASS.ordinal()] = 32;
         } catch (NoSuchFieldError var153) {
         }

         try {
            var0[Material.MAGMA_CREAM.ordinal()] = 295;
         } catch (NoSuchFieldError var152) {
         }

         try {
            var0[Material.MAP.ordinal()] = 275;
         } catch (NoSuchFieldError var151) {
         }

         try {
            var0[Material.MELON.ordinal()] = 277;
         } catch (NoSuchFieldError var150) {
         }

         try {
            var0[Material.MELON_BLOCK.ordinal()] = 105;
         } catch (NoSuchFieldError var149) {
         }

         try {
            var0[Material.MELON_SEEDS.ordinal()] = 279;
         } catch (NoSuchFieldError var148) {
         }

         try {
            var0[Material.MELON_STEM.ordinal()] = 107;
         } catch (NoSuchFieldError var147) {
         }

         try {
            var0[Material.MILK_BUCKET.ordinal()] = 252;
         } catch (NoSuchFieldError var146) {
         }

         try {
            var0[Material.MINECART.ordinal()] = 245;
         } catch (NoSuchFieldError var145) {
         }

         try {
            var0[Material.MOB_SPAWNER.ordinal()] = 53;
         } catch (NoSuchFieldError var144) {
         }

         try {
            var0[Material.MONSTER_EGG.ordinal()] = 300;
         } catch (NoSuchFieldError var143) {
         }

         try {
            var0[Material.MONSTER_EGGS.ordinal()] = 99;
         } catch (NoSuchFieldError var142) {
         }

         try {
            var0[Material.MOSSY_COBBLESTONE.ordinal()] = 49;
         } catch (NoSuchFieldError var141) {
         }

         try {
            var0[Material.MUSHROOM_SOUP.ordinal()] = 199;
         } catch (NoSuchFieldError var140) {
         }

         try {
            var0[Material.MYCEL.ordinal()] = 112;
         } catch (NoSuchFieldError var139) {
         }

         try {
            var0[Material.NAME_TAG.ordinal()] = 330;
         } catch (NoSuchFieldError var138) {
         }

         try {
            var0[Material.NETHERRACK.ordinal()] = 88;
         } catch (NoSuchFieldError var137) {
         }

         try {
            var0[Material.NETHER_BRICK.ordinal()] = 114;
         } catch (NoSuchFieldError var136) {
         }

         try {
            var0[Material.NETHER_BRICK_ITEM.ordinal()] = 322;
         } catch (NoSuchFieldError var135) {
         }

         try {
            var0[Material.NETHER_BRICK_STAIRS.ordinal()] = 116;
         } catch (NoSuchFieldError var134) {
         }

         try {
            var0[Material.NETHER_FENCE.ordinal()] = 115;
         } catch (NoSuchFieldError var133) {
         }

         try {
            var0[Material.NETHER_STALK.ordinal()] = 289;
         } catch (NoSuchFieldError var132) {
         }

         try {
            var0[Material.NETHER_STAR.ordinal()] = 316;
         } catch (NoSuchFieldError var131) {
         }

         try {
            var0[Material.NETHER_WARTS.ordinal()] = 117;
         } catch (NoSuchFieldError var130) {
         }

         try {
            var0[Material.NOTE_BLOCK.ordinal()] = 26;
         } catch (NoSuchFieldError var129) {
         }

         try {
            var0[Material.OBSIDIAN.ordinal()] = 50;
         } catch (NoSuchFieldError var128) {
         }

         try {
            var0[Material.PACKED_ICE.ordinal()] = 171;
         } catch (NoSuchFieldError var127) {
         }

         try {
            var0[Material.PAINTING.ordinal()] = 238;
         } catch (NoSuchFieldError var126) {
         }

         try {
            var0[Material.PAPER.ordinal()] = 256;
         } catch (NoSuchFieldError var125) {
         }

         try {
            var0[Material.PISTON_BASE.ordinal()] = 34;
         } catch (NoSuchFieldError var124) {
         }

         try {
            var0[Material.PISTON_EXTENSION.ordinal()] = 35;
         } catch (NoSuchFieldError var123) {
         }

         try {
            var0[Material.PISTON_MOVING_PIECE.ordinal()] = 37;
         } catch (NoSuchFieldError var122) {
         }

         try {
            var0[Material.PISTON_STICKY_BASE.ordinal()] = 30;
         } catch (NoSuchFieldError var121) {
         }

         try {
            var0[Material.POISONOUS_POTATO.ordinal()] = 311;
         } catch (NoSuchFieldError var120) {
         }

         try {
            var0[Material.PORK.ordinal()] = 236;
         } catch (NoSuchFieldError var119) {
         }

         try {
            var0[Material.PORTAL.ordinal()] = 91;
         } catch (NoSuchFieldError var118) {
         }

         try {
            var0[Material.POTATO.ordinal()] = 144;
         } catch (NoSuchFieldError var117) {
         }

         try {
            var0[Material.POTATO_ITEM.ordinal()] = 309;
         } catch (NoSuchFieldError var116) {
         }

         try {
            var0[Material.POTION.ordinal()] = 290;
         } catch (NoSuchFieldError var115) {
         }

         try {
            var0[Material.POWERED_MINECART.ordinal()] = 260;
         } catch (NoSuchFieldError var114) {
         }

         try {
            var0[Material.POWERED_RAIL.ordinal()] = 28;
         } catch (NoSuchFieldError var113) {
         }

         try {
            var0[Material.PUMPKIN.ordinal()] = 87;
         } catch (NoSuchFieldError var112) {
         }

         try {
            var0[Material.PUMPKIN_PIE.ordinal()] = 317;
         } catch (NoSuchFieldError var111) {
         }

         try {
            var0[Material.PUMPKIN_SEEDS.ordinal()] = 278;
         } catch (NoSuchFieldError var110) {
         }

         try {
            var0[Material.PUMPKIN_STEM.ordinal()] = 106;
         } catch (NoSuchFieldError var109) {
         }

         try {
            var0[Material.QUARTZ.ordinal()] = 323;
         } catch (NoSuchFieldError var108) {
         }

         try {
            var0[Material.QUARTZ_BLOCK.ordinal()] = 157;
         } catch (NoSuchFieldError var107) {
         }

         try {
            var0[Material.QUARTZ_ORE.ordinal()] = 155;
         } catch (NoSuchFieldError var106) {
         }

         try {
            var0[Material.QUARTZ_STAIRS.ordinal()] = 158;
         } catch (NoSuchFieldError var105) {
         }

         try {
            var0[Material.RAILS.ordinal()] = 67;
         } catch (NoSuchFieldError var104) {
         }

         try {
            var0[Material.RAW_BEEF.ordinal()] = 280;
         } catch (NoSuchFieldError var103) {
         }

         try {
            var0[Material.RAW_CHICKEN.ordinal()] = 282;
         } catch (NoSuchFieldError var102) {
         }

         try {
            var0[Material.RAW_FISH.ordinal()] = 266;
         } catch (NoSuchFieldError var101) {
         }

         try {
            var0[Material.RECORD_10.ordinal()] = 341;
         } catch (NoSuchFieldError var100) {
         }

         try {
            var0[Material.RECORD_11.ordinal()] = 342;
         } catch (NoSuchFieldError var99) {
         }

         try {
            var0[Material.RECORD_12.ordinal()] = 343;
         } catch (NoSuchFieldError var98) {
         }

         try {
            var0[Material.RECORD_3.ordinal()] = 334;
         } catch (NoSuchFieldError var97) {
         }

         try {
            var0[Material.RECORD_4.ordinal()] = 335;
         } catch (NoSuchFieldError var96) {
         }

         try {
            var0[Material.RECORD_5.ordinal()] = 336;
         } catch (NoSuchFieldError var95) {
         }

         try {
            var0[Material.RECORD_6.ordinal()] = 337;
         } catch (NoSuchFieldError var94) {
         }

         try {
            var0[Material.RECORD_7.ordinal()] = 338;
         } catch (NoSuchFieldError var93) {
         }

         try {
            var0[Material.RECORD_8.ordinal()] = 339;
         } catch (NoSuchFieldError var92) {
         }

         try {
            var0[Material.RECORD_9.ordinal()] = 340;
         } catch (NoSuchFieldError var91) {
         }

         try {
            var0[Material.REDSTONE.ordinal()] = 248;
         } catch (NoSuchFieldError var90) {
         }

         try {
            var0[Material.REDSTONE_BLOCK.ordinal()] = 154;
         } catch (NoSuchFieldError var89) {
         }

         try {
            var0[Material.REDSTONE_COMPARATOR.ordinal()] = 321;
         } catch (NoSuchFieldError var88) {
         }

         try {
            var0[Material.REDSTONE_COMPARATOR_OFF.ordinal()] = 151;
         } catch (NoSuchFieldError var87) {
         }

         try {
            var0[Material.REDSTONE_COMPARATOR_ON.ordinal()] = 152;
         } catch (NoSuchFieldError var86) {
         }

         try {
            var0[Material.REDSTONE_LAMP_OFF.ordinal()] = 125;
         } catch (NoSuchFieldError var85) {
         }

         try {
            var0[Material.REDSTONE_LAMP_ON.ordinal()] = 126;
         } catch (NoSuchFieldError var84) {
         }

         try {
            var0[Material.REDSTONE_ORE.ordinal()] = 74;
         } catch (NoSuchFieldError var83) {
         }

         try {
            var0[Material.REDSTONE_TORCH_OFF.ordinal()] = 76;
         } catch (NoSuchFieldError var82) {
         }

         try {
            var0[Material.REDSTONE_TORCH_ON.ordinal()] = 77;
         } catch (NoSuchFieldError var81) {
         }

         try {
            var0[Material.REDSTONE_WIRE.ordinal()] = 56;
         } catch (NoSuchFieldError var80) {
         }

         try {
            var0[Material.RED_MUSHROOM.ordinal()] = 41;
         } catch (NoSuchFieldError var79) {
         }

         try {
            var0[Material.RED_ROSE.ordinal()] = 39;
         } catch (NoSuchFieldError var78) {
         }

         try {
            var0[Material.ROTTEN_FLESH.ordinal()] = 284;
         } catch (NoSuchFieldError var77) {
         }

         try {
            var0[Material.SADDLE.ordinal()] = 246;
         } catch (NoSuchFieldError var76) {
         }

         try {
            var0[Material.SAND.ordinal()] = 13;
         } catch (NoSuchFieldError var75) {
         }

         try {
            var0[Material.SANDSTONE.ordinal()] = 25;
         } catch (NoSuchFieldError var74) {
         }

         try {
            var0[Material.SANDSTONE_STAIRS.ordinal()] = 130;
         } catch (NoSuchFieldError var73) {
         }

         try {
            var0[Material.SAPLING.ordinal()] = 7;
         } catch (NoSuchFieldError var72) {
         }

         try {
            var0[Material.SEEDS.ordinal()] = 212;
         } catch (NoSuchFieldError var71) {
         }

         try {
            var0[Material.SHEARS.ordinal()] = 276;
         } catch (NoSuchFieldError var70) {
         }

         try {
            var0[Material.SIGN.ordinal()] = 240;
         } catch (NoSuchFieldError var69) {
         }

         try {
            var0[Material.SIGN_POST.ordinal()] = 64;
         } catch (NoSuchFieldError var68) {
         }

         try {
            var0[Material.SKULL.ordinal()] = 146;
         } catch (NoSuchFieldError var67) {
         }

         try {
            var0[Material.SKULL_ITEM.ordinal()] = 314;
         } catch (NoSuchFieldError var66) {
         }

         try {
            var0[Material.SLIME_BALL.ordinal()] = 258;
         } catch (NoSuchFieldError var65) {
         }

         try {
            var0[Material.SMOOTH_BRICK.ordinal()] = 100;
         } catch (NoSuchFieldError var64) {
         }

         try {
            var0[Material.SMOOTH_STAIRS.ordinal()] = 111;
         } catch (NoSuchFieldError var63) {
         }

         try {
            var0[Material.SNOW.ordinal()] = 79;
         } catch (NoSuchFieldError var62) {
         }

         try {
            var0[Material.SNOW_BALL.ordinal()] = 249;
         } catch (NoSuchFieldError var61) {
         }

         try {
            var0[Material.SNOW_BLOCK.ordinal()] = 81;
         } catch (NoSuchFieldError var60) {
         }

         try {
            var0[Material.SOIL.ordinal()] = 61;
         } catch (NoSuchFieldError var59) {
         }

         try {
            var0[Material.SOUL_SAND.ordinal()] = 89;
         } catch (NoSuchFieldError var58) {
         }

         try {
            var0[Material.SPECKLED_MELON.ordinal()] = 299;
         } catch (NoSuchFieldError var57) {
         }

         try {
            var0[Material.SPIDER_EYE.ordinal()] = 292;
         } catch (NoSuchFieldError var56) {
         }

         try {
            var0[Material.SPONGE.ordinal()] = 20;
         } catch (NoSuchFieldError var55) {
         }

         try {
            var0[Material.SPRUCE_WOOD_STAIRS.ordinal()] = 136;
         } catch (NoSuchFieldError var54) {
         }

         try {
            var0[Material.STAINED_CLAY.ordinal()] = 161;
         } catch (NoSuchFieldError var53) {
         }

         try {
            var0[Material.STAINED_GLASS.ordinal()] = 97;
         } catch (NoSuchFieldError var52) {
         }

         try {
            var0[Material.STAINED_GLASS_PANE.ordinal()] = 162;
         } catch (NoSuchFieldError var51) {
         }

         try {
            var0[Material.STATIONARY_LAVA.ordinal()] = 12;
         } catch (NoSuchFieldError var50) {
         }

         try {
            var0[Material.STATIONARY_WATER.ordinal()] = 10;
         } catch (NoSuchFieldError var49) {
         }

         try {
            var0[Material.STEP.ordinal()] = 45;
         } catch (NoSuchFieldError var48) {
         }

         try {
            var0[Material.STICK.ordinal()] = 197;
         } catch (NoSuchFieldError var47) {
         }

         try {
            var0[Material.STONE.ordinal()] = 2;
         } catch (NoSuchFieldError var46) {
         }

         try {
            var0[Material.STONE_AXE.ordinal()] = 192;
         } catch (NoSuchFieldError var45) {
         }

         try {
            var0[Material.STONE_BUTTON.ordinal()] = 78;
         } catch (NoSuchFieldError var44) {
         }

         try {
            var0[Material.STONE_HOE.ordinal()] = 208;
         } catch (NoSuchFieldError var43) {
         }

         try {
            var0[Material.STONE_PICKAXE.ordinal()] = 191;
         } catch (NoSuchFieldError var42) {
         }

         try {
            var0[Material.STONE_PLATE.ordinal()] = 71;
         } catch (NoSuchFieldError var41) {
         }

         try {
            var0[Material.STONE_SPADE.ordinal()] = 190;
         } catch (NoSuchFieldError var40) {
         }

         try {
            var0[Material.STONE_SWORD.ordinal()] = 189;
         } catch (NoSuchFieldError var39) {
         }

         try {
            var0[Material.STORAGE_MINECART.ordinal()] = 259;
         } catch (NoSuchFieldError var38) {
         }

         try {
            var0[Material.STRING.ordinal()] = 204;
         } catch (NoSuchFieldError var37) {
         }

         try {
            var0[Material.SUGAR.ordinal()] = 270;
         } catch (NoSuchFieldError var36) {
         }

         try {
            var0[Material.SUGAR_CANE.ordinal()] = 255;
         } catch (NoSuchFieldError var35) {
         }

         try {
            var0[Material.SUGAR_CANE_BLOCK.ordinal()] = 84;
         } catch (NoSuchFieldError var34) {
         }

         try {
            var0[Material.SULPHUR.ordinal()] = 206;
         } catch (NoSuchFieldError var33) {
         }

         try {
            var0[Material.THIN_GLASS.ordinal()] = 104;
         } catch (NoSuchFieldError var32) {
         }

         try {
            var0[Material.TNT.ordinal()] = 47;
         } catch (NoSuchFieldError var31) {
         }

         try {
            var0[Material.TORCH.ordinal()] = 51;
         } catch (NoSuchFieldError var30) {
         }

         try {
            var0[Material.TRAPPED_CHEST.ordinal()] = 148;
         } catch (NoSuchFieldError var29) {
         }

         try {
            var0[Material.TRAP_DOOR.ordinal()] = 98;
         } catch (NoSuchFieldError var28) {
         }

         try {
            var0[Material.TRIPWIRE.ordinal()] = 134;
         } catch (NoSuchFieldError var27) {
         }

         try {
            var0[Material.TRIPWIRE_HOOK.ordinal()] = 133;
         } catch (NoSuchFieldError var26) {
         }

         try {
            var0[Material.VINE.ordinal()] = 108;
         } catch (NoSuchFieldError var25) {
         }

         try {
            var0[Material.WALL_SIGN.ordinal()] = 69;
         } catch (NoSuchFieldError var24) {
         }

         try {
            var0[Material.WATCH.ordinal()] = 264;
         } catch (NoSuchFieldError var23) {
         }

         try {
            var0[Material.WATER.ordinal()] = 9;
         } catch (NoSuchFieldError var22) {
         }

         try {
            var0[Material.WATER_BUCKET.ordinal()] = 243;
         } catch (NoSuchFieldError var21) {
         }

         try {
            var0[Material.WATER_LILY.ordinal()] = 113;
         } catch (NoSuchFieldError var20) {
         }

         try {
            var0[Material.WEB.ordinal()] = 31;
         } catch (NoSuchFieldError var19) {
         }

         try {
            var0[Material.WHEAT.ordinal()] = 213;
         } catch (NoSuchFieldError var18) {
         }

         try {
            var0[Material.WOOD.ordinal()] = 6;
         } catch (NoSuchFieldError var17) {
         }

         try {
            var0[Material.WOODEN_DOOR.ordinal()] = 65;
         } catch (NoSuchFieldError var16) {
         }

         try {
            var0[Material.WOOD_AXE.ordinal()] = 188;
         } catch (NoSuchFieldError var15) {
         }

         try {
            var0[Material.WOOD_BUTTON.ordinal()] = 145;
         } catch (NoSuchFieldError var14) {
         }

         try {
            var0[Material.WOOD_DOOR.ordinal()] = 241;
         } catch (NoSuchFieldError var13) {
         }

         try {
            var0[Material.WOOD_DOUBLE_STEP.ordinal()] = 127;
         } catch (NoSuchFieldError var12) {
         }

         try {
            var0[Material.WOOD_HOE.ordinal()] = 207;
         } catch (NoSuchFieldError var11) {
         }

         try {
            var0[Material.WOOD_PICKAXE.ordinal()] = 187;
         } catch (NoSuchFieldError var10) {
         }

         try {
            var0[Material.WOOD_PLATE.ordinal()] = 73;
         } catch (NoSuchFieldError var9) {
         }

         try {
            var0[Material.WOOD_SPADE.ordinal()] = 186;
         } catch (NoSuchFieldError var8) {
         }

         try {
            var0[Material.WOOD_STAIRS.ordinal()] = 54;
         } catch (NoSuchFieldError var7) {
         }

         try {
            var0[Material.WOOD_STEP.ordinal()] = 128;
         } catch (NoSuchFieldError var6) {
         }

         try {
            var0[Material.WOOD_SWORD.ordinal()] = 185;
         } catch (NoSuchFieldError var5) {
         }

         try {
            var0[Material.WOOL.ordinal()] = 36;
         } catch (NoSuchFieldError var4) {
         }

         try {
            var0[Material.WORKBENCH.ordinal()] = 59;
         } catch (NoSuchFieldError var3) {
         }

         try {
            var0[Material.WRITTEN_BOOK.ordinal()] = 304;
         } catch (NoSuchFieldError var2) {
         }

         try {
            var0[Material.YELLOW_FLOWER.ordinal()] = 38;
         } catch (NoSuchFieldError var1) {
         }

         $SWITCH_TABLE$org$bukkit$Material = var0;
         return var0;
      }
   }
}
