package me.botsko.prism.actions;

import com.helion3.prism.libs.elixr.InventoryUtils;
import com.helion3.prism.libs.elixr.ItemUtils;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.appliers.PrismProcessType;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class ItemStackAction extends GenericAction {
   protected ItemStack item;
   protected ItemStackActionData actionData;
   protected Map enchantments;

   public void setItem(ItemStack item, int quantity, int slot, Map enchantments) {
      this.actionData = new ItemStackActionData();
      if (enchantments != null) {
         this.enchantments = enchantments;
      }

      if (item != null && item.getAmount() > 0) {
         this.item = item;
         if (enchantments == null) {
            this.enchantments = item.getEnchantments();
         }

         this.block_id = item.getTypeId();
         this.block_subid = item.getDurability();
         this.actionData.amt = quantity;
         if (slot >= 0) {
            this.actionData.slot = slot;
         }

         ItemMeta meta = item.getItemMeta();
         if (meta != null && meta.getDisplayName() != null) {
            this.actionData.name = meta.getDisplayName();
         }

         if (meta != null && item.getType().name().contains("LEATHER_")) {
            LeatherArmorMeta lam = (LeatherArmorMeta)meta;
            if (lam.getColor() != null) {
               this.actionData.color = lam.getColor().asRGB();
            }
         } else if (meta != null && item.getType().equals(Material.SKULL_ITEM)) {
            SkullMeta skull = (SkullMeta)meta;
            if (skull.hasOwner()) {
               this.actionData.owner = skull.getOwner();
            }
         }

         if (meta != null && meta instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta)meta;
            this.actionData.by = bookMeta.getAuthor();
            this.actionData.title = bookMeta.getTitle();
            this.actionData.content = (String[])bookMeta.getPages().toArray(new String[0]);
         }

         if (meta != null && meta.getLore() != null) {
            this.actionData.lore = (String[])meta.getLore().toArray(new String[0]);
         }

         if (!this.enchantments.isEmpty()) {
            String[] enchs = new String[this.enchantments.size()];
            int i = 0;

            for(Iterator i$ = this.enchantments.entrySet().iterator(); i$.hasNext(); ++i) {
               Map.Entry ench = (Map.Entry)i$.next();
               enchs[i] = ((Enchantment)ench.getKey()).getId() + ":" + ench.getValue();
            }

            this.actionData.enchs = enchs;
         } else if (meta != null && item.getType().equals(Material.ENCHANTED_BOOK)) {
            EnchantmentStorageMeta bookEnchantments = (EnchantmentStorageMeta)meta;
            if (bookEnchantments.hasStoredEnchants() && bookEnchantments.getStoredEnchants().size() > 0) {
               String[] enchs = new String[bookEnchantments.getStoredEnchants().size()];
               int i = 0;

               for(Iterator i$ = bookEnchantments.getStoredEnchants().entrySet().iterator(); i$.hasNext(); ++i) {
                  Map.Entry ench = (Map.Entry)i$.next();
                  enchs[i] = ((Enchantment)ench.getKey()).getId() + ":" + ench.getValue();
               }

               this.actionData.enchs = enchs;
            }
         }

         if (meta != null && this.block_id == 402) {
            FireworkEffectMeta fireworkMeta = (FireworkEffectMeta)meta;
            if (fireworkMeta.hasEffect()) {
               FireworkEffect effect = fireworkMeta.getEffect();
               Color fadeColor;
               int[] fadeColors;
               Iterator i$;
               if (!effect.getColors().isEmpty()) {
                  fadeColors = new int[effect.getColors().size()];
                  int i = 0;

                  for(i$ = effect.getColors().iterator(); i$.hasNext(); ++i) {
                     fadeColor = (Color)i$.next();
                     fadeColors[i] = fadeColor.asRGB();
                  }

                  this.actionData.effectColors = fadeColors;
               }

               if (!effect.getFadeColors().isEmpty()) {
                  fadeColors = new int[effect.getColors().size()];
                  int i = false;

                  for(i$ = effect.getFadeColors().iterator(); i$.hasNext(); fadeColors[0] = fadeColor.asRGB()) {
                     fadeColor = (Color)i$.next();
                  }

                  this.actionData.fadeColors = fadeColors;
               }

               if (effect.hasFlicker()) {
                  this.actionData.hasFlicker = true;
               }

               if (effect.hasTrail()) {
                  this.actionData.hasTrail = true;
               }
            }
         }

      } else {
         this.setCanceled(true);
      }
   }

   public void setData(String data) {
      this.data = data;
      this.setItemStackFromData();
   }

   protected void setItemStackFromData() {
      if (this.item == null && this.data != null) {
         this.setItemStackFromNewDataFormat();
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   protected void setItemStackFromNewDataFormat() {
      if (this.data != null && this.data.startsWith("{")) {
         this.actionData = (ItemStackActionData)this.gson.fromJson(this.data, ItemStackActionData.class);
         this.item = new ItemStack(this.block_id, this.actionData.amt, (short)this.block_subid);
         int i;
         if (this.actionData.enchs != null && this.actionData.enchs.length > 0) {
            String[] arr$ = this.actionData.enchs;
            int len$ = arr$.length;

            for(i = 0; i < len$; ++i) {
               String ench = arr$[i];
               String[] enchArgs = ench.split(":");
               Enchantment enchantment = Enchantment.getById(Integer.parseInt(enchArgs[0]));
               if (this.item.getType().equals(Material.ENCHANTED_BOOK)) {
                  EnchantmentStorageMeta bookEnchantments = (EnchantmentStorageMeta)this.item.getItemMeta();
                  bookEnchantments.addStoredEnchant(enchantment, Integer.parseInt(enchArgs[1]), false);
                  this.item.setItemMeta(bookEnchantments);
               } else {
                  this.item.addUnsafeEnchantment(enchantment, Integer.parseInt(enchArgs[1]));
               }
            }
         }

         if (this.item.getType().name().contains("LEATHER_") && this.actionData.color > 0) {
            LeatherArmorMeta lam = (LeatherArmorMeta)this.item.getItemMeta();
            lam.setColor(Color.fromRGB(this.actionData.color));
            this.item.setItemMeta(lam);
         } else if (this.item.getType().equals(Material.SKULL_ITEM) && this.actionData.owner != null) {
            SkullMeta meta = (SkullMeta)this.item.getItemMeta();
            meta.setOwner(this.actionData.owner);
            this.item.setItemMeta(meta);
         } else if (this.item.getItemMeta() instanceof BookMeta) {
            BookMeta bookMeta = (BookMeta)this.item.getItemMeta();
            bookMeta.setAuthor(this.actionData.by);
            bookMeta.setTitle(this.actionData.title);
            bookMeta.setPages(this.actionData.content);
            this.item.setItemMeta(bookMeta);
         }

         if (this.block_id == 402 && this.actionData.effectColors != null && this.actionData.effectColors.length > 0) {
            FireworkEffectMeta fireworkMeta = (FireworkEffectMeta)this.item.getItemMeta();
            FireworkEffect.Builder effect = FireworkEffect.builder();
            if (this.actionData.effectColors != null) {
               for(i = 0; i < this.actionData.effectColors.length; ++i) {
                  effect.withColor(Color.fromRGB(this.actionData.effectColors[i]));
               }

               fireworkMeta.setEffect(effect.build());
            }

            if (this.actionData.fadeColors != null) {
               for(i = 0; i < this.actionData.fadeColors.length; ++i) {
                  effect.withFade(Color.fromRGB(this.actionData.fadeColors[i]));
               }

               fireworkMeta.setEffect(effect.build());
            }

            if (this.actionData.hasFlicker) {
               effect.flicker(true);
            }

            if (this.actionData.hasTrail) {
               effect.trail(true);
            }

            fireworkMeta.setEffect(effect.build());
            this.item.setItemMeta(fireworkMeta);
         }

         ItemMeta meta = this.item.getItemMeta();
         if (this.actionData.name != null) {
            meta.setDisplayName(this.actionData.name);
         }

         if (this.actionData.lore != null) {
            meta.setLore(Arrays.asList(this.actionData.lore));
         }

         this.item.setItemMeta(meta);
      }
   }

   public ItemStackActionData getActionData() {
      return this.actionData;
   }

   public ItemStack getItem() {
      return this.item;
   }

   public String getNiceName() {
      String name = "";
      if (this.item != null) {
         String fullItemName = ItemUtils.getItemFullNiceName(this.item, this.materialAliases);
         name = this.actionData.amt + " " + fullItemName;
      }

      return name;
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      return this.placeItems(player, parameters, is_preview);
   }

   public ChangeResult applyRestore(Player player, QueryParameters parameters, boolean is_preview) {
      return this.placeItems(player, parameters, is_preview);
   }

   protected ChangeResult placeItems(Player player, QueryParameters parameters, boolean is_preview) {
      ChangeResultType result = null;
      if (is_preview) {
         return new ChangeResult(ChangeResultType.PLANNED, (BlockStateChange)null);
      } else {
         if (this.plugin.getConfig().getBoolean("prism.appliers.allow-rollback-items-removed-from-container")) {
            Block block = this.getWorld().getBlockAt(this.getLoc());
            Inventory inventory = null;
            int slot;
            if (!this.getType().getName().equals("item-drop") && !this.getType().getName().equals("item-pickup")) {
               if (block.getType().equals(Material.JUKEBOX)) {
                  Jukebox jukebox = (Jukebox)block.getState();
                  jukebox.setPlaying(this.item.getType());
                  jukebox.update();
               } else if (block.getState() instanceof InventoryHolder) {
                  InventoryHolder ih = (InventoryHolder)block.getState();
                  inventory = ih.getInventory();
               } else {
                  Entity[] foundEntities = block.getChunk().getEntities();
                  if (foundEntities.length > 0) {
                     Entity[] arr$ = foundEntities;
                     int len$ = foundEntities.length;

                     for(slot = 0; slot < len$; ++slot) {
                        Entity e = arr$[slot];
                        if (e.getType().equals(EntityType.ITEM_FRAME) && block.getWorld().equals(e.getWorld())) {
                           Prism.debug(block.getLocation());
                           Prism.debug(e.getLocation());
                           if (block.getLocation().distance(e.getLocation()) < 2.0) {
                              ItemFrame frame = (ItemFrame)e;
                              if ((!this.getType().getName().equals("item-remove") || !parameters.getProcessType().equals(PrismProcessType.ROLLBACK)) && (!this.getType().getName().equals("item-insert") || !parameters.getProcessType().equals(PrismProcessType.RESTORE))) {
                                 frame.setItem((ItemStack)null);
                              } else {
                                 frame.setItem(this.item);
                              }

                              result = ChangeResultType.APPLIED;
                           }
                        }
                     }
                  }
               }
            } else {
               String playerName = this.getPlayerName();
               Player onlinePlayer = Bukkit.getServer().getPlayer(playerName);
               if (onlinePlayer == null) {
                  Prism.debug("Skipping inventory process because player is offline");
                  return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
               }

               inventory = onlinePlayer.getInventory();
            }

            if (inventory != null) {
               PrismProcessType pt = parameters.getProcessType();
               String n = this.getType().getName();
               boolean removed;
               ItemStack currentSlotItem;
               if (pt.equals(PrismProcessType.ROLLBACK) && (n.equals("item-remove") || n.equals("item-drop")) || pt.equals(PrismProcessType.RESTORE) && (n.equals("item-insert") || n.equals("item-pickup"))) {
                  removed = false;
                  if (this.getActionData().slot >= 0 && this.getActionData().slot < ((Inventory)inventory).getSize()) {
                     currentSlotItem = ((Inventory)inventory).getItem(this.getActionData().slot);
                     if (currentSlotItem == null) {
                        result = ChangeResultType.APPLIED;
                        removed = true;
                        ((Inventory)inventory).setItem(this.getActionData().slot, this.getItem());
                     }
                  }

                  if (!removed) {
                     HashMap leftovers = InventoryUtils.addItemToInventory((Inventory)inventory, this.getItem());
                     if (leftovers.size() > 0) {
                        Prism.debug("Skipping adding items because there are leftovers");
                        result = ChangeResultType.SKIPPED;
                     } else {
                        result = ChangeResultType.APPLIED;
                        removed = true;
                     }
                  }

                  if (removed && (n.equals("item-drop") || n.equals("item-pickup"))) {
                     Entity[] entities = this.getLoc().getChunk().getEntities();
                     Entity[] arr$ = entities;
                     int len$ = entities.length;

                     for(int i$ = 0; i$ < len$; ++i$) {
                        Entity entity = arr$[i$];
                        if (entity instanceof Item) {
                           ItemStack stack = ((Item)entity).getItemStack();
                           if (stack.isSimilar(this.getItem())) {
                              stack.setAmount(stack.getAmount() - this.getItem().getAmount());
                              if (stack.getAmount() == 0) {
                                 entity.remove();
                              }
                              break;
                           }
                        }
                     }
                  }
               }

               if (pt.equals(PrismProcessType.ROLLBACK) && (n.equals("item-insert") || n.equals("item-pickup")) || pt.equals(PrismProcessType.RESTORE) && (n.equals("item-remove") || n.equals("item-drop"))) {
                  removed = false;
                  if (this.getActionData().slot >= 0) {
                     if (this.getActionData().slot > ((Inventory)inventory).getContents().length) {
                        ((Inventory)inventory).addItem(new ItemStack[]{this.getItem()});
                     } else {
                        currentSlotItem = ((Inventory)inventory).getItem(this.getActionData().slot);
                        if (currentSlotItem != null) {
                           currentSlotItem.setAmount(currentSlotItem.getAmount() - this.getItem().getAmount());
                           result = ChangeResultType.APPLIED;
                           removed = true;
                           ((Inventory)inventory).setItem(this.getActionData().slot, currentSlotItem);
                        }
                     }
                  }

                  if (!removed) {
                     slot = InventoryUtils.inventoryHasItem((Inventory)inventory, this.getItem().getTypeId(), this.getItem().getDurability());
                     if (slot > -1) {
                        ((Inventory)inventory).removeItem(new ItemStack[]{this.getItem()});
                        result = ChangeResultType.APPLIED;
                        removed = true;
                     } else {
                        Prism.debug("Item removal from container skipped because it's not currently inside.");
                        result = ChangeResultType.SKIPPED;
                     }
                  }

                  if (removed && (n.equals("item-drop") || n.equals("item-pickup"))) {
                     ItemUtils.dropItem(this.getLoc(), this.getItem());
                  }
               }
            }
         }

         return new ChangeResult(result, (BlockStateChange)null);
      }
   }

   public class ItemStackActionData {
      public int amt;
      public String name;
      public int color;
      public String owner;
      public String[] enchs;
      public String by;
      public String title;
      public String[] lore;
      public String[] content;
      public int slot = -1;
      public int[] effectColors;
      public int[] fadeColors;
      public boolean hasFlicker;
      public boolean hasTrail;
   }
}
