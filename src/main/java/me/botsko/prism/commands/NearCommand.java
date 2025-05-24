package me.botsko.prism.commands;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.SubHandler;

public class NearCommand implements SubHandler {
   private final Prism plugin;

   public NearCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(final CallInfo call) {
      final QueryParameters parameters = new QueryParameters();
      parameters.setPerPage(this.plugin.getConfig().getInt("prism.queries.default-results-per-page"));
      parameters.setWorld(call.getPlayer().getWorld().getName());
      int radius = this.plugin.getConfig().getInt("prism.near.default-radius");
      if (call.getArgs().length == 2) {
         if (!TypeUtils.isNumeric(call.getArg(1))) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Radius must be a number. Or leave it off to use the default. Use /prism ? for help."));
            return;
         }

         int _tmp_radius = Integer.parseInt(call.getArg(1));
         if (_tmp_radius <= 0) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Radius must be greater than zero. Or leave it off to use the default. Use /prism ? for help."));
            return;
         }

         radius = _tmp_radius;
      }

      parameters.setRadius(radius);
      parameters.setMinMaxVectorsFromPlayerLocation(call.getPlayer().getLocation());
      parameters.setLimit(this.plugin.getConfig().getInt("prism.near.max-results"));
      this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
         public void run() {
            ActionsQuery aq = new ActionsQuery(NearCommand.this.plugin);
            QueryResult results = aq.lookup(parameters, call.getPlayer());
            if (!results.getActionResults().isEmpty()) {
               call.getPlayer().sendMessage(Prism.messenger.playerSubduedHeaderMsg("All changes within " + parameters.getRadius() + " blocks of you..."));
               call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages()));
               List paginated = results.getPaginatedActionResults();
               if (paginated != null) {
                  int result_count = results.getIndexOfFirstResult();

                  for(Iterator i$ = paginated.iterator(); i$.hasNext(); ++result_count) {
                     Handler a = (Handler)i$.next();
                     ActionMessage am = new ActionMessage(a);
                     if (parameters.allowsNoRadius() || parameters.hasFlag(Flag.EXTENDED) || NearCommand.this.plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                        am.showExtended();
                     }

                     am.setResultIndex(result_count);
                     call.getPlayer().sendMessage(Prism.messenger.playerMsg(am.getMessage()));
                  }

                  NearCommand.this.plugin.eventTimer.printTimeRecord();
               } else {
                  call.getPlayer().sendMessage(Prism.messenger.playerError("Pagination can't find anything. Do you have the right page number?"));
               }
            } else {
               call.getPlayer().sendMessage(Prism.messenger.playerError("Couldn't find anything."));
            }

         }
      });
   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
