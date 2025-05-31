package me.botsko.prism.database.mysql;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import me.botsko.prism.database.PrismDatabaseHandler;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import me.botsko.elixr.TypeUtils;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.database.QueryBuilder;

public class SelectQueryBuilder extends QueryBuilder {

    private final String prefix;
    private final String dbType;

    public SelectQueryBuilder(Prism plugin) {
        super( plugin );
        this.prefix = PrismDatabaseHandler.getTablePrefix();
        this.dbType = PrismDatabaseHandler.getDbType();
    }

    @Override
    protected String select() {
        String query = "";
        query += "SELECT ";
        columns.add( "`" + tableNameData + "`.`id`" );
        columns.add( "`" + tableNameData + "`.`epoch`" );
        columns.add( "`" + tableNameData + "`.`action_id`" );
        columns.add( "p.`player`" );
        columns.add( "`" + tableNameData + "`.`world_id`" );

        if( shouldGroup ) {
            columns.add( "AVG(`" + tableNameData + "`.`x`)" );
            columns.add( "AVG(`" + tableNameData + "`.`y`)" );
            columns.add( "AVG(`" + tableNameData + "`.`z`)" );
        } else {
            columns.add( "`" + tableNameData + "`.`x`" );
            columns.add( "`" + tableNameData + "`.`y`" );
            columns.add( "`" + tableNameData + "`.`z`" );
        }

        columns.add( "`" + tableNameData + "`.`block_id`" );
        columns.add( "`" + tableNameData + "`.`block_subid`" );
        columns.add( "`" + tableNameData + "`.`old_block_id`" );
        columns.add( "`" + tableNameData + "`.`old_block_subid`" );
        columns.add( "ex.`data`" );

        if( shouldGroup ) {
            columns.add( "COUNT(*) AS `counted`" );
        }

        if( columns.size() > 0 ) {
            query += TypeUtils.join( columns, ", " );
        }

        query += " FROM `" + tableNameData + "` ";
        query += "INNER JOIN `" + prefix + "players` p ON p.`player_id` = `" + tableNameData + "`.`player_id` ";
        query += "LEFT JOIN `" + tableNameDataExtra + "` ex ON ex.`data_id` = `" + tableNameData + "`.`id` ";
        return query;
    }

    @Override
    protected String where() {
        final int id = parameters.getId();
        if( id > 0 ) { return "WHERE `" + tableNameData + "`.`id` = " + id; }

        final int minId = parameters.getMinPrimaryKey();
        final int maxId = parameters.getMaxPrimaryKey();
        if( minId > 0 && maxId > 0 && minId != maxId ) {
            addCondition( "`" + tableNameData + "`.`id` >= " + minId );
            addCondition( "`" + tableNameData + "`.`id` < " + maxId );
        }

        worldCondition();
        actionCondition();
        playerCondition();
        radiusCondition();
        blockCondition();
        entityCondition();
        timeCondition();
        keywordCondition();
        coordinateCondition();
        return buildWhereConditions();
    }

    protected void worldCondition() {
        if( parameters.getWorld() != null ) {
            addCondition( "`" + tableNameData + "`.`world_id` = ( SELECT w.`world_id` FROM `" + prefix + "worlds` w WHERE w.`world` = '"+parameters.getWorld().replace("'", "''")+"')");
        }
    }

    protected void actionCondition() {
        final HashMap<String, MatchRule> action_types = parameters.getActionTypeNames();
        boolean containsPrismProcessType = false;
        final ArrayList<String> prismActionIds = new ArrayList<String>();
        for ( final Entry<String, Integer> entry : Prism.prismActions.entrySet() ) {
            if( entry.getKey().contains( "prism" ) ) {
                containsPrismProcessType = true;
                prismActionIds.add( "" + Prism.prismActions.get( entry.getKey() ) );
            }
        }
        if( action_types.size() > 0 ) {
            final ArrayList<String> includeIds = new ArrayList<String>();
            final ArrayList<String> excludeIds = new ArrayList<String>();
            for ( final Entry<String, MatchRule> entry : action_types.entrySet() ) {
                if( entry.getValue().equals( MatchRule.INCLUDE ) ) {
                    Integer actionId = Prism.prismActions.get( entry.getKey() );
                    if(actionId != null) includeIds.add( actionId.toString() );
                }
                if( entry.getValue().equals( MatchRule.EXCLUDE ) ) {
                    Integer actionId = Prism.prismActions.get( entry.getKey() );
                    if(actionId != null) excludeIds.add( actionId.toString() );
                }
            }
            if( includeIds.size() > 0 ) {
                addCondition( "`" + tableNameData + "`.`action_id` IN (" + TypeUtils.join( includeIds, "," ) + ")" );
            }
            if( excludeIds.size() > 0 ) {
                addCondition( "`" + tableNameData + "`.`action_id` NOT IN (" + TypeUtils.join( excludeIds, "," ) + ")" );
            }
        } else {
            if( !containsPrismProcessType && !parameters.getProcessType().equals( PrismProcessType.DELETE ) && !prismActionIds.isEmpty()) {
                addCondition( "`" + tableNameData + "`.`action_id` NOT IN (" + TypeUtils.join( prismActionIds, "," ) + ")" );
            }
        }
    }

