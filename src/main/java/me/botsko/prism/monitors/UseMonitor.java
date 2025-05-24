package me.botsko.prism.monitors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import me.botsko.prism.Prism;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class UseMonitor {
   private final Prism plugin;
   protected final ArrayList blocksToAlertOnPlace;
   protected final ArrayList blocksToAlertOnBreak;
   private ConcurrentHashMap countedEvents = new ConcurrentHashMap();

   public UseMonitor(Prism plugin) {
      this.plugin = plugin;
      this.blocksToAlertOnPlace = (ArrayList)plugin.getConfig().getList("prism.alerts.uses.item-placement");
      this.blocksToAlertOnBreak = (ArrayList)plugin.getConfig().getList("prism.alerts.uses.item-break");
      this.resetEventsQueue();
   }

   protected void incrementCount(String playername, String msg) {
      int count = 0;
      if (this.countedEvents.containsKey(playername)) {
         count = (Integer)this.countedEvents.get(playername);
      }

      ++count;
      this.countedEvents.put(playername, count);
      msg = ChatColor.GRAY + playername + " " + msg;
      if (count == 5) {
         msg = playername + " continues - pausing warnings.";
      }

      if (count <= 5) {
         if (this.plugin.getConfig().getBoolean("prism.alerts.uses.log-to-console")) {
            this.plugin.alertPlayers((Player)null, msg);
            Prism.log(msg);
         }

         List commands = this.plugin.getConfig().getStringList("prism.alerts.uses.log-commands");
         MiscUtils.dispatchAlert(msg, commands);
      }

   }

   protected boolean checkFeatureShouldProceed(Player player) {
      if (!this.plugin.getConfig().getBoolean("prism.alerts.uses.enabled")) {
         return false;
      } else if (this.plugin.getConfig().getBoolean("prism.alerts.uses.ignore-staff") && player.hasPermission("prism.alerts")) {
         return false;
      } else {
         return !player.hasPermission("prism.bypass-use-alerts");
      }
   }

   public void alertOnBlockPlacement(Player player, Block block) {
      if (this.checkFeatureShouldProceed(player)) {
         String playername = player.getName();
         String blockType = "" + block.getTypeId();
         if (this.blocksToAlertOnPlace.contains(blockType) || this.blocksToAlertOnPlace.contains(block.getTypeId() + ":" + block.getData())) {
            String alias = Prism.getItems().getAlias(block.getTypeId(), block.getData());
            this.incrementCount(playername, "placed " + alias);
         }

      }
   }

   public void alertOnBlockBreak(Player player, Block block) {
      if (this.checkFeatureShouldProceed(player)) {
         String playername = player.getName();
         String blockType = "" + block.getTypeId();
         if (this.blocksToAlertOnBreak.contains(blockType) || this.blocksToAlertOnBreak.contains(block.getTypeId() + ":" + block.getData())) {
            String alias = Prism.getItems().getAlias(block.getTypeId(), block.getData());
            this.incrementCount(playername, "broke " + alias);
         }

      }
   }

   public void alertOnItemUse(Player player, String use_msg) {
      if (this.checkFeatureShouldProceed(player)) {
         String playername = player.getName();
         this.incrementCount(playername, use_msg);
      }
   }

   public void alertOnVanillaXray(Player player, String use_msg) {
      if (this.checkFeatureShouldProceed(player)) {
         String playername = player.getName();
         this.incrementCount(playername, use_msg);
      }
   }

   public void resetEventsQueue() {
      this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
         public void run() {
            UseMonitor.this.countedEvents = new ConcurrentHashMap();
         }
      }, 7000L, 7000L);
   }
}
