package me.botsko.prism.database.mysql;

import me.botsko.prism.Prism;
import me.botsko.prism.database.PrismDatabaseHandler;

public class DeleteQueryBuilder extends SelectQueryBuilder {

    public DeleteQueryBuilder(Prism plugin) {
        super( plugin );
    }

    @Override
    public String select() {
        String prefix = PrismDatabaseHandler.getTablePrefix();
        if (PrismDatabaseHandler.getDbType().equalsIgnoreCase("sqlite")) {
            return "DELETE FROM `" + prefix + "data`";
        } else {
            return "DELETE `" + prefix + "data` FROM `" + prefix + "data`" +
                    " LEFT JOIN `" + prefix + "data_extra` ex ON (`" + prefix + "data`.`id` = ex.`data_id`) ";
        }
    }

    @Override
    protected String group() {
        return "";
    }

    @Override
    protected String order() {
        return "";
    }

    @Override
    protected String limit() {
        if (PrismDatabaseHandler.getDbType().equalsIgnoreCase("sqlite")) {
            return ""; // SQLite DELETE does not support LIMIT directly in this form with WHERE from main query
        }
        return "";
    }
}