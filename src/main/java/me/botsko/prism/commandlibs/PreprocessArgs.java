package me.botsko.prism.commandlibs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.parameters.PrismParameterHandler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PreprocessArgs {
   public static QueryParameters process(Prism plugin, CommandSender sender, String[] args, PrismProcessType processType, int startAt, boolean useDefaults) {
      return process(plugin, sender, args, processType, startAt, useDefaults, false);
   }

   public static QueryParameters process(Prism plugin, CommandSender sender, String[] args, PrismProcessType processType, int startAt, boolean useDefaults, boolean optional) {
      Player player = null;
      if (sender != null && sender instanceof Player) {
         player = (Player)sender;
      }

      QueryParameters parameters = new QueryParameters();
      parameters.setProcessType(processType);
      if (parameters.getProcessType().equals(PrismProcessType.LOOKUP)) {
         parameters.setLimit(plugin.getConfig().getInt("prism.queries.lookup-max-results"));
         parameters.setPerPage(plugin.getConfig().getInt("prism.queries.default-results-per-page"));
      }

      HashMap registeredParams = Prism.getParameters();
      Set foundArgsNames = new HashSet();
      List foundArgsList = new ArrayList();
      if (args == null) {
         return parameters;
      } else {
         for(int i = startAt; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.isEmpty() && parseParam(plugin, sender, parameters, registeredParams, foundArgsNames, foundArgsList, arg) == PreprocessArgs.ParseResult.NotFound) {
               return null;
            }
         }

         parameters.setFoundArgs(foundArgsNames);
         if (foundArgsList.isEmpty() && !optional) {
            if (sender != null) {
               sender.sendMessage(Prism.messenger.playerError("You're missing valid parameters. Use /prism ? for assistance."));
            }

            return null;
         } else {
            Iterator i$;
            if (useDefaults) {
               i$ = registeredParams.entrySet().iterator();

               while(i$.hasNext()) {
                  Map.Entry entry = (Map.Entry)i$.next();
                  if (!foundArgsNames.contains(((String)entry.getKey()).toLowerCase())) {
                     ((PrismParameterHandler)entry.getValue()).defaultTo(parameters, sender);
                  }
               }
            }

            i$ = foundArgsList.iterator();

            while(i$.hasNext()) {
               MatchedParam matchedParam = (MatchedParam)i$.next();

               try {
                  PrismParameterHandler handler = matchedParam.getHandler();
                  handler.process(parameters, matchedParam.getArg(), sender);
               } catch (IllegalArgumentException var15) {
                  if (sender != null) {
                     sender.sendMessage(Prism.messenger.playerError(var15.getMessage()));
                  }

                  return null;
               }
            }

            if (sender != null && sender.hasPermission("prism.parameters.action.required") && parameters.getActionTypes().isEmpty()) {
               sender.sendMessage(Prism.messenger.playerError("You're missing valid actions. Use /prism ? for assistance."));
               return null;
            } else {
               if (player != null && !plugin.getConfig().getBoolean("prism.queries.never-use-defaults") && parameters.getPlayerLocation() == null && (parameters.getMaxLocation() == null || parameters.getMinLocation() == null)) {
                  parameters.setMinMaxVectorsFromPlayerLocation(player.getLocation());
               }

               return parameters;
            }
         }
      }
   }

   private static ParseResult parseParam(Prism plugin, CommandSender sender, QueryParameters parameters, HashMap registeredParams, Set foundArgsNames, List foundArgsList, String arg) {
      ParseResult result = PreprocessArgs.ParseResult.NotFound;
      Iterator i$ = registeredParams.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         PrismParameterHandler parameterHandler = (PrismParameterHandler)entry.getValue();
         if (parameterHandler.applicable(arg, sender)) {
            if (parameterHandler.hasPermission(arg, sender)) {
               result = PreprocessArgs.ParseResult.Found;
               foundArgsList.add(new MatchedParam(parameterHandler, arg));
               foundArgsNames.add(parameterHandler.getName().toLowerCase());
               break;
            }

            result = PreprocessArgs.ParseResult.NoPermission;
         }
      }

      if (result == PreprocessArgs.ParseResult.NotFound) {
         Player autoFillPlayer = plugin.getServer().getPlayer(arg);
         if (autoFillPlayer != null) {
            MatchRule match = MatchRule.INCLUDE;
            if (arg.startsWith("!")) {
               match = MatchRule.EXCLUDE;
            }

            result = PreprocessArgs.ParseResult.Found;
            parameters.addPlayerName(arg.replace("!", ""), match);
         }
      }

      switch (result) {
         case NotFound:
            if (sender != null) {
               sender.sendMessage(Prism.messenger.playerError("Unrecognized parameter '" + arg + "'. Use /prism ? for help."));
            }
            break;
         case NoPermission:
            if (sender != null) {
               sender.sendMessage(Prism.messenger.playerError("No permission for parameter '" + arg + "', skipped."));
            }
      }

      return result;
   }

   public static List complete(CommandSender sender, String[] args, int arg) {
      return args != null && args.length > arg ? complete(sender, args[arg]) : null;
   }

   public static List complete(CommandSender sender, String[] args) {
      return complete(sender, args, args.length - 1);
   }

   public static List complete(CommandSender sender, String arg) {
      if (arg.isEmpty()) {
         return null;
      } else {
         HashMap registeredParams = Prism.getParameters();
         Iterator i$ = registeredParams.entrySet().iterator();

         Map.Entry entry;
         do {
            if (!i$.hasNext()) {
               return null;
            }

            entry = (Map.Entry)i$.next();
         } while(!((PrismParameterHandler)entry.getValue()).applicable(arg, sender) || !((PrismParameterHandler)entry.getValue()).hasPermission(arg, sender));

         return ((PrismParameterHandler)entry.getValue()).tabComplete(arg, sender);
      }
   }

   private static enum ParseResult {
      NotFound,
      NoPermission,
      Found;
   }

   private static class MatchedParam {
      private final PrismParameterHandler handler;
      private final String arg;

      public MatchedParam(PrismParameterHandler handler, String arg) {
         this.handler = handler;
         this.arg = arg;
      }

      public PrismParameterHandler getHandler() {
         return this.handler;
      }

      public String getArg() {
         return this.arg;
      }
   }
}
