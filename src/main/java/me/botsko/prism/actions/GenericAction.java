package me.botsko.prism.actions;

import com.helion3.prism.libs.com.google.gson.Gson;
import com.helion3.prism.libs.com.google.gson.GsonBuilder;
import com.helion3.prism.libs.elixr.MaterialAliases;
import java.text.SimpleDateFormat;
import java.util.Date;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class GenericAction implements Handler {
   protected Plugin plugin;
   protected boolean canceled = false;
   protected final Gson gson = (new GsonBuilder()).disableHtmlEscaping().create();
   protected ActionType type;
   protected MaterialAliases materialAliases;
   protected int id;
   protected String epoch;
   protected String display_date;
   protected String display_time;
   protected String world_name;
   protected String player_name;
   protected double x;
   protected double y;
   protected double z;
   protected int block_id;
   protected int block_subid;
   protected int old_block_id;
   protected int old_block_subid;
   protected String data;
   protected int aggregateCount = 0;

   public void setPlugin(Plugin plugin) {
      this.plugin = plugin;
   }

   public void setActionType(String action_type) {
      if (action_type != null) {
         this.type = Prism.getActionRegistry().getAction(action_type);
      }

   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public String getUnixEpoch() {
      return this.epoch;
   }

   public String getDisplayDate() {
      return this.display_date;
   }

   public void setUnixEpoch(String epoch) {
      this.epoch = epoch;
      Date action_time = new Date(Long.parseLong(epoch) * 1000L);
      SimpleDateFormat date = new SimpleDateFormat("yy/MM/dd");
      this.display_date = date.format(action_time);
      SimpleDateFormat time = new SimpleDateFormat("hh:mm:ssa");
      this.display_time = time.format(action_time);
   }

   public String getDisplayTime() {
      return this.display_time;
   }

   public String getTimeSince() {
      String time_ago = "";
      Date start = new Date(Long.parseLong(this.epoch) * 1000L);
      Date end = new Date();
      long diffInSeconds = (end.getTime() - start.getTime()) / 1000L;
      long[] diff = new long[]{diffInSeconds /= 24L, (diffInSeconds /= 60L) >= 24L ? diffInSeconds % 24L : diffInSeconds, (diffInSeconds /= 60L) >= 60L ? diffInSeconds % 60L : diffInSeconds, diffInSeconds >= 60L ? diffInSeconds % 60L : diffInSeconds};
      if (diff[0] >= 1L) {
         time_ago = time_ago + diff[0] + "d";
      }

      if (diff[1] >= 1L) {
         time_ago = time_ago + diff[1] + "h";
      }

      if (diff[2] > 1L && diff[2] < 60L) {
         time_ago = time_ago + diff[2] + "m";
      }

      if (!time_ago.isEmpty()) {
         time_ago = time_ago + " ago";
      }

      if (diff[0] == 0L && diff[1] == 0L && diff[2] <= 1L) {
         time_ago = "just now";
      }

      return time_ago;
   }

   public ActionType getType() {
      return this.type;
   }

   public void setType(ActionType type) {
      this.type = type;
   }

   public String getWorldName() {
      return this.world_name;
   }

   public void setWorldName(String world_name) {
      this.world_name = world_name;
   }

   public void setPlayerName(Player player) {
      if (player != null) {
         this.player_name = player.getName();
      }

   }

   public String getPlayerName() {
      return this.player_name;
   }

   public void setPlayerName(String player_name) {
      this.player_name = player_name;
   }

   public double getX() {
      return this.x;
   }

   public void setX(double x) {
      this.x = x;
   }

   public double getY() {
      return this.y;
   }

   public void setY(double y) {
      this.y = y;
   }

   public double getZ() {
      return this.z;
   }

   public void setZ(double z) {
      this.z = z;
   }

   public void setLoc(Location loc) {
      if (loc != null) {
         this.world_name = loc.getWorld().getName();
         this.x = loc.getX();
         this.y = loc.getY();
         this.z = loc.getZ();
      }

   }

   public World getWorld() {
      return this.plugin.getServer().getWorld(this.getWorldName());
   }

   public Location getLoc() {
      return new Location(this.getWorld(), this.getX(), this.getY(), this.getZ());
   }

   public void setBlockId(int id) {
      if (this.type.getName().equals("block-place") && (id == 8 || id == 10)) {
         id = id == 8 ? 9 : 11;
      }

      this.block_id = id;
   }

   public void setBlockSubId(int id) {
      this.block_subid = id;
   }

   public int getBlockId() {
      return this.block_id;
   }

   public int getBlockSubId() {
      return this.block_subid;
   }

   public void setOldBlockId(int id) {
      this.old_block_id = id;
   }

   public void setOldBlockSubId(int id) {
      this.old_block_subid = id;
   }

   public int getOldBlockId() {
      return this.old_block_id;
   }

   public int getOldBlockSubId() {
      return this.old_block_subid;
   }

   public String getData() {
      return this.data;
   }

   public void setData(String data) {
      this.data = data;
   }

   public void setMaterialAliases(MaterialAliases m) {
      this.materialAliases = m;
   }

   public void setAggregateCount(int aggregateCount) {
      this.aggregateCount = aggregateCount;
   }

   public int getAggregateCount() {
      return this.aggregateCount;
   }

   public String getNiceName() {
      return "something";
   }

   public boolean isCanceled() {
      return this.canceled;
   }

   public void setCanceled(boolean cancel) {
      this.canceled = cancel;
   }

   public void save() {
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      return null;
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      return null;
   }

   public ChangeResult applyUndo(Player player, QueryParameters parameters, boolean is_preview) {
      return null;
   }

   public ChangeResult applyDeferred(Player player, QueryParameters parameters, boolean is_preview) {
      return null;
   }
}
