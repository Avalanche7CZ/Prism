package me.botsko.prism.parameters;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.commandlibs.Flag;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

public class FlagParameter implements PrismParameterHandler {
   public String getName() {
      return "Flag";
   }

   public String[] getHelp() {
      return new String[0];
   }

   public boolean applicable(String parameter, CommandSender sender) {
      return Pattern.compile("(-)([^\\s]+)?").matcher(parameter).matches();
   }

   public void process(QueryParameters query, String parameter, CommandSender sender) {
      String[] flagComponents = parameter.substring(1).split("=");

      Flag flag;
      try {
         flag = Flag.valueOf(flagComponents[0].replace("-", "_").toUpperCase());
      } catch (IllegalArgumentException var11) {
         throw new IllegalArgumentException("Flag -" + flagComponents[0] + " not found", var11);
      }

      if (!query.hasFlag(flag)) {
         query.addFlag(flag);
         if (flagComponents.length > 1) {
            if (flag.equals(Flag.PER_PAGE)) {
               if (!TypeUtils.isNumeric(flagComponents[1])) {
                  throw new IllegalArgumentException("Per-page flag value must be a number. Use /prism ? for help.");
               }

               query.setPerPage(Integer.parseInt(flagComponents[1]));
            } else if (flag.equals(Flag.SHARE)) {
               String[] arr$ = flagComponents[1].split(",");
               int len$ = arr$.length;

               for(int i$ = 0; i$ < len$; ++i$) {
                  String sharePlayer = arr$[i$];
                  if (sharePlayer.equals(sender.getName())) {
                     throw new IllegalArgumentException("You can't share lookup results with yourself!");
                  }

                  Player shareWith = Bukkit.getServer().getPlayer(sharePlayer);
                  if (shareWith == null) {
                     throw new IllegalArgumentException("Can't share with " + sharePlayer + ". Are they online?");
                  }

                  query.addSharedPlayer(shareWith);
               }
            }
         }
      }

   }

   public void defaultTo(QueryParameters query, CommandSender sender) {
   }

   public List tabComplete(String partialParameter, CommandSender sender) {
      String[] flagComponents = partialParameter.substring(1).split("=", 2);
      String name = flagComponents[0].replace("-", "_").toUpperCase();

      Flag flag;
      try {
         flag = Flag.valueOf(name);
      } catch (IllegalArgumentException var13) {
         List completions = new ArrayList();
         Flag[] arr$ = Flag.values();
         int len$ = arr$.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            Flag possibleFlag = arr$[i$];
            String flagName = possibleFlag.toString();
            if (flagName.startsWith(name)) {
               completions.add("-" + flagName.replace('_', '-').toLowerCase());
            }
         }

         return completions;
      }

      if (flagComponents.length <= 1) {
         return null;
      } else {
         String prefix = "-" + flag.toString().replace('_', '-').toLowerCase() + "=";
         if (flag.equals(Flag.SHARE)) {
            String value = flagComponents[1];
            int end = value.lastIndexOf(44);
            String partialName = value;
            if (end != -1) {
               partialName = value.substring(end + 1);
               prefix = prefix + value.substring(0, end) + ",";
            }

            partialName = partialName.toLowerCase();
            List completions = new ArrayList();
            Iterator i$ = Bukkit.getOnlinePlayers().iterator();

            while(i$.hasNext()) {
               Player player = (Player)i$.next();
               if (player.getName().toLowerCase().startsWith(partialName)) {
                  completions.add(prefix + player.getName());
               }
            }

            return completions;
         } else {
            return null;
         }
      }
   }

   public boolean hasPermission(String parameter, Permissible permissible) {
      String[] flagComponents = parameter.substring(1).split("=");

      Flag flag;
      try {
         flag = Flag.valueOf(flagComponents[0].replace("-", "_").toUpperCase());
      } catch (IllegalArgumentException var6) {
         return false;
      }

      return flag.hasPermission(permissible);
   }
}
