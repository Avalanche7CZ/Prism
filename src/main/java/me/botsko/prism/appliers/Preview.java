package me.botsko.prism.appliers;

import com.helion3.prism.libs.elixr.EntityUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.events.BlockStateChange;
import me.botsko.prism.events.PrismBlocksRollbackEvent;
import me.botsko.prism.wands.RollbackWand;
import me.botsko.prism.wands.Wand;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Preview implements Previewable {
   protected final Prism plugin;
   protected final PrismProcessType processType;
   protected final CommandSender sender;
   protected final Player player;
   protected final QueryParameters parameters;
   protected boolean is_preview = false;
   protected final HashMap entities_moved = new HashMap();
   protected final ArrayList blockStateChanges = new ArrayList();
   protected int skipped_block_count;
   protected int changes_applied_count;
   protected int changes_planned_count;
   protected int blockChangesRead = 0;
   protected final List worldChangeQueue = Collections.synchronizedList(new LinkedList());
   protected int worldChangeQueueTaskId;
   protected ApplierCallback callback;

   public Preview(Prism plugin, CommandSender sender, List results, QueryParameters parameters, ApplierCallback callback) {
      this.processType = parameters.getProcessType();
      this.plugin = plugin;
      this.sender = sender;
      this.parameters = parameters;
      if (sender instanceof Player) {
         this.player = (Player)sender;
      } else {
         this.player = null;
      }

      if (callback != null) {
         this.callback = callback;
      }

      this.worldChangeQueue.addAll(results);
   }

   public void setIsPreview(boolean is_preview) {
      this.is_preview = is_preview;
   }

   public void cancel_preview() {
      if (this.player != null) {
         if (!this.blockStateChanges.isEmpty()) {
            ArrayList previewPlayers = this.parameters.getSharedPlayers();
            previewPlayers.add(this.player);
            Iterator i$ = this.blockStateChanges.iterator();

            while(i$.hasNext()) {
               BlockStateChange u = (BlockStateChange)i$.next();
               Iterator i$ = previewPlayers.iterator();

               while(i$.hasNext()) {
                  CommandSender sharedPlayer = (CommandSender)i$.next();
                  if (sharedPlayer instanceof Player) {
                     ((Player)sharedPlayer).sendBlockChange(u.getOriginalBlock().getLocation(), u.getOriginalBlock().getTypeId(), u.getOriginalBlock().getRawData());
                  }
               }
            }
         }

         this.sender.sendMessage(Prism.messenger.playerHeaderMsg("Preview canceled." + ChatColor.GRAY + " Please come again!"));
      }
   }

   public void apply_preview() {
      if (this.player != null) {
         this.sender.sendMessage(Prism.messenger.playerHeaderMsg("Applying rollback from preview..."));
         this.setIsPreview(false);
         this.changes_applied_count = 0;
         this.skipped_block_count = 0;
         this.changes_planned_count = 0;
         this.apply();
      }
   }

   public void preview() {
   }

   public void apply() {
      if (!this.worldChangeQueue.isEmpty()) {
         if (!this.is_preview && this.player != null) {
            Wand oldwand = null;
            if (Prism.playersWithActiveTools.containsKey(this.player.getName())) {
               oldwand = (Wand)Prism.playersWithActiveTools.get(this.player.getName());
            }

            boolean show_nearby = true;
            if (oldwand != null && oldwand instanceof RollbackWand) {
               show_nearby = false;
            }

            if (show_nearby) {
               this.plugin.notifyNearby(this.player, this.parameters.getRadius(), this.player.getDisplayName() + " is performing a " + this.processType.name().toLowerCase() + " near you.");
               if (this.plugin.getConfig().getBoolean("prism.alerts.alert-staff-to-applied-process")) {
                  String cmd = this.parameters.getOriginalCommand();
                  if (cmd != null) {
                     this.plugin.alertPlayers(this.player, ChatColor.WHITE + this.processType.name().toLowerCase() + " by " + this.player.getDisplayName() + ChatColor.GRAY + this.parameters.getOriginalCommand());
                  }
               }
            }
         }

         this.processWorldChanges();
      }

   }

   public void processWorldChanges() {
      this.blockChangesRead = 0;
      this.worldChangeQueueTaskId = this.plugin.getServer().getScheduler().scheduleSyncRepeatingTask(this.plugin, new Runnable() {
         public void run() {
            if (Preview.this.plugin.getConfig().getBoolean("prism.debug")) {
               Prism.debug("World change queue size: " + Preview.this.worldChangeQueue.size());
            }

            if (Preview.this.worldChangeQueue.isEmpty()) {
               Preview.this.sender.sendMessage(Prism.messenger.playerError(ChatColor.GRAY + "No actions found that match the criteria."));
            } else {
               int iterationCount = 0;
               int currentQueueOffset = Preview.this.blockChangesRead;
               if (currentQueueOffset < Preview.this.worldChangeQueue.size()) {
                  Iterator iterator = Preview.this.worldChangeQueue.listIterator(currentQueueOffset);

                  label93:
                  while(true) {
                     while(true) {
                        if (!iterator.hasNext()) {
                           break label93;
                        }

                        Handler a = (Handler)iterator.next();
                        if (Preview.this.is_preview) {
                           ++Preview.this.blockChangesRead;
                        }

                        ++iterationCount;
                        if (iterationCount >= 1000) {
                           break label93;
                        }

                        if (Preview.this.processType.equals(PrismProcessType.ROLLBACK) && !a.getType().canRollback()) {
                           iterator.remove();
                        } else if (Preview.this.processType.equals(PrismProcessType.RESTORE) && !a.getType().canRestore()) {
                           iterator.remove();
                        } else {
                           ChangeResult result = null;

                           try {
                              if (Preview.this.processType.equals(PrismProcessType.ROLLBACK)) {
                                 result = a.applyRollback(Preview.this.player, Preview.this.parameters, Preview.this.is_preview);
                              }

                              if (Preview.this.processType.equals(PrismProcessType.RESTORE)) {
                                 result = a.applyRestore(Preview.this.player, Preview.this.parameters, Preview.this.is_preview);
                              }

                              if (Preview.this.processType.equals(PrismProcessType.UNDO)) {
                                 result = a.applyUndo(Preview.this.player, Preview.this.parameters, Preview.this.is_preview);
                              }

                              if (result == null) {
                                 iterator.remove();
                              } else if (result.getType() == null) {
                                 ++Preview.this.skipped_block_count;
                                 iterator.remove();
                              } else if (result.getType().equals(ChangeResultType.SKIPPED)) {
                                 ++Preview.this.skipped_block_count;
                                 iterator.remove();
                              } else if (result.getType().equals(ChangeResultType.PLANNED)) {
                                 ++Preview.this.changes_planned_count;
                              } else {
                                 Preview.this.blockStateChanges.add(result.getBlockStateChange());
                                 ++Preview.this.changes_applied_count;
                                 if (!Preview.this.is_preview) {
                                    iterator.remove();
                                 }
                              }
                           } catch (Exception var7) {
                              Prism.log("Applier error: " + var7.getMessage());
                              var7.printStackTrace();
                              ++Preview.this.skipped_block_count;
                              iterator.remove();
                           }
                        }
                     }
                  }
               }

               if (Preview.this.worldChangeQueue.isEmpty() || Preview.this.blockChangesRead >= Preview.this.worldChangeQueue.size()) {
                  Preview.this.plugin.getServer().getScheduler().cancelTask(Preview.this.worldChangeQueueTaskId);
                  if (Preview.this.is_preview) {
                     Preview.this.postProcessPreview();
                  } else {
                     Preview.this.postProcess();
                  }
               }

            }
         }
      }, 2L, 2L);
   }

   public void postProcessPreview() {
      if (this.is_preview && (this.changes_applied_count > 0 || this.changes_planned_count > 0)) {
         PreviewSession ps = new PreviewSession(this.player, this);
         this.plugin.playerActivePreviews.put(this.player.getName(), ps);
         this.moveEntitiesToSafety();
      }

      this.fireApplierCallback();
   }

   public void postProcess() {
      if (this.processType.equals(PrismProcessType.ROLLBACK)) {
      }

      this.moveEntitiesToSafety();
      this.fireApplierCallback();
   }

   protected void moveEntitiesToSafety() {
      if (this.parameters.getWorld() != null && this.player != null) {
         List entities = this.player.getNearbyEntities((double)this.parameters.getRadius(), (double)this.parameters.getRadius(), (double)this.parameters.getRadius());
         entities.add(this.player);
         Iterator i$ = entities.iterator();

         while(true) {
            Entity entity;
            int add;
            do {
               do {
                  if (!i$.hasNext()) {
                     return;
                  }

                  entity = (Entity)i$.next();
               } while(!(entity instanceof LivingEntity));

               add = 0;
            } while(!EntityUtils.inCube(this.parameters.getPlayerLocation(), this.parameters.getRadius(), entity.getLocation()));

            Location l = entity.getLocation();

            while(!EntityUtils.playerMayPassThrough(l.getBlock().getType())) {
               ++add;
               if (l.getY() >= 256.0) {
                  break;
               }

               l.setY(l.getY() + 1.0);
            }

            if (add > 0) {
               this.entities_moved.put(entity, add);
               entity.teleport(l);
            }
         }
      }
   }

   public void fireApplierCallback() {
      if (this.is_preview) {
         this.changes_planned_count += this.changes_applied_count;
         this.changes_applied_count = 0;
      }

      ApplierResult results = new ApplierResult(this.is_preview, this.changes_applied_count, this.skipped_block_count, this.changes_planned_count, this.blockStateChanges, this.parameters, this.entities_moved);
      if (this.callback != null) {
         this.callback.handle(this.sender, results);
      }

      if (this.processType.equals(PrismProcessType.ROLLBACK)) {
         PrismBlocksRollbackEvent event = new PrismBlocksRollbackEvent(this.blockStateChanges, this.player, this.parameters, results);
         this.plugin.getServer().getPluginManager().callEvent(event);
      }

      this.plugin.eventTimer.recordTimedEvent("applier function complete");
      if (this.plugin.getConfig().getBoolean("prism.debug")) {
         this.plugin.eventTimer.printTimeRecord();
         Prism.debug("Changes: " + this.changes_applied_count);
         Prism.debug("Planned: " + this.changes_planned_count);
         Prism.debug("Skipped: " + this.skipped_block_count);
      }

   }
}
