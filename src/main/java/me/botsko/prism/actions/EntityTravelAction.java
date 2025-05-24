package me.botsko.prism.actions;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class EntityTravelAction extends GenericAction {
   protected EntityTravelActionData actionData = new EntityTravelActionData();

   public void setEntity(Entity entity) {
      if (entity != null) {
         if (entity instanceof Player) {
            this.player_name = ((Player)entity).getName();
         } else {
            this.player_name = entity.getType().name().toLowerCase();
         }
      }

   }

   public void setToLocation(Location to) {
      if (to != null) {
         this.actionData.to_x = to.getBlockX();
         this.actionData.to_y = to.getBlockY();
         this.actionData.to_z = to.getBlockZ();
      }

   }

   public void setCause(PlayerTeleportEvent.TeleportCause cause) {
      if (cause != null) {
         this.actionData.cause = cause.name().toLowerCase();
      }

   }

   public void setData(String data) {
      this.data = data;
      if (data != null && data.startsWith("{")) {
         this.actionData = (EntityTravelActionData)this.gson.fromJson(data, EntityTravelActionData.class);
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public EntityTravelActionData getActionData() {
      return this.actionData;
   }

   public String getNiceName() {
      if (this.actionData != null) {
         String cause = this.actionData.cause == null ? "unknown" : this.actionData.cause.replace("_", " ");
         return "using " + cause + " to " + this.actionData.to_x + " " + this.actionData.to_y + " " + this.actionData.to_z;
      } else {
         return "teleported somewhere";
      }
   }

   public class EntityTravelActionData {
      int to_x;
      int to_y;
      int to_z;
      String cause;
   }
}
