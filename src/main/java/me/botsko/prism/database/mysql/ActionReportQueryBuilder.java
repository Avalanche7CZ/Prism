package me.botsko.prism.database.mysql;

import java.util.ArrayList;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.database.PrismDatabaseHandler;

public class ActionReportQueryBuilder extends SelectQueryBuilder {

    public ActionReportQueryBuilder(Prism plugin) {
        super( plugin );
    }

    @Override
    public String getQuery(QueryParameters parameters, boolean shouldGroup) {
        this.parameters = parameters;
        this.shouldGroup = shouldGroup;
        columns = new ArrayList<String>();
        conditions = new ArrayList<String>();
        String query = select();
        query += ";";
        if( plugin.getConfig().getBoolean( "prism.debug" ) ) {
            Prism.debug( query );
        }
        return query;
    }

    @Override
    public String select() {
        String prefix = PrismDatabaseHandler.getTablePrefix();
        final String sql = "SELECT COUNT(*) AS `counted`, a.`action` " + "FROM `" + prefix + "data` "
                + "INNER JOIN `" + prefix + "actions` a ON a.`action_id` = `" + prefix + "data`.`action_id` " + where() + " "
                + "GROUP BY a.`action_id`, a.`action` " + "ORDER BY `counted` DESC";
        return sql;
    }
}