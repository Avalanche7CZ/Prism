package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.Iterator;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.events.BlockStateChange;
import me.botsko.prism.utils.BlockUtils;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.CommandBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class BlockAction extends GenericAction {
   protected BlockActionData actionData;

   public void setBlock(Block block) {
      if (block != null) {
         this.setBlock(block.getState());
      }

   }

   public void setBlock(BlockState state) {
      if (state != null) {
         this.block_id = BlockUtils.blockIdMustRecordAs(state.getTypeId());
         this.block_subid = state.getRawData();
         if (this.block_id == 144 || this.block_id == 397 || this.block_id == 52 || this.block_id == 63 || this.block_id == 68) {
            this.actionData = new BlockActionData();
         }

         if (state.getTypeId() == 52) {
            SpawnerActionData spawnerActionData = new SpawnerActionData();
            CreatureSpawner s = (CreatureSpawner)state;
            spawnerActionData.entity_type = s.getSpawnedType().name().toLowerCase();
            spawnerActionData.delay = s.getDelay();
            this.actionData = spawnerActionData;
         } else if (state.getTypeId() != 144 && state.getTypeId() != 397) {
            if (state.getTypeId() != 63 && state.getTypeId() != 68) {
               if (state.getTypeId() == 137) {
                  CommandBlock cmdblock = (CommandBlock)state;
                  this.data = cmdblock.getCommand();
               }
            } else {
               SignActionData signActionData = new SignActionData();
               Sign s = (Sign)state;
               signActionData.lines = s.getLines();
               this.actionData = signActionData;
            }
         } else {
            SkullActionData skullActionData = new SkullActionData();
            Skull s = (Skull)state;
            skullActionData.rotation = s.getRotation().name().toLowerCase();
            skullActionData.owner = s.getOwner();
            skullActionData.skull_type = s.getSkullType().name().toLowerCase();
            this.actionData = skullActionData;
         }

         this.world_name = state.getWorld().getName();
         this.x = (double)state.getLocation().getBlockX();
         this.y = (double)state.getLocation().getBlockY();
         this.z = (double)state.getLocation().getBlockZ();
      }

   }

   public void setData(String data) {
      this.data = data;
      if (data != null && data.startsWith("{")) {
         if (this.block_id != 144 && this.block_id != 397) {
            if (this.block_id == 52) {
               this.actionData = (BlockActionData)this.gson.fromJson(data, SpawnerActionData.class);
            } else if (this.block_id != 63 && this.block_id != 68) {
               if (this.block_id == 137) {
                  this.actionData = new BlockActionData();
               }
            } else {
               this.actionData = (BlockActionData)this.gson.fromJson(data, SignActionData.class);
            }
         } else {
            this.actionData = (BlockActionData)this.gson.fromJson(data, SkullActionData.class);
         }
      }

   }

   public void save() {
      if (this.actionData != null) {
         this.data = this.gson.toJson((Object)this.actionData);
      }

   }

   public BlockActionData getActionData() {
      return this.actionData;
   }

   public String getNiceName() {
      String name = "";
      if (this.actionData instanceof SkullActionData) {
         SkullActionData ad = (SkullActionData)this.getActionData();
         name = name + ad.skull_type + " ";
      } else if (this.actionData instanceof SpawnerActionData) {
         SpawnerActionData ad = (SpawnerActionData)this.getActionData();
         name = name + ad.entity_type + " ";
      }

      name = name + this.materialAliases.getAlias(this.block_id, this.block_subid);
      if (this.actionData instanceof SignActionData) {
         SignActionData ad = (SignActionData)this.getActionData();
         if (ad.lines != null && ad.lines.length > 0) {
            name = name + " (" + TypeUtils.join(ad.lines, ", ") + ")";
         }
      } else if (this.block_id == 137) {
         name = name + " (" + this.data + ")";
      }

      return this.type.getName().equals("crop-trample") && this.block_id == 0 ? "empty soil" : name;
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.getType().doesCreateBlock() ? this.removeBlock(player, parameters, is_preview, block) : this.placeBlock(player, parameters, is_preview, block, false);
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.getType().doesCreateBlock() ? this.placeBlock(player, parameters, is_preview, block, false) : this.removeBlock(player, parameters, is_preview, block);
   }

   public ChangeResult applyUndo(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, block, false);
   }

   public ChangeResult applyDeferred(Player player, QueryParameters parameters, boolean is_preview) {
      Block block = this.getWorld().getBlockAt(this.getLoc());
      return this.placeBlock(player, parameters, is_preview, block, true);
   }

   protected ChangeResult placeBlock(Player player, QueryParameters parameters, boolean is_preview, Block block, boolean is_deferred) {
      Material m = Material.getMaterial(this.getBlockId());
      if (!this.getType().requiresHandler("BlockChangeAction") && !this.getType().requiresHandler("PrismRollbackAction") && !com.helion3.prism.libs.elixr.BlockUtils.isAcceptableForBlockPlace(block.getType()) && !parameters.hasFlag(Flag.OVERWRITE)) {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      } else if (Prism.getIllegalBlocks().contains(this.getBlockId()) && !parameters.getProcessType().equals(PrismProcessType.UNDO)) {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      } else {
         BlockState originalBlock;
         BlockStateChange stateChange;
         if (!is_preview) {
            originalBlock = block.getState();
            Block below;
            if (this.getBlockId() == 111) {
               below = block.getRelative(BlockFace.DOWN);
               if (!below.getType().equals(Material.WATER) && !below.getType().equals(Material.AIR) && !below.getType().equals(Material.STATIONARY_WATER)) {
                  return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
               }

               below.setType(Material.STATIONARY_WATER);
            }

            if (this.getBlockId() == 90) {
               below = com.helion3.prism.libs.elixr.BlockUtils.getFirstBlockOfMaterialBelow(Material.OBSIDIAN, block.getLocation());
               if (below != null) {
                  Block above = below.getRelative(BlockFace.UP);
                  if (above.getType() != Material.PORTAL) {
                     above.setType(Material.FIRE);
                     return new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null);
                  }
               }
            }

            if (this.getBlockId() == 84) {
               this.block_subid = 0;
            }

            block.setTypeId(this.getBlockId());
            block.setData((byte)this.getBlockSubId());
            if ((this.getBlockId() == 144 || this.getBlockId() == 397) && this.getActionData() instanceof SkullActionData) {
               SkullActionData s = (SkullActionData)this.getActionData();
               Skull skull = (Skull)block.getState();
               skull.setRotation(s.getRotation());
               skull.setSkullType(s.getSkullType());
               if (!s.owner.isEmpty()) {
                  skull.setOwner(s.owner);
               }

               skull.update();
            }

            if (this.getBlockId() == 52) {
               SpawnerActionData s = (SpawnerActionData)this.getActionData();
               CreatureSpawner spawner = (CreatureSpawner)block.getState();
               spawner.setDelay(s.getDelay());
               spawner.setSpawnedType(s.getEntityType());
               spawner.update();
            }

            if (this.getBlockId() == 137) {
               CommandBlock cmdblock = (CommandBlock)block.getState();
               cmdblock.setCommand(this.data);
               cmdblock.update();
            }

            if (parameters.getProcessType().equals(PrismProcessType.ROLLBACK) && (this.getBlockId() == 63 || this.getBlockId() == 68) && this.getActionData() instanceof SignActionData) {
               SignActionData s = (SignActionData)this.getActionData();
               if (block.getState() instanceof Sign) {
                  Sign sign = (Sign)block.getState();
                  int i = 0;
                  if (s.lines != null && s.lines.length > 0) {
                     String[] arr$ = s.lines;
                     int len$ = arr$.length;

                     for(int i$ = 0; i$ < len$; ++i$) {
                        String line = arr$[i$];
                        sign.setLine(i, line);
                        ++i;
                     }
                  }

                  sign.update();
               }
            }

            if (com.helion3.prism.libs.elixr.BlockUtils.materialRequiresSoil(block.getType())) {
               below = block.getRelative(BlockFace.DOWN);
               if (!below.getType().equals(Material.DIRT) && !below.getType().equals(Material.AIR) && !below.getType().equals(Material.GRASS)) {
                  return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
               }

               below.setType(Material.SOIL);
            }

            BlockState newBlock = block.getState();
            stateChange = new BlockStateChange(originalBlock, newBlock);
            if (BlockUtils.isDoor(m)) {
               BlockUtils.properlySetDoor(block, this.getBlockId(), (byte)this.getBlockSubId());
            } else if (m.equals(Material.BED_BLOCK)) {
               BlockUtils.properlySetBed(block, this.getBlockId(), (byte)this.getBlockSubId());
            } else if (m.equals(Material.DOUBLE_PLANT)) {
               BlockUtils.properlySetDoublePlant(block, this.getBlockId(), (byte)this.getBlockSubId());
            }
         } else {
            originalBlock = block.getState();
            stateChange = new BlockStateChange(originalBlock, originalBlock);
            player.sendBlockChange(block.getLocation(), this.getBlockId(), (byte)this.getBlockSubId());
            Iterator i$ = parameters.getSharedPlayers().iterator();

            while(i$.hasNext()) {
               CommandSender sharedPlayer = (CommandSender)i$.next();
               if (sharedPlayer instanceof Player) {
                  ((Player)sharedPlayer).sendBlockChange(block.getLocation(), this.getBlockId(), (byte)this.getBlockSubId());
               }
            }
         }

         return new ChangeResult(ChangeResultType.APPLIED, stateChange);
      }
   }

   protected ChangeResult removeBlock(Player player, QueryParameters parameters, boolean is_preview, Block block) {
      if (!block.getType().equals(Material.AIR)) {
         if (!com.helion3.prism.libs.elixr.BlockUtils.isAcceptableForBlockPlace(block.getType()) && !com.helion3.prism.libs.elixr.BlockUtils.areBlockIdsSameCoreItem(block.getTypeId(), this.getBlockId()) && !parameters.hasFlag(Flag.OVERWRITE)) {
            return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
         } else {
            BlockState originalBlock;
            BlockStateChange stateChange;
            if (!is_preview) {
               originalBlock = block.getState();
               block.setType(Material.AIR);
               BlockState newBlock = block.getState();
               stateChange = new BlockStateChange(originalBlock, newBlock);
            } else {
               originalBlock = block.getState();
               stateChange = new BlockStateChange(originalBlock, originalBlock);
               player.sendBlockChange(block.getLocation(), Material.AIR, (byte)0);
               Iterator i$ = parameters.getSharedPlayers().iterator();

               while(i$.hasNext()) {
                  CommandSender sharedPlayer = (CommandSender)i$.next();
                  if (sharedPlayer instanceof Player) {
                     ((Player)sharedPlayer).sendBlockChange(block.getLocation(), this.getBlockId(), (byte)this.getBlockSubId());
                  }
               }
            }

            return new ChangeResult(ChangeResultType.APPLIED, stateChange);
         }
      } else {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      }
   }

   public class SignActionData extends BlockActionData {
      public String[] lines;

      public SignActionData() {
         super();
      }
   }

   public class SkullActionData extends BlockActionData {
      public String rotation;
      public String owner;
      public String skull_type;

      public SkullActionData() {
         super();
      }

      public SkullType getSkullType() {
         return this.skull_type != null ? SkullType.valueOf(this.skull_type.toUpperCase()) : null;
      }

      public BlockFace getRotation() {
         return this.rotation != null ? BlockFace.valueOf(this.rotation.toUpperCase()) : null;
      }
   }

   public class SpawnerActionData extends BlockActionData {
      public String entity_type;
      public int delay;

      public SpawnerActionData() {
         super();
      }

      public EntityType getEntityType() {
         return EntityType.valueOf(this.entity_type.toUpperCase());
      }

      public int getDelay() {
         return this.delay;
      }
   }

   public class BlockActionData {
   }
}
