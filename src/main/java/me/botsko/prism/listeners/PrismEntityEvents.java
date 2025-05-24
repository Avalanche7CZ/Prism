package me.botsko.prism.listeners;

import com.helion3.prism.libs.elixr.DeathUtils;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.ActionFactory;
import me.botsko.prism.actionlibs.RecordingQueue;
import me.botsko.prism.utils.BlockUtils;
import me.botsko.prism.utils.MiscUtils;
import me.botsko.prism.utils.WandUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Horse;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.minecart.PoweredMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityUnleashEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;

public class PrismEntityEvents implements Listener {
   private final Prism plugin;

   public PrismEntityEvents(Prism plugin) {
      this.plugin = plugin;
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityDamageEvent(EntityDamageByEntityEvent event) {
      if (event.getDamager() instanceof Player) {
         Entity entity = event.getEntity();
         Player player = (Player)event.getDamager();
         if (WandUtils.playerUsesWandOnClick(player, entity.getLocation())) {
            event.setCancelled(true);
         } else {
            if (entity instanceof ItemFrame) {
               ItemFrame frame = (ItemFrame)event.getEntity();
               if (!frame.getItem().getType().equals(Material.AIR) && Prism.getIgnore().event("item-remove", player)) {
                  RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(), 1, 0, (Map)null, entity.getLocation(), player.getName()));
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityDeath(EntityDeathEvent event) {
      Entity entity = event.getEntity();
      String name;
      if (!(entity instanceof Player)) {
         if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            if (entity instanceof Horse) {
               Horse horse = (Horse)entity;
               if (horse.isCarryingChest() && Prism.getIgnore().event("item-drop", entity.getWorld())) {
                  ItemStack[] arr$ = horse.getInventory().getContents();
                  int len$ = arr$.length;

                  for(int i$ = 0; i$ < len$; ++i$) {
                     ItemStack i = arr$[i$];
                     if (i != null) {
                        RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", i, i.getAmount(), -1, (Map)null, entity.getLocation(), "horse"));
                     }
                  }
               }
            }

            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent)entity.getLastDamageCause();
            if (entityDamageByEntityEvent.getDamager() instanceof Player) {
               Player player = (Player)entityDamageByEntityEvent.getDamager();
               if (!Prism.getIgnore().event("player-kill", player)) {
                  return;
               }

               RecordingQueue.addToQueue(ActionFactory.createEntity("player-kill", entity, player.getName()));
            } else if (entityDamageByEntityEvent.getDamager() instanceof Arrow) {
               Arrow arrow = (Arrow)entityDamageByEntityEvent.getDamager();
               if (arrow.getShooter() instanceof Player) {
                  Player player = (Player)arrow.getShooter();
                  if (!Prism.getIgnore().event("player-kill", player)) {
                     return;
                  }

                  RecordingQueue.addToQueue(ActionFactory.createEntity("player-kill", entity, player.getName()));
               }
            } else {
               Entity damager = entityDamageByEntityEvent.getDamager();
               name = "unknown";
               if (damager != null) {
                  name = damager.getType().getName();
               }

               if (name == null) {
                  name = "unknown";
               }

               if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) {
                  return;
               }

               RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity, name));
            }
         } else {
            if (!Prism.getIgnore().event("entity-kill", entity.getWorld())) {
               return;
            }

            String killer = "unknown";
            EntityDamageEvent damage = entity.getLastDamageCause();
            if (damage != null) {
               EntityDamageEvent.DamageCause cause = damage.getCause();
               if (cause != null) {
                  killer = cause.name().toLowerCase();
               }
            }

            RecordingQueue.addToQueue(ActionFactory.createEntity("entity-kill", entity, killer));
         }
      } else {
         Player p = (Player)event.getEntity();
         if (Prism.getIgnore().event("player-death", p)) {
            String cause = DeathUtils.getCauseNiceName(p);
            name = DeathUtils.getAttackerName(p);
            if (name.equals("pvpwolf")) {
               String owner = DeathUtils.getTameWolfOwner(event);
               name = owner + "'s wolf";
            }

            RecordingQueue.addToQueue(ActionFactory.createPlayerDeath("player-death", p, cause, name));
         }

         if (Prism.getIgnore().event("item-drop", p) && !event.getDrops().isEmpty()) {
            Iterator i$ = event.getDrops().iterator();

            while(i$.hasNext()) {
               ItemStack i = (ItemStack)i$.next();
               RecordingQueue.addToQueue(ActionFactory.createItemStack("item-drop", i, i.getAmount(), -1, (Map)null, p.getLocation(), p.getName()));
            }
         }
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onCreatureSpawn(CreatureSpawnEvent event) {
      if (Prism.getIgnore().event("entity-spawn", event.getEntity().getWorld())) {
         String reason = event.getSpawnReason().name().toLowerCase().replace("_", " ");
         if (!reason.equals("natural")) {
            RecordingQueue.addToQueue(ActionFactory.createEntity("entity-spawn", event.getEntity(), reason));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityTargetEvent(EntityTargetEvent event) {
      if (Prism.getIgnore().event("entity-follow", event.getEntity().getWorld())) {
         if (event.getTarget() instanceof Player && event.getEntity().getType().equals(EntityType.CREEPER)) {
            Player player = (Player)event.getTarget();
            RecordingQueue.addToQueue(ActionFactory.createEntity("entity-follow", event.getEntity(), player.getName()));
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerShearEntity(PlayerShearEntityEvent event) {
      if (Prism.getIgnore().event("entity-shear", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createEntity("entity-shear", event.getEntity(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
      Player p = event.getPlayer();
      Entity e = event.getRightClicked();
      if (WandUtils.playerUsesWandOnClick(p, e.getLocation())) {
         event.setCancelled(true);
      } else {
         if (e instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame)e;
            if (!frame.getItem().getType().equals(Material.AIR)) {
               RecordingQueue.addToQueue(ActionFactory.createPlayer("item-rotate", event.getPlayer(), frame.getRotation().name().toLowerCase()));
            }

            if (frame.getItem().getType().equals(Material.AIR) && p.getItemInHand() != null && Prism.getIgnore().event("item-insert", p)) {
               RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", p.getItemInHand(), 1, 0, (Map)null, e.getLocation(), p.getName()));
            }
         }

         if (p.getItemInHand().getType().equals(Material.COAL) && e instanceof PoweredMinecart) {
            if (!Prism.getIgnore().event("item-insert", p)) {
               return;
            }

            RecordingQueue.addToQueue(ActionFactory.createItemStack("item-insert", p.getItemInHand(), 1, 0, (Map)null, e.getLocation(), p.getName()));
         }

         if (Prism.getIgnore().event("entity-dye", p)) {
            if (p.getItemInHand().getTypeId() == 351 && e.getType().equals(EntityType.SHEEP)) {
               String newColor = Prism.getItems().getAlias(p.getItemInHand().getTypeId(), (byte)p.getItemInHand().getDurability());
               RecordingQueue.addToQueue(ActionFactory.createEntity("entity-dye", event.getRightClicked(), event.getPlayer().getName(), newColor));
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityBreakDoor(EntityBreakDoorEvent event) {
      if (Prism.getIgnore().event("entity-break", event.getEntity().getWorld())) {
         RecordingQueue.addToQueue(ActionFactory.createBlock("entity-break", event.getBlock(), event.getEntityType().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerEntityLeash(PlayerLeashEntityEvent event) {
      if (Prism.getIgnore().event("entity-leash", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createEntity("entity-leash", event.getEntity(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPlayerEntityUnleash(PlayerUnleashEntityEvent event) {
      if (Prism.getIgnore().event("entity-unleash", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createEntity("entity-unleash", event.getEntity(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityUnleash(EntityUnleashEvent event) {
      if (Prism.getIgnore().event("entity-unleash")) {
         RecordingQueue.addToQueue(ActionFactory.createEntity("entity-unleash", event.getEntity(), event.getReason().toString().toLowerCase()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onPotionSplashEvent(PotionSplashEvent event) {
      ProjectileSource source = event.getPotion().getShooter();
      if (source instanceof Player) {
         Player player = (Player)source;
         if (Prism.getIgnore().event("potion-splash", player)) {
            Collection potion = event.getPotion().getEffects();
            String name = "";

            PotionEffect eff;
            for(Iterator i$ = potion.iterator(); i$.hasNext(); name = eff.getType().getName().toLowerCase()) {
               eff = (PotionEffect)i$.next();
            }

            RecordingQueue.addToQueue(ActionFactory.createPlayer("potion-splash", player, name));
         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onHangingPlaceEvent(HangingPlaceEvent event) {
      if (WandUtils.playerUsesWandOnClick(event.getPlayer(), event.getEntity().getLocation())) {
         event.setCancelled(true);
      } else if (Prism.getIgnore().event("hangingitem-place", event.getPlayer())) {
         RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-place", event.getEntity(), event.getPlayer().getName()));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onHangingBreakEvent(HangingBreakEvent event) {
      if (event.getCause().equals(RemoveCause.PHYSICS)) {
         if (Prism.getIgnore().event("hangingitem-break", event.getEntity().getWorld())) {
            Hanging e = event.getEntity();
            String coord_key = e.getLocation().getBlockX() + ":" + e.getLocation().getBlockY() + ":" + e.getLocation().getBlockZ();
            if (this.plugin.preplannedBlockFalls.containsKey(coord_key)) {
               String player = (String)this.plugin.preplannedBlockFalls.get(coord_key);
               RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-break", e, player));
               this.plugin.preplannedBlockFalls.remove(coord_key);
               if (!Prism.getIgnore().event("item-remove", event.getEntity().getWorld())) {
                  return;
               }

               if (e instanceof ItemFrame) {
                  ItemFrame frame = (ItemFrame)e;
                  if (frame.getItem() != null) {
                     RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(), frame.getItem().getAmount(), -1, (Map)null, e.getLocation(), player));
                  }
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {
      Entity entity = event.getEntity();
      Entity remover = event.getRemover();
      Player player = null;
      if (remover instanceof Player) {
         player = (Player)remover;
      }

      if (player != null && WandUtils.playerUsesWandOnClick(player, event.getEntity().getLocation())) {
         event.setCancelled(true);
      } else if (Prism.getIgnore().event("hangingitem-break", event.getEntity().getWorld())) {
         String breaking_name = remover.getType().getName();
         if (player != null) {
            breaking_name = player.getName();
         }

         RecordingQueue.addToQueue(ActionFactory.createHangingItem("hangingitem-break", event.getEntity(), breaking_name));
         if (Prism.getIgnore().event("item-remove", event.getEntity().getWorld())) {
            if (event.getEntity() instanceof ItemFrame) {
               ItemFrame frame = (ItemFrame)event.getEntity();
               if (frame.getItem() != null) {
                  RecordingQueue.addToQueue(ActionFactory.createItemStack("item-remove", frame.getItem(), frame.getItem().getAmount(), -1, (Map)null, entity.getLocation(), breaking_name));
               }
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityChangeBlock(EntityChangeBlockEvent event) {
      String entity = MiscUtils.getEntityName(event.getEntity());
      Material to = event.getTo();
      Material from = event.getBlock().getType();
      if (from == Material.GRASS && to == Material.DIRT) {
         if (event.getEntityType() != EntityType.SHEEP) {
            return;
         }

         if (!Prism.getIgnore().event("sheep-eat", event.getBlock())) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createBlock("sheep-eat", event.getBlock(), entity));
      } else if (to == Material.AIR ^ from == Material.AIR && event.getEntity() instanceof Enderman) {
         if (from == Material.AIR) {
            if (!Prism.getIgnore().event("enderman-place", event.getBlock())) {
               return;
            }

            BlockState state = event.getBlock().getState();
            state.setType(to);
            RecordingQueue.addToQueue(ActionFactory.createBlock("enderman-place", state, entity));
         } else {
            if (!Prism.getIgnore().event("enderman-pickup", event.getBlock())) {
               return;
            }

            Enderman enderman = (Enderman)event.getEntity();
            if (enderman.getCarriedMaterial() != null) {
               BlockState state = event.getBlock().getState();
               state.setData(enderman.getCarriedMaterial());
               RecordingQueue.addToQueue(ActionFactory.createBlock("enderman-pickup", state, entity));
            }
         }
      } else if (to == Material.AIR && event.getEntity() instanceof Wither) {
         if (!Prism.getIgnore().event("entity-break", event.getBlock())) {
            return;
         }

         RecordingQueue.addToQueue(ActionFactory.createBlock("block-break", event.getBlock(), event.getEntityType().getName()));
      }

   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityBlockForm(EntityBlockFormEvent event) {
      if (Prism.getIgnore().event("entity-form", event.getBlock())) {
         Block block = event.getBlock();
         Location loc = block.getLocation();
         BlockState newState = event.getNewState();
         String entity = event.getEntity().getType().name().toLowerCase();
         RecordingQueue.addToQueue(ActionFactory.createBlockChange("entity-form", loc, block.getTypeId(), block.getData(), newState.getTypeId(), newState.getRawData(), entity));
      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR,
      ignoreCancelled = true
   )
   public void onEntityExplodeChangeBlock(EntityExplodeEvent event) {
      if (event.blockList() != null && !event.blockList().isEmpty()) {
         String action = "entity-explode";
         String name;
         if (event.getEntity() != null) {
            if (event.getEntity() instanceof Creeper) {
               if (!Prism.getIgnore().event("creeper-explode", event.getEntity().getWorld())) {
                  return;
               }

               action = "creeper-explode";
               name = "creeper";
            } else if (event.getEntity() instanceof TNTPrimed) {
               if (!Prism.getIgnore().event("tnt-explode", event.getEntity().getWorld())) {
                  return;
               }

               action = "tnt-explode";
               Entity source = ((TNTPrimed)event.getEntity()).getSource();
               name = this.followTNTTrail(source);
            } else if (event.getEntity() instanceof EnderDragon) {
               if (!Prism.getIgnore().event("dragon-eat", event.getEntity().getWorld())) {
                  return;
               }

               action = "dragon-eat";
               name = "enderdragon";
            } else {
               if (!Prism.getIgnore().event("entity-explode", event.getLocation().getWorld())) {
                  return;
               }

               try {
                  name = event.getEntity().getType().getName().replace("_", " ");
                  name = name.length() > 15 ? name.substring(0, 15) : name;
               } catch (NullPointerException var8) {
                  name = "unknown";
               }
            }
         } else {
            if (!Prism.getIgnore().event("entity-explode", event.getLocation().getWorld())) {
               return;
            }

            name = "magic";
         }

         PrismBlockEvents be = new PrismBlockEvents(this.plugin);
         Iterator i$ = event.blockList().iterator();

         while(true) {
            Block block;
            do {
               if (!i$.hasNext()) {
                  return;
               }

               block = (Block)i$.next();
            } while(BlockUtils.isDoor(block.getType()) && block.getData() >= 4);

            Block sibling = com.helion3.prism.libs.elixr.BlockUtils.getSiblingForDoubleLengthBlock(block);
            if (sibling != null && !block.getType().equals(Material.CHEST) && !block.getType().equals(Material.TRAPPED_CHEST)) {
               block = sibling;
            }

            be.logItemRemoveFromDestroyedContainer(name, block);
            RecordingQueue.addToQueue(ActionFactory.createBlock(action, block, name));
            be.logBlockRelationshipsForBlock(name, block);
         }
      }
   }

   private String followTNTTrail(Entity initial) {
      for(int counter = 10000000; initial != null; --counter) {
         if (initial instanceof Player) {
            return ((Player)initial).getName();
         }

         if (!(initial instanceof TNTPrimed)) {
            return initial.getType().name();
         }

         initial = ((TNTPrimed)initial).getSource();
         if (counter < 0) {
            Location last = initial.getLocation();
            this.plugin.getLogger().warning("TnT chain has exceeded one million, will not continue!");
            this.plugin.getLogger().warning("Last Tnt was at " + last.getX() + ", " + last.getY() + ". " + last.getZ() + " in world " + last.getWorld());
            return "tnt";
         }
      }

      return "tnt";
   }
}
