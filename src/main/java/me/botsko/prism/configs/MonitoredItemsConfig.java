package me.botsko.prism.configs;

import me.botsko.prism.Prism;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MonitoredItemsConfig {

    private Plugin plugin;
    private File configFile;
    private FileConfiguration fileConfiguration;

    private List<String> monitoredPlacement;
    private List<String> monitoredBreak;

    public MonitoredItemsConfig(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "monitored-items.yml");
        loadDefaultsAndSave();
        loadSettings();
    }

    private void loadDefaultsAndSave() {
        if (!configFile.exists()) {
            plugin.saveResource("monitored-items.yml", false);
            Prism.log(Level.INFO, "Default monitored-items.yml created. Please review if needed.");
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultConfigStream = plugin.getResource("monitored-items.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            fileConfiguration.setDefaults(defaultConfig);
        } else {
            Prism.log(Level.WARNING, "Could not find default monitored-items.yml in JAR. Defaults may not be applied correctly if file is missing keys.");
            List<String> fallbackPlacement = new ArrayList<>();
            fallbackPlacement.add("7");
            fallbackPlacement.add("46");
            fallbackPlacement.add("10");
            fallbackPlacement.add("11");
            fileConfiguration.addDefault("alerts.uses.item-placement", fallbackPlacement);
            fileConfiguration.addDefault("alerts.uses.item-break", new ArrayList<String>());
        }
        fileConfiguration.options().copyDefaults(true);
        try {
            fileConfiguration.save(configFile);
        } catch (IOException e) {
            Prism.log(Level.SEVERE, "Could not save monitored-items.yml: " + e.getMessage());
        }
    }

    public void loadSettings() {
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);
        InputStream defaultConfigStream = plugin.getResource("monitored-items.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            fileConfiguration.setDefaults(defaultConfig);
        }

        monitoredPlacement = fileConfiguration.getStringList("alerts.uses.item-placement");
        monitoredBreak = fileConfiguration.getStringList("alerts.uses.item-break");
        Prism.log(Level.INFO, "Loaded " + (monitoredPlacement != null ? monitoredPlacement.size() : 0) + " items to monitor for placement and " + (monitoredBreak != null ? monitoredBreak.size() : 0) + " for break.");
    }

    public void reloadConfig() {
        if (configFile == null) {
            configFile = new File(plugin.getDataFolder(), "monitored-items.yml");
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        InputStream defaultConfigStream = plugin.getResource("monitored-items.yml");
        if (defaultConfigStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream, StandardCharsets.UTF_8));
            fileConfiguration.setDefaults(defaultConfig);
        } else {
            Prism.log(Level.WARNING, "Could not find default monitored-items.yml in JAR during reload. Defaults may not be applied correctly if file is missing keys.");
        }

        loadSettings();
        Prism.log(Level.INFO, "Monitored items configuration reloaded.");
    }

    public List<String> getMonitoredPlacement() {
        if (monitoredPlacement == null) {
            loadSettings();
        }
        return monitoredPlacement != null ? monitoredPlacement : new ArrayList<>();
    }

    public List<String> getMonitoredBreak() {
        if (monitoredBreak == null) {
            loadSettings();
        }
        return monitoredBreak != null ? monitoredBreak : new ArrayList<>();
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            reloadConfig();
        }
        return fileConfiguration;
    }
}

