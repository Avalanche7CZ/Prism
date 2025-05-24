package me.botsko.prism.actionlibs;

import com.helion3.prism.libs.elixr.TypeUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import me.botsko.prism.Prism;
import me.botsko.prism.exceptions.InvalidActionException;
import org.bukkit.plugin.Plugin;

public class ActionRegistry {
   private final TreeMap registeredActions = new TreeMap();

   public ActionRegistry() {
      this.registerPrismDefaultActions();
   }

   protected void registerAction(ActionType actionType) {
      this.registeredActions.put(actionType.getName(), actionType);
   }

   public void registerCustomAction(Plugin apiPlugin, ActionType actionType) throws InvalidActionException {
      ArrayList allowedPlugins = (ArrayList)Prism.config.getList("prism.tracking.api.allowed-plugins");
      if (!allowedPlugins.contains(apiPlugin.getName())) {
         throw new InvalidActionException("Registering action type not allowed. Plugin '" + apiPlugin.getName() + "' is not in list of allowed plugins.");
      } else if (TypeUtils.subStrOccurences(actionType.getName(), "-") != 2) {
         throw new InvalidActionException("Invalid action type. Custom actions must contain two hyphens.");
      } else {
         Prism.addActionName(actionType.getName());
         this.registeredActions.put(actionType.getName(), actionType);
      }
   }

   public TreeMap getRegisteredAction() {
      return this.registeredActions;
   }

   public ActionType getAction(String name) {
      return (ActionType)this.registeredActions.get(name);
   }

   public ArrayList getActionsByShortname(String name) {
      ArrayList actions = new ArrayList();
      Iterator i$ = this.registeredActions.entrySet().iterator();

      while(true) {
         Map.Entry entry;
         do {
            if (!i$.hasNext()) {
               return actions;
            }

            entry = (Map.Entry)i$.next();
         } while(!((ActionType)entry.getValue()).getFamilyName().equals(name) && !((ActionType)entry.getValue()).getShortName().equals(name) && !((ActionType)entry.getValue()).getName().equals(name));

         actions.add(entry.getValue());
      }
   }

   public String[] listAll() {
      String[] names = new String[this.registeredActions.size()];
      int i = 0;

      for(Iterator i$ = this.registeredActions.entrySet().iterator(); i$.hasNext(); ++i) {
         Map.Entry entry = (Map.Entry)i$.next();
         names[i] = (String)entry.getKey();
      }

      return names;
   }

   public ArrayList listActionsThatAllowRollback() {
      ArrayList names = new ArrayList();
      Iterator i$ = this.registeredActions.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         if (((ActionType)entry.getValue()).canRollback()) {
            names.add(entry.getKey());
         }
      }

