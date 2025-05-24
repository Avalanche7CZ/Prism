package me.botsko.prism.utils;

import me.botsko.prism.Prism;
import me.botsko.prism.wands.Wand;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WandUtils {
   public static boolean playerUsesWandOnClick(Player player, Location loc) {
      if (Prism.playersWithActiveTools.containsKey(player.getName())) {
         Wand wand = (Wand)Prism.playersWithActiveTools.get(player.getName());
         if (wand == null) {
            return false;
         }

         int item_id = wand.getItemId();
         byte item_subid = wand.getItemSubId();
         if (player.getItemInHand().getTypeId() == item_id && player.getItemInHand().getDurability() == item_subid) {
            wand.playerLeftClick(player, loc);
            return true;
         }
      }

      return false;
   }
}
