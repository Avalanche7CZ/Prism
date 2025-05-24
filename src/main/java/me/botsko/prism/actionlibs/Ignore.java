package me.botsko.prism.actionlibs;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.List;
import me.botsko.prism.Prism;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Ignore {
   private final Prism plugin;
   private final List ignore_players;
   private final boolean ignore_players_whitelist;
   private final List ignore_worlds;
   private final boolean ignore_worlds_whitelist;
   private final boolean ignore_creative;

   public Ignore(Prism plugin) {
      this.plugin = plugin;
      this.ignore_players = plugin.getConfig().getList("prism.ignore.players");
      this.ignore_players_whitelist = plugin.getConfig().getBoolean("prism.ignore.players_whitelist");
      this.ignore_worlds = plugin.getConfig().getList("prism.ignore.worlds");
      this.ignore_worlds_whitelist = plugin.getConfig().getBoolean("prism.ignore.worlds_whitelist");
      this.ignore_creative = plugin.getConfig().getBoolean("prism.ignore.players-in-creative");
   }

   public boolean event(String actionTypeName) {
      if (actionTypeName.contains("prism")) {
         return true;
      } else {
         return TypeUtils.subStrOccurences(actionTypeName, "-") != 1 || this.plugin.getConfig().getBoolean("prism.tracking." + actionTypeName);
      }
   }

   public boolean event(String actionTypeName, World world, String player) {
      return this.event(actionTypeName, world) && this.event(player);
   }

   public boolean event(String actionTypeName, Player player) {
      if (!this.event(actionTypeName, player.getWorld())) {
         return false;
      } else if (this.plugin.getConfig().getBoolean("prism.ignore.enable-perm-nodes") && player.hasPermission("prism.ignore.tracking." + actionTypeName)) {
         Prism.debug("Player has permission node to ignore " + actionTypeName);
         return false;
      } else {
         return this.event(player);
      }
   }

   public boolean event(Player player) {
      if (player != null && player.getName() != null) {
         if (this.ignore_players != null && this.ignore_players.contains(player.getName()) != this.ignore_players_whitelist) {
            Prism.debug("Player is being ignored, per config: " + player.getName());
            return false;
         } else if (this.ignore_creative && player.getGameMode().equals(GameMode.CREATIVE)) {
            Prism.debug("Player is in creative mode, creative mode ignored: " + player.getName());
            return false;
         } else {
            return true;
         }
      } else {
         Prism.debug("Player is being ignored, name is null");
         return false;
      }
   }

   public boolean event(String actionTypeName, Block block) {
      return this.event(actionTypeName, block.getWorld());
   }

   public boolean event(String actionTypeName, World world) {
      if (this.ignore_worlds != null && this.ignore_worlds.contains(world.getName()) != this.ignore_worlds_whitelist) {
         Prism.debug("World is being ignored, per config: " + world.getName());
         return false;
      } else {
         return this.event(actionTypeName);
      }
   }
}
