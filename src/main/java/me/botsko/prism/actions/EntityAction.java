package me.botsko.prism.actions;

import me.botsko.prism.Prism;
import me.botsko.prism.actionlibs.QueryParameters;
import me.botsko.prism.appliers.ChangeResult;
import me.botsko.prism.appliers.ChangeResultType;
import me.botsko.prism.events.BlockStateChange;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Ocelot;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Horse.Color;
import org.bukkit.entity.Horse.Style;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Ocelot.Type;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

public class EntityAction extends GenericAction {
   protected EntityActionData actionData;

   public void setEntity(Entity entity, String dyeUsed) {
      this.actionData = new EntityActionData();
      if (entity != null && entity.getType() != null && entity.getType().name() != null) {
         this.actionData.entity_name = entity.getType().name().toLowerCase();
         this.world_name = entity.getWorld().getName();
         this.x = (double)entity.getLocation().getBlockX();
         this.y = (double)entity.getLocation().getBlockY();
         this.z = (double)entity.getLocation().getBlockZ();
         if (entity instanceof LivingEntity) {
            this.actionData.custom_name = ((LivingEntity)entity).getCustomName();
         }

         if (entity instanceof Ageable && !(entity instanceof Monster)) {
            Ageable a = (Ageable)entity;
            this.actionData.isAdult = a.isAdult();
         } else {
            this.actionData.isAdult = true;
         }

         if (entity instanceof Sheep) {
            Sheep sheep = (Sheep)entity;
            this.actionData.color = sheep.getColor().name().toLowerCase();
         }

         if (dyeUsed != null) {
            this.actionData.newColor = dyeUsed;
         }

         if (entity instanceof Villager) {
            Villager v = (Villager)entity;
            if (v.getProfession() != null) {
               this.actionData.profession = v.getProfession().toString().toLowerCase();
            }
         }

         if (entity instanceof Wolf) {
            Wolf wolf = (Wolf)entity;
            if (wolf.isTamed()) {
               if (wolf.getOwner() instanceof Player) {
                  this.actionData.taming_owner = wolf.getOwner().getName();
               }

               if (wolf.getOwner() instanceof OfflinePlayer) {
                  this.actionData.taming_owner = wolf.getOwner().getName();
               }
            }

            this.actionData.color = wolf.getCollarColor().name().toLowerCase();
            if (wolf.isSitting()) {
               this.actionData.sitting = true;
            }
         }

         if (entity instanceof Ocelot) {
            Ocelot ocelot = (Ocelot)entity;
            if (ocelot.isTamed()) {
               if (ocelot.getOwner() instanceof Player) {
                  this.actionData.taming_owner = ocelot.getOwner().getName();
               }

               if (ocelot.getOwner() instanceof OfflinePlayer) {
                  this.actionData.taming_owner = ocelot.getOwner().getName();
               }
            }

            this.actionData.var = ocelot.getCatType().toString().toLowerCase();
            if (ocelot.isSitting()) {
               this.actionData.sitting = true;
            }
         }

         if (entity instanceof Horse) {
            Horse h = (Horse)entity;
            this.actionData.var = h.getVariant().toString();
            this.actionData.hColor = h.getColor().toString();
            this.actionData.style = h.getStyle().toString();
            this.actionData.chest = h.isCarryingChest();
            this.actionData.dom = h.getDomestication();
            this.actionData.maxDom = h.getMaxDomestication();
            this.actionData.jump = h.getJumpStrength();
            this.actionData.maxHealth = h.getMaxHealth();
            HorseInventory hi = h.getInventory();
            if (hi.getSaddle() != null) {
               this.actionData.saddle = "" + hi.getSaddle().getTypeId();
            }

            if (hi.getArmor() != null) {
               this.actionData.armor = "" + hi.getArmor().getTypeId();
            }

            if (h.isTamed()) {
               if (h.getOwner() instanceof Player) {
                  this.actionData.taming_owner = h.getOwner().getName();
               }

               if (h.getOwner() instanceof OfflinePlayer) {
                  this.actionData.taming_owner = h.getOwner().getName();
               }
            }
         }
      }

   }