    protected void playerCondition() {
        final HashMap<String, MatchRule> playerNames = parameters.getPlayerNames();
        if( playerNames.size() > 0 ) {
            MatchRule playerMatch = MatchRule.INCLUDE;
            for ( final MatchRule match : playerNames.values() ) {
                playerMatch = match;
                break;
            }
            final String matchQuery = ( playerMatch.equals( MatchRule.INCLUDE ) ? "IN" : "NOT IN" );
            for( Entry<String,MatchRule> entry : playerNames.entrySet() ){
                entry.setValue( MatchRule.INCLUDE );
            }
            addCondition( "`" + tableNameData + "`.`player_id` " + matchQuery
                    + " ( SELECT p.`player_id` FROM `" + prefix + "players` p WHERE "
                    + buildMultipleConditions( playerNames, "p.`player`", null ) + ")" );
        }
    }

    protected void radiusCondition() {
        buildRadiusCondition( parameters.getMinLocation(), parameters.getMaxLocation() );
    }

    protected void blockCondition() {
        final HashMap<Integer, Short> blockfilters = parameters.getBlockFilters();
        if( !blockfilters.isEmpty() ) {
            final String[] blockArr = new String[blockfilters.size()];
            int i = 0;
            for ( final Entry<Integer, Short> entry : blockfilters.entrySet() ) {
                if( entry.getValue() == -1 ) {
                    blockArr[i] = "`" + tableNameData + "`.`block_id` = " + entry.getKey();
                } else {
                    blockArr[i] = "(`" + tableNameData + "`.`block_id` = " + entry.getKey() + " AND `" + tableNameData
                            + "`.`block_subid` = " + entry.getValue() + ")";
                }
                i++;
            }
            addCondition( buildGroupConditions( null, blockArr, "%s%s", "OR", null ) );
        }
    }

    protected void entityCondition() {
        final HashMap<String, MatchRule> entityNames = parameters.getEntities();
        if( entityNames.size() > 0 ) {
            addCondition( buildMultipleConditions( entityNames, "ex.`data`", "entity_name\":\"%s" ) );
        }
    }

    protected void timeCondition() {
        Long time = parameters.getBeforeTime();
        if( time != null && time != 0 ) {
            addCondition( buildTimeCondition( time, "<=" ) );
        }
        time = parameters.getSinceTime();
        if( time != null && time != 0 ) {
            addCondition( buildTimeCondition( time, ">=" ) );
        }
    }

    protected void keywordCondition() {
        final String keyword = parameters.getKeyword();
        if( keyword != null ) {
            addCondition( "ex.`data` LIKE '%" + keyword.replace("'", "''") + "%'" );
        }
    }

    protected void coordinateCondition() {
        final ArrayList<Location> locations = parameters.getSpecificBlockLocations();
        if( locations.size() > 0 ) {
            String coordCond = "(";
            int l = 0;
            for ( final Location loc : locations ) {
                coordCond += ( l > 0 ? " OR" : "" ) + " (`" + tableNameData + "`.`x` = " + loc.getBlockX() + " AND `"
                        + tableNameData + "`.`y` = " + loc.getBlockY() + " AND `" + tableNameData + "`.`z` = "
                        + loc.getBlockZ() + ")";
                l++;
            }
            coordCond += ")";
            addCondition( coordCond );
        }
    }

    protected String buildWhereConditions() {
        int condCount = 1;
        String query = "";
        if( conditions.size() > 0 ) {
            for ( final String cond : conditions ) {
                if( condCount == 1 ) {
                    query += " WHERE ";
                } else {
                    query += " AND ";
                }
                query += cond;
                condCount++;
            }
        }
        return query;
    }

    @Override
    protected String group() {
        if( shouldGroup ) {
            if (dbType != null && dbType.equalsIgnoreCase("sqlite")) {
                return " GROUP BY `" + tableNameData + "`.`action_id`, `" + tableNameData + "`.`player_id`, `"
                        + tableNameData + "`.`block_id`, ex.`data`, strftime('%Y-%m-%d', `" + tableNameData + "`.`epoch`, 'unixepoch')";
            } else {
                return " GROUP BY `" + tableNameData + "`.`action_id`, `" + tableNameData + "`.`player_id`, `"
                        + tableNameData + "`.`block_id`, ex.`data`, DATE(FROM_UNIXTIME(`" + tableNameData + "`.`epoch`))";
            }
        }
        return "";
    }

