package me.botsko.elixr;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class ConfigBase {
   protected Plugin plugin;
   protected FileConfiguration config;

   public ConfigBase(Plugin plugin) {
      this.plugin = plugin;
   }

   public FileConfiguration getConfig() {
      this.config = this.plugin.getConfig();
      return this.config;
   }

   protected File getDirectory() {
      File dir = new File("" + this.plugin.getDataFolder());
      return dir;
   }

   protected File getFilename(String filename) {
      File file = new File(this.getDirectory(), filename + ".yml");
      return file;
   }

   protected FileConfiguration loadConfig(String default_folder, String filename) {
      File file = this.getFilename(filename);
      if (file.exists()) {
         return YamlConfiguration.loadConfiguration(file);
      } else {
         InputStream defConfigStream = this.plugin.getResource(default_folder + filename + ".yml");
         return defConfigStream != null ? YamlConfiguration.loadConfiguration(defConfigStream) : null;
      }
   }

   protected void saveConfig(String filename, FileConfiguration config) {
      File file = this.getFilename(filename);

      try {
         config.save(file);
      } catch (IOException var5) {
         var5.printStackTrace();
      }

   }

   protected void write(String filename, FileConfiguration config) {
      try {
         BufferedWriter bw = new BufferedWriter(new FileWriter(this.getFilename(filename), true));
         this.saveConfig(filename, config);
         bw.flush();
         bw.close();
      } catch (IOException var4) {
      }

   }
}
