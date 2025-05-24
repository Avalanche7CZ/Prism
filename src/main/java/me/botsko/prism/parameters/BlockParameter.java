package me.botsko.prism.parameters;

import com.helion3.prism.libs.elixr.ItemUtils;
import com.helion3.prism.libs.elixr.MaterialAliases;
import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import org.bukkit.command.CommandSender;

public class BlockParameter extends SimplePrismParameterHandler {
   public BlockParameter() {
      super("Block", Pattern.compile("[\\w,:]+"), "b");
   }

   public void process(QueryParameters query, String alias, String input, CommandSender sender) {
      String[] blocks = input.split(",");
      if (blocks.length > 0) {
         String[] arr$ = blocks;
         int len$ = blocks.length;

         for(int i$ = 0; i$ < len$; ++i$) {
            String b = arr$[i$];
            if (b.contains(":") && b.length() >= 3) {
               String[] ids = b.split(":");
               if (ids.length != 2 || !TypeUtils.isNumeric(ids[0]) || !TypeUtils.isNumeric(ids[1])) {
                  throw new IllegalArgumentException("Invalid block name '" + b + "'. Try /pr ? for help");
               }

               query.addBlockFilter(Integer.parseInt(ids[0]), Short.parseShort(ids[1]));
            } else if (TypeUtils.isNumeric(b)) {
               query.addBlockFilter(Integer.parseInt(b), (short)0);
            } else {
               MaterialAliases items = Prism.getItems();
               ArrayList itemIds = items.getIdsByAlias(b);
               if (itemIds.size() <= 0) {
                  throw new IllegalArgumentException("Invalid block name '" + b + "'. Try /pr ? for help");
               }

               Iterator i$ = itemIds.iterator();

               while(i$.hasNext()) {
                  int[] ids = (int[])i$.next();
                  if (ids.length == 2) {
                     if (ItemUtils.dataValueUsedForSubitems(ids[0])) {
                        query.addBlockFilter(ids[0], (short)ids[1]);
                     } else {
                        query.addBlockFilter(ids[0], (short)0);
                     }
                  }
               }
            }
         }
      }

   }
}