   public void save() {
      this.data = this.gson.toJson((Object)this.actionData);
   }

   public void setData(String data) {
      if (data != null && data.startsWith("{")) {
         this.actionData = (EntityActionData)this.gson.fromJson(data, EntityActionData.class);
      }

   }

   public EntityType getEntityType() {
      try {
         EntityType e = EntityType.valueOf(this.actionData.entity_name.toUpperCase());
         if (e != null) {
            return e;
         }
      } catch (IllegalArgumentException var2) {
      }

      return null;
   }

   public boolean isAdult() {
      return this.actionData.isAdult;
   }

   public boolean isSitting() {
      return this.actionData.sitting;
   }

   public DyeColor getColor() {
      return this.actionData.color != null ? DyeColor.valueOf(this.actionData.color.toUpperCase()) : null;
   }

   public Villager.Profession getProfession() {
      return this.actionData.profession != null ? Profession.valueOf(this.actionData.profession.toUpperCase()) : null;
   }

   public String getTamingOwner() {
      return this.actionData.taming_owner;
   }

   public String getCustomName() {
      return this.actionData.custom_name;
   }

   public Ocelot.Type getCatType() {
      return Type.valueOf(this.actionData.var.toUpperCase());
   }

   public String getNiceName() {
      String name = "";
      if (this.actionData.color != null && !this.actionData.color.isEmpty()) {
         name = name + this.actionData.color + " ";
      }

      if (this.actionData.profession != null) {
         name = name + this.actionData.profession + " ";
      }

      if (this.actionData.taming_owner != null) {
         name = name + this.actionData.taming_owner + "'s ";
      }

      if ((this.actionData.entity_name.equals("ocelot") || this.actionData.entity_name.equals("horse")) && this.actionData.var != null) {
         name = name + this.actionData.var.toLowerCase().replace("_", " ");
      } else {
         name = name + this.actionData.entity_name;
      }

      if (this.actionData.newColor != null) {
         name = name + " " + this.actionData.newColor;
      }

      if (this.actionData.custom_name != null) {
         name = name + " named " + this.actionData.custom_name;
      }

      return name;
   }

   public Horse.Variant getVariant() {
      return !this.actionData.var.isEmpty() ? Variant.valueOf(this.actionData.var) : null;
   }

   public Horse.Color getHorseColor() {
      return this.actionData.hColor != null && !this.actionData.hColor.isEmpty() ? Color.valueOf(this.actionData.hColor) : null;
   }

   public Horse.Style getStyle() {
      return !this.actionData.style.isEmpty() ? Style.valueOf(this.actionData.style) : null;
   }

   public ItemStack getSaddle() {
      return this.actionData.saddle != null ? new ItemStack(Integer.parseInt(this.actionData.saddle), 1) : null;
   }

   public ItemStack getArmor() {
      return this.actionData.armor != null ? new ItemStack(Integer.parseInt(this.actionData.armor), 1) : null;
   }

   public double getMaxHealth() {
      return this.actionData.maxHealth;
   }

