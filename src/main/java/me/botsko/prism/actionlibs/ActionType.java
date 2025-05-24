package me.botsko.prism.actionlibs;

public class ActionType {
   private final boolean doesCreateBlock;
   private final boolean canRollback;
   private final boolean canRestore;
   private final String handler;
   private final String niceDescription;
   private final String name;

   public ActionType(String name, String handler, String niceDescription) {
      this(name, false, false, false, handler, niceDescription);
   }

   public ActionType(String name, boolean doesCreateBlock, boolean canRollback, boolean canRestore, String handler, String niceDescription) {
      this.doesCreateBlock = doesCreateBlock;
      this.canRollback = canRollback;
      this.canRestore = canRestore;
      this.handler = handler;
      this.niceDescription = niceDescription;
      this.name = name;
   }

   public boolean canRollback() {
      return this.canRollback;
   }

   public boolean canRestore() {
      return this.canRestore;
   }

   public String getHandler() {
      return this.handler;
   }

   public String getNiceDescription() {
      return this.niceDescription;
   }

   public boolean requiresHandler(String handler) {
      return this.getHandler() != null && this.getHandler().equals(handler);
   }

   public boolean doesCreateBlock() {
      return this.doesCreateBlock;
   }

   public String getName() {
      return this.name;
   }

   public String getFamilyName() {
      String[] _tmp = this.name.toLowerCase().split("-(?!.*-.*)");
      return _tmp.length == 2 ? _tmp[0] : this.name;
   }

   public String getShortName() {
      String[] _tmp = this.name.toLowerCase().split("-(?!.*-.*)");
      return _tmp.length == 2 ? _tmp[1] : this.name;
   }

   public boolean shouldTriggerRollbackFor(String at) {
      return false;
   }
}
