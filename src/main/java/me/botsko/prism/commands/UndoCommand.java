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
import me.botsko.prism.actions.PrismProcessAction;
import me.botsko.prism.appliers.PrismApplierCallback;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.appliers.Undo;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.SubHandler;
import org.bukkit.ChatColor;

public class UndoCommand implements SubHandler {
   private final Prism plugin;

   public UndoCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(CallInfo call) {
      if (call.getArgs().length > 1) {
         ActionsQuery aq = new ActionsQuery(this.plugin);
         int record_id = 0;
         if (TypeUtils.isNumeric(call.getArg(1))) {
            record_id = Integer.parseInt(call.getArg(1));
            if (record_id <= 0) {
               call.getPlayer().sendMessage(Prism.messenger.playerError("Record ID must be greater than zero."));
               return;
            }
         } else if (call.getArg(1).equals("last")) {
            record_id = aq.getUsersLastPrismProcessId(call.getPlayer().getName());
         }

         if (record_id == 0) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Either you have no last process or an invalid ID."));
            return;
         }

         PrismProcessAction process = aq.getPrismProcessRecord(record_id);
         if (process == null) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("A process does not exists with that value."));
            return;
         }

         if (!process.getProcessChildActionType().equals("prism-drain")) {
            call.getPlayer().sendMessage(Prism.messenger.playerError("You can't currently undo anything other than a drain process."));
            return;
         }

         QueryParameters parameters = new QueryParameters();
         parameters.setWorld(call.getPlayer().getWorld().getName());
         parameters.addActionType(process.getProcessChildActionType());
         parameters.addPlayerName(call.getPlayer().getName());
         parameters.setParentId(record_id);
         parameters.setProcessType(PrismProcessType.UNDO);
         QueryResult results = aq.lookup(parameters, call.getPlayer());
         if (!results.getActionResults().isEmpty()) {
            call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Undoing..." + ChatColor.GRAY + " Abandon ship!"));
            Undo rb = new Undo(this.plugin, call.getPlayer(), results.getActionResults(), parameters, new PrismApplierCallback());
            rb.apply();
         } else {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Nothing found to undo. Must be a problem with Prism."));
         }
      } else {
         QueryParameters parameters = new QueryParameters();
         parameters.setAllowNoRadius(true);
         parameters.addActionType("prism-process");
         parameters.addPlayerName(call.getPlayer().getName());
         parameters.setLimit(5);
         ActionsQuery aq = new ActionsQuery(this.plugin);
         QueryResult results = aq.lookup(parameters, call.getPlayer());
         if (!results.getActionResults().isEmpty()) {
            call.getPlayer().sendMessage(Prism.messenger.playerHeaderMsg("Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages()));
            call.getPlayer().sendMessage(Prism.messenger.playerSubduedHeaderMsg("Use /prism undo [id] to reverse a process"));
            List paginated = results.getPaginatedActionResults();
            ActionMessage am;
            if (paginated != null) {
               for(Iterator i$ = paginated.iterator(); i$.hasNext(); call.getPlayer().sendMessage(Prism.messenger.playerMsg(am.getMessage()))) {
                  Handler a = (Handler)i$.next();
                  am = new ActionMessage(a);
                  if (parameters.allowsNoRadius() || parameters.hasFlag(Flag.EXTENDED) || this.plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                     am.showExtended();
                  }
               }
            } else {
               call.getPlayer().sendMessage(Prism.messenger.playerError("Pagination can't find anything. Do you have the right page number?"));
            }
         } else {
            call.getPlayer().sendMessage(Prism.messenger.playerError("Nothing found." + ChatColor.GRAY + " Either you're missing something, or we are."));
         }
      }

   }

   public List handleComplete(CallInfo call) {
      return null;
   }
}
