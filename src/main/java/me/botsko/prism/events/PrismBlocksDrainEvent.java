package me.botsko.prism.events;

import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrismBlocksDrainEvent extends Event {
   private static final HandlerList handlers = new HandlerList();
   private final ArrayList blockStateChanges;
   private final Player onBehalfOf;
   protected final int radius;

   public PrismBlocksDrainEvent(ArrayList blockStateChanges, Player onBehalfOf, int radius) {
      this.blockStateChanges = blockStateChanges;
      this.onBehalfOf = onBehalfOf;
      this.radius = radius;
   }

   public ArrayList getBlockStateChanges() {
      return this.blockStateChanges;
   }

   public Player onBehalfOf() {
      return this.onBehalfOf;
   }

   public int getRadius() {
      return this.radius;
   }

   public HandlerList getHandlers() {
      return handlers;
   }

   public static HandlerList getHandlerList() {
      return handlers;
   }
}
