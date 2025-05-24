package me.botsko.prism.database.mysql;

import java.util.ArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;

public class ActionReportQueryBuilder extends SelectQueryBuilder {
   public ActionReportQueryBuilder(Prism plugin) {
      super(plugin);
   }

   public String getQuery(QueryParameters parameters, boolean shouldGroup) {
      this.parameters = parameters;
      this.shouldGroup = shouldGroup;
      this.columns = new ArrayList();
      this.conditions = new ArrayList();
      String query = this.select();
      query = query + ";";
      if (this.plugin.getConfig().getBoolean("prism.debug")) {
         Prism.debug(query);
      }

      return query;
   }

   public String select() {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      String sql = "SELECT COUNT(*), a.action FROM " + prefix + "data " + "INNER JOIN " + prefix + "actions a ON a.action_id = " + prefix + "data.action_id " + this.where() + " " + "GROUP BY a.action_id " + "ORDER BY COUNT(*) DESC";
      return sql;
   }
}
