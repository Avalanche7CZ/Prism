package me.botsko.prism.events;

import java.util.ArrayList;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ApplierResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrismBlocksRollbackEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final ArrayList blockStateChanges;
   private final Player onBehalfOf;
   private final QueryParameters parameters;
   private final ApplierResult result;

   public PrismBlocksRollbackEvent(ArrayList blockStateChanges, Player onBehalfOf, QueryParameters parameters, ApplierResult result) {
      this.blockStateChanges = blockStateChanges;
      this.onBehalfOf = onBehalfOf;
      this.parameters = parameters;
      this.result = result;
   }

   public ArrayList getBlockStateChanges() {
      return this.blockStateChanges;
   }

   public Player onBehalfOf() {
      return this.onBehalfOf;
   }

   public QueryParameters getQueryParameters() {
      return this.parameters;
   }

   public ApplierResult getResult() {
      return this.result;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }
}
