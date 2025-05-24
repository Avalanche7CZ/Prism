package com.helion3.prism.libs.elixr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class MaterialAliases {
   protected HashMap itemAliases = new HashMap();

   public MaterialAliases() {
      FileConfiguration items = null;
      InputStream defConfigStream = this.getClass().getResourceAsStream("/items.yml");
      if (defConfigStream != null) {
         System.out.println("Elixr: Loaded items directory");
         items = YamlConfiguration.loadConfiguration(defConfigStream);
      }

      if (items != null) {
         Map itemaliases = items.getConfigurationSection("items").getValues(false);
         if (itemaliases != null) {
            Iterator var4 = itemaliases.keySet().iterator();

            while(true) {
               String key;
               ArrayList aliases;
               do {
                  if (!var4.hasNext()) {
                     return;
                  }

                  key = (String)var4.next();
                  aliases = (ArrayList)itemaliases.get(key);
               } while(aliases.size() <= 0);

               Iterator var7 = aliases.iterator();

               while(var7.hasNext()) {
                  String alias = (String)var7.next();
                  this.itemAliases.put(key, alias);
               }
            }
         }
      } else {
         System.out.println("ERROR: The Elixr library was unable to load an internal item alias list.");
      }

   }

   public HashMap getItemAliases() {
      return this.itemAliases;
   }

   public String getAlias(int typeid, int subid) {
      String item_name = null;
      if (!this.itemAliases.isEmpty()) {
         String key = typeid + ":" + subid;
         item_name = (String)this.itemAliases.get(key);
      }

      if (item_name == null) {
         ItemStack i = new ItemStack(typeid, subid);
         item_name = i.getType().name().toLowerCase().replace("_", " ");
      }

      return item_name;
   }

   public String getAlias(ItemStack i) {
      return this.getAlias(i.getTypeId(), (byte)i.getDurability());
   }

   public ArrayList getIdsByAlias(String alias) {
      ArrayList itemIds = new ArrayList();
      if (!this.itemAliases.isEmpty()) {
         Iterator var3 = this.itemAliases.entrySet().iterator();

         while(var3.hasNext()) {
            Map.Entry entry = (Map.Entry)var3.next();
            int[] ids = new int[2];
            if (((String)entry.getValue()).equals(alias)) {
               String[] _tmp = ((String)entry.getKey()).split(":");
               ids[0] = Integer.parseInt(_tmp[0]);
               ids[1] = Integer.parseInt(_tmp[1]);
               itemIds.add(ids);
            }
         }
      }

      return itemIds;
   }
}