      return names;
   }

   public ArrayList listActionsThatAllowRestore() {
      ArrayList names = new ArrayList();
      Iterator i$ = this.registeredActions.entrySet().iterator();

      while(i$.hasNext()) {
         Map.Entry entry = (Map.Entry)i$.next();
         if (((ActionType)entry.getValue()).canRestore()) {
            names.add(entry.getKey());
         }
      }

      return names;
   }

   private void registerPrismDefaultActions() {
      this.registerAction(new ActionType("block-break", false, true, true, "BlockAction", "broke"));
      this.registerAction(new ActionType("block-burn", false, true, true, "BlockAction", "burned"));
      this.registerAction(new ActionType("block-dispense", false, false, false, "ItemStackAction", "dispensed"));
      this.registerAction(new ActionType("block-fade", false, true, true, "BlockChangeAction", "faded"));
      this.registerAction(new ActionType("block-fall", false, true, true, "BlockAction", "fell"));
      this.registerAction(new ActionType("block-form", false, true, true, "BlockChangeAction", "formed"));
      this.registerAction(new ActionType("block-place", true, true, true, "BlockChangeAction", "placed"));
      this.registerAction(new ActionType("block-shift", true, false, false, "BlockShift", "moved"));
      this.registerAction(new ActionType("block-spread", true, true, true, "BlockChangeAction", "grew"));
      this.registerAction(new ActionType("block-use", false, false, false, "BlockAction", "used"));
      this.registerAction(new ActionType("bonemeal-use", false, false, false, "UseAction", "used"));
      this.registerAction(new ActionType("bucket-fill", false, false, false, "PlayerAction", "filled"));
      this.registerAction(new ActionType("cake-eat", false, false, false, "UseAction", "ate"));
      this.registerAction(new ActionType("container-access", false, false, false, "BlockAction", "accessed"));
      this.registerAction(new ActionType("craft-item", false, false, false, "ItemStackAction", "crafted"));
      this.registerAction(new ActionType("creeper-explode", false, true, true, "BlockAction", "blew up"));
      this.registerAction(new ActionType("crop-trample", false, true, true, "BlockAction", "trampled"));
      this.registerAction(new ActionType("dragon-eat", false, true, true, "BlockAction", "ate"));
      this.registerAction(new ActionType("enchant-item", false, false, false, "ItemStackAction", "enchanted"));
      this.registerAction(new ActionType("enderman-pickup", false, true, true, "BlockAction", "picked up"));
      this.registerAction(new ActionType("enderman-place", true, true, true, "BlockAction", "placed"));
      this.registerAction(new ActionType("entity-break", true, true, true, "BlockAction", "broke"));
      this.registerAction(new ActionType("entity-dye", false, false, false, "EntityAction", "dyed"));
      this.registerAction(new ActionType("entity-explode", false, true, true, "BlockAction", "blew up"));
      this.registerAction(new ActionType("entity-follow", false, false, false, "EntityAction", "lured"));
      this.registerAction(new ActionType("entity-form", true, true, true, "BlockChangeAction", "formed"));
      this.registerAction(new ActionType("entity-kill", false, true, false, "EntityAction", "killed"));
      this.registerAction(new ActionType("entity-leash", true, false, false, "EntityAction", "leashed"));
      this.registerAction(new ActionType("entity-shear", false, false, false, "EntityAction", "sheared"));
      this.registerAction(new ActionType("entity-spawn", false, false, false, "EntityAction", "spawned"));
      this.registerAction(new ActionType("entity-unleash", false, false, false, "EntityAction", "unleashed"));
      this.registerAction(new ActionType("fireball", false, false, false, (String)null, "ignited"));
      this.registerAction(new ActionType("fire-spread", true, true, true, "BlockChangeAction", "spread"));
      this.registerAction(new ActionType("firework-launch", false, false, false, "ItemStackAction", "launched"));
      this.registerAction(new ActionType("hangingitem-break", false, true, true, "HangingItemAction", "broke"));
      this.registerAction(new ActionType("hangingitem-place", true, true, true, "HangingItemAction", "hung"));
      this.registerAction(new ActionType("item-drop", false, true, true, "ItemStackAction", "dropped"));
      this.registerAction(new ActionType("item-insert", false, true, true, "ItemStackAction", "inserted"));
      this.registerAction(new ActionType("item-pickup", false, true, true, "ItemStackAction", "picked up"));
      this.registerAction(new ActionType("item-remove", false, true, true, "ItemStackAction", "removed"));
      this.registerAction(new ActionType("item-rotate", false, false, false, "UseAction", "turned item"));
      this.registerAction(new ActionType("lava-break", false, true, true, "BlockAction", "broke"));
      this.registerAction(new ActionType("lava-bucket", true, true, true, "BlockChangeAction", "poured"));
      this.registerAction(new ActionType("lava-flow", true, true, true, "BlockAction", "flowed into"));
      this.registerAction(new ActionType("lava-ignite", false, false, false, (String)null, "ignited"));
      this.registerAction(new ActionType("leaf-decay", false, true, true, "BlockAction", "decayed"));
      this.registerAction(new ActionType("lighter", false, false, false, (String)null, "set a fire"));
      this.registerAction(new ActionType("lightning", false, false, false, (String)null, "ignited"));
      this.registerAction(new ActionType("mushroom-grow", true, true, true, "GrowAction", "grew"));
      this.registerAction(new ActionType("player-chat", false, false, false, "PlayerAction", "said"));
      this.registerAction(new ActionType("player-command", false, false, false, "PlayerAction", "ran command"));
      this.registerAction(new ActionType("player-death", false, false, false, "PlayerDeathAction", "died"));
      this.registerAction(new ActionType("player-join", false, false, false, "PlayerAction", "joined"));
      this.registerAction(new ActionType("player-kill", false, true, false, "EntityAction", "killed"));
      this.registerAction(new ActionType("player-quit", false, false, false, "PlayerAction", "quit"));
      this.registerAction(new ActionType("player-teleport", false, false, false, "EntityTravelAction", "teleported"));
      this.registerAction(new ActionType("potion-splash", false, false, false, "PlayerAction", "threw potion"));
      this.registerAction(new ActionType("prism-drain", false, true, true, "PrismRollbackAction", "drained"));
      this.registerAction(new ActionType("prism-extinguish", false, true, true, "PrismRollbackAction", "extinguished"));
      this.registerAction(new ActionType("prism-process", false, false, false, "PrismProcessAction", "ran process"));
      this.registerAction(new ActionType("prism-rollback", true, false, false, "PrismRollbackAction", "rolled back"));
      this.registerAction(new ActionType("sheep-eat", false, false, false, "BlockAction", "ate"));
      this.registerAction(new ActionType("sign-change", false, false, true, "SignAction", "wrote"));
      this.registerAction(new ActionType("spawnegg-use", false, false, false, "UseAction", "used"));
      this.registerAction(new ActionType("tnt-explode", false, true, true, "BlockAction", "blew up"));
      this.registerAction(new ActionType("tnt-prime", false, false, false, "UseAction", "primed"));
      this.registerAction(new ActionType("tree-grow", true, true, true, "GrowAction", "grew"));
      this.registerAction(new ActionType("vehicle-break", false, true, false, "VehicleAction", "broke"));
      this.registerAction(new ActionType("vehicle-enter", false, false, false, "VehicleAction", "entered"));
      this.registerAction(new ActionType("vehicle-exit", false, false, false, "VehicleAction", "exited"));
      this.registerAction(new ActionType("vehicle-place", true, false, false, "VehicleAction", "placed"));
      this.registerAction(new ActionType("water-break", false, true, true, "BlockAction", "broke"));
      this.registerAction(new ActionType("water-bucket", true, true, true, "BlockChangeAction", "poured"));
      this.registerAction(new ActionType("water-flow", true, true, true, "BlockAction", "flowed into"));
      this.registerAction(new ActionType("world-edit", true, true, true, "BlockChangeAction", "edited"));
      this.registerAction(new ActionType("xp-pickup", false, false, false, "PlayerAction", "picked up"));
   }
}
