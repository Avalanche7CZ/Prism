package me.botsko.prism.actions;

import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.entity.minecart.SpawnerMinecart;
import org.bukkit.entity.minecart.StorageMinecart;

public class VehicleAction extends GenericAction {
   public void setVehicle(Vehicle vehicle) {
      if (vehicle instanceof PoweredMinecart) {
         this.data = "powered minecart";
      } else if (vehicle instanceof HopperMinecart) {
         this.data = "minecart hopper";
      } else if (vehicle instanceof SpawnerMinecart) {
         this.data = "spawner minecart";
      } else if (vehicle instanceof ExplosiveMinecart) {
         this.data = "tnt minecart";
      } else if (vehicle instanceof StorageMinecart) {
         this.data = "storage minecart";
      } else {
         this.data = vehicle.getType().name().toLowerCase();
      }

   }

   public String getNiceName() {
      return this.data;
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      Entity vehicle = null;
      if (this.data.equals("powered minecart")) {
         vehicle = this.getWorld().spawn(this.getLoc(), PoweredMinecart.class);
      } else if (this.data.equals("storage minecart")) {
         vehicle = this.getWorld().spawn(this.getLoc(), StorageMinecart.class);
      } else if (this.data.equals("tnt minecart")) {
         vehicle = this.getWorld().spawn(this.getLoc(), ExplosiveMinecart.class);
      } else if (this.data.equals("spawner minecart")) {
         vehicle = this.getWorld().spawn(this.getLoc(), SpawnerMinecart.class);
      } else if (this.data.equals("minecart hopper")) {
         vehicle = this.getWorld().spawn(this.getLoc(), HopperMinecart.class);
      } else if (this.data.equals("minecart")) {
         vehicle = this.getWorld().spawn(this.getLoc(), Minecart.class);
      } else if (this.data.equals("boat")) {
         vehicle = this.getWorld().spawn(this.getLoc(), Boat.class);
      }

      return vehicle != null ? new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null) : new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
   }
}
