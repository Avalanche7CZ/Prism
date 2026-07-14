package me.botsko.prism.wands;

import java.util.ArrayList;
import java.util.List;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionMessage;
import me.botsko.prism.actionlibs.ActionsQuery;
import me.botsko.prism.actionlibs.MatchRule;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actionlibs.QueryResult;
import me.botsko.prism.commandlibs.Flag;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public class InspectorWand extends QueryWandBase implements Wand {

    /**
     * 
     * @param plugin
     */
    public InspectorWand(Prism plugin) {
        super( plugin );
    }

    /**
	 * 
	 */
    @Override
    public void playerLeftClick(Player player, Location loc) {
        showLocationHistory( player, loc );
    }

    /**
	 * 
	 */
    @Override
    public void playerRightClick(Player player, Location loc) {
        showLocationHistory( player, loc );
    }

    /**
     * 
     * @param player
     * @param block
     * @param loc
     */
    protected void showLocationHistory(final Player player, final Location loc) {

        final Block block = loc.getBlock();

        // Build all Bukkit-dependent context on the primary server thread.
        QueryParameters queryParameters;
        try {
            queryParameters = parameters.clone();
        } catch ( final CloneNotSupportedException ex ) {
            queryParameters = new QueryParameters();
            player.sendMessage( Prism.messenger
                    .playerError( "Error retrieving parameters. Checking with default parameters." ) );
        }
        queryParameters.setWorld( player.getWorld().getName() );
        queryParameters.setSpecificBlockLocation( loc );

        // Do we need a second location? (For beds, doors, etc)
        final Block sibling = me.botsko.elixr.BlockUtils.getSiblingForDoubleLengthBlock( block );
        if( sibling != null ) {
            queryParameters.addSpecificBlockLocation( sibling.getLocation() );
        }

        // Ignoring any actions via config?
        if( queryParameters.getActionTypes().size() == 0 ) {
            @SuppressWarnings("unchecked")
            final ArrayList<String> ignoreActions = (ArrayList<String>) plugin.getConfig().getList(
                    "prism.wands.inspect.ignore-actions" );
            if( ignoreActions != null && !ignoreActions.isEmpty() ) {
                for ( final String ignore : ignoreActions ) {
                    queryParameters.addActionType( ignore, MatchRule.EXCLUDE );
                }
            }
        }
        for ( final String _default : queryParameters.getDefaultsUsed() ) {
            if( _default.startsWith( "t:" ) ) {
                queryParameters.setIgnoreTime( true );
                break;
            }
        }

        final QueryParameters params = queryParameters;
        final String blockname = Prism.getItems().getAlias( block.getTypeId(), block.getData() );
        final String spaceName = ( block.getType().equals( Material.AIR ) ? "space" : block.getType()
                .toString().replaceAll( "_", " " ).toLowerCase()
                + ( block.getType().toString().endsWith( "BLOCK" ) ? "" : " block" ) );

        /**
         * Run the lookup itself in an async task so the lookup query isn't done
         * on the main thread
         */
        plugin.getServer().getScheduler().runTaskAsynchronously( plugin, new Runnable() {
            @Override
            public void run() {
                // Query
                final ActionsQuery aq = new ActionsQuery( plugin );
                final QueryResult results = aq.lookup( params, player );
                plugin.getServer().getScheduler().runTask( plugin, new Runnable() {
                    @Override
                    public void run() {
                        showResults( player, loc, blockname, spaceName, params, results );
                    }
                } );
            }
        } );
    }

    protected void showResults(Player player, Location loc, String blockname, String spaceName,
            QueryParameters params, QueryResult results) {
        if( !results.getActionResults().isEmpty() ) {
            player.sendMessage( Prism.messenger.playerHeaderMsg( ChatColor.GOLD + "--- Inspecting " + blockname
                    + " at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " ---" ) );
            final List<me.botsko.prism.actions.Handler> paginated = results.getPaginatedActionResults();
            if( results.getTotal_pages() > 1 && paginated != null ) {
                player.sendMessage( Prism.messenger.playerHeaderMsg( buildPaginationMessage( results, paginated.size() ) ) );
            }
            if( paginated != null ) {
                for ( final me.botsko.prism.actions.Handler a : paginated ) {
                    final ActionMessage am = new ActionMessage( a );
                    if( params.hasFlag( Flag.EXTENDED )
                            || plugin.getConfig().getBoolean( "prism.messenger.always-show-extended" ) ) {
                        am.showExtended();
                    }
                    player.sendMessage( Prism.messenger.playerMsg( am.getMessage() ) );
                }
            }
        } else {
            player.sendMessage( Prism.messenger.playerError( "No history for this " + spaceName + " found." ) );
        }
    }

    static String buildPaginationMessage(QueryResult results, int displayedResults) {
        return "Showing " + displayedResults + " of " + results.getTotalResults() + " results. Page "
                + results.getPage() + " of " + results.getTotal_pages()
                + ". Use '/prism page <#>' to view other pages";
    }

    /**
	 * 
	 */
    @Override
    public void playerRightClick(Player player, Entity entity) {
        return;
    }
}
