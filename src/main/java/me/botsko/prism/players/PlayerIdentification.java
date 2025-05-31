package me.botsko.prism.players;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import me.botsko.prism.Prism;

import me.botsko.prism.database.PrismDatabaseHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
@SuppressWarnings("deprecation")
public class PlayerIdentification {

    private static byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }

    private static UUID bytesToUuid(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

    public static PrismPlayer cachePrismPlayer( final Player player ){
        PrismPlayer prismPlayer = getPrismPlayer( player );
        if( prismPlayer != null ){
            prismPlayer = comparePlayerToCache( player, prismPlayer );
            Prism.debug("Loaded player " + player.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
            Prism.prismPlayers.put( player.getUniqueId(), prismPlayer );
            return prismPlayer;
        }
        prismPlayer = addPlayer( player );
        return prismPlayer;
    }

    public static PrismPlayer cachePrismPlayer( final String playerName ){
        PrismPlayer prismPlayer = getPrismPlayer( playerName );
        if( prismPlayer != null ){
            Prism.debug("Loaded player " + prismPlayer.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
            if(prismPlayer.getUUID() != null){
                Prism.prismPlayers.put( prismPlayer.getUUID(), prismPlayer );
            }
            return prismPlayer;
        }
        prismPlayer = addPlayer( playerName );
        return prismPlayer;
    }

    public static PrismPlayer getPrismPlayer( String playerName ){
        Player player = Bukkit.getPlayer(playerName);
        if( player != null ) return getPrismPlayer( player );
        PrismPlayer prismPlayer = lookupByName( playerName );
        if( prismPlayer != null ) return prismPlayer;
        return null;
    }

    public static PrismPlayer getPrismPlayer( Player player ){
        if( player.getUniqueId() == null ){
            if( player.getName() != null && !player.getName().trim().isEmpty() ){
                return getPrismPlayer( player.getName() );
            }
            return null;
        }
        PrismPlayer prismPlayer = null;
        prismPlayer = Prism.prismPlayers.get( player.getUniqueId() );
        if( prismPlayer != null ) return prismPlayer;
        prismPlayer = lookupByUUID( player.getUniqueId() );
        if( prismPlayer != null ) return prismPlayer;
        prismPlayer = lookupByName( player.getName() );
        if( prismPlayer != null ) return prismPlayer;
        return null;
    }

    protected static PrismPlayer comparePlayerToCache( Player player, PrismPlayer prismPlayer ){
        if( !player.getName().equals( prismPlayer.getName() ) ){
            prismPlayer.setName( player.getName() );
            updatePlayer(prismPlayer);
        }
        if( !player.getUniqueId().equals( prismPlayer.getUUID() ) ){
            Prism.log("Player UUID for " +player.getName() + " does not match our cache! " +player.getUniqueId()+ " versus cache of " + prismPlayer.getUUID());
            prismPlayer.setUUID( player.getUniqueId() );
            updatePlayer(prismPlayer);
        }
        return prismPlayer;
    }

    protected static String uuidToDbString( UUID id ){
        return id.toString().replace("-", "");
    }

    protected static UUID uuidFromDbString( String uuid ){
        if (uuid == null || uuid.length() != 32) return null;
        String completeUuid = uuid.substring(0, 8);
        completeUuid += "-" + uuid.substring(8,12);
        completeUuid += "-" + uuid.substring(12,16);
        completeUuid += "-" + uuid.substring(16,20);
        completeUuid += "-" + uuid.substring(20, 32);
        completeUuid = completeUuid.toLowerCase();
        try {
            return UUID.fromString(completeUuid);
        } catch (IllegalArgumentException e){
            return null;
        }
    }

    protected static PrismPlayer addPlayer( Player player ){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return null;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        PrismPlayer prismPlayer = new PrismPlayer( 0, player.getUniqueId(), player.getName() );
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                s = conn.prepareStatement( "INSERT INTO `" + prefix + "players` (`player`,`player_uuid`) VALUES (?,UNHEX(?))" , Statement.RETURN_GENERATED_KEYS);
                s.setString(1, player.getName() );
                s.setString(2, uuidToDbString( player.getUniqueId() ) );
            } else {
                s = conn.prepareStatement( "INSERT INTO `" + prefix + "players` (`player`,`player_uuid`) VALUES (?,?)" , Statement.RETURN_GENERATED_KEYS);
                s.setString(1, player.getName() );
                s.setBytes(2, uuidToBytes( player.getUniqueId() ) );
            }
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (rs.next()) {
                prismPlayer.setId(rs.getInt(1));
                Prism.debug("Saved and loaded player " + player.getName() + " (" + player.getUniqueId() + ") into the cache.");
                Prism.prismPlayers.put( player.getUniqueId(), new PrismPlayer( rs.getInt(1), player.getUniqueId(), player.getName() ) );
            } else {
                throw new SQLException("Insert statement failed - no generated key obtained.");
            }
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(rs != null) try { rs.close(); } catch (SQLException e) {}
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
        return prismPlayer;
    }

    protected static PrismPlayer addPlayer( String playerName ){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return null;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        PrismPlayer fakePlayer = new PrismPlayer( 0, UUID.randomUUID(), playerName );
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                s = conn.prepareStatement( "INSERT INTO `" + prefix + "players` (`player`,`player_uuid`) VALUES (?,UNHEX(?))" , Statement.RETURN_GENERATED_KEYS);
                s.setString(1, fakePlayer.getName() );
                s.setString(2, uuidToDbString( fakePlayer.getUUID() ) );
            } else {
                s = conn.prepareStatement( "INSERT INTO `" + prefix + "players` (`player`,`player_uuid`) VALUES (?,?)" , Statement.RETURN_GENERATED_KEYS);
                s.setString(1, fakePlayer.getName() );
                s.setBytes(2, uuidToBytes( fakePlayer.getUUID() ) );
            }
            s.executeUpdate();
            rs = s.getGeneratedKeys();
            if (rs.next()){
                fakePlayer.setId( rs.getInt(1) );
                Prism.debug("Saved and loaded fake player " + fakePlayer.getName() + " into the cache.");
                Prism.prismPlayers.put( fakePlayer.getUUID(), fakePlayer );
            } else {
                throw new SQLException("Insert statement failed - no generated key obtained.");
            }
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(rs != null) try { rs.close(); } catch (SQLException e) {}
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
        return fakePlayer;
    }

    protected static void updatePlayer( PrismPlayer prismPlayer ){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        Connection conn = null;
        PreparedStatement s = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                s = conn.prepareStatement( "UPDATE `" + prefix + "players` SET `player` = ?, `player_uuid` = UNHEX(?) WHERE `player_id` = ?");
                s.setString(1, prismPlayer.getName() );
                s.setString(2, uuidToDbString( prismPlayer.getUUID() ) );
                s.setInt(3, prismPlayer.getId() );
            } else {
                s = conn.prepareStatement( "UPDATE `" + prefix + "players` SET `player` = ?, `player_uuid` = ? WHERE `player_id` = ?");
                s.setString(1, prismPlayer.getName() );
                s.setBytes(2, uuidToBytes( prismPlayer.getUUID() ) );
                s.setInt(3, prismPlayer.getId() );
            }
            s.executeUpdate();
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }

    protected static PrismPlayer lookupByName( String playerName ){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return null;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        PrismPlayer prismPlayer = null;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            String sql;
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                sql = "SELECT `player_id`, `player`, HEX(`player_uuid`) as `uuid_str` FROM `" + prefix + "players` WHERE `player` = ?";
            } else {
                sql = "SELECT `player_id`, `player`, `player_uuid` as `uuid_blob` FROM `" + prefix + "players` WHERE `player` = ?";
            }
            s = conn.prepareStatement(sql);
            s.setString(1, playerName);
            rs = s.executeQuery();
            if( rs.next() ){
                UUID uuid;
                if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                    uuid = uuidFromDbString(rs.getString("uuid_str"));
                } else {
                    uuid = bytesToUuid(rs.getBytes("uuid_blob"));
                }
                prismPlayer = new PrismPlayer( rs.getInt("player_id"), uuid, rs.getString("player") );
            }
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(rs != null) try { rs.close(); } catch (SQLException e) {}
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
        return prismPlayer;
    }

    protected static PrismPlayer lookupByUUID( UUID uuid ){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return null;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        PrismPlayer prismPlayer = null;
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            String sql;
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                sql = "SELECT `player_id`, `player`, HEX(`player_uuid`) as `uuid_str` FROM `" + prefix + "players` WHERE `player_uuid` = UNHEX(?)";
                s = conn.prepareStatement(sql);
                s.setString(1, uuidToDbString(uuid));
            } else {
                sql = "SELECT `player_id`, `player`, `player_uuid` as `uuid_blob` FROM `" + prefix + "players` WHERE `player_uuid` = ?";
                s = conn.prepareStatement(sql);
                s.setBytes(1, uuidToBytes(uuid));
            }
            rs = s.executeQuery();
            if( rs.next() ){
                UUID foundUuid;
                if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                    foundUuid = uuidFromDbString(rs.getString("uuid_str"));
                } else {
                    foundUuid = bytesToUuid(rs.getBytes("uuid_blob"));
                }
                prismPlayer = new PrismPlayer( rs.getInt("player_id"), foundUuid, rs.getString("player") );
            }
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(rs != null) try { rs.close(); } catch (SQLException e) {}
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
        return prismPlayer;
    }

    public static void cacheOnlinePlayerPrimaryKeys(){
        Prism pluginInstance = (Prism) Bukkit.getPluginManager().getPlugin("Prism");
        if (pluginInstance == null) return;
        String prefix = PrismDatabaseHandler.getTablePrefix();
        String dbType = PrismDatabaseHandler.getDbType();

        Player[] onlinePlayersArray = Bukkit.getServer().getOnlinePlayers().toArray(new Player[0]);
        List<String> playerNames = new ArrayList<>();
        for( Player pl : onlinePlayersArray ){
            playerNames.add(pl.getName());
        }
        if (playerNames.isEmpty()) {
            return;
        }
        Connection conn = null;
        PreparedStatement s = null;
        ResultSet rs = null;
        try {
            conn = PrismDatabaseHandler.dbc();
            StringBuilder sqlBuilder = new StringBuilder();
            if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                sqlBuilder.append("SELECT `player_id`, `player`, HEX(`player_uuid`) as `uuid_str` FROM `");
            } else {
                sqlBuilder.append("SELECT `player_id`, `player`, `player_uuid` as `uuid_blob` FROM `");
            }
            sqlBuilder.append(prefix).append("players` WHERE `player` IN (");
            for (int i = 0; i < playerNames.size(); i++) {
                sqlBuilder.append("?");
                if (i < playerNames.size() - 1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(")");
            s = conn.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < playerNames.size(); i++) {
                s.setString(i + 1, playerNames.get(i));
            }
            rs = s.executeQuery();
            while( rs.next() ){
                UUID uuid;
                if (dbType.equalsIgnoreCase("mysql") || dbType.equalsIgnoreCase("mariadb")) {
                    uuid = uuidFromDbString(rs.getString("uuid_str"));
                } else {
                    uuid = bytesToUuid(rs.getBytes("uuid_blob"));
                }
                PrismPlayer prismPlayer = new PrismPlayer( rs.getInt("player_id"), uuid, rs.getString("player") );
                Prism.debug("Loaded player " + rs.getString("player") + ", id: " + rs.getInt("player_id") + " into the cache.");
                if(prismPlayer.getUUID() != null){
                    Prism.prismPlayers.put( prismPlayer.getUUID(), prismPlayer );
                }
            }
        } catch (SQLException e) {
            PrismDatabaseHandler.handleDatabaseException(e);
        } finally {
            if(rs != null) try { rs.close(); } catch (SQLException e) {}
            if(s != null) try { s.close(); } catch (SQLException e) {}
            if(conn != null) try { conn.close(); } catch (SQLException e) {}
        }
    }
}