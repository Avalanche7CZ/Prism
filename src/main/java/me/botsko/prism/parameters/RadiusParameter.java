package me.botsko.prism.parameters;

import com.helion3.prism.libs.elixr.ChunkUtils;
import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.regex.Pattern;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.bridge.WorldEditBridge;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class RadiusParameter extends SimplePrismParameterHandler {
   public RadiusParameter() {
      super("Radius", Pattern.compile("[\\w,:-]+"), "r");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      Player player = null;
      if (sender instanceof Player) {
         player = (Player)sender;
      }

      FileConfiguration config = Bukkit.getPluginManager().getPlugin("Prism").getConfig();
      if (!TypeUtils.isNumeric(input) && (!input.contains(":") || input.split(":").length < 1 || !TypeUtils.isNumeric(input.split(":")[1]))) {
         if (player == null) {
            throw new IllegalArgumentException("The radius parameter must be used by a player. Use w:worldname if attempting to limit to a world.");
         }

         if (input.equals("we")) {
            if (Prism.plugin_worldEdit == null) {
               throw new IllegalArgumentException("This feature is disabled because Prism couldn't find WorldEdit.");
            }

            Prism prism = (Prism)Bukkit.getPluginManager().getPlugin("Prism");
            if (!WorldEditBridge.getSelectedArea(prism, player, query)) {
               throw new IllegalArgumentException("Invalid region selected. Make sure you have a region selected, and that it doesn't exceed the max radius.");
            }
         } else if (!input.equals("c") && !input.equals("chunk")) {
            if (input.equals("world")) {
               if (query.getProcessType().equals(PrismProcessType.LOOKUP) && !player.hasPermission("prism.override-max-lookup-radius")) {
                  throw new IllegalArgumentException("You do not have permission to override the max radius.");
               }

               if (!query.getProcessType().equals(PrismProcessType.LOOKUP) && !player.hasPermission("prism.override-max-applier-radius")) {
                  throw new IllegalArgumentException("You do not have permission to override the max radius.");
               }

               String inputValue;
               if (query.getWorld() != null) {
                  inputValue = query.getWorld();
               } else {
                  inputValue = player.getWorld().getName();
               }

               query.setWorld(inputValue);
               query.setAllowNoRadius(true);
            } else {
               if (!input.equals("global")) {
                  throw new IllegalArgumentException("Radius is invalid. There's a bunch of choice, so use /prism actions for assistance.");
               }

               if (query.getProcessType().equals(PrismProcessType.LOOKUP) && !player.hasPermission("prism.override-max-lookup-radius")) {
                  throw new IllegalArgumentException("You do not have permission to override the max radius.");
               }

               if (!query.getProcessType().equals(PrismProcessType.LOOKUP) && !player.hasPermission("prism.override-max-applier-radius")) {
                  throw new IllegalArgumentException("You do not have permission to override the max radius.");
               }

               query.setWorld((String)null);
               query.setAllowNoRadius(true);
            }
         } else {
            Chunk ch = player.getLocation().getChunk();
            query.setWorld(ch.getWorld().getName());
            query.setMinLocation(ChunkUtils.getChunkMinVector(ch));
            query.setMaxLocation(ChunkUtils.getChunkMaxVector(ch));
         }
      } else {
         Location coordsLoc = null;
         int desiredRadius;
         if (!input.contains(":")) {
            desiredRadius = Integer.parseInt(input);
         } else {
            desiredRadius = Integer.parseInt(input.split(":")[1]);
            String radiusLocOrPlayer = input.split(":")[0];
            if (radiusLocOrPlayer.contains(",") && player != null) {
               String[] coordinates = radiusLocOrPlayer.split(",");
               if (coordinates.length != 3) {
                  throw new IllegalArgumentException("Couldn't parse the coordinates '" + radiusLocOrPlayer + "'. Perhaps you have more than two commas?");
               }

               String[] arr$ = coordinates;
               int len$ = coordinates.length;

               for(int i$ = 0; i$ < len$; ++i$) {
                  String s = arr$[i$];
                  if (!TypeUtils.isNumeric(s)) {
                     throw new IllegalArgumentException("The coordinate '" + s + "' is not a number.");
                  }
               }

               coordsLoc = new Location(player.getWorld(), (double)Integer.parseInt(coordinates[0]), (double)Integer.parseInt(coordinates[1]), (double)Integer.parseInt(coordinates[2]));
            } else {
               if (Bukkit.getServer().getPlayer(radiusLocOrPlayer) == null) {
                  throw new IllegalArgumentException("Couldn't find the player named '" + radiusLocOrPlayer + "'. Perhaps they are not online or you misspelled their name?");
               }

               player = Bukkit.getServer().getPlayer(radiusLocOrPlayer);
            }
         }

         if (desiredRadius <= 0) {
            throw new IllegalArgumentException("Radius must be greater than zero. Or leave it off to use the default. Use /prism ? for help.");
         }

         if (player == null) {
            throw new IllegalArgumentException("The radius parameter must be used by a player. Use w:worldname if attempting to limit to a world.");
         }

         int radius = MiscUtils.clampRadius(player, desiredRadius, query.getProcessType(), config);
         if (desiredRadius != radius && sender != null) {
            sender.sendMessage(Prism.messenger.playerError("Forcing radius to " + radius + " as allowed by config."));
         }

         if (radius > 0) {
            query.setRadius(radius);
            if (coordsLoc != null) {
               query.setMinMaxVectorsFromPlayerLocation(coordsLoc);
            } else {
               query.setMinMaxVectorsFromPlayerLocation(player.getLocation());
            }
         }
      }

   }

   public void defaultTo(QueryParameters query, CommandSender sender) {
      if (!query.getProcessType().equals(PrismProcessType.DELETE)) {
         if (sender != null && sender instanceof Player && !query.allowsNoRadius()) {
            FileConfiguration config = Bukkit.getPluginManager().getPlugin("Prism").getConfig();
            query.setRadius(config.getInt("prism.queries.default-radius"));
            query.addDefaultUsed("r:" + query.getRadius());
         }

      }
   }
}
