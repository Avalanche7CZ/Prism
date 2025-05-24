package me.botsko.prism.settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import me.botsko.prism.Prism;
import org.bukkit.entity.Player;

public class Settings {
   public static String getPlayerKey(Player player, String key) {
      return player.getName() + "." + key;
   }

   public static void deleteSetting(String key) {
      deleteSetting(key, (Player)null);
   }

   public static void deleteSetting(String key, Player player) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      Connection conn = null;
      PreparedStatement s = null;

      try {
         String finalKey = key;
         if (player != null) {
            finalKey = getPlayerKey(player, key);
         }

         conn = Prism.dbc();
         s = conn.prepareStatement("DELETE FROM " + prefix + "meta WHERE k = ?");
         s.setString(1, finalKey);
         s.executeUpdate();
      } catch (SQLException var18) {
      } finally {
         if (s != null) {
            try {
               s.close();
            } catch (SQLException var17) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var16) {
            }
         }

      }

   }

   public static void saveSetting(String key, String value) {
      saveSetting(key, value, (Player)null);
   }

   public static void saveSetting(String key, String value, Player player) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      Connection conn = null;
      PreparedStatement s = null;

      try {
         String finalKey = key;
         if (player != null) {
            finalKey = getPlayerKey(player, key);
         }

         conn = Prism.dbc();
         s = conn.prepareStatement("DELETE FROM " + prefix + "meta WHERE k = ?");
         s.setString(1, finalKey);
         s.executeUpdate();
         s = conn.prepareStatement("INSERT INTO " + prefix + "meta (k,v) VALUES (?,?)");
         s.setString(1, finalKey);
         s.setString(2, value);
         s.executeUpdate();
      } catch (SQLException var19) {
      } finally {
         if (s != null) {
            try {
               s.close();
            } catch (SQLException var18) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var17) {
            }
         }

      }

   }

   public static String getSetting(String key) {
      return getSetting(key, (Player)null);
   }

   public static String getSetting(String key, Player player) {
      String prefix = Prism.config.getString("prism.mysql.prefix");
      String value = null;
      Connection conn = null;
      PreparedStatement s = null;
      ResultSet rs = null;

      try {
         String finalKey = key;
         if (player != null) {
            finalKey = getPlayerKey(player, key);
         }

         conn = Prism.dbc();
         s = conn.prepareStatement("SELECT v FROM " + prefix + "meta WHERE k = ? LIMIT 0,1");
         s.setString(1, finalKey);

         for(rs = s.executeQuery(); rs.next(); value = rs.getString("v")) {
         }
      } catch (SQLException var24) {
      } finally {
         if (rs != null) {
            try {
               rs.close();
            } catch (SQLException var23) {
            }
         }

         if (s != null) {
            try {
               s.close();
            } catch (SQLException var22) {
            }
         }

         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException var21) {
            }
         }

      }

      return value;
   }
}
