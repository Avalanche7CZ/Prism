package me.botsko.prism.actionlibs;

import java.util.Map;
import me.botsko.prism.actions.BlockAction;
import me.botsko.prism.actions.BlockChangeAction;
import me.botsko.prism.actions.BlockShiftAction;
import me.botsko.prism.actions.EntityAction;
import me.botsko.prism.actions.EntityTravelAction;
import me.botsko.prism.actions.GrowAction;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.actions.HangingItemAction;
import me.botsko.prism.actions.ItemStackAction;
import me.botsko.prism.actions.PlayerAction;
import me.botsko.prism.actions.PlayerDeathAction;
import me.botsko.prism.actions.PrismProcessAction;
import me.botsko.prism.actions.PrismRollbackAction;
import me.botsko.prism.actions.SignAction;
import me.botsko.prism.actions.UseAction;
import me.botsko.prism.actions.VehicleAction;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class ActionFactory {
   public static Handler createBlock(String action_type, String player) {
      BlockAction a = new BlockAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      return a;
   }

   public static Handler createBlock(String action_type, Block block, String player) {
      BlockAction a = new BlockAction();
      a.setActionType(action_type);
      a.setBlock(block);
      a.setPlayerName(player);
      return a;
   }

   public static Handler createBlock(String action_type, BlockState state, String player) {
      BlockAction a = new BlockAction();
      a.setActionType(action_type);
      a.setBlock(state);
      a.setPlayerName(player);
      return a;
   }

   public static Handler createBlockChange(String action_type, Location loc, int oldId, byte oldSubid, int newId, byte newSubid, String player) {
      BlockChangeAction a = new BlockChangeAction();
      a.setActionType(action_type);
      a.setBlockId(newId);
      a.setBlockSubId(newSubid);
      a.setOldBlockId(oldId);
      a.setOldBlockSubId(oldSubid);
      a.setPlayerName(player);
      a.setLoc(loc);
      return a;
   }

   public static Handler createBlockShift(String action_type, Block from, Location to, String player) {
      BlockShiftAction a = new BlockShiftAction();
      a.setActionType(action_type);
      a.setBlock(from);
      a.setPlayerName(player);
      a.setToLocation(to);
      return a;
   }

   public static Handler createEntity(String action_type, Entity entity, String player) {
      return createEntity(action_type, entity, player, (String)null);
   }

   public static Handler createEntity(String action_type, Entity entity, String player, String dyeUsed) {
      EntityAction a = new EntityAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setEntity(entity, dyeUsed);
      return a;
   }

   public static Handler createEntityTravel(String action_type, Entity entity, Location from, Location to, PlayerTeleportEvent.TeleportCause cause) {
      EntityTravelAction a = new EntityTravelAction();
      a.setEntity(entity);
      a.setActionType(action_type);
      a.setLoc(from);
      a.setToLocation(to);
      a.setCause(cause);
      return a;
   }

   public static Handler createGrow(String action_type, BlockState blockstate, String player) {
      GrowAction a = new GrowAction();
      a.setActionType(action_type);
      a.setBlock(blockstate);
      a.setPlayerName(player);
      return a;
   }

   public static Handler createHangingItem(String action_type, Hanging hanging, String player) {
      HangingItemAction a = new HangingItemAction();
      a.setActionType(action_type);
      a.setHanging(hanging);
      a.setPlayerName(player);
      return a;
   }

   public static Handler createItemStack(String action_type, ItemStack item, Map enchantments, Location loc, String player) {
      return createItemStack(action_type, item, 1, -1, enchantments, loc, player);
   }

   public static Handler createItemStack(String action_type, ItemStack item, int quantity, int slot, Map enchantments, Location loc, String player) {
      ItemStackAction a = new ItemStackAction();
      a.setActionType(action_type);
      a.setLoc(loc);
      a.setPlayerName(player);
      a.setItem(item, quantity, slot, enchantments);
      return a;
   }

   public static Handler createPlayer(String action_type, Player player, String additionalInfo) {
      PlayerAction a = new PlayerAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(player.getLocation());
      a.setData(additionalInfo);
      return a;
   }

   public static Handler createPlayerDeath(String action_type, Player player, String cause, String attacker) {
      PlayerDeathAction a = new PlayerDeathAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(player.getLocation());
      a.setCause(cause);
      a.setAttacker(attacker);
      return a;
   }

   public static Handler createPrismProcess(String action_type, PrismProcessType processType, Player player, String parameters) {
      PrismProcessAction a = new PrismProcessAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(player.getLocation());
      a.setProcessData(processType, parameters);
      return a;
   }

   public static Handler createPrismRollback(String action_type, BlockState oldblock, BlockState newBlock, String player, int parent_id) {
      PrismRollbackAction a = new PrismRollbackAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(oldblock.getLocation());
      a.setBlockChange(oldblock, newBlock, parent_id);
      return a;
   }

   public static Handler createSign(String action_type, Block block, String[] lines, String player) {
      SignAction a = new SignAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setBlock(block, lines);
      return a;
   }

   public static Handler createUse(String action_type, String item_used, Block block, String player) {
      UseAction a = new UseAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(block.getLocation());
      a.setData(item_used);
      return a;
   }

   public static Handler createVehicle(String action_type, Vehicle vehicle, String player) {
      VehicleAction a = new VehicleAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(vehicle.getLocation());
      a.setVehicle(vehicle);
      return a;
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, String player) {
      return createBlock(action_type, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Block block, String player) {
      return createBlock(action_type, block, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Location loc, int oldId, byte oldSubid, int newId, byte newSubid, String player) {
      return createBlockChange(action_type, loc, oldId, oldSubid, newId, newSubid, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Block from, Location to, String player) {
      return createBlockShift(action_type, from, to, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Entity entity, String player) {
      return createEntity(action_type, entity, player);
   }

   public static Handler create(String action_type, Entity entity, String player, String dyeUsed) {
      return createEntity(action_type, entity, player, dyeUsed);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Entity entity, Location from, Location to, PlayerTeleportEvent.TeleportCause cause) {
      return createEntityTravel(action_type, entity, from, to, cause);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, BlockState blockstate, String player) {
      return createGrow(action_type, blockstate, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Hanging hanging, String player) {
      return createHangingItem(action_type, hanging, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, ItemStack item, Map enchantments, Location loc, String player) {
      return createItemStack(action_type, item, enchantments, loc, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, ItemStack item, int quantity, int slot, Map enchantments, Location loc, String player) {
      return createItemStack(action_type, item, quantity, slot, enchantments, loc, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Player player, String additionalInfo) {
      return createPlayer(action_type, player, additionalInfo);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Player player, String cause, String attacker) {
      return createPlayerDeath(action_type, player, cause, attacker);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, PrismProcessType processType, Player player, String parameters) {
      PrismProcessAction a = new PrismProcessAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(player.getLocation());
      a.setProcessData(processType, parameters);
      return a;
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, BlockState oldblock, BlockState newBlock, String player, int parent_id) {
      PrismRollbackAction a = new PrismRollbackAction();
      a.setActionType(action_type);
      a.setPlayerName(player);
      a.setLoc(oldblock.getLocation());
      a.setBlockChange(oldblock, newBlock, parent_id);
      return a;
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Block block, String[] lines, String player) {
      return createSign(action_type, block, lines, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, String item_used, Block block, String player) {
      return createUse(action_type, item_used, block, player);
   }

   /** @deprecated */
   @Deprecated
   public static Handler create(String action_type, Vehicle vehicle, String player) {
      return createVehicle(action_type, vehicle, player);
   }
}
