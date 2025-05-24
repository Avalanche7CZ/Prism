package me.botsko.prism.database.mysql;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.database.QueryBuilder;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class SelectQueryBuilder extends QueryBuilder {
   private final String prefix;

   public SelectQueryBuilder(Prism plugin) {
      super(plugin);
      this.prefix = Prism.config.getString("prism.mysql.prefix");
   }

   protected String select() {
      String query = "";
      query = query + "SELECT ";
      this.columns.add("id");
      this.columns.add("epoch");
      this.columns.add("action_id");
      this.columns.add("player");
      this.columns.add("world_id");
      if (this.shouldGroup) {
         this.columns.add("AVG(x)");
         this.columns.add("AVG(y)");
         this.columns.add("AVG(z)");
      } else {
         this.columns.add("x");
         this.columns.add("y");
         this.columns.add("z");
      }

      this.columns.add("block_id");
      this.columns.add("block_subid");
      this.columns.add("old_block_id");
      this.columns.add("old_block_subid");
      this.columns.add("data");
      if (this.shouldGroup) {
         this.columns.add("COUNT(*) counted");
      }

      if (this.columns.size() > 0) {
         query = query + TypeUtils.join(this.columns, ", ");
      }

      query = query + " FROM " + this.tableNameData + " ";
      query = query + "INNER JOIN " + this.prefix + "players p ON p.player_id = " + this.tableNameData + ".player_id ";
      query = query + "LEFT JOIN " + this.tableNameDataExtra + " ex ON ex.data_id = " + this.tableNameData + ".id ";
      return query;
   }

   protected String where() {
      int id = this.parameters.getId();
      if (id > 0) {
         return "WHERE " + this.tableNameData + ".id = " + id;
      } else {
         int minId = this.parameters.getMinPrimaryKey();
         int maxId = this.parameters.getMaxPrimaryKey();
         if (minId > 0 && maxId > 0 && minId != maxId) {
            this.addCondition(this.tableNameData + ".id >= " + minId);
            this.addCondition(this.tableNameData + ".id < " + maxId);
         }

         this.worldCondition();
         this.actionCondition();
         this.playerCondition();
         this.radiusCondition();
         this.blockCondition();
         this.entityCondition();
         this.timeCondition();
         this.keywordCondition();
         this.coordinateCondition();
         return this.buildWhereConditions();
      }
   }

   protected void worldCondition() {
      if (this.parameters.getWorld() != null) {
         this.addCondition(String.format("world_id = ( SELECT w.world_id FROM " + this.prefix + "worlds w WHERE w.world = '%s')", this.parameters.getWorld()));
      }
   }

   protected void actionCondition() {
      HashMap action_types = this.parameters.getActionTypeNames();
      boolean containsPrismProcessType = false;
      ArrayList prismActionIds = new ArrayList();
      Iterator prismActionsIter = Prism.prismActions.entrySet().iterator();

      while(prismActionsIter.hasNext()) {
         Map.Entry entry = (Map.Entry)prismActionsIter.next();
         if (((String)entry.getKey()).contains("prism")) {
            containsPrismProcessType = true;
            Object actionIdObj = Prism.prismActions.get(entry.getKey());
            if (actionIdObj != null) {
               prismActionIds.add(actionIdObj.toString());
            }
         }
      }

      if (action_types.size() > 0) {
         ArrayList includeIds = new ArrayList();
         ArrayList excludeIds = new ArrayList();
         Iterator actionTypesIter = action_types.entrySet().iterator();

         while(actionTypesIter.hasNext()) {
            Map.Entry entry = (Map.Entry)actionTypesIter.next();
            Object actionIdObj = Prism.prismActions.get(entry.getKey());
            if (actionIdObj != null) {
               if (((MatchRule)entry.getValue()).equals(MatchRule.INCLUDE)) {
                  includeIds.add(actionIdObj.toString());
               }
               if (((MatchRule)entry.getValue()).equals(MatchRule.EXCLUDE)) {
                  excludeIds.add(actionIdObj.toString());
               }
            }
         }

         if (includeIds.size() > 0) {
            this.addCondition("action_id IN (" + TypeUtils.join((List)includeIds, ",") + ")");
         }

         if (excludeIds.size() > 0) {
            this.addCondition("action_id NOT IN (" + TypeUtils.join((List)excludeIds, ",") + ")");
         }
      } else if (!containsPrismProcessType && !this.parameters.getProcessType().equals(PrismProcessType.DELETE)) {
         this.addCondition("action_id NOT IN (" + TypeUtils.join((List)prismActionIds, ",") + ")");
      }
   }

   protected void playerCondition() {
      HashMap playerNames = this.parameters.getPlayerNames();
      if (playerNames.size() > 0) {
         MatchRule playerMatch = MatchRule.INCLUDE;
         Iterator playerValuesIter = playerNames.values().iterator();
         if (playerValuesIter.hasNext()) {
            MatchRule match = (MatchRule)playerValuesIter.next();
            playerMatch = match;
         }

         String matchQuery = playerMatch.equals(MatchRule.INCLUDE) ? "IN" : "NOT IN";

         Iterator playerEntriesIter = playerNames.entrySet().iterator();
         while(playerEntriesIter.hasNext()) {
            Map.Entry entry = (Map.Entry)playerEntriesIter.next();
            entry.setValue(MatchRule.INCLUDE);
         }

         this.addCondition(this.tableNameData + ".player_id " + matchQuery + " ( SELECT p.player_id FROM " + this.prefix + "players p WHERE " + this.buildMultipleConditions(playerNames, "p.player", null) + ")");
      }
   }

   protected void radiusCondition() {
      this.buildRadiusCondition(this.parameters.getMinLocation(), this.parameters.getMaxLocation());
   }

   protected void blockCondition() {
      HashMap blockfilters = this.parameters.getBlockFilters();
      if (!blockfilters.isEmpty()) {
         String[] blockArr = new String[blockfilters.size()];
         int i = 0;

         for(Iterator blockFiltersIter = blockfilters.entrySet().iterator(); blockFiltersIter.hasNext(); ++i) {
            Map.Entry entry = (Map.Entry)blockFiltersIter.next();
            if ((Short)entry.getValue() == 0) {
               blockArr[i] = this.tableNameData + ".block_id = " + entry.getKey();
            } else {
               blockArr[i] = this.tableNameData + ".block_id = " + entry.getKey() + " AND " + this.tableNameData + ".block_subid = " + entry.getValue();
            }
         }
         this.addCondition(this.buildGroupConditions(null, blockArr, "%s%s", "OR", null));
      }
   }

   protected void entityCondition() {
      HashMap entityNames = this.parameters.getEntities();
      if (entityNames.size() > 0) {
         this.addCondition(this.buildMultipleConditions(entityNames, "ex.data", "entity_name\":\"%s"));
      }
   }

   protected void timeCondition() {
      Long time = this.parameters.getBeforeTime();
      if (time != null && time != 0L) {
         this.addCondition(this.buildTimeCondition(time, "<="));
      }

      time = this.parameters.getSinceTime();
      if (time != null && time != 0L) {
         this.addCondition(this.buildTimeCondition(time, null));
      }
   }

   protected void keywordCondition() {
      String keyword = this.parameters.getKeyword();
      if (keyword != null) {
         this.addCondition("ex.data LIKE '%" + keyword.replace("'", "''") + "%'");
      }
   }

   protected void coordinateCondition() {
      ArrayList locations = this.parameters.getSpecificBlockLocations();
      if (locations.size() > 0) {
         String coordCond = "(";
         int l = 0;

         for(Iterator locationsIter = locations.iterator(); locationsIter.hasNext(); ++l) {
            Location loc = (Location)locationsIter.next();
            coordCond = coordCond + (l > 0 ? " OR" : "") + " (" + this.tableNameData + ".x = " + loc.getBlockX() + " AND " + this.tableNameData + ".y = " + loc.getBlockY() + " AND " + this.tableNameData + ".z = " + loc.getBlockZ() + ")";
         }
         coordCond = coordCond + ")";
         this.addCondition(coordCond);
      }
   }

   protected String buildWhereConditions() {
      int condCount = 1;
      String query = "";
      if (this.conditions.size() > 0) {
         for(Iterator conditionsIter = this.conditions.iterator(); conditionsIter.hasNext(); ++condCount) {
            String cond = (String)conditionsIter.next();
            if (condCount == 1) {
               query = query + " WHERE ";
            } else {
               query = query + " AND ";
            }
            query = query + cond;
         }
      }
      return query;
   }

   protected String group() {
      return this.shouldGroup ? " GROUP BY " + this.tableNameData + ".action_id, " + this.tableNameData + ".player_id, " + this.tableNameData + ".block_id, ex.data, DATE(FROM_UNIXTIME(" + this.tableNameData + ".epoch))" : "";
   }

   protected String order() {
      String sort_dir = this.parameters.getSortDirection();
      return " ORDER BY " + this.tableNameData + ".epoch " + sort_dir + ", x ASC, z ASC, y ASC, id " + sort_dir;
   }

   protected String limit() {
      if (this.parameters.getProcessType().equals(PrismProcessType.LOOKUP)) {
         int limit = this.parameters.getLimit();
         if (limit > 0) {
            return " LIMIT " + limit;
         }
      }
      return "";
   }

   protected String buildMultipleConditions(HashMap origValues, String field_name, String format) {
      String query = "";
      if (!origValues.isEmpty()) {
         ArrayList whereIs = new ArrayList();
         ArrayList whereNot = new ArrayList();
         ArrayList whereIsLike = new ArrayList();
         Iterator origValuesIter = origValues.entrySet().iterator();

         while(origValuesIter.hasNext()) {
            Map.Entry entry = (Map.Entry)origValuesIter.next();
            if (((MatchRule)entry.getValue()).equals(MatchRule.EXCLUDE)) {
               whereNot.add(entry.getKey());
            } else if (((MatchRule)entry.getValue()).equals(MatchRule.PARTIAL)) {
               whereIsLike.add(entry.getKey());
            } else {
               whereIs.add(entry.getKey());
            }
         }

         String[] whereValuesArray;
         if (!whereIs.isEmpty()) {
            whereValuesArray = (String[])whereIs.toArray(new String[0]);
            if (format == null) {
               query = query + this.buildGroupConditions(field_name, whereValuesArray, "%s = '%s'", "OR", null);
            } else {
               query = query + this.buildGroupConditions(field_name, whereValuesArray, "%s LIKE '%%%s%%'", "OR", format);
            }
         }

         if (!whereIsLike.isEmpty()) {
            if(!query.isEmpty() && query.charAt(query.length()-1) != '(' && !query.trim().endsWith("OR")) query += " OR ";
            whereValuesArray = (String[])whereIsLike.toArray(new String[0]);
            query = query + this.buildGroupConditions(field_name, whereValuesArray, "%s LIKE '%%%s%%'", "OR", format);
         }

         if (!whereNot.isEmpty()) {
            if(!query.isEmpty() && query.charAt(query.length()-1) != '(' && !query.trim().endsWith("AND")) query += " AND ";
            whereValuesArray = (String[])whereNot.toArray(new String[0]);
            if (format == null) {
               query = query + this.buildGroupConditions(field_name, whereValuesArray, "%s != '%s'", "AND", null);
            } else {
               query = query + this.buildGroupConditions(field_name, whereValuesArray, "%s NOT LIKE '%%%s%%'", "AND", format);
            }
         }
      }
      return query;
   }

   protected String buildGroupConditions(String fieldname, String[] arg_values, String matchFormat, String matchType, String dataFormat) {
      String where = "";
      matchFormat = matchFormat == null ? "%s = %s" : matchFormat;
      matchType = matchType == null ? "AND" : matchType;
      dataFormat = dataFormat == null ? "%s" : dataFormat;
      if (arg_values.length > 0 && !matchFormat.isEmpty()) {
         where = where + "(";
         int c = 1;
         for(String val : arg_values) {
            if (c > 1) {
               where = where + " " + matchType + " ";
            }
            String currentFieldname = fieldname == null ? "" : fieldname;
            String sanitizedVal = (val != null) ? val.replace("'", "''") : "";
            where = where + String.format(matchFormat, currentFieldname, String.format(dataFormat, sanitizedVal));
            ++c;
         }
         where = where + ")";
      }
      return where;
   }

   protected void buildRadiusCondition(Vector minLoc, Vector maxLoc) {
      if (minLoc != null && maxLoc != null) {
         this.addCondition("(" + this.tableNameData + ".x BETWEEN " + minLoc.getBlockX() + " AND " + maxLoc.getBlockX() + ")");
         this.addCondition("(" + this.tableNameData + ".y BETWEEN " + minLoc.getBlockY() + " AND " + maxLoc.getBlockY() + ")");
         this.addCondition("(" + this.tableNameData + ".z BETWEEN " + minLoc.getBlockZ() + " AND " + maxLoc.getBlockZ() + ")");
      }
   }

   protected String buildTimeCondition(Long dateFrom, String equation) {
      if (dateFrom != null) {
         if (equation == null) {
            this.addCondition(this.tableNameData + ".epoch >= " + dateFrom / 1000L);
         } else {
            this.addCondition(this.tableNameData + ".epoch " + equation + " " + dateFrom / 1000L);
         }
      }
      return "";
   }
}
