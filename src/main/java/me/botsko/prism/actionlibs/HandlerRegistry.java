package me.botsko.prism.actionlibs;

import java.util.ArrayList;
import java.util.HashMap;
import me.botsko.prism.Prism;
import me.botsko.prism.actions.BlockAction;
import me.botsko.prism.actions.BlockChangeAction;
import me.botsko.prism.actions.BlockShiftAction;
import me.botsko.prism.actions.EntityAction;
import me.botsko.prism.actions.EntityTravelAction;
import me.botsko.prism.actions.GenericAction;
import me.botsko.prism.actions.GrowAction;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.actions.HangingItemAction;
import me.botsko.prism.actions.ItemStackAction;
import me.botsko.prism.actions.PlayerAction;
import me.botsko.prism.actions.PlayerDeathAction;
import me.botsko.prism.actions.PrismProcessAction;
import me.botsko.prism.actions.PrismRollbackAction;
import me.botsko.prism.actions.SignAction;
import me.botsko.prism.actions.UseAction;
import me.botsko.prism.actions.VehicleAction;
import me.botsko.prism.exceptions.InvalidActionException;
import org.bukkit.plugin.Plugin;

public class HandlerRegistry {
   private final HashMap registeredHandlers = new HashMap();

   public HandlerRegistry() {
      this.registerPrismDefaultHandlers();
   }

   protected void registerHandler(Class handlerClass) {
      String[] names = handlerClass.getName().split("\\.");
      if (names.length > 0) {
         this.registeredHandlers.put(names[names.length - 1], handlerClass);
      }

   }

   public void registerCustomHandler(Plugin apiPlugin, Class handlerClass) throws InvalidActionException {
      ArrayList allowedPlugins = (ArrayList)Prism.config.getList("prism.tracking.api.allowed-plugins");
      if (!allowedPlugins.contains(apiPlugin.getName())) {
         throw new InvalidActionException("Registering action type not allowed. Plugin '" + apiPlugin.getName() + "' is not in list of allowed plugins.");
      } else {
         String[] names = handlerClass.getName().split("\\.");
         if (names.length > 0) {
            this.registeredHandlers.put(names[names.length - 1], handlerClass);
         }

      }
   }

   public Handler getHandler(String name) {
      if (name != null && this.registeredHandlers.containsKey(name)) {
         try {
            Class handlerClass = (Class)this.registeredHandlers.get(name);
            return (new HandlerFactory(handlerClass)).create();
         } catch (InstantiationException var3) {
            var3.printStackTrace();
         } catch (IllegalAccessException var4) {
            var4.printStackTrace();
         }
      }

      return new GenericAction();
   }

   private void registerPrismDefaultHandlers() {
      this.registerHandler(GenericAction.class);
      this.registerHandler(BlockAction.class);
      this.registerHandler(BlockChangeAction.class);
      this.registerHandler(BlockShiftAction.class);
      this.registerHandler(EntityAction.class);
      this.registerHandler(EntityTravelAction.class);
      this.registerHandler(GrowAction.class);
      this.registerHandler(HangingItemAction.class);
      this.registerHandler(ItemStackAction.class);
      this.registerHandler(PlayerAction.class);
      this.registerHandler(PlayerDeathAction.class);
      this.registerHandler(PrismProcessAction.class);
      this.registerHandler(PrismRollbackAction.class);
      this.registerHandler(SignAction.class);
      this.registerHandler(UseAction.class);
      this.registerHandler(VehicleAction.class);
   }
}
