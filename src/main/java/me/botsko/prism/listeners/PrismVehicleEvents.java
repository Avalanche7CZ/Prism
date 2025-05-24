package me.botsko.prism.listeners;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleCreateEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class PrismVehicleEvents implements Listener {
   private final Prism plugin;

   public PrismVehicleEvents(Prism plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onVehicleCreate(VehicleCreateEvent event) {
      Vehicle vehicle = event.getVehicle();
      Location loc = vehicle.getLocation();
      String coord_key = loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
      String player = (String)this.plugin.preplannedVehiclePlacement.get(coord_key);
      if (player != null) {
         if (!Prism.getIgnore().event("vehicle-place", loc.getWorld(), player)) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-place", vehicle, player));
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onVehicleDestroy(VehicleDestroyEvent event) {
      Vehicle vehicle = event.getVehicle();
      Entity attacker = event.getAttacker();
      if (attacker != null) {
         if (attacker instanceof Player) {
            if (!Prism.getIgnore().event("vehicle-break", (Player)attacker)) {
               return;
            }

            RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-break", vehicle, ((Player)attacker).getName()));
         } else {
            if (!Prism.getIgnore().event("vehicle-break", attacker.getWorld())) {
               return;
            }

            RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-break", vehicle, attacker.getType().name().toLowerCase()));
         }
      } else {
         Entity passenger = vehicle.getPassenger();
         if (passenger != null && passenger instanceof Player) {
            if (!Prism.getIgnore().event("vehicle-break", (Player)passenger)) {
               return;
            }

            RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-break", vehicle, ((Player)passenger).getName()));
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onVehicleEnter(VehicleEnterEvent event) {
      Vehicle vehicle = event.getVehicle();
      Entity entity = event.getEntered();
      if (entity instanceof Player) {
         if (!Prism.getIgnore().event("vehicle-enter", (Player)entity)) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-enter", vehicle, ((Player)entity).getName()));
      } else {
         if (!Prism.getIgnore().event("vehicle-enter", entity.getWorld())) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-enter", vehicle, entity.getType().name().toLowerCase()));
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onVehicleExit(VehicleExitEvent event) {
      Vehicle vehicle = event.getVehicle();
      Entity entity = event.getExited();
      if (entity instanceof Player) {
         if (!Prism.getIgnore().event("vehicle-enter", (Player)entity)) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-exit", vehicle, ((Player)entity).getName()));
      } else {
         if (!Prism.getIgnore().event("vehicle-enter", entity.getWorld())) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createVehicle("vehicle-exit", vehicle, entity.getType().name().toLowerCase()));
      }

   }
}
