package me.botsko.prism.players;

import java.util.UUID;
import org.bukkit.entity.Player;

public class PrismPlayer {
   private int playerId;
   private String player;
   private UUID playerUuid;

   public PrismPlayer(int playerId, Player player) {
      this(playerId, player.getUniqueId(), player.getName());
   }

   public PrismPlayer(int playerId, UUID playerUuid, String player) {
      this.playerId = playerId;
      this.playerUuid = playerUuid;
      this.player = player;
   }

   public void setId(int newId) {
      if (this.playerId > 0) {
         throw new IllegalArgumentException("Cannot overwrite PrismPlayer primary key.");
      } else {
         this.playerId = newId;
      }
   }

   public int getId() {
      return this.playerId;
   }

   public String getName() {
      return this.player;
   }

   public void setName(String name) {
      this.player = name;
   }

   public UUID getUUID() {
      return this.playerUuid;
   }

   public void setUUID(UUID uuid) {
      this.playerUuid = uuid;
   }
}
