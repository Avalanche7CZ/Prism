package com.helion3.prism.libs.elixr;

import java.util.Iterator;
import java.util.Map;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemUtils {
   public static String getUsedDurabilityPercentage(ItemStack item) {
      short dura = item.getDurability();
      short max_dura = item.getType().getMaxDurability();
      if (dura > 0 && max_dura > 0 && dura != max_dura) {
         double diff = (double)(dura / max_dura * 100);
         if (diff > 0.0) {
            return Math.floor(diff) + "%";
         }
      }

      return "";
   }

   public static String getDurabilityPercentage(ItemStack item) {
      short dura = item.getDurability();
      short max_dura = item.getType().getMaxDurability();
      if (dura > 0 && max_dura > 0 && dura != max_dura) {
         double diff = (double)(max_dura - dura);
         diff = diff / (double)max_dura * 100.0;
         return diff > 0.0 ? Math.floor(diff) + "%" : "0%";
      } else {
         return "";
      }
   }

   public static String getItemFullNiceName(ItemStack item, MaterialAliases aliases) {
      String item_name = "";
      if (item.getType().name().contains("LEATHER_")) {
         LeatherArmorMeta lam = (LeatherArmorMeta)item.getItemMeta();
         if (lam.getColor() != null) {
            item_name = item_name + "dyed ";
         }
      } else if (item.getType().equals(Material.SKULL_ITEM)) {
         SkullMeta skull = (SkullMeta)item.getItemMeta();
         if (skull.hasOwner()) {
            item_name = item_name + skull.getOwner() + "'s ";
         }
      }

      if (dataValueUsedForSubitems(item.getTypeId())) {
         item_name = item_name + aliases.getAlias(item.getTypeId(), item.getDurability());
      } else {
         item_name = item_name + aliases.getAlias(item.getTypeId(), 0);
      }

      if (item_name.isEmpty()) {
         item_name = item_name + item.getType().toString().toLowerCase().replace("_", " ");
      }

      if (item.getTypeId() == 145) {
         if (item.getDurability() == 1) {
            item_name = "slightly damaged anvil";
         } else if (item.getDurability() == 2) {
            item_name = "very damaged anvil";
         }
      }

      if (item.getType().equals(Material.WRITTEN_BOOK)) {
         BookMeta meta = (BookMeta)item.getItemMeta();
         if (meta != null) {
            item_name = item_name + " '" + meta.getTitle() + "' by " + meta.getAuthor();
         }
      } else if (item.getType().equals(Material.ENCHANTED_BOOK)) {
         EnchantmentStorageMeta bookEnchantments = (EnchantmentStorageMeta)item.getItemMeta();
         if (bookEnchantments.hasStoredEnchants()) {
            int i = 1;
            Map enchs = bookEnchantments.getStoredEnchants();
            if (enchs.size() > 0) {
               item_name = item_name + " with";

               for(Iterator var6 = enchs.entrySet().iterator(); var6.hasNext(); ++i) {
                  Map.Entry ench = (Map.Entry)var6.next();
                  item_name = item_name + " " + EnchantmentUtils.getClientSideEnchantmentName((Enchantment)ench.getKey(), (Integer)ench.getValue());
                  item_name = item_name + (i < enchs.size() ? ", " : "");
               }
            }
         }
      }

      int i = 1;
      Map enchs = item.getEnchantments();
      if (enchs.size() > 0) {
         item_name = item_name + " with";

         for(Iterator var16 = enchs.entrySet().iterator(); var16.hasNext(); ++i) {
            Map.Entry ench = (Map.Entry)var16.next();
            item_name = item_name + " " + EnchantmentUtils.getClientSideEnchantmentName((Enchantment)ench.getKey(), (Integer)ench.getValue());
            item_name = item_name + (i < enchs.size() ? ", " : "");
         }
      }

      if (item.getTypeId() == 402) {
         FireworkEffectMeta fireworkMeta = (FireworkEffectMeta)item.getItemMeta();
         if (fireworkMeta.hasEffect()) {
            FireworkEffect effect = fireworkMeta.getEffect();
            if (!effect.getColors().isEmpty()) {
               item_name = item_name + " " + effect.getColors().size() + " colors";
            }

            if (!effect.getFadeColors().isEmpty()) {
               item_name = item_name + " " + effect.getFadeColors().size() + " fade colors";
            }

            if (effect.hasFlicker()) {
               item_name = item_name + " flickering";
            }

            if (effect.hasTrail()) {
               item_name = item_name + " with trail";
            }
         }
      }

      ItemMeta im = item.getItemMeta();
      if (im != null) {
         String displayName = im.getDisplayName();
         if (displayName != null) {
            item_name = item_name + ", named \"" + displayName + "\"";
         }
      }

      return item_name;
   }

   public static boolean dataValueUsedForSubitems(int id) {
      return id == 17 || id == 18 || id == 24 || id == 31 || id == 35 || id == 43 || id == 44 || id == 98 || id == 139 || id == 263 || id == 351 || id == 6 || id == 373 || id == 383 || id == 397;
   }

   public static boolean canSafelyStack(ItemStack item) {
      if (item.getMaxStackSize() == 1) {
         return false;
      } else {
         ItemMeta im = item.getItemMeta();
         return !im.hasDisplayName() && !im.hasEnchants() && !im.hasLore();
      }
   }

   public static void dropItem(Location location, ItemStack itemStack) {
      location.getWorld().dropItemNaturally(location, itemStack);
   }

   public static void dropItem(Location location, ItemStack is, int quantity) {
      for(int i = 0; i < quantity; ++i) {
         dropItem(location, is);
      }

   }
}
