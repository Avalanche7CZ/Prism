package me.botsko.prism.listeners;

import java.util.List;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.actions.BlockAction;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.utils.MiscUtils;
import me.botsko.prism.wands.ProfileWand;
import me.botsko.prism.wands.Wand;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;

public class PrismPlayerEvents implements Listener {
   private final Prism plugin;
   private final List illegalCommands;
   private final List ignoreCommands;

   public PrismPlayerEvents(Prism plugin) {
      this.plugin = plugin;
      this.illegalCommands = plugin.getConfig().getList("prism.alerts.illegal-commands.commands");
      this.ignoreCommands = plugin.getConfig().getList("prism.do-not-track.commands");
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
      Player player = event.getPlayer();
      String cmd = event.getMessage().toLowerCase();
      String[] cmdArgs = cmd.split(" ");
      String primaryCmd = cmdArgs[0].substring(1);
      if (this.plugin.getConfig().getBoolean("prism.alerts.illegal-commands.enabled") && this.illegalCommands.contains(primaryCmd)) {
         String msg = player.getName() + " attempted an illegal command: " + primaryCmd + ". Originally: " + cmd;
         player.sendMessage(Prism.messenger.playerError("Sorry, this command is not available in-game."));
         this.plugin.alertPlayers((Player)null, msg);
         event.setCancelled(true);
         if (this.plugin.getConfig().getBoolean("prism.alerts.illegal-commands.log-to-console")) {
            Prism.log(msg);
         }

         List commands = this.plugin.getConfig().getStringList("prism.alerts.illegal-commands.log-commands");
         MiscUtils.dispatchAlert(msg, commands);
      }

      if (Prism.getIgnore().event("player-command", player)) {
         if (!this.ignoreCommands.contains(primaryCmd)) {
            RecordingQueue.addToQueue(ActionFactory.createPlayer("player-command", player, event.getMessage()));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      PlayerIdentification.cachePrismPlayer(player);
      if (Prism.getIgnore().event("player-join", player)) {
         String ip = null;
         if (this.plugin.getConfig().getBoolean("prism.track-player-ip-on-join")) {
            ip = player.getAddress().getAddress().getHostAddress().toString();
         }

         RecordingQueue.addToQueue(ActionFactory.createPlayer("player-join", event.getPlayer(), ip));
      }
   }

   @EventHandler(
      priority = EventPriority.NORMAL
   )
   public void onPlayerQuit(PlayerQuitEvent event) {
      Prism.prismPlayers.remove(event.getPlayer().getName());
      if (Prism.getIgnore().event("player-quit", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createPlayer("player-quit", event.getPlayer(), (String)null));
         if (Prism.playersWithActiveTools.containsKey(event.getPlayer().getName())) {
            Prism.playersWithActiveTools.remove(event.getPlayer().getName());
         }

         if (this.plugin.playerActivePreviews.containsKey(event.getPlayer().getName())) {
            this.plugin.playerActivePreviews.remove(event.getPlayer().getName());
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerChat(AsyncPlayerChatEvent event) {
      if (Prism.getIgnore().event("player-chat", event.getPlayer())) {
         if (!this.plugin.dependencyEnabled("Herochat")) {
            RecordingQueue.addToQueue(ActionFactory.createPlayer("player-chat", event.getPlayer(), event.getMessage()));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      if (Prism.getIgnore().event("item-drop", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", event.getItemDrop().getItemStack(), event.getItemDrop().getItemStack().getAmount(), -1, (Map)null, event.getPlayer().getLocation(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerPickupItem(PlayerPickupItemEvent event) {
      if (Prism.getIgnore().event("item-pickup", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createItemStack("item-pickup", event.getItem().getItemStack(), event.getItem().getItemStack().getAmount(), -1, (Map)null, event.getPlayer().getLocation(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerExpChangeEvent(PlayerExpChangeEvent event) {
      if (Prism.getIgnore().event("xp-pickup", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createPlayer("xp-pickup", event.getPlayer(), "" + event.getAmount()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
      Player player = event.getPlayer();
      String cause = event.getBucket() == Material.LAVA_BUCKET ? "lava-bucket" : "water-bucket";
      if (Prism.getIgnore().event(cause, player)) {
         Block spot = event.getBlockClicked().getRelative(event.getBlockFace());
         int newId = cause.equals("lava-bucket") ? 11 : 9;
         RecordingQueue.addToQueue(ActionFactory.createBlockChange(cause, spot.getLocation(), spot.getTypeId(), spot.getData(), newId, (byte)0, player.getName()));
         if (this.plugin.getConfig().getBoolean("prism.alerts.uses.lava") && event.getBucket() == Material.LAVA_BUCKET && !player.hasPermission("prism.alerts.use.lavabucket.ignore") && !player.hasPermission("prism.alerts.ignore")) {
            this.plugin.useMonitor.alertOnItemUse(player, "poured lava");
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerBucketFill(PlayerBucketFillEvent event) {
      Player player = event.getPlayer();
      if (Prism.getIgnore().event("bucket-fill", player)) {
         Block spot = event.getBlockClicked().getRelative(event.getBlockFace());
         String liquid_type = "milk";
         if (spot.getTypeId() != 8 && spot.getTypeId() != 9) {
            if (spot.getTypeId() == 10 || spot.getTypeId() == 11) {
               liquid_type = "lava";
            }
         } else {
            liquid_type = "water";
         }

         Handler pa = ActionFactory.createPlayer("bucket-fill", player, liquid_type);
         pa.setX((double)spot.getX());
         pa.setY((double)spot.getY());
         pa.setZ((double)spot.getZ());
         RecordingQueue.addToQueue(pa);
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerTeleport(PlayerTeleportEvent event) {
      if (Prism.getIgnore().event("player-teleport", event.getPlayer())) {
         PlayerTeleportEvent.TeleportCause c = event.getCause();
         if (c.equals(TeleportCause.END_PORTAL) || c.equals(TeleportCause.NETHER_PORTAL) || c.equals(TeleportCause.ENDER_PEARL)) {
            RecordingQueue.addToQueue(ActionFactory.createEntityTravel("player-teleport", event.getPlayer(), event.getFrom(), event.getTo(), event.getCause()));
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEnchantItem(EnchantItemEvent event) {
      if (Prism.getIgnore().event("enchant-item", event.getEnchanter())) {
         Player player = event.getEnchanter();
         RecordingQueue.addToQueue(ActionFactory.createItemStack("enchant-item", event.getItem(), event.getEnchantsToAdd(), event.getEnchantBlock().getLocation(), player.getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onCraftItem(CraftItemEvent event) {
      Player player = (Player)event.getWhoClicked();
      if (Prism.getIgnore().event("craft-item", player)) {
         ItemStack item = event.getRecipe().getResult();
         RecordingQueue.addToQueue(ActionFactory.createItemStack("craft-item", item, 1, -1, (Map)null, player.getLocation(), player.getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerInteract(PlayerInteractEvent event) {
      Player player = event.getPlayer();
      Block block = event.getClickedBlock();
      if (Prism.playersWithActiveTools.containsKey(player.getName())) {
         Wand wand = (Wand)Prism.playersWithActiveTools.get(player.getName());
         int item_id = wand.getItemId();
         byte item_subid = wand.getItemSubId();
         if (wand != null && player.getItemInHand().getTypeId() == item_id && player.getItemInHand().getDurability() == item_subid) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
               wand.playerLeftClick(player, block.getLocation());
            }

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
               block = block.getRelative(event.getBlockFace());
               wand.playerRightClick(player, block.getLocation());
            }

            if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK) {
               Prism.debug("Cancelling event for wand use.");
               event.setCancelled(true);
               player.updateInventory();
               return;
            }
         }
      }

      if (!event.isCancelled()) {
         if (block != null && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            String coord_key;
            switch (block.getType()) {
               case FURNACE:
               case DISPENSER:
               case CHEST:
               case ENDER_CHEST:
               case ENCHANTMENT_TABLE:
               case ANVIL:
               case BREWING_STAND:
               case TRAPPED_CHEST:
               case HOPPER:
               case DROPPER:
                  if (!Prism.getIgnore().event("container-access", player)) {
                     return;
                  }

                  RecordingQueue.addToQueue(ActionFactory.createBlock("container-access", block, player.getName()));
                  break;
               case JUKEBOX:
                  this.recordDiscInsert(block, player);
                  break;
               case CAKE_BLOCK:
                  this.recordCakeEat(block, player);
                  break;
               case WOODEN_DOOR:
               case ACACIA_DOOR:
               case BIRCH_DOOR:
               case DARK_OAK_DOOR:
               case JUNGLE_DOOR:
               case SPRUCE_DOOR:
               case TRAP_DOOR:
               case FENCE_GATE:
               case LEVER:
               case STONE_BUTTON:
               case WOOD_BUTTON:
                  if (!Prism.getIgnore().event("block-use", player)) {
                     return;
                  }

                  RecordingQueue.addToQueue(ActionFactory.createBlock("block-use", block, player.getName()));
                  break;
               case LOG:
                  this.recordCocoaPlantEvent(block, player.getItemInHand(), event.getBlockFace(), player.getName());
                  break;
               case CROPS:
               case GRASS:
               case MELON_STEM:
               case PUMPKIN_STEM:
               case SAPLING:
               case CARROT:
               case POTATO:
                  this.recordBonemealEvent(block, player.getItemInHand(), event.getBlockFace(), player.getName());
                  break;
               case RAILS:
               case DETECTOR_RAIL:
               case POWERED_RAIL:
               case ACTIVATOR_RAIL:
                  coord_key = block.getX() + ":" + block.getY() + ":" + block.getZ();
                  this.plugin.preplannedVehiclePlacement.put(coord_key, player.getName());
                  break;
               case TNT:
                  if (player.getItemInHand().getType().equals(Material.FLINT_AND_STEEL)) {
                     if (!Prism.getIgnore().event("tnt-prime", player)) {
                        return;
                     }

                     RecordingQueue.addToQueue(ActionFactory.createUse("tnt-prime", "tnt", block, player.getName()));
                  }
            }

            if (player.getItemInHand().getType().equals(Material.MONSTER_EGG)) {
               this.recordMonsterEggUse(block, player.getItemInHand(), player.getName());
            }

            if (player.getItemInHand().getType().equals(Material.FIREWORK)) {
               this.recordRocketLaunch(block, player.getItemInHand(), event.getBlockFace(), player.getName());
            }

            if (player.getItemInHand().getType().equals(Material.BOAT)) {
               coord_key = block.getX() + ":" + (block.getY() + 1) + ":" + block.getZ();
               this.plugin.preplannedVehiclePlacement.put(coord_key, player.getName());
            }
         }

         if (block != null && event.getAction() == Action.LEFT_CLICK_BLOCK) {
            Block above = block.getRelative(BlockFace.UP);
            if (above.getType().equals(Material.FIRE)) {
               RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", above, player.getName()));
            }
         }

         if (this.plugin.getConfig().getBoolean("prism.tracking.crop-trample")) {
            if (block != null && event.getAction() == Action.PHYSICAL && block.getType() == Material.SOIL) {
               if (!Prism.getIgnore().event("crop=trample", player)) {
                  return;
               }

               RecordingQueue.addToQueue(ActionFactory.createBlock("crop-trample", block.getRelative(BlockFace.UP), player.getName()));
            }

         }
      }
   }

   protected void recordCocoaPlantEvent(Block block, ItemStack inhand, BlockFace clickedFace, String player) {
      if (Prism.getIgnore().event("block-place", block)) {
         if (block.getType().equals(Material.LOG) && block.getData() >= 3 && inhand.getTypeId() == 351 && inhand.getDurability() == 3) {
            Location newLoc = block.getRelative(clickedFace).getLocation();
            Block actualBlock = block.getWorld().getBlockAt(newLoc);
            BlockAction action = new BlockAction();
            action.setActionType("block-place");
            action.setPlayerName(player);
            action.setX((double)actualBlock.getX());
            action.setY((double)actualBlock.getY());
            action.setZ((double)actualBlock.getZ());
            action.setWorldName(newLoc.getWorld().getName());
            action.setBlockId(127);
            action.setBlockSubId(1);
            RecordingQueue.addToQueue(action);
         }

      }
   }

   protected void recordBonemealEvent(Block block, ItemStack inhand, BlockFace clickedFace, String player) {
      if (inhand.getTypeId() == 351 && inhand.getDurability() == 15) {
         if (!Prism.getIgnore().event("bonemeal-use", block)) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createUse("bonemeal-use", "bonemeal", block, player));
      }

   }

   protected void recordMonsterEggUse(Block block, ItemStack inhand, String player) {
      if (Prism.getIgnore().event("spawnegg-use", block)) {
         RecordingQueue.addToQueue(ActionFactory.createUse("spawnegg-use", "monster egg", block, player));
      }
   }

   protected void recordRocketLaunch(Block block, ItemStack inhand, BlockFace clickedFace, String player) {
      if (Prism.getIgnore().event("firework-launch", block)) {
         RecordingQueue.addToQueue(ActionFactory.createItemStack("firework-launch", inhand, (Map)null, block.getLocation(), player));
      }
   }

   protected void recordCakeEat(Block block, Player player) {
      if (Prism.getIgnore().event("cake-eat", block)) {
         RecordingQueue.addToQueue(ActionFactory.createUse("cake-eat", "cake", block, player.getName()));
      }
   }

   protected void recordDiscInsert(Block block, Player player) {
      if (player.getItemInHand().getType().isRecord()) {
         Jukebox jukebox = (Jukebox)block.getState();
         if (!jukebox.getPlaying().equals(Material.AIR)) {
            ItemStack i = new ItemStack(jukebox.getPlaying(), 1);
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", i, i.getAmount(), 0, (Map)null, block.getLocation(), player.getName()));
         } else {
            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", player.getItemInHand(), 1, 0, (Map)null, block.getLocation(), player.getName()));
         }

      }
   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerEntityInteract(PlayerInteractEntityEvent event) {
      Player player = event.getPlayer();
      Entity entity = event.getRightClicked();
      if (Prism.playersWithActiveTools.containsKey(player.getName())) {
         Wand wand = (Wand)Prism.playersWithActiveTools.get(player.getName());
         if (wand != null && wand instanceof ProfileWand) {
            wand.playerRightClick(player, entity);
            event.setCancelled(true);
         }
      }

   }
}
