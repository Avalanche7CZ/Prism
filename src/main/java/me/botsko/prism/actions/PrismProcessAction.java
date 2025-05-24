package me.botsko.prism.actions;

import me.botsko.prism.Prism;
import me.botsko.prism.appliers.PrismProcessType;

public class PrismProcessAction extends GenericAction {
   private PrismProcessActionData actionData;

   public void setProcessData(PrismProcessType processType, String parameters) {
      this.actionData = new PrismProcessActionData();
      if (processType != null) {
         this.actionData.params = parameters;
         this.actionData.processType = processType.name().toLowerCase();
      }

   }

   public void setData(String data) {
      this.data = data;
      if (data != null && !this.data.isEmpty()) {
         this.actionData = (PrismProcessActionData)this.gson.fromJson(data, PrismProcessActionData.class);
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public String getProcessChildActionType() {
      return Prism.getActionRegistry().getAction("prism-" + this.actionData.processType).getName();
   }

   public String getNiceName() {
      return this.actionData.processType + " (" + this.actionData.params + ")";
   }

   public class PrismProcessActionData {
      public String params = "";
      public String processType;
   }
}
