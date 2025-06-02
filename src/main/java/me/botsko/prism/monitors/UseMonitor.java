package me.botsko.prism.monitors;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import me.botsko.prism.configs.MonitoredItemsConfig;
import me.botsko.prism.utils.MiscUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.botsko.prism.Prism;

public class UseMonitor {

    private final Prism plugin;
    private List<String> blocksToAlertOnPlace;
    private List<String> blocksToAlertOnBreak;
    private ConcurrentHashMap<String, Integer> countedEvents = new ConcurrentHashMap<String, Integer>();

    public UseMonitor(Prism plugin) {
        this.plugin = plugin;
        loadMonitoredItemLists();
        resetEventsQueue();
    }

    private void loadMonitoredItemLists() {
        MonitoredItemsConfig monitoredConf = Prism.getMonitoredItemsConfig();
        if (monitoredConf != null) {
            List<String> placement = monitoredConf.getMonitoredPlacement();
            List<String> breakage = monitoredConf.getMonitoredBreak();
            this.blocksToAlertOnPlace = (placement != null) ? new ArrayList<>(placement) : new ArrayList<>();
            this.blocksToAlertOnBreak = (breakage != null) ? new ArrayList<>(breakage) : new ArrayList<>();
        } else {
            Prism.log(Level.WARNING, "UseMonitor could not load monitored items config because MonitoredItemsConfig instance is null. Alerts for item placement/break might not work.");
            this.blocksToAlertOnPlace = new ArrayList<>();
            this.blocksToAlertOnBreak = new ArrayList<>();
        }
    }

    protected void incrementCount(String playername, String msg) {
        int count = 0;
        if (countedEvents.containsKey(playername)) {
            count = countedEvents.get(playername);
        }
        count++;
        countedEvents.put(playername, count);

        msg = ChatColor.GRAY + playername + " " + msg;
        if (count == 5) {
            msg = playername + " continues - pausing warnings.";
        }

        if (count <= 5) {
            if (plugin.getConfig().getBoolean("prism.alerts.uses.log-to-console")) {
                plugin.alertPlayers( null, msg );
                Prism.log(msg);
            }
            List<String> commands = plugin.getConfig().getStringList("prism.alerts.uses.log-commands");
            if (commands != null && !commands.isEmpty()) {
                MiscUtils.dispatchAlert(msg, commands);
            }
        }
    }

    protected boolean checkFeatureShouldProceed(Player player) {
        if (!plugin.getConfig().getBoolean("prism.alerts.uses.enabled")) return false;
        if (plugin.getConfig().getBoolean("prism.alerts.uses.ignore-staff") && player.hasPermission("prism.alerts")) return false;
        if (player.hasPermission("prism.bypass-use-alerts")) return false;
        return true;
    }

    public void alertOnBlockPlacement(Player player, Block block) {
        if (!checkFeatureShouldProceed(player)) return;
        if (blocksToAlertOnPlace == null) {
            loadMonitoredItemLists();
            if(blocksToAlertOnPlace == null) return;
        }

        final String playername = player.getName();
        final String blockType = String.valueOf(block.getTypeId());
        final String blockTypeWithData = block.getTypeId() + ":" + block.getData();

        if (blocksToAlertOnPlace.contains(blockType) || blocksToAlertOnPlace.contains(blockTypeWithData)) {
            final String alias = Prism.getItems().getAlias(block.getTypeId(), block.getData());
            Location loc = block.getLocation();
            String locationString = " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            incrementCount(playername, "placed " + alias + locationString);
        }
    }

    public void alertOnBlockBreak(Player player, Block block) {
        if (!checkFeatureShouldProceed(player)) return;
        if (blocksToAlertOnBreak == null) {
            loadMonitoredItemLists();
            if(blocksToAlertOnBreak == null) return;
        }

        final String playername = player.getName();
        final String blockType = String.valueOf(block.getTypeId());
        final String blockTypeWithData = block.getTypeId() + ":" + block.getData();

        if (blocksToAlertOnBreak.contains(blockType) || blocksToAlertOnBreak.contains(blockTypeWithData)) {
            final String alias = Prism.getItems().getAlias(block.getTypeId(), block.getData());
            Location loc = block.getLocation();
            String locationString = " at " + loc.getWorld().getName() + ":" + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
            incrementCount(playername, "broke " + alias + locationString);
        }
    }

    public void alertOnItemUse(Player player, String use_msg) {
        if (!checkFeatureShouldProceed(player)) return;
        final String playername = player.getName();
        incrementCount(playername, use_msg);
    }

    public void alertOnVanillaXray(Player player, String use_msg) {
        if (!checkFeatureShouldProceed(player)) return;
        final String playername = player.getName();
        incrementCount(playername, use_msg);
    }

    public void resetEventsQueue() {
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                countedEvents = new ConcurrentHashMap<String, Integer>();
            }
        }, 7000L, 7000L);
    }
}
