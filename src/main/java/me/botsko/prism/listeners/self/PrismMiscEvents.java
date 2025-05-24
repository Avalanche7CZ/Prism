package me.botsko.prism.listeners.self;

import java.util.ArrayList;
import java.util.Iterator;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.actionlibs.RecordingTask;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.events.BlockStateChange;
import me.botsko.prism.events.PrismBlocksDrainEvent;
import me.botsko.prism.events.PrismBlocksExtinguishEvent;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PrismMiscEvents implements Listener {
   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPrismBlocksDrainEvent(PrismBlocksDrainEvent event) {
      ArrayList blockStateChanges = event.getBlockStateChanges();
      if (!blockStateChanges.isEmpty()) {
         Handler primaryAction = ActionFactory.createPrismProcess("prism-process", PrismProcessType.DRAIN, event.onBehalfOf(), "" + event.getRadius());
         int id = RecordingTask.insertActionIntoDatabase(primaryAction);
         if (id == 0) {
            return;
         }

         Iterator i$ = blockStateChanges.iterator();

         while(i$.hasNext()) {
            BlockStateChange stateChange = (BlockStateChange)i$.next();
            BlockState orig = stateChange.getOriginalBlock();
            BlockState newBlock = stateChange.getNewBlock();
            RecordingQueue.addToQueue(ActionFactory.createPrismRollback("prism-drain", orig, newBlock, event.onBehalfOf().getName(), id));
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPrismBlocksExtinguishEvent(PrismBlocksExtinguishEvent event) {
      ArrayList blockStateChanges = event.getBlockStateChanges();
      if (!blockStateChanges.isEmpty()) {
         Handler primaryAction = ActionFactory.createPrismProcess("prism-process", PrismProcessType.EXTINGUISH, event.onBehalfOf(), "" + event.getRadius());
         int id = RecordingTask.insertActionIntoDatabase(primaryAction);
         if (id == 0) {
            return;
         }

         Iterator i$ = blockStateChanges.iterator();

         while(i$.hasNext()) {
            BlockStateChange stateChange = (BlockStateChange)i$.next();
            BlockState orig = stateChange.getOriginalBlock();
            BlockState newBlock = stateChange.getNewBlock();
            RecordingQueue.addToQueue(ActionFactory.createPrismRollback("prism-extinguish", orig, newBlock, event.onBehalfOf().getName(), id));
         }
      }

   }
}
