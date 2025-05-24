package me.botsko.prism.utils;

import com.google.common.base.CaseFormat;
import com.helion3.prism.libs.elixr.TypeUtils;
import com.helion3.pste.api.Paste;
import com.helion3.pste.api.PsteApi;
import com.helion3.pste.api.Results;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.appliers.PrismProcessType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class MiscUtils {
   public static int clampRadius(Player player, int desiredRadius, PrismProcessType processType, FileConfiguration config) {
      if (desiredRadius <= 0) {
         return config.getInt("prism.near.default-radius");
      } else {
         int max_lookup_radius = config.getInt("prism.queries.max-lookup-radius");
         if (max_lookup_radius <= 0) {
            max_lookup_radius = 5;
            Prism.log("Max lookup radius may not be lower than one. Using safe inputue of five.");
         }

         int max_applier_radius = config.getInt("prism.queries.max-applier-radius");
         if (max_applier_radius <= 0) {
            max_applier_radius = 5;
            Prism.log("Max applier radius may not be lower than one. Using safe inputue of five.");
         }

         if (processType.equals(PrismProcessType.LOOKUP) && desiredRadius > max_lookup_radius) {
            return player != null && !player.hasPermission("prism.override-max-lookup-radius") ? max_lookup_radius : desiredRadius;
         } else if (!processType.equals(PrismProcessType.LOOKUP) && desiredRadius > max_applier_radius) {
            return player != null && !player.hasPermission("prism.override-max-applier-radius") ? max_applier_radius : desiredRadius;
         } else {
            return desiredRadius;
         }
      }
   }

   public static String paste_results(Prism prism, String results) {
      String prismWebUrl = "https://pste.me/";
      if (!prism.getConfig().getBoolean("prism.paste.enable")) {
         return Prism.messenger.playerError("PSTE.me paste bin support is currently disabled by config.");
      } else {
         String apiUsername = prism.getConfig().getString("prism.paste.username");
         String apiKey = prism.getConfig().getString("prism.paste.api-key");
         if (!apiKey.matches("[0-9a-z]+")) {
            return Prism.messenger.playerError("Invalid API key.");
         } else {
            PsteApi api = new PsteApi(apiUsername, apiKey);

            try {
               Paste paste = new Paste();
               paste.setPaste(results);
               Results response = api.createPaste(paste);
               return Prism.messenger.playerSuccess("Successfully pasted results: https://pste.me/#/" + response.getResults().getSlug());
            } catch (Exception var8) {
               Prism.debug(var8.toString());
               return Prism.messenger.playerError("Unable to paste results (" + ChatColor.YELLOW + var8.getMessage() + ChatColor.RED + ").");
            }
         }
      }
   }

   public static List getStartingWith(String start, Iterable options, boolean caseSensitive) {
      List result = new ArrayList();
      Iterator i$;
      String option;
      if (caseSensitive) {
         i$ = options.iterator();

         while(i$.hasNext()) {
            option = (String)i$.next();
            if (option.startsWith(start)) {
               result.add(option);
            }
         }
      } else {
         start = start.toLowerCase();
         i$ = options.iterator();

         while(i$.hasNext()) {
            option = (String)i$.next();
            if (option.toLowerCase().startsWith(start)) {
               result.add(option);
            }
         }
      }

      return result;
   }

   public static List getStartingWith(String arg, Iterable options) {
      return getStartingWith(arg, options, true);
   }

   public static void dispatchAlert(String msg, List commands) {
      String colorized = TypeUtils.colorize(msg);
      String stripped = ChatColor.stripColor(colorized);
      Iterator i$ = commands.iterator();

      while(i$.hasNext()) {
         String command = (String)i$.next();
         if (!command.equals("examplecommand <alert>")) {
            String processedCommand = command.replace("<alert>", stripped);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
         }
      }

   }

   public static String getEntityName(Entity entity) {
      if (entity == null) {
         return "unknown";
      } else {
         return entity.getType() == EntityType.PLAYER ? ((Player)entity).getName() : CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, entity.getType().name());
      }
   }
}
