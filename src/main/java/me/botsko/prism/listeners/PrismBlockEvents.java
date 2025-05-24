package me.botsko.prism.listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.utils.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Sign;

public class PrismBlockEvents implements Listener {
   private final Prism plugin;

   public PrismBlockEvents(Prism plugin) {
      this.plugin = plugin;
   }

   public void logItemRemoveFromDestroyedContainer(String player_name, Block block) {
      if (block.getType().equals(Material.JUKEBOX)) {
         Jukebox jukebox = (Jukebox)block.getState();
         Material playing = jukebox.getPlaying();
         if (playing != null && !playing.equals(Material.AIR)) {
            ItemStack i = new ItemStack(jukebox.getPlaying(), 1);
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", i, i.getAmount(), 0, (Map)null, block.getLocation(), player_name));
         }
      } else {
         if (block.getState() instanceof InventoryHolder) {
            InventoryHolder container = (InventoryHolder)block.getState();
            int slot = 0;
            ItemStack[] arr$ = container.getInventory().getContents();
            int len$ = arr$.length;

            for(int i$ = 0; i$ < len$; ++i$) {
               ItemStack i = arr$[i$];
               if ((block.getType().equals(Material.CHEST) || block.getType().equals(Material.TRAPPED_CHEST)) && slot > 26) {
                  break;
               }

               if (i != null) {
                  RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", i, i.getAmount(), slot, (Map)null, block.getLocation(), player_name));
               }

               ++slot;
            }
         }

      }
   }

