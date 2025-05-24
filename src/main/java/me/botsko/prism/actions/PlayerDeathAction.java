package me.botsko.prism.actions;

public class PlayerDeathAction extends GenericAction {
   protected String cause;
   protected String attacker;

   public void setCause(String cause) {
      this.cause = cause;
   }

   public void setAttacker(String attacker) {
      this.attacker = attacker;
   }

   public void setData(String data) {
      this.data = data;
      if (this.cause == null && data != null) {
         String[] dataArr = data.split(":");
         this.cause = dataArr[0];
         if (dataArr.length > 1) {
            this.attacker = dataArr[1];
         }
      }

   }

   public void save() {
      if (this.data == null && this.cause != null) {
         this.data = this.cause + ":" + this.attacker;
      }

   }

   public String getNiceName() {
      String name = "";
      if (this.attacker != null && !this.attacker.isEmpty()) {
         name = name + this.attacker;
      }

      if (this.cause != null && !this.cause.isEmpty()) {
         name = name + "(" + this.cause + ")";
      }

      return name;
   }
}
