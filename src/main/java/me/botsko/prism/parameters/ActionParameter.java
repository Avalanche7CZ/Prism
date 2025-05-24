package me.botsko.prism.parameters;

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.utils.LevenshteinDistance;
import org.bukkit.command.CommandSender;

public class ActionParameter extends SimplePrismParameterHandler {
   public ActionParameter() {
      super("Action", Pattern.compile("[~|!]?[\\w,-]+"), "a");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      MatchRule match = MatchRule.INCLUDE;
      if (input.startsWith("!")) {
         match = MatchRule.EXCLUDE;
      }

      String[] actions = input.split(",");
      if (actions.length > 0) {
         String[] arr$ = actions;
         int len$ = actions.length;

         label65:
         for(int i$ = 0; i$ < len$; ++i$) {
            String action = arr$[i$];
            ArrayList actionTypes = Prism.getActionRegistry().getActionsByShortname(action.replace("!", ""));
            if (actionTypes.isEmpty()) {
               if (sender != null) {
                  sender.sendMessage(Prism.messenger.playerError("Ignoring action '" + action.replace("!", "") + "' because it's unrecognized. Did you mean '" + LevenshteinDistance.getClosestAction(action) + "'? Type '/prism params' for help."));
               }
            } else {
               List noPermission = new ArrayList();
               Iterator i$ = actionTypes.iterator();

               while(true) {
                  ActionType actionType;
                  do {
                     do {
                        if (!i$.hasNext()) {
                           if (!noPermission.isEmpty()) {
                              String message = "Ignoring action '" + action + "' because you don't have permission for ";
                              if (noPermission.size() != 1) {
                                 message = message + "any of " + Joiner.on(',').join(noPermission) + ".";
                              } else if (((String)noPermission.get(0)).equals(action)) {
                                 message = message + "it.";
                              } else {
                                 message = message + (String)noPermission.get(0) + ".";
                              }

                              sender.sendMessage(Prism.messenger.playerError(message));
                           }
                           continue label65;
                        }

                        actionType = (ActionType)i$.next();
                     } while(query.getProcessType().equals(PrismProcessType.ROLLBACK) && !actionType.canRollback());
                  } while(query.getProcessType().equals(PrismProcessType.RESTORE) && !actionType.canRestore());

                  if (!sender.hasPermission(this.getPermission() + "." + actionType.getName())) {
                     noPermission.add(actionType.getName());
                  } else {
                     query.addActionType(actionType.getName(), match);
                  }
               }
            }
         }

         if (query.getActionTypes().size() == 0) {
            throw new IllegalArgumentException("Action parameter value not recognized. Try /pr ? for help");
         }
      }

   }
}
