package me.botsko.elixr;

import java.util.Arrays;
import org.bukkit.entity.Player;

public class ExperienceUtils {
   public static final int MAX_LEVEL_SUPPORTED = 150;
   private static final int[] xpRequiredForNextLevel = new int[150];
   private static final int[] xpTotalToReachLevel = new int[150];

   static {
      xpTotalToReachLevel[0] = 0;
      int incr = 7;

      for(int i = 1; i < xpTotalToReachLevel.length; ++i) {
         xpRequiredForNextLevel[i - 1] = incr;
         xpTotalToReachLevel[i] = xpTotalToReachLevel[i - 1] + incr;
         incr += i % 2 == 0 ? 4 : 3;
      }

   }

   public static void changeExp(Player player, int amt) {
      int xp = getCurrentExp(player) + amt;
      if (xp < 0) {
         xp = 0;
      }

      int curLvl = player.getLevel();
      int newLvl = getCurrentLevel(xp);
      if (curLvl != newLvl) {
         player.setLevel(newLvl);
      }

      float pct = (float)(xp - xpTotalToReachLevel[newLvl]) / (float)xpRequiredForNextLevel[newLvl];
      player.setExp(pct);
   }

   public static int getCurrentExp(Player player) {
      int lvl = player.getLevel();
      return xpTotalToReachLevel[lvl] + (int)((float)xpRequiredForNextLevel[lvl] * player.getExp());
   }

   public static boolean hasExp(Player player, int amt) {
      return getCurrentExp(player) >= amt;
   }

   public static int getCurrentLevel(int exp) {
      if (exp <= 0) {
         return 0;
      } else {
         int pos = Arrays.binarySearch(xpTotalToReachLevel, exp);
         return pos < 0 ? -pos - 2 : pos;
      }
   }
}
