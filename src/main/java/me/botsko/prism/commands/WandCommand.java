package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.InventoryUtils;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.settings.Settings;
import me.botsko.prism.utils.ItemUtils;
import me.botsko.prism.wands.InspectorWand;
import me.botsko.prism.wands.ProfileWand;
import me.botsko.prism.wands.QueryWandBase;
import me.botsko.prism.wands.RestoreWand;
import me.botsko.prism.wands.RollbackWand;
import me.botsko.prism.wands.Wand;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class WandCommand implements SubHandler {
   private final Prism plugin;

   public WandCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      String type = "i";
      boolean isInspect = call.getArg(0).equalsIgnoreCase("inspect") || call.getArg(0).equalsIgnoreCase("i");
      if (!isInspect) {
         if (call.getArgs().length < 2) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("You need to specify a wand type. Use '/prism ?' for help."));
            return;
         }

         type = call.getArg(1);
      }

      Wand oldwand = null;
      if (Prism.playersWithActiveTools.containsKey(call.getPlayer().getName())) {
         oldwand = (Wand)Prism.playersWithActiveTools.get(call.getPlayer().getName());
      }

      Prism.playersWithActiveTools.remove(call.getPlayer().getName());
      String mode = this.plugin.getConfig().getString("prism.wands.default-mode");
      if (this.plugin.getConfig().getBoolean("prism.wands.allow-user-override")) {
         String personalMode = Settings.getSetting("wand.mode", call.getPlayer());
         if (personalMode != null) {
            mode = personalMode;
         }
      }

      int item_id = 0;
      byte item_subid = -1;
      String toolKey = null;
      if (mode.equals("item")) {
         toolKey = this.plugin.getConfig().getString("prism.wands.default-item-mode-id");
      } else if (mode.equals("block")) {
         toolKey = this.plugin.getConfig().getString("prism.wands.default-block-mode-id");
      }

      String wandOn;
      if (this.plugin.getConfig().getBoolean("prism.wands.allow-user-override")) {
         wandOn = Settings.getSetting("wand.item", call.getPlayer());
         if (wandOn != null) {
            toolKey = wandOn;
         }
      }

      if (toolKey != null) {
         if (!toolKey.contains(":")) {
            toolKey = toolKey + ":0";
         }

         String[] toolKeys = toolKey.split(":");
         item_id = Integer.parseInt(toolKeys[0]);
         item_subid = Byte.parseByte(toolKeys[1]);
      }

      wandOn = "";
      String item_name = "";
      String parameters = "";
      if (item_id != 0) {
         item_name = Prism.getItems().getAlias(item_id, item_subid);
         wandOn = wandOn + " on a " + item_name;
      }

      for(int i = isInspect ? 1 : 2; i < call.getArgs().length; ++i) {
         if (parameters.isEmpty()) {
            parameters = parameters + " using:" + ChatColor.GRAY;
         }

         parameters = parameters + " " + call.getArg(i);
      }

      if (!ItemUtils.isAcceptableWand(item_id, item_subid)) {
         call.getPlayer().sendMessage(Prism.messenger.playerError("Sorry, but you may not use " + item_name + " for a wand."));
      } else {
         boolean enabled = false;
         Wand wand = null;
         if (!type.equalsIgnoreCase("i") && !type.equalsIgnoreCase("inspect")) {
            if (!type.equalsIgnoreCase("p") && !type.equalsIgnoreCase("profile")) {
               if (!type.equalsIgnoreCase("rollback") && !type.equalsIgnoreCase("rb")) {
                  if (!type.equalsIgnoreCase("restore") && !type.equalsIgnoreCase("rs")) {
                     if (!type.equalsIgnoreCase("off")) {
                        call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid wand type. Use /prism ? for help."));
                        return;
                     }

                     call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Current wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
                  } else {
                     if (!call.getPlayer().hasPermission("prism.restore") && !call.getPlayer().hasPermission("prism.wand.restore")) {
                        call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
                        return;
                     }

                     if (oldwand != null && oldwand instanceof RestoreWand) {
                        call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Restore wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
                     } else {
                        wand = new RestoreWand(this.plugin);
                        call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Restore wand " + ChatColor.GREEN + "enabled" + ChatColor.WHITE + wandOn + parameters + "."));
                        enabled = true;
                     }
                  }
               } else {
                  if (!call.getPlayer().hasPermission("prism.rollback") && !call.getPlayer().hasPermission("prism.wand.rollback")) {
                     call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
                     return;
                  }

                  if (oldwand != null && oldwand instanceof RollbackWand) {
                     call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Rollback wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
                  } else {
                     wand = new RollbackWand(this.plugin);
                     call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Rollback wand " + ChatColor.GREEN + "enabled" + ChatColor.WHITE + wandOn + parameters + "."));
                     enabled = true;
                  }
               }
            } else {
               if (!call.getPlayer().hasPermission("prism.lookup") && !call.getPlayer().hasPermission("prism.wand.profile")) {
                  call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
                  return;
               }

               if (oldwand != null && oldwand instanceof ProfileWand) {
                  call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Profile wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
               } else {
                  wand = new ProfileWand();
                  call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Profile wand " + ChatColor.GREEN + "enabled" + ChatColor.WHITE + wandOn + "."));
                  enabled = true;
               }
            }
         } else {
            if (!call.getPlayer().hasPermission("prism.lookup") && !call.getPlayer().hasPermission("prism.wand.inspect")) {
               call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
               return;
            }

            if (oldwand != null && oldwand instanceof InspectorWand) {
               call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Inspection wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
            } else {
               wand = new InspectorWand(this.plugin);
               call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Inspection wand " + ChatColor.GREEN + "enabled" + ChatColor.WHITE + wandOn + parameters + "."));
               enabled = true;
            }
         }

         PlayerInventory inv = call.getPlayer().getInventory();
         if (enabled) {
            ((Wand)wand).setWandMode(mode);
            ((Wand)wand).setItemId(item_id);
            ((Wand)wand).setItemSubId(item_subid);
            Prism.debug("Wand activated for player - mode: " + mode + " Item:" + item_id + ":" + item_subid);
            if (this.plugin.getConfig().getBoolean("prism.wands.auto-equip")) {
               if (!InventoryUtils.moveItemToHand(inv, item_id, item_subid)) {
                  ((Wand)wand).setOriginallyHeldItem(inv.getItemInHand());
                  if (InventoryUtils.handItemToPlayer(inv, new ItemStack(item_id, 1, (short)item_subid))) {
                     ((Wand)wand).setItemWasGiven(true);
                  } else {
                     call.getPlayer().sendMessage(Prism.messenger.playerError("Can't fit the wand item into your inventory."));
                  }
               }

               call.getPlayer().updateInventory();
            }

            if (wand instanceof QueryWandBase && !((QueryWandBase)wand).setParameters(call.getPlayer(), call.getArgs(), isInspect ? 1 : 2)) {
               call.getPlayer().sendMessage(Prism.messenger.playerError("Notice: Only some parameters used.."));
            }

            Prism.playersWithActiveTools.put(call.getPlayer().getName(), wand);
         } else if (oldwand != null) {
            oldwand.disable(call.getPlayer());
         }

      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
