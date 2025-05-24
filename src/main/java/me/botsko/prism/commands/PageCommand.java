package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PageCommand implements SubHandler {
   private final Prism plugin;

   public PageCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      String keyName = "console";
      if (call.getSender() instanceof Player) {
         keyName = call.getSender().getName();
      }

      if (!this.plugin.cachedQueries.containsKey(keyName)) {
         call.getSender().sendMessage(Prism.messenger.playerError("There's no saved query to paginate. Maybe they expired? Try your lookup again."));
      } else {
         QueryResult results = (QueryResult)this.plugin.cachedQueries.get(keyName);
         if (call.getArgs().length != 2) {
            call.getSender().sendMessage(Prism.messenger.playerError("Please specify a page number. Like /prism page 2"));
         } else {
            int page;
            if (TypeUtils.isNumeric(call.getArg(1))) {
               page = Integer.parseInt(call.getArg(1));
            } else if (!call.getArg(1).equals("next") && !call.getArg(1).equals("n")) {
               if (!call.getArg(1).equals("prev") && !call.getArg(1).equals("p")) {
                  call.getSender().sendMessage(Prism.messenger.playerError("Page numbers need to actually be numbers, or next/prev. Like /prism page 2"));
                  return;
               }

               if (results.getPage() <= 1) {
                  call.getSender().sendMessage(Prism.messenger.playerError("There is no previous page."));
                  return;
               }

               page = results.getPage() - 1;
            } else {
               page = results.getPage() + 1;
            }

            if (page <= 0) {
               call.getSender().sendMessage(Prism.messenger.playerError("Page must be greater than zero."));
            } else {
               results.setPage(page);
               results.setQueryTime();
               this.plugin.cachedQueries.replace(keyName, results);
               if (results.getActionResults().isEmpty()) {
                  call.getSender().sendMessage(Prism.messenger.playerError("Nothing found." + ChatColor.GRAY + " Either you're missing something, or we are."));
               } else {
                  call.getSender().sendMessage(Prism.messenger.playerHeaderMsg("Showing " + results.getTotalResults() + " results. Page " + page + " of " + results.getTotal_pages()));
                  List paginated = results.getPaginatedActionResults();
                  if (paginated != null && paginated.size() != 0) {
                     int result_count = results.getIndexOfFirstResult();

                     for(Iterator i$ = paginated.iterator(); i$.hasNext(); ++result_count) {
                        Handler a = (Handler)i$.next();
                        ActionMessage am = new ActionMessage(a);
                        if (results.getParameters().allowsNoRadius() || results.getParameters().hasFlag(Flag.EXTENDED) || this.plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                           am.showExtended();
                        }

                        am.setResultIndex(result_count);
                        call.getSender().sendMessage(Prism.messenger.playerMsg(am.getMessage()));
                     }

                  } else {
                     call.getSender().sendMessage(Prism.messenger.playerError("Pagination can't find anything. Do you have the right page number?"));
                  }
               }
            }
         }
      }
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