   public ChangeResult applyRollback(Player player, QueryParameters parameters, boolean is_preview) {
      if (this.getEntityType() == null) {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      } else if (Prism.getIllegalEntities().contains(this.getEntityType().name().toLowerCase())) {
         return new ChangeResult(ChangeResultType.SKIPPED, (BlockStateChange)null);
      } else if (!is_preview) {
         Location loc = this.getLoc();
         loc.setX(loc.getX() + 0.5);
         loc.setZ(loc.getZ() + 0.5);
         Entity entity = loc.getWorld().spawnEntity(loc, this.getEntityType());
         if (entity instanceof LivingEntity && this.getCustomName() != null) {
            LivingEntity namedEntity = (LivingEntity)entity;
            namedEntity.setCustomName(this.getCustomName());
         }

         if (entity instanceof Ageable) {
            Ageable age = (Ageable)entity;
            if (!this.isAdult()) {
               age.setBaby();
            } else {
               age.setAdult();
            }
         }

         if (entity.getType().equals(EntityType.SHEEP) && this.getColor() != null) {
            Sheep sheep = (Sheep)entity;
            sheep.setColor(this.getColor());
         }

         if (entity instanceof Villager && this.getProfession() != null) {
            Villager v = (Villager)entity;
            v.setProfession(this.getProfession());
         }

         String tamingOwner;
         Player owner;
         OfflinePlayer offlinePlayer;
         if (entity instanceof Wolf) {
            Wolf wolf = (Wolf)entity;
            tamingOwner = this.getTamingOwner();
            if (tamingOwner != null) {
               owner = this.plugin.getServer().getPlayer(tamingOwner);
               if (owner == null) {
                  offlinePlayer = this.plugin.getServer().getOfflinePlayer(tamingOwner);
                  if (offlinePlayer.hasPlayedBefore()) {
                     owner = offlinePlayer.getPlayer();
                  }
               }

               if (owner != null) {
                  wolf.setOwner(owner);
               }
            }

            if (this.getColor() != null) {
               wolf.setCollarColor(this.getColor());
            }

            if (this.isSitting()) {
               wolf.setSitting(true);
            }
         }

         if (entity instanceof Ocelot) {
            Ocelot ocelot = (Ocelot)entity;
            tamingOwner = this.getTamingOwner();
            if (tamingOwner != null) {
               owner = this.plugin.getServer().getPlayer(tamingOwner);
               if (owner == null) {
                  offlinePlayer = this.plugin.getServer().getOfflinePlayer(tamingOwner);
                  if (offlinePlayer.hasPlayedBefore()) {
                     owner = offlinePlayer.getPlayer();
                  }
               }

               if (owner != null) {
                  ocelot.setOwner(owner);
               }
            }

            if (this.getCatType() != null) {
               ocelot.setCatType(this.getCatType());
            }

            if (this.isSitting()) {
               ocelot.setSitting(true);
            }
         }

         if (entity instanceof Horse) {
            Horse h = (Horse)entity;
            if (this.getVariant() != null) {
               h.setVariant(this.getVariant());
            }

            if (this.getHorseColor() != null) {
               h.setColor(this.getHorseColor());
            }

            if (this.getStyle() != null) {
               h.setStyle(this.getStyle());
            }

            h.setCarryingChest(this.actionData.chest);
            h.setDomestication(this.actionData.dom);
            h.setMaxDomestication(this.actionData.maxDom);
            h.setJumpStrength(this.actionData.jump);
            h.setMaxHealth(this.actionData.maxHealth);
            h.getInventory().setSaddle(this.getSaddle());
            h.getInventory().setArmor(this.getArmor());
            tamingOwner = this.getTamingOwner();
            if (tamingOwner != null) {
               owner = this.plugin.getServer().getPlayer(tamingOwner);
               if (owner == null) {
                  offlinePlayer = this.plugin.getServer().getOfflinePlayer(tamingOwner);
                  if (offlinePlayer.hasPlayedBefore()) {
                     owner = offlinePlayer.getPlayer();
                  }
               }

               if (owner != null) {
                  h.setOwner(owner);
               }
            }
         }

         return new ChangeResult(ChangeResultType.APPLIED, (BlockStateChange)null);
      } else {
         return new ChangeResult(ChangeResultType.PLANNED, (BlockStateChange)null);
      }
   }

   public class EntityActionData {
      public String entity_name;
      public String custom_name;
      public boolean isAdult;
      public boolean sitting;
      public String color;
      public String newColor;
      public String profession;
      public String taming_owner;
      public String var;
      public String hColor;
      public String style;
      public boolean chest;
      public int dom;
      public int maxDom;
      public double jump;
      public String saddle;
      public String armor;
      public double maxHealth;
   }
}
