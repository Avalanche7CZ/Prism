package me.botsko.prism.wands;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface Wand {
   void playerLeftClick(Player var1, Location var2);

   void playerRightClick(Player var1, Location var2);

   void playerRightClick(Player var1, Entity var2);

   void setItemWasGiven(boolean var1);

   boolean itemWasGiven();

   void setWandMode(String var1);

   String getWandMode();

   int getItemId();

   void setItemId(int var1);

   byte getItemSubId();

   void setItemSubId(byte var1);

   void setItemFromKey(String var1);

   void setOriginallyHeldItem(ItemStack var1);

   void disable(Player var1);
}
