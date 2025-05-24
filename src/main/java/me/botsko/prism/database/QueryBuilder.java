package me.botsko.prism.database;

import java.util.ArrayList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;

public abstract class QueryBuilder {
   protected final Prism plugin;
   protected List columns = new ArrayList();
   protected List conditions = new ArrayList();
   protected final String tableNameData;
   protected final String tableNameDataExtra;
   protected QueryParameters parameters;
   protected boolean shouldGroup;

   public QueryBuilder(Prism plugin) {
      this.plugin = plugin;
      String prefix = plugin.getConfig().getString("prism.mysql.prefix");
      this.tableNameData = prefix + "data";
      this.tableNameDataExtra = prefix + "data_extra";
   }

   public String getQuery(QueryParameters parameters, boolean shouldGroup) {
      this.parameters = parameters;
      this.shouldGroup = shouldGroup;
      this.columns = new ArrayList();
      this.conditions = new ArrayList();
      String query = this.select() + this.where() + this.group() + this.order() + this.limit();
      query = query + ";";
      if (this.plugin.getConfig().getBoolean("prism.debug")) {
         Prism.debug(query);
      }

      return query;
   }

   protected String select() {
      return "";
   }

   protected String where() {
      return "";
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

   protected void addCondition(String condition) {
      if (!condition.isEmpty()) {
         this.conditions.add(condition);
      }

   }
}
