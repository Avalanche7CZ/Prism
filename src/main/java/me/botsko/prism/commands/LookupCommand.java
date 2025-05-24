package me.botsko.prism.commands;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.CallInfo;
import me.botsko.prism.commandlibs.Flag;
import me.botsko.prism.commandlibs.PreprocessArgs;
import me.botsko.prism.commandlibs.SubHandler;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class LookupCommand implements SubHandler {
   private final Prism plugin;

   public LookupCommand(Prism plugin) {
      this.plugin = plugin;
   }

   public void handle(final CallInfo call) {
      final QueryParameters parameters = PreprocessArgs.process(this.plugin, call.getSender(), call.getArgs(), PrismProcessType.LOOKUP, 1, !this.plugin.getConfig().getBoolean("prism.queries.never-use-defaults"));
      if (parameters != null) {
         this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            public void run() {
               ArrayList defaultsUsed = parameters.getDefaultsUsed();
               String defaultsReminder = "";
               if (!defaultsUsed.isEmpty()) {
                  defaultsReminder = defaultsReminder + "Using defaults:";

                  String d;
                  for(Iterator i$x = defaultsUsed.iterator(); i$x.hasNext(); defaultsReminder = defaultsReminder + " " + d) {
                     d = (String)i$x.next();
                  }
               }

               ActionsQuery aq = new ActionsQuery(LookupCommand.this.plugin);
               QueryResult results = aq.lookup(parameters, call.getSender());
               String sharingWithPlayers = "";

               Iterator i$xx;
               CommandSender player;
               for(i$xx = parameters.getSharedPlayers().iterator(); i$xx.hasNext(); sharingWithPlayers = sharingWithPlayers + player.getName() + ", ") {
                  player = (CommandSender)i$xx.next();
               }

               sharingWithPlayers = sharingWithPlayers.substring(0, sharingWithPlayers.isEmpty() ? 0 : sharingWithPlayers.length() - 2);
               parameters.addSharedPlayer(call.getSender());
               i$xx = parameters.getSharedPlayers().iterator();

               while(true) {
                  Iterator i$;
                  Handler a;
                  label82:
                  do {
                     while(i$xx.hasNext()) {
                        player = (CommandSender)i$xx.next();
                        boolean isSender = player.getName().equals(call.getSender().getName());
                        if (!isSender) {
                           player.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.YELLOW + "" + ChatColor.ITALIC + call.getSender().getName() + ChatColor.GOLD + " shared these Prism lookup logs with you:"));
                        } else if (!sharingWithPlayers.isEmpty()) {
                           player.sendMessage(Prism.messenger.playerHeaderMsg(ChatColor.GOLD + "Sharing results with players: " + ChatColor.YELLOW + "" + ChatColor.ITALIC + sharingWithPlayers));
                        }

                        if (!results.getActionResults().isEmpty()) {
                           player.sendMessage(Prism.messenger.playerHeaderMsg("Showing " + results.getTotalResults() + " results. Page 1 of " + results.getTotal_pages()));
                           if (!defaultsReminder.isEmpty() && isSender) {
                              player.sendMessage(Prism.messenger.playerSubduedHeaderMsg(defaultsReminder));
                           }

                           List paginated = results.getPaginatedActionResults();
                           if (paginated == null) {
                              player.sendMessage(Prism.messenger.playerError("Pagination can't find anything. Do you have the right page number?"));
                           } else {
                              int result_count = results.getIndexOfFirstResult();

                              for(i$ = paginated.iterator(); i$.hasNext(); ++result_count) {
                                 a = (Handler)i$.next();
                                 ActionMessage am = new ActionMessage(a);
                                 if (parameters.allowsNoRadius() || parameters.hasFlag(Flag.EXTENDED) || LookupCommand.this.plugin.getConfig().getBoolean("prism.messenger.always-show-extended")) {
                                    am.showExtended();
                                 }

                                 am.setResultIndex(result_count);
                                 player.sendMessage(Prism.messenger.playerMsg(am.getMessage()));
                              }
                           }
                           continue label82;
                        }

                        if (!defaultsReminder.isEmpty() && isSender) {
                           player.sendMessage(Prism.messenger.playerSubduedHeaderMsg(defaultsReminder));
                        }

                        if (isSender) {
                           player.sendMessage(Prism.messenger.playerError("Nothing found." + ChatColor.GRAY + " Either you're missing something, or we are."));
                        }
                     }

                     LookupCommand.this.plugin.eventTimer.printTimeRecord();
                     return;
                  } while(!parameters.hasFlag(Flag.PASTE));

                  String paste = "";

                  for(i$ = results.getActionResults().iterator(); i$.hasNext(); paste = paste + (new ActionMessage(a)).getRawMessage() + "\r\n") {
                     a = (Handler)i$.next();
                  }

                  player.sendMessage(MiscUtils.paste_results(LookupCommand.this.plugin, paste));
               }
            }
         });
      }
   }

   public List handleComplete(CallInfo call) {
      return PreprocessArgs.complete(call.getSender(), call.getArgs());
   }
}