   protected void logBlockRelationshipsForBlock(String playername, Block block) {
      if (!BlockUtils.isDoor(block.getType())) {
         ArrayList falling_blocks = com.helion3.prism.libs.elixr.BlockUtils.findFallingBlocksAboveBlock(block);
         if (falling_blocks.size() > 0) {
            Iterator i$ = falling_blocks.iterator();

            while(i$.hasNext()) {
               Block b = (Block)i$.next();
               RecordingQueue.addToQueue(ActionFactory.createBlock("block-fall", b, playername));
            }
         }

         if (block.getType().isSolid() || block.getType().equals(Material.SUGAR_CANE_BLOCK)) {
            Block b;
            ArrayList detached_blocks;
            Iterator i$;
            if (block.getType().equals(Material.PISTON_EXTENSION) || block.getType().equals(Material.PISTON_MOVING_PIECE)) {
               detached_blocks = com.helion3.prism.libs.elixr.BlockUtils.findSideFaceAttachedBlocks(block);
               if (detached_blocks.size() > 0) {
                  i$ = detached_blocks.iterator();

                  while(i$.hasNext()) {
                     b = (Block)i$.next();
                     RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", b, playername));
                  }
               }
            }

            detached_blocks = com.helion3.prism.libs.elixr.BlockUtils.findSideFaceAttachedBlocks(block);
            if (detached_blocks.size() > 0) {
               i$ = detached_blocks.iterator();

               while(i$.hasNext()) {
                  b = (Block)i$.next();
                  RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", b, playername));
               }
            }

            detached_blocks = com.helion3.prism.libs.elixr.BlockUtils.findTopFaceAttachedBlocks(block);
            if (detached_blocks.size() > 0) {
               i$ = detached_blocks.iterator();

               while(i$.hasNext()) {
                  b = (Block)i$.next();
                  RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", b, playername));
               }
            }

            ArrayList hanging = com.helion3.prism.libs.elixr.BlockUtils.findHangingEntities(block);
            if (hanging.size() > 0) {
               Iterator i$ = hanging.iterator();

               while(i$.hasNext()) {
                  Entity e = (Entity)i$.next();
                  String coord_key = e.getLocation().getBlockX() + ":" + e.getLocation().getBlockY() + ":" + e.getLocation().getBlockZ();
                  this.plugin.preplannedBlockFalls.put(coord_key, playername);
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBreak(BlockBreakEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      if (!block.getType().equals(Material.AIR)) {
         if (!player.hasPermission("prism.alerts.ores.ignore") && !player.hasPermission("prism.alerts.ignore")) {
            this.plugin.oreMonitor.processAlertsFromBlock(player, block);
         }

         if (Prism.getIgnore().event("block-break", player)) {
            Block sibling = com.helion3.prism.libs.elixr.BlockUtils.getSiblingForDoubleLengthBlock(block);
            if (sibling != null && !block.getType().equals(Material.CHEST) && !block.getType().equals(Material.TRAPPED_CHEST)) {
               block = sibling;
            }

            this.logItemRemoveFromDestroyedContainer(player.getName(), block);
            RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", block, player.getName()));
            this.logBlockRelationshipsForBlock(player.getName(), block);
            if (block.getType().equals(Material.OBSIDIAN)) {
               ArrayList blocks = com.helion3.prism.libs.elixr.BlockUtils.findConnectedBlocksOfType(Material.PORTAL, block, (ArrayList)null);
               if (!blocks.isEmpty()) {
                  RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", (Block)blocks.get(0), player.getName()));
               }
            }

            if (!player.hasPermission("prism.alerts.use.break.ignore") && !player.hasPermission("prism.alerts.ignore")) {
               this.plugin.useMonitor.alertOnBlockBreak(player, event.getBlock());
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockPlace(BlockPlaceEvent event) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      if (Prism.getIgnore().event("block-place", player)) {
         if (!block.getType().equals(Material.AIR)) {
            BlockState s = event.getBlockReplacedState();
            RecordingQueue.addToQueue(ActionFactory.createBlockChange("block-place", block.getLocation(), s.getTypeId(), s.getRawData(), block.getTypeId(), block.getData(), player.getName()));
            if (!player.hasPermission("prism.alerts.use.place.ignore") && !player.hasPermission("prism.alerts.ignore")) {
               this.plugin.useMonitor.alertOnBlockPlacement(player, block);
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockSpread(BlockSpreadEvent event) {
      String type = "block-spread";
      if (event.getNewState().getType().equals(Material.FIRE)) {
         if (!Prism.getIgnore().event("fire-spread")) {
            return;
         }

         type = "fire-spread";
      } else if (!Prism.getIgnore().event("block-spread", event.getBlock())) {
         return;
      }

      Block b = event.getBlock();
      BlockState s = event.getNewState();
      RecordingQueue.addToQueue(ActionFactory.createBlockChange(type, b.getLocation(), b.getTypeId(), b.getData(), s.getTypeId(), s.getRawData(), "Environment"));
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockForm(BlockFormEvent event) {
      if (Prism.getIgnore().event("block-form", event.getBlock())) {
         Block b = event.getBlock();
         BlockState s = event.getNewState();
         RecordingQueue.addToQueue(ActionFactory.createBlockChange("block-form", b.getLocation(), b.getTypeId(), b.getData(), s.getTypeId(), s.getRawData(), "Environment"));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockFade(BlockFadeEvent event) {
      if (Prism.getIgnore().event("block-fade", event.getBlock())) {
         Block b = event.getBlock();
         if (!b.getType().equals(Material.FIRE)) {
            BlockState s = event.getNewState();
            RecordingQueue.addToQueue(ActionFactory.createBlockChange("block-fade", b.getLocation(), b.getTypeId(), b.getData(), s.getTypeId(), s.getRawData(), "Environment"));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onLeavesDecay(LeavesDecayEvent event) {
      if (Prism.getIgnore().event("leaf-decay", event.getBlock())) {
         RecordingQueue.addToQueue(ActionFactory.createBlock("leaf-decay", event.getBlock(), "Environment"));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockBurn(BlockBurnEvent event) {
      if (Prism.getIgnore().event("block-burn", event.getBlock())) {
         Block block = event.getBlock();
         RecordingQueue.addToQueue(ActionFactory.createBlock("block-burn", block, "Environment"));
         Block sibling = com.helion3.prism.libs.elixr.BlockUtils.getSiblingForDoubleLengthBlock(block);
         if (sibling != null && !block.getType().equals(Material.CHEST) && !block.getType().equals(Material.TRAPPED_CHEST)) {
            block = sibling;
         }

         this.logBlockRelationshipsForBlock("Environment", block);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onSignChange(SignChangeEvent event) {
      if (Prism.getIgnore().event("sign-change", event.getPlayer())) {
         if (event.getBlock().getState().getData() instanceof Sign) {
            RecordingQueue.addToQueue(ActionFactory.createSign("sign-change", event.getBlock(), event.getLines(), event.getPlayer().getName()));
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onSetFire(BlockIgniteEvent event) {
      String cause = null;
      switch (event.getCause()) {
         case FIREBALL:
            cause = "fireball";
            break;
         case FLINT_AND_STEEL:
            cause = "lighter";
            break;
         case LAVA:
            cause = "lava-ignite";
            break;
         case LIGHTNING:
            cause = "lightning";
      }

      if (cause != null) {
         if (!Prism.getIgnore().event(cause, event.getBlock().getWorld())) {
            return;
         }

         Player player = event.getPlayer();
         if (player != null && (cause.equals("lighter") || cause.equals("fireball")) && this.plugin.getConfig().getBoolean("prism.alerts.uses.lighter") && !player.hasPermission("prism.alerts.use.lighter.ignore") && !player.hasPermission("prism.alerts.ignore")) {
            this.plugin.useMonitor.alertOnItemUse(player, "used a " + cause);
         }

         RecordingQueue.addToQueue(ActionFactory.createBlock(cause, event.getBlock(), player == null ? "Environment" : player.getName()));
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockDispense(BlockDispenseEvent event) {
      if (Prism.getIgnore().event("block-dispense")) {
         RecordingQueue.addToQueue(ActionFactory.createItemStack("block-dispense", event.getItem(), event.getItem().getAmount(), -1, (Map)null, event.getBlock().getLocation(), "dispenser"));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPistonExtend(BlockPistonExtendEvent event) {
      Iterator i$;
      if (this.plugin.getConfig().getBoolean("prism.alerts.vanilla-xray.enabled")) {
         Block noPlayer = event.getBlock().getRelative(event.getDirection()).getRelative(event.getDirection()).getRelative(BlockFace.DOWN);
         i$ = this.plugin.getServer().getOnlinePlayers().iterator();

         while(i$.hasNext()) {
            Player pl = (Player)i$.next();
            Location loc = pl.getLocation();
            if (loc.getBlockX() == noPlayer.getX() && loc.getBlockY() == noPlayer.getY() && loc.getBlockZ() == noPlayer.getZ()) {
               this.plugin.useMonitor.alertOnVanillaXray(pl, "possibly used a vanilla piston/xray trick");
               break;
            }
         }
      }

      if (Prism.getIgnore().event("block-shift", event.getBlock())) {
         List blocks = event.getBlocks();
         if (!blocks.isEmpty()) {
            i$ = blocks.iterator();

            while(i$.hasNext()) {
               Block block = (Block)i$.next();
               if (!block.getType().equals(Material.AIR)) {
                  RecordingQueue.addToQueue(ActionFactory.createBlockShift("block-shift", block, block.getRelative(event.getDirection()).getLocation(), "Piston"));
               }
            }
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPistonRetract(BlockPistonRetractEvent event) {
      if (Prism.getIgnore().event("block-shift", event.getBlock())) {
         if (event.isSticky()) {
            Block block = event.getBlock();
            if (!block.getType().equals(Material.AIR)) {
               RecordingQueue.addToQueue(ActionFactory.createBlockShift("block-shift", event.getRetractLocation().getBlock(), block.getRelative(event.getDirection()).getLocation(), "Piston"));
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onBlockFromTo(BlockFromToEvent event) {
      if (event.getBlock().isLiquid()) {
         BlockState from = event.getBlock().getState();
         BlockState to = event.getToBlock().getState();
         if (com.helion3.prism.libs.elixr.BlockUtils.canFlowBreakMaterial(to.getType())) {
            if (from.getType() != Material.STATIONARY_WATER && from.getType() != Material.WATER) {
               if ((from.getType() == Material.STATIONARY_LAVA || from.getType() == Material.LAVA) && Prism.getIgnore().event("lava-break", event.getBlock())) {
                  RecordingQueue.addToQueue(ActionFactory.createBlock("lava-break", event.getToBlock(), "Lava"));
               }
            } else if (Prism.getIgnore().event("water-break", event.getBlock())) {
               RecordingQueue.addToQueue(ActionFactory.createBlock("water-break", event.getToBlock(), "Water"));
            }
         }

         if ((from.getType() == Material.STATIONARY_WATER || from.getType() == Material.WATER) && Prism.getIgnore().event("water-flow", event.getBlock())) {
            RecordingQueue.addToQueue(ActionFactory.createBlock("water-flow", event.getBlock(), "Water"));
         }

         if ((from.getType() == Material.STATIONARY_LAVA || from.getType() == Material.LAVA) && Prism.getIgnore().event("lava-flow", event.getBlock())) {
            RecordingQueue.addToQueue(ActionFactory.createBlock("lava-flow", event.getBlock(), "Lava"));
         }

         if (Prism.getIgnore().event("block-form", event.getBlock())) {
            if (from.getType().equals(Material.STATIONARY_LAVA) && to.getType().equals(Material.STATIONARY_WATER)) {
               Block newTo = event.getToBlock();
               newTo.setType(Material.STONE);
               RecordingQueue.addToQueue(ActionFactory.createBlock("block-form", newTo, "Environment"));
            }

         }
      }
   }
}
