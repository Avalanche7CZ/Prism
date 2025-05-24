package me.botsko.prism.actions;

public class PlayerAction extends GenericAction {
   public String getNiceName() {
      if (this.data != null && !this.data.isEmpty()) {
         if (this.type.getName().equals("player-join")) {
            return "from " + this.data;
         } else if (this.type.getName().equals("xp-pickup")) {
            return this.data + " xp";
         } else {
            return this.type.getName().equals("bucket-fill") ? "a " + this.data + " bucket" : this.data;
         }
      } else {
         return "";
      }
   }
}
