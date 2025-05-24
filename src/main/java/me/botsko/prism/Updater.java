package me.botsko.prism;

import me.botsko.prism.settings.Settings;

public class Updater {
   protected final int currentDbSchemaVersion = 5;
   protected final Prism plugin;

   public Updater(Prism plugin) {
      this.plugin = plugin;
   }

   protected int getClientDbSchemaVersion() {
      String schema_ver = Settings.getSetting("schema_ver");
      return schema_ver != null ? Integer.parseInt(schema_ver) : 5;
   }

   public void apply_updates() {
      this.saveCurrentSchemaVersion();
   }

   public void saveCurrentSchemaVersion() {
      Settings.saveSetting("schema_ver", "5");
   }
}