    @Override
    protected String order() {
        final String sort_dir = parameters.getSortDirection();
        return " ORDER BY `" + tableNameData + "`.`epoch` " + sort_dir + ", `" + tableNameData + "`.`x` ASC, `" + tableNameData + "`.`z` ASC, `" + tableNameData + "`.`y` ASC, `" + tableNameData + "`.`id` " + sort_dir;
    }

    @Override
    protected String limit() {
        if( parameters.getProcessType().equals( PrismProcessType.LOOKUP ) ) {
            final int limit = parameters.getLimit();
            if( limit > 0 ) { return " LIMIT " + limit; }
        }
        return "";
    }

    protected String buildMultipleConditions(HashMap<String, MatchRule> origValues, String field_name, String format) {
        String query = "";
        if( !origValues.isEmpty() ) {
            final ArrayList<String> whereIs = new ArrayList<String>();
            final ArrayList<String> whereNot = new ArrayList<String>();
            final ArrayList<String> whereIsLike = new ArrayList<String>();
            for ( final Entry<String, MatchRule> entry : origValues.entrySet() ) {
                String value = entry.getKey().replace("'", "''");
                if( entry.getValue().equals( MatchRule.EXCLUDE ) ) {
                    whereNot.add( value );
                } else if( entry.getValue().equals( MatchRule.PARTIAL ) ) {
                    whereIsLike.add( value );
                } else {
                    whereIs.add( value );
                }
            }
            if( !whereIs.isEmpty() ) {
                String[] whereValues = new String[whereIs.size()];
                whereValues = whereIs.toArray( whereValues );
                if( format == null ) {
                    query += buildGroupConditions( field_name, whereValues, "%s = '%s'", "OR", null );
                } else {
                    query += buildGroupConditions( field_name, whereValues, "%s LIKE '%%%s%%'", "OR", format );
                }
            }
            if( !whereIsLike.isEmpty() ) {
                String[] whereValues = new String[whereIsLike.size()];
                whereValues = whereIsLike.toArray( whereValues );
                query += buildGroupConditions( field_name, whereValues, "%s LIKE '%%%s%%'", "OR", format );
            }
            if( !whereNot.isEmpty() ) {
                String[] whereNotValues = new String[whereNot.size()];
                whereNotValues = whereNot.toArray( whereNotValues );
                if( format == null ) {
                    query += buildGroupConditions( field_name, whereNotValues, "%s != '%s'", "AND", null );
                } else {
                    query += buildGroupConditions( field_name, whereNotValues, "%s NOT LIKE '%%%s%%'", "AND", format );
                }
            }
        }
        return query;
    }

    protected String buildGroupConditions(String fieldname, String[] arg_values, String matchFormat, String matchType,
                                          String dataFormat) {
        String where = "";
        matchFormat = ( matchFormat == null ? "%s = %s" : matchFormat );
        matchType = ( matchType == null ? "AND" : matchType );
        dataFormat = ( dataFormat == null ? "%s" : dataFormat );
        if( arg_values.length > 0 && !matchFormat.isEmpty() ) {
            where += "(";
            int c = 1;
            for ( final String val : arg_values ) {
                if( c > 1 && c <= arg_values.length ) {
                    where += " " + matchType + " ";
                }
                fieldname = ( fieldname == null ? "" : fieldname );
                where += String.format( matchFormat, fieldname, String.format( dataFormat, val ) );
                c++;
            }
            where += ")";
        }
        return where;
    }

    protected void buildRadiusCondition(Vector minLoc, Vector maxLoc) {
        if( minLoc != null && maxLoc != null ) {
            addCondition( "(`" + tableNameData + "`.`x` BETWEEN " + minLoc.getBlockX() + " AND " + maxLoc.getBlockX() + ")" );
            addCondition( "(`" + tableNameData + "`.`y` BETWEEN " + minLoc.getBlockY() + " AND " + maxLoc.getBlockY() + ")" );
            addCondition( "(`" + tableNameData + "`.`z` BETWEEN " + minLoc.getBlockZ() + " AND " + maxLoc.getBlockZ() + ")" );
        }
    }

    protected String buildTimeCondition(Long dateFrom, String equation) {
        if( dateFrom != null ) {
            if( equation == null ) {
                return "`" + tableNameData + "`.`epoch` >= " + ( dateFrom / 1000 );
            } else {
                return "`" + tableNameData + "`.`epoch` " + equation + " " + ( dateFrom / 1000 );
            }
        }
        return "";
    }
}