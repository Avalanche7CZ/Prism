package com.helion3.prism.libs.elixr;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

public class DeathUtils {
   public static String getCauseNiceName(Entity entity) {
      EntityDamageEvent e = entity.getLastDamageCause();
      if (e == null) {
         return "unknown";
      } else {
         EntityDamageEvent.DamageCause damageCause = e.getCause();
         Entity killer = null;
         if (entity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent entityDamageByEntityEvent = (EntityDamageByEntityEvent)entity.getLastDamageCause();
            if (entityDamageByEntityEvent.getDamager() instanceof Arrow) {
               Arrow arrow = (Arrow)entityDamageByEntityEvent.getDamager();
               ProjectileSource source = arrow.getShooter();
               if (source instanceof Player) {
                  killer = (Player)source;
               }
            } else {
               killer = entityDamageByEntityEvent.getDamager();
            }
         }

         if (entity instanceof Player) {
            Player player = (Player)entity;
            if (killer instanceof Player) {
               if (((Player)killer).getName().equals(player.getName())) {
                  return "suicide";
               }

               if (damageCause.equals(DamageCause.ENTITY_ATTACK) || damageCause.equals(DamageCause.PROJECTILE)) {
                  return "pvp";
               }
            }
         }

         if (damageCause.equals(DamageCause.ENTITY_ATTACK)) {
            return "mob";
         } else if (damageCause.equals(DamageCause.PROJECTILE)) {
            return "skeleton";
         } else if (damageCause.equals(DamageCause.ENTITY_EXPLOSION)) {
            return "creeper";
         } else if (damageCause.equals(DamageCause.CONTACT)) {
            return "cactus";
         } else if (damageCause.equals(DamageCause.BLOCK_EXPLOSION)) {
            return "tnt";
         } else if (!damageCause.equals(DamageCause.FIRE) && !damageCause.equals(DamageCause.FIRE_TICK)) {
            return damageCause.equals(DamageCause.MAGIC) ? "potion" : damageCause.name().toLowerCase();
         } else {
            return "fire";
         }
      }
   }

   public static String getAttackerName(Entity victim) {
      String cause = getCauseNiceName(victim);
      if (victim instanceof Player) {
         Player killer = ((Player)victim).getKiller();
         if (killer != null) {
            return killer.getName();
         }
      }

      if (cause == "mob") {
         Entity killer = ((EntityDamageByEntityEvent)victim.getLastDamageCause()).getDamager();
         if (killer instanceof Player) {
            return ((Player)killer).getName();
         } else if (killer instanceof Skeleton) {
            Skeleton skele = (Skeleton)killer;
            return skele.getSkeletonType() == SkeletonType.WITHER ? "witherskeleton" : "skeleton";
         } else if (killer instanceof Arrow) {
            return "skeleton";
         } else if (killer instanceof Wolf) {
            Wolf wolf = (Wolf)killer;
            if (wolf.isTamed()) {
               return !(wolf.getOwner() instanceof Player) && !(wolf.getOwner() instanceof OfflinePlayer) ? "wolf" : "pvpwolf";
            } else {
               return "wolf";
            }
         } else {
            return killer.getType().getName().toLowerCase();
         }
      } else {
         return cause;
      }
   }

   public static String getVictimName(Entity victim) {
      if (victim instanceof Player) {
         return ((Player)victim).getName();
      } else if (victim instanceof Skeleton) {
         Skeleton skele = (Skeleton)victim;
         return skele.getSkeletonType() == SkeletonType.WITHER ? "witherskeleton" : "skeleton";
      } else if (victim instanceof Arrow) {
         return "skeleton";
      } else if (victim instanceof Wolf) {
         Wolf wolf = (Wolf)victim;
         if (wolf.isTamed()) {
            return !(wolf.getOwner() instanceof Player) && !(wolf.getOwner() instanceof OfflinePlayer) ? "wolf" : "pvpwolf";
         } else {
            return "wolf";
         }
      } else {
         return victim.getType().getName().toLowerCase();
      }
   }

   public static String getTameWolfOwner(EntityDeathEvent event) {
      String owner = "";
      Entity killer = ((EntityDamageByEntityEvent)event.getEntity().getLastDamageCause()).getDamager();
      if (killer instanceof Wolf) {
         Wolf wolf = (Wolf)killer;
         if (wolf.isTamed()) {
            if (wolf.getOwner() instanceof Player) {
               owner = ((Player)wolf.getOwner()).getName();
            }

            if (wolf.getOwner() instanceof OfflinePlayer) {
               owner = ((OfflinePlayer)wolf.getOwner()).getName();
            }
         }
      }

      return owner;
   }

   public static String getWeapon(Player p) {
      String death_weapon = "";
      if (p.getKiller() instanceof Player) {
         ItemStack weapon = p.getKiller().getItemInHand();
         death_weapon = weapon.getType().toString().toLowerCase();
         death_weapon = death_weapon.replaceAll("_", " ");
         if (death_weapon.equalsIgnoreCase("air")) {
            death_weapon = " hands";
         }
      }

      return death_weapon;
   }
}
