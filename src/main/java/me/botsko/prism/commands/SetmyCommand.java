package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.settings.Settings;
import me.botsko.prism.utils.ItemUtils;
import me.botsko.prism.wands.Wand;
import org.bukkit.ChatColor;

public class SetmyCommand implements SubHandler {
   private final Prism plugin;

   public SetmyCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      String setType = null;
      if (call.getArgs().length >= 2) {
         setType = call.getArg(1);
      }

      if (setType != null && !setType.equalsIgnoreCase("wand")) {
         call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid arguments. Use /prism ? for help."));
      } else {
         if (!this.plugin.getConfig().getBoolean("prism.wands.allow-user-override")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Sorry, but personalizing the wand is currently not allowed."));
         }

         if (!call.getPlayer().hasPermission("prism.rollback") && !call.getPlayer().hasPermission("prism.restore") && !call.getPlayer().hasPermission("prism.wand.*") && !call.getPlayer().hasPermission("prism.wand.inspect") && !call.getPlayer().hasPermission("prism.wand.profile") && !call.getPlayer().hasPermission("prism.wand.rollback") && !call.getPlayer().hasPermission("prism.wand.restore")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("You do not have permission for this."));
         } else {
            if (Prism.playersWithActiveTools.containsKey(call.getPlayer().getName())) {
               Wand oldwand = (Wand)Prism.playersWithActiveTools.get(call.getPlayer().getName());
               oldwand.disable(call.getPlayer());
               Prism.playersWithActiveTools.remove(call.getPlayer().getName());
               call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Current wand " + ChatColor.RED + "disabled" + ChatColor.WHITE + "."));
            }

            String setSubType = null;
            if (call.getArgs().length >= 3) {
               setSubType = call.getArg(2);
            }

            String setWandItem;
            if (setSubType != null && setSubType.equalsIgnoreCase("mode")) {
               setWandItem = null;
               if (call.getArgs().length >= 4) {
                  setWandItem = call.getArg(3);
               }

               if (setWandItem == null || !setWandItem.equals("hand") && !setWandItem.equals("item") && !setWandItem.equals("block")) {
                  call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid arguments. Use /prism ? for help."));
               } else {
                  Settings.saveSetting("wand.mode", setWandItem, call.getPlayer());
                  Settings.deleteSetting("wand.item", call.getPlayer());
                  call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Changed your personal wand to " + ChatColor.GREEN + setWandItem + ChatColor.WHITE + " mode."));
               }
            } else if (setSubType.equalsIgnoreCase("item")) {
               setWandItem = null;
               if (call.getArgs().length >= 4) {
                  setWandItem = call.getArg(3);
               }

               if (setWandItem != null) {
                  if (!TypeUtils.isNumeric(setWandItem)) {
                     ArrayList itemIds = Prism.getItems().getIdsByAlias(setWandItem);
                     if (itemIds.size() <= 0) {
                        call.getPlayer().sendMessage(Prism.messenger.playerError("There's no item matching that name."));
                        return;
                     }

                     int[] ids = (int[])itemIds.get(0);
                     setWandItem = ids[0] + ":" + ids[1];
                  }

                  if (!setWandItem.contains(":")) {
                     setWandItem = setWandItem + ":0";
                  }

                  int item_id = -1;
                  byte item_subid = 0;
                  String[] itemIds = setWandItem.split(":");
                  if (itemIds.length == 2) {
                     item_id = Integer.parseInt(itemIds[0]);
                     item_subid = Byte.parseByte(itemIds[1]);
                  }

                  if (item_id > -1) {
                     String item_name = Prism.getItems().getAlias(item_id, item_subid);
                     if (item_name != null) {
                        if (!ItemUtils.isAcceptableWand(item_id, item_subid)) {
                           call.getPlayer().sendMessage(Prism.messenger.playerError("Sorry, but you may not use " + item_name + " for a wand."));
                           return;
                        }

                        Settings.saveSetting("wand.item", setWandItem, call.getPlayer());
                        call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Changed your personal wand item to " + ChatColor.GREEN + item_name + ChatColor.WHITE + "."));
                        return;
                     }
                  }
               }

               call.getPlayer().sendMessage(Prism.messenger.playerError("Invalid arguments. Use /prism ? for help."));
            }
         }
      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
