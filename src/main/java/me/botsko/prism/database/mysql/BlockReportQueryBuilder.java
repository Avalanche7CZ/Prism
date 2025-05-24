package me.botsko.prism.database.mysql;

import java.util.ArrayList;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;

public class BlockReportQueryBuilder extends SelectQueryBuilder {
   public BlockReportQueryBuilder(Prism plugin) {
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
      this.parameters.addActionType("block-place");
      String sql = "SELECT block_id, SUM(placed) AS placed, SUM(broken) AS broken FROM ((SELECT block_id, COUNT(id) AS placed, 0 AS broken FROM " + prefix + "data " + this.where() + " " + "GROUP BY block_id) ";
      this.conditions.clear();
      this.parameters.getActionTypes().clear();
      this.parameters.addActionType("block-break");
      sql = sql + "UNION ( SELECT block_id, 0 AS placed, count(id) AS broken FROM " + prefix + "data " + this.where() + " " + "GROUP BY block_id)) " + "AS PR_A " + "GROUP BY block_id ORDER BY (SUM(placed) + SUM(broken)) DESC";
      return sql;
   }
}
