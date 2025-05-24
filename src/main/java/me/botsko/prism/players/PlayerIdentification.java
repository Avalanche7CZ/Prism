package me.botsko.prism.players;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.UUID;
import me.botsko.prism.Prism;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlayerIdentification {
   public static PrismPlayer cachePrismPlayer(Player player) {
      PrismPlayer prismPlayer = getPrismPlayer(player);
      if (prismPlayer != null) {
         prismPlayer = comparePlayerToCache(player, prismPlayer);
         Prism.debug("Loaded player " + player.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
         Prism.prismPlayers.put(player.getUniqueId(), prismPlayer);
         return prismPlayer;
      } else {
         prismPlayer = addPlayer(player);
         return prismPlayer;
      }
   }

   public static PrismPlayer cachePrismPlayer(String playerName) {
      PrismPlayer prismPlayer = getPrismPlayer(playerName);
      if (prismPlayer != null) {
         Prism.debug("Loaded player " + prismPlayer.getName() + ", id: " + prismPlayer.getId() + " into the cache.");
         return prismPlayer;
      } else {
         prismPlayer = addPlayer(playerName);
         return prismPlayer;
      }
   }

   public static PrismPlayer getPrismPlayer(String playerName) {
      Player player = Bukkit.getPlayer(playerName);
      if (player != null) {
         return getPrismPlayer(player);
      } else {
         PrismPlayer prismPlayer = lookupByName(playerName);
         return prismPlayer != null ? prismPlayer : null;
      }
   }

   public static PrismPlayer getPrismPlayer(Player player) {
      if (player.getUniqueId() == null) {
         return player.getName() != null && !player.getName().trim().isEmpty() ? getPrismPlayer(player.getName()) : null;
      } else {
         PrismPlayer prismPlayer = null;
         prismPlayer = (PrismPlayer)Prism.prismPlayers.get(player.getUniqueId());
         if (prismPlayer != null) {
            return prismPlayer;
         } else {
            prismPlayer = lookupByUUID(player.getUniqueId());
            if (prismPlayer != null) {
               return prismPlayer;
            } else {
               prismPlayer = lookupByName(player.getName());
               return prismPlayer != null ? prismPlayer : null;
            }
         }
      }
   }

   protected static PrismPlayer comparePlayerToCache(Player player, PrismPlayer prismPlayer) {
      if (!player.getName().equals(prismPlayer.getName())) {
         prismPlayer.setName(player.getName());
         updatePlayer(prismPlayer);
      }

      if (!player.getUniqueId().equals(prismPlayer.getUUID())) {
         Prism.log("Player UUID for " + player.getName() + " does not match our cache! " + player.getUniqueId() + " versus cache of " + prismPlayer.getUUID());
         prismPlayer.setUUID(player.getUniqueId());
         updatePlayer(prismPlayer);
      }

      return prismPlayer;
   }

   protected static String uuidToDbString(UUID id) {
      return id.toString().replace("-", "");
   }

   protected static UUID uuidFromDbString(String uuid) {
      String completeUuid = uuid.substring(0, 8);
      completeUuid = completeUuid + "-" + uuid.substring(8, 12);
      completeUuid = completeUuid + "-" + uuid.substring(12, 16);
      completeUuid = completeUuid + "-" + uuid.substring(16, 20);
      completeUuid = completeUuid + "-" + uuid.substring(20, uuid.length());
      completeUuid = completeUuid.toLowerCase();
      return UUID.fromString(completeUuid);
   }

   protected static PrismPlayer addPlayer(Player player) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      PrismPlayer prismPlayer = new PrismPlayer(0, player.getUniqueId(), player.getName());
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("INSERT INTO " + prefix + "players (player,player_uuid) VALUES (?,UNHEX(?))", 1);
         s.setString(1, player.getName());
         s.setString(2, uuidToDbString(player.getUniqueId()));
         s.executeUpdate();
         rs = s.getGeneratedKeys();
         if (!rs.next()) {
            throw new SQLException("Insert statement failed - no generated key obtained.");
         }

         prismPlayer.setId(rs.getInt(1));
         Prism.debug("Saved and loaded player " + player.getName() + " (" + player.getUniqueId() + ") into the cache.");
         Prism.prismPlayers.put(player.getUniqueId(), new PrismPlayer(rs.getInt(1), player.getUniqueId(), player.getName()));
      } catch (SQLException var23) {
         var23.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var22) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var21) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var20) {
            }
         }

      }

      return prismPlayer;
   }

   protected static PrismPlayer addPlayer(String playerName) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      PrismPlayer fakePlayer = new PrismPlayer(0, UUID.randomUUID(), playerName);
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("INSERT INTO " + prefix + "players (player,player_uuid) VALUES (?,UNHEX(?))", 1);
         s.setString(1, fakePlayer.getName());
         s.setString(2, uuidToDbString(fakePlayer.getUUID()));
         s.executeUpdate();
         rs = s.getGeneratedKeys();
         if (!rs.next()) {
            throw new SQLException("Insert statement failed - no generated key obtained.");
         }

         fakePlayer.setId(rs.getInt(1));
         Prism.debug("Saved and loaded fake player " + fakePlayer.getName() + " into the cache.");
         Prism.prismPlayers.put(fakePlayer.getUUID(), fakePlayer);
      } catch (SQLException var23) {
         var23.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var22) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var21) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var20) {
            }
         }

      }

      return fakePlayer;
   }

   protected static void updatePlayer(PrismPlayer prismPlayer) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("UPDATE " + prefix + "players SET player = ?, player_uuid = UNHEX(?) WHERE player_id = ?");
         s.setString(1, prismPlayer.getName());
         s.setString(2, uuidToDbString(prismPlayer.getUUID()));
         s.setInt(3, prismPlayer.getId());
         s.executeUpdate();
      } catch (SQLException var22) {
         var22.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               ((ResultSet)rs).close();
            } catch (SQLException var21) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var20) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var19) {
            }
         }

      }

   }

   protected static PrismPlayer lookupByName(String playerName) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      PrismPlayer prismPlayer = null;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("SELECT player_id, player, HEX(player_uuid) FROM " + prefix + "players WHERE player = ?");
         s.setString(1, playerName);
         rs = s.executeQuery();
         if (rs.next()) {
            prismPlayer = new PrismPlayer(rs.getInt(1), uuidFromDbString(rs.getString(3)), rs.getString(2));
         }
      } catch (SQLException var23) {
         var23.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var22) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var21) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var20) {
            }
         }

      }

      return prismPlayer;
   }

   protected static PrismPlayer lookupByUUID(UUID uuid) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      PrismPlayer prismPlayer = null;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("SELECT player_id, player, HEX(player_uuid) FROM " + prefix + "players WHERE player_uuid = UNHEX(?)");
         s.setString(1, uuidToDbString(uuid));
         rs = s.executeQuery();
         if (rs.next()) {
            prismPlayer = new PrismPlayer(rs.getInt(1), uuidFromDbString(rs.getString(3)), rs.getString(2));
         }
      } catch (SQLException var23) {
         var23.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var22) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var21) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var20) {
            }
         }

      }

      return prismPlayer;
   }

   public static void cacheOnlinePlayerPrimaryKeys() {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      String[] playerNames = new String[Bukkit.getServer().getOnlinePlayers().size()];
      int i = 0;

      for(Iterator i$ = Bukkit.getServer().getOnlinePlayers().iterator(); i$.hasNext(); ++i) {
         Player pl = (Player)i$.next();
         playerNames[i] = pl.getName();
      }

      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         conn = Prism.dbc();
         s = conn.prepareStatement("SELECT player_id, player, HEX(player_uuid) FROM " + prefix + "players WHERE player IN (?)");
         s.setString(1, "'" + TypeUtils.join(playerNames, "','") + "'");
         rs = s.executeQuery();

         while(rs.next()) {
            PrismPlayer prismPlayer = new PrismPlayer(rs.getInt(1), uuidFromDbString(rs.getString(3)), rs.getString(2));
            Prism.debug("Loaded player " + rs.getString(2) + ", id: " + rs.getInt(1) + " into the cache.");
            Prism.prismPlayers.put(UUID.fromString(rs.getString(2)), prismPlayer);
         }
      } catch (SQLException var23) {
         var23.printStackTrace();
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var22) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var21) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var20) {
            }
         }

      }

   }
}
