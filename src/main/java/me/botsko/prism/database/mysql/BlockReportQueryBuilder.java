package me.botsko.prism.database.mysql;

import java.util.ArrayList;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.database.PrismDatabaseHandler;

public class BlockReportQueryBuilder extends SelectQueryBuilder {

    public BlockReportQueryBuilder(Prism plugin) {
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
        parameters.addActionType( "block-place" );
        String originalWhere = where();

        String sql = "SELECT t.`block_id`, SUM(t.`placed`) AS `placed`, SUM(t.`broken`) AS `broken` FROM (("
                + "SELECT `block_id`, COUNT(`id`) AS `placed`, 0 AS `broken` " + "FROM `" + prefix + "data` " + originalWhere + " "
                + "GROUP BY `block_id`) ";

        conditions.clear();
        parameters.getActionTypes().clear();
        parameters.addActionType( "block-break" );
        String breakWhere = where();

        sql += "UNION ALL ( " + "SELECT `block_id`, 0 AS `placed`, COUNT(`id`) AS `broken` " + "FROM `" + prefix + "data` " + breakWhere + " "
                + "GROUP BY `block_id`)) t " + "GROUP BY t.`block_id` ORDER BY (SUM(t.`placed`) + SUM(t.`broken`)) DESC";
        return sql;
    }
}