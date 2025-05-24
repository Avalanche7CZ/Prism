package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.commandlibs.Flag;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Vector;

public class QueryParameters implements Cloneable {
   protected Set foundArgs = new HashSet();
   protected PrismProcessType processType;
   protected final ArrayList defaultsUsed;
   protected String original_command;
   protected boolean allow_no_radius;
   protected int id;
   protected int minId;
   protected int maxId;
   protected Vector maxLoc;
   protected Vector minLoc;
   protected int parent_id;
   protected Location player_location;
   protected int radius;
   protected final ArrayList specific_block_locations;
   protected Long since_time;
   protected Long before_time;
   protected String world;
   protected String keyword;
   protected boolean ignoreTime;
   protected HashMap actionTypeRules;
   protected final HashMap block_filters;
   protected final HashMap entity_filters;
   protected final HashMap player_names;
   protected final ArrayList flags;
   protected final ArrayList shared_players;
   protected int per_page;
   protected int limit;

   public QueryParameters() {
      this.processType = PrismProcessType.LOOKUP;
      this.defaultsUsed = new ArrayList();
      this.allow_no_radius = false;
      this.id = 0;
      this.minId = 0;
      this.maxId = 0;
      this.parent_id = 0;
      this.specific_block_locations = new ArrayList();
      this.actionTypeRules = new HashMap();
      this.block_filters = new HashMap();
      this.entity_filters = new HashMap();
      this.player_names = new HashMap();
      this.flags = new ArrayList();
      this.shared_players = new ArrayList();
      this.per_page = 5;
      this.limit = 1000000;
   }

   public int getId() {
      return this.id;
   }

   public void setId(int id) {
      this.id = id;
   }

   public void setMinPrimaryKey(int minId) {
      this.minId = minId;
   }

   public int getMinPrimaryKey() {
      return this.minId;
   }

   public void setMaxPrimaryKey(int maxId) {
      this.maxId = maxId;
   }

   public int getMaxPrimaryKey() {
      return this.maxId;
   }

   public HashMap getEntities() {
      return this.entity_filters;
   }

   public void addEntity(String entity) {
      this.addEntity(entity, MatchRule.INCLUDE);
   }

   public void addEntity(String entity, MatchRule match) {
      this.entity_filters.put(entity, match);
   }

   public HashMap getBlockFilters() {
      return this.block_filters;
   }

   public void addBlockFilter(int id, short data) {
      this.block_filters.put(id, data);
   }

   public ArrayList getSpecificBlockLocations() {
      return this.specific_block_locations;
   }

   public void setSpecificBlockLocation(Location loc) {
      this.specific_block_locations.clear();
      this.addSpecificBlockLocation(loc);
   }

   public void addSpecificBlockLocation(Location loc) {
      this.specific_block_locations.add(loc);
   }

   public Location getPlayerLocation() {
      return this.player_location;
   }

   public void setMinMaxVectorsFromPlayerLocation(Location loc) {
      this.player_location = loc;
      if (this.radius > 0) {
         this.minLoc = new Vector(loc.getX() - (double)this.radius, loc.getY() - (double)this.radius, loc.getZ() - (double)this.radius);
         this.maxLoc = new Vector(loc.getX() + (double)this.radius, loc.getY() + (double)this.radius, loc.getZ() + (double)this.radius);
      }

   }

   public void resetMinMaxVectors() {
      this.minLoc = null;
      this.maxLoc = null;
   }

   public Vector getMinLocation() {
      return this.minLoc;
   }

   public void setMinLocation(Vector minLoc) {
      this.minLoc = minLoc;
   }

   public Vector getMaxLocation() {
      return this.maxLoc;
   }

   public void setMaxLocation(Vector maxLoc) {
      this.maxLoc = maxLoc;
   }

   public int getRadius() {
      return this.radius;
   }

   public void setRadius(int radius) {
      this.radius = radius;
   }

   public boolean allowsNoRadius() {
      return this.allow_no_radius;
   }

   public void setAllowNoRadius(boolean allow_no_radius) {
      this.allow_no_radius = allow_no_radius;
   }

   public HashMap getPlayerNames() {
      return this.player_names;
   }

   public void addPlayerName(String player) {
      this.addPlayerName(player, MatchRule.INCLUDE);
   }

   public void addPlayerName(String player, MatchRule match) {
      this.player_names.put(player, match);
   }

   public String getWorld() {
      return this.world;
   }

   public void setWorld(String world) {
      this.world = world;
   }

   public String getKeyword() {
      return this.keyword;
   }

   public void setKeyword(String keyword) {
      this.keyword = keyword;
   }

   public HashMap getActionTypes() {
      return this.actionTypeRules;
   }

   public HashMap getActionTypeNames() {
      return this.actionTypeRules;
   }

   public void addActionType(String action_type) {
      this.addActionType(action_type, MatchRule.INCLUDE);
   }

   public void addActionType(String action_type, MatchRule match) {
      this.actionTypeRules.put(action_type, match);
   }

   public void removeActionType(ActionType a) {
      this.actionTypeRules.remove(a.getName());
   }

   public void resetActionTypes() {
      this.actionTypeRules.clear();
   }

   public Long getBeforeTime() {
      return this.before_time;
   }

   public void setBeforeTime(Long epoch) {
      this.before_time = epoch;
   }

   public Long getSinceTime() {
      return this.since_time;
   }

   public void setSinceTime(Long epoch) {
      this.since_time = epoch;
   }

   public int getLimit() {
      return this.limit;
   }

   public void setLimit(int limit) {
      this.limit = limit;
   }

   public PrismProcessType getProcessType() {
      return this.processType;
   }

   public void setProcessType(PrismProcessType lookup_type) {
      this.processType = lookup_type;
   }

   public Set getFoundArgs() {
      return this.foundArgs;
   }

   public void setFoundArgs(Set foundArgs) {
      this.foundArgs = foundArgs;
   }

   public void setParentId(int id) {
      this.parent_id = id;
   }

   public int getParentId() {
      return this.parent_id;
   }

   public String getSortDirection() {
      return !this.processType.equals(PrismProcessType.RESTORE) ? "DESC" : "ASC";
   }

   public void addFlag(Flag flag) {
      if (!this.hasFlag(flag)) {
         this.flags.add(flag);
      }
   }

   public boolean hasFlag(Flag flag) {
      return this.flags.contains(flag);
   }

   public int getPerPage() {
      return this.per_page;
   }

   public void setPerPage(int per_page) {
      this.per_page = per_page;
   }

   public void addDefaultUsed(String d) {
      this.defaultsUsed.add(d);
   }

   public ArrayList getDefaultsUsed() {
      return this.defaultsUsed;
   }

   public void setStringFromRawArgs(String[] args, int start) {
      String params = "";
      if (args.length > 0) {
         for(int i = start; i < args.length; ++i) {
            params = params + " " + args[i];
         }
      }

      this.original_command = params;
   }

   public String getOriginalCommand() {
      return this.original_command;
   }

   public ArrayList getSharedPlayers() {
      return this.shared_players;
   }

   public void addSharedPlayer(CommandSender sender) {
      this.shared_players.add(sender);
   }

   public QueryParameters clone() throws CloneNotSupportedException {
      QueryParameters cloned = (QueryParameters)super.clone();
      cloned.actionTypeRules = new HashMap(this.actionTypeRules);
      return cloned;
   }

   public void setIgnoreTime(boolean ignore) {
      this.ignoreTime = ignore;
   }

   public boolean getIgnoreTime() {
      return this.ignoreTime;
   }
}
