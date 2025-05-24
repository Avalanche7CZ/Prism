package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.MaterialAliases;
import me.botsko.prism.actionlibs.ActionType;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public interface Handler {
   void setPlugin(Plugin var1);

   int getId();

   void setId(int var1);

   String getUnixEpoch();

   String getDisplayDate();

   String getDisplayTime();

   void setUnixEpoch(String var1);

   String getTimeSince();

   ActionType getType();

   void setType(ActionType var1);

   String getWorldName();

   void setWorldName(String var1);

   String getPlayerName();

   void setPlayerName(String var1);

   double getX();

   void setX(double var1);

   double getY();

   void setY(double var1);

   double getZ();

   void setZ(double var1);

   void setBlockId(int var1);

   void setBlockSubId(int var1);

   int getBlockId();

   int getBlockSubId();

   void setOldBlockId(int var1);

   void setOldBlockSubId(int var1);

   int getOldBlockId();

   int getOldBlockSubId();

   String getData();

   void setData(String var1);

   void setMaterialAliases(MaterialAliases var1);

   void setAggregateCount(int var1);

   int getAggregateCount();

   String getNiceName();

   void save();

   boolean isCanceled();

   void setCanceled(boolean var1);

   ChangeResult applyRollback(Player var1, QueryParameters var2, boolean var3);

   ChangeResult applyRestore(Player var1, QueryParameters var2, boolean var3);

   ChangeResult applyUndo(Player var1, QueryParameters var2, boolean var3);

   ChangeResult applyDeferred(Player var1, QueryParameters var2, boolean var3);
}
