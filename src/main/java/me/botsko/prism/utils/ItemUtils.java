package me.botsko.prism.utils;

public class ItemUtils extends com.helion3.prism.libs.elixr.ItemUtils {
   public static boolean isAcceptableWand(int item_id, byte sub_id) {
      if (item_id >= 8 && item_id <= 11) {
         return false;
      } else if (item_id != 51 && item_id != 259) {
         if (item_id != 90 && item_id != 119) {
            return item_id != 383;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }
}
