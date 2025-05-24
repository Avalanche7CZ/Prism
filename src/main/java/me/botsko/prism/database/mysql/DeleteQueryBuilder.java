package me.botsko.prism.database.mysql;

import me.botsko.prism.Prism;

public class DeleteQueryBuilder extends SelectQueryBuilder {
   public DeleteQueryBuilder(Prism plugin) {
      super(plugin);
   }

   public String select() {
      return "DELETE FROM " + this.tableNameData + " USING " + this.tableNameData + " LEFT JOIN " + this.tableNameDataExtra + " ex ON (" + this.tableNameData + ".id = ex.data_id) ";
   }

   protected String group() {
      return "";
   }

   protected String order() {
      return "";
   }

   protected String limit() {
      return "";
   }
}
