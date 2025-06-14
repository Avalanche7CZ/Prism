package me.botsko.prism.actionlibs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import me.botsko.prism.Prism;
import me.botsko.prism.actions.Handler;
import me.botsko.prism.database.PrismDatabaseHandler;
import me.botsko.prism.players.PlayerIdentification;
import me.botsko.prism.players.PrismPlayer;

public class RecordingTask implements Runnable {

    private final Prism plugin;

    public RecordingTask(Prism plugin) {
        this.plugin = plugin;
    }

    public void save() {
        if (!plugin.isEnabled() && !plugin.getServer().isPrimaryThread()) {
            Prism.log("RecordingTask.save: Plugin not enabled and not on main thread, aborting save for async task.");
            return;
        }
        if (!RecordingQueue.getQueue().isEmpty()) {
            insertActionsIntoDatabase();
        }
    }

    public static int insertActionIntoDatabase(Handler a) {
        Prism prismInstance = Prism.getPlugin(Prism.class);
        if (prismInstance == null || Prism.config == null) {
            System.out.println("[Prism] Critical error: Plugin instance or config not available for single insert.");
            return 0;
        }

        String prefix = Prism.config.getString("prism.database.tablePrefix", "prism_");
        int generatedDataId = 0;
        Connection conn = null;
        PreparedStatement psData = null;
        PreparedStatement psExtraData = null;
        ResultSet generatedKeys = null;

        try {
            conn = PrismDatabaseHandler.dbc();
            if (conn == null || conn.isClosed()) {
                Prism.log("Prism database error during single insert. Connection is null/closed. Action not logged.");
                return 0;
            }
            conn.setAutoCommit(false);

            int world_id = Prism.prismWorlds.getOrDefault(a.getWorldName(), 0);
            int action_id = Prism.prismActions.getOrDefault(a.getType().getName(), 0);
            PrismPlayer prismPlayer = PlayerIdentification.cachePrismPlayer(a.getPlayerName());
            int player_id = (prismPlayer != null) ? prismPlayer.getId() : 0;

            if (world_id == 0 || action_id == 0 || player_id == 0) {
                Prism.log("Error during single insert: Missing critical ID. World: " + a.getWorldName() + "(" + world_id +
                        "), ActionType: " + a.getType().getName() + "(" + action_id +
                        "), Player: " + a.getPlayerName() + "(" + player_id + "). Action not logged.");
                if (conn != null) conn.rollback();
                return 0;
            }

            String sqlData = "INSERT INTO `" + prefix + "data` (`epoch`,`action_id`,`player_id`,`world_id`,`block_id`,`block_subid`,`old_block_id`,`old_block_subid`,`x`,`y`,`z`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            psData = conn.prepareStatement(sqlData, Statement.RETURN_GENERATED_KEYS);
            psData.setLong(1, System.currentTimeMillis() / 1000L);
            psData.setInt(2, action_id);
            psData.setInt(3, player_id);
            psData.setInt(4, world_id);
            psData.setInt(5, a.getBlockId());
            psData.setInt(6, a.getBlockSubId());
            psData.setInt(7, a.getOldBlockId());
            psData.setInt(8, a.getOldBlockSubId());
            psData.setInt(9, (int) a.getX());
            psData.setInt(10, (int) a.getY());
            psData.setInt(11, (int) a.getZ());
            psData.executeUpdate();

            generatedKeys = psData.getGeneratedKeys();
            if (generatedKeys.next()) {
                generatedDataId = generatedKeys.getInt(1);
            } else {
                Prism.log("Error during single insert: Failed to retrieve generated key for prism_data. Action for '" + a.getPlayerName() + "' not fully logged.");
                conn.rollback();
                return 0;
            }
            if (generatedDataId > 0 && a.getData() != null && !a.getData().isEmpty()) {
                String sqlExtraData = "INSERT INTO `" + prefix + "data_extra` (`data_id`,`data`) VALUES (?,?)";
                psExtraData = conn.prepareStatement(sqlExtraData);
                psExtraData.setInt(1, generatedDataId);
                psExtraData.setString(2, a.getData());
                psExtraData.executeUpdate();
            }
            conn.commit();
        } catch (final SQLException e) {
            Prism.log("SQLException during single action insert: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    if(!conn.isClosed()) conn.rollback();
                } catch (SQLException ex) {
                    Prism.log("SQLException on rollback during single insert: " + ex.getMessage());
                }
            }
            generatedDataId = 0;
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (final SQLException ignored) {}
            if (psData != null) try { psData.close(); } catch (final SQLException ignored) {}
            if (psExtraData != null) try { psExtraData.close(); } catch (final SQLException ignored) {}
            if (conn != null) {
                try {
                    if(!conn.isClosed()) conn.setAutoCommit(true);
                } catch (SQLException ex) { }
                try {
                    conn.close();
                } catch (final SQLException ignored) {}
            }
        }
        return generatedDataId;
    }

    public void insertActionsIntoDatabase() {
        boolean isForceDrainContext = !plugin.isEnabled() && plugin.getServer().isPrimaryThread();

        if (!plugin.isEnabled() && !isForceDrainContext) {
            Prism.log("RecordingTask.insertActions: Plugin not enabled and not in force drain context, aborting batch insert.");
            return;
        }

        String prefix = plugin.getConfig().getString("prism.database.tablePrefix", "prism_");
        List<Handler> handlersInCurrentBatch = new ArrayList<>();
        Connection conn = null;
        PreparedStatement psData = null;
        PreparedStatement psExtraData = null;
        ResultSet generatedKeys = null;

        try {
            int minBatchSize = plugin.getConfig().getInt("prism.database.min-actions-per-insert-batch", 50);
            int maxBatchSize = plugin.getConfig().getInt("prism.database.actions-per-insert-batch", 1000);
            if (maxBatchSize < 1) maxBatchSize = 1000;
            if (minBatchSize < 1) minBatchSize = 1;
            if (minBatchSize > maxBatchSize) minBatchSize = maxBatchSize;

            if (RecordingQueue.getQueue().isEmpty()) {
                return;
            }

            if (!isForceDrainContext && RecordingQueue.getQueue().size() < minBatchSize) {
                Prism.debug("Queue size (" + RecordingQueue.getQueue().size() + ") is less than min batch size (" + minBatchSize + "). Postponing batch insert.");
                return;
            }

            Prism.debug("Beginning batch insert from queue. Current time: " + System.currentTimeMillis());

            conn = PrismDatabaseHandler.dbc();

            if (conn == null || conn.isClosed()) {
                if (!isForceDrainContext) {
                    RecordingManager.failedDbConnectionCount++;
                }
                Prism.log("Prism DB Error: Connection null or closed before batch. Actions remain in queue. Failed count: " + RecordingManager.failedDbConnectionCount);
                return;
            }
            if (!isForceDrainContext) {
                RecordingManager.failedDbConnectionCount = 0;
            }

            conn.setAutoCommit(false);
            String sqlData = "INSERT INTO `" + prefix + "data` (`epoch`,`action_id`,`player_id`,`world_id`,`block_id`,`block_subid`,`old_block_id`,`old_block_subid`,`x`,`y`,`z`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
            psData = conn.prepareStatement(sqlData, Statement.RETURN_GENERATED_KEYS);

            String sqlExtraData = "INSERT INTO `" + prefix + "data_extra` (`data_id`,`data`) VALUES (?,?)";
            psExtraData = conn.prepareStatement(sqlExtraData);

            int currentBatchSize = 0;
            while (!RecordingQueue.getQueue().isEmpty() && currentBatchSize < maxBatchSize) {
                if (!plugin.isEnabled() && !isForceDrainContext) {
                    Prism.log("Plugin disabled during batch preparation, aborting.");
                    conn.rollback();
                    return;
                }
                final Handler a = RecordingQueue.getQueue().peek();
                if (a == null) break;
                if (a.isCanceled()) {
                    RecordingQueue.getQueue().poll();
                    continue;
                }

                int world_id = Prism.prismWorlds.getOrDefault(a.getWorldName(), 0);
                int action_id = Prism.prismActions.getOrDefault(a.getType().getName(), 0);
                PrismPlayer prismPlayer = PlayerIdentification.cachePrismPlayer(a.getPlayerName());
                int player_id = (prismPlayer != null) ? prismPlayer.getId() : 0;

                if (world_id == 0 || action_id == 0 || player_id == 0) {
                    Prism.log("Skipping action in batch due to missing ID: World: " + a.getWorldName() + "(" + world_id +
                            "), ActionType: " + a.getType().getName() + "(" + action_id +
                            "), Player: " + a.getPlayerName() + "(" + player_id + ")");
                    RecordingQueue.getQueue().poll();
                    continue;
                }
                RecordingQueue.getQueue().poll();
                psData.setLong(1, System.currentTimeMillis() / 1000L);
                psData.setInt(2, action_id);
                psData.setInt(3, player_id);
                psData.setInt(4, world_id);
                psData.setInt(5, a.getBlockId());
                psData.setInt(6, a.getBlockSubId());
                psData.setInt(7, a.getOldBlockId());
                psData.setInt(8, a.getOldBlockSubId());
                psData.setInt(9, (int) a.getX());
                psData.setInt(10, (int) a.getY());
                psData.setInt(11, (int) a.getZ());
                psData.addBatch();
                handlersInCurrentBatch.add(a);
                currentBatchSize++;
            }

            if (!handlersInCurrentBatch.isEmpty()) {
                if (!plugin.isEnabled() && !isForceDrainContext) {
                    Prism.log("Plugin disabled before prism_data batch execution, aborting.");
                    conn.rollback();
                    return;
                }
                Prism.debug("Executing prism_data batch for " + handlersInCurrentBatch.size() + " actions.");
                psData.executeBatch();

                generatedKeys = psData.getGeneratedKeys();
                int keyIndex = 0;
                boolean hasAnyExtraDataInThisBatch = false;
                while (generatedKeys.next()) {
                    if (!plugin.isEnabled() && !isForceDrainContext) {
                        Prism.log("Plugin disabled during generated keys processing, aborting.");
                        conn.rollback();
                        return;
                    }
                    int currentGeneratedDataId = generatedKeys.getInt(1);
                    if (keyIndex < handlersInCurrentBatch.size()) {
                        Handler currentHandler = handlersInCurrentBatch.get(keyIndex);
                        if (currentHandler.getData() != null && !currentHandler.getData().isEmpty()) {
                            psExtraData.setInt(1, currentGeneratedDataId);
                            psExtraData.setString(2, currentHandler.getData());
                            psExtraData.addBatch();
                            hasAnyExtraDataInThisBatch = true;
                        }
                    } else {
                        Prism.log("Warning: More generated keys than handlers in batch. Key Index: " + keyIndex + ", Handler Count: " + handlersInCurrentBatch.size());
                    }
                    keyIndex++;
                }
                if (keyIndex != handlersInCurrentBatch.size()) {
                    Prism.log("Warning: Mismatch in generated keys count (" + keyIndex +
                            ") and batched prism_data actions (" + handlersInCurrentBatch.size() + "). Data integrity for extra_data might be affected.");
                }

                if (hasAnyExtraDataInThisBatch) {
                    if (!plugin.isEnabled() && !isForceDrainContext) {
                        Prism.log("Plugin disabled before prism_data_extra batch execution, aborting.");
                        conn.rollback();
                        return;
                    }
                    Prism.debug("Executing prism_data_extra batch.");
                    psExtraData.executeBatch();
                }

                if (!plugin.isEnabled() && !isForceDrainContext) {
                    Prism.log("Plugin disabled before commit, aborting and rolling back.");
                    conn.rollback();
                    return;
                }
                conn.commit();
                Prism.debug("Batch insert committed. Actions recorded: " + handlersInCurrentBatch.size());
                if (!isForceDrainContext) {
                    plugin.queueStats.addRunCount(handlersInCurrentBatch.size());
                }
            } else {
                if (conn != null && !conn.getAutoCommit() && !conn.isClosed()) conn.commit();
                Prism.debug("No actions processed in this batch run (queue might be empty or actions filtered).");
            }
        } catch (final SQLException e) {
            Prism.log("SQLException during batch insert: " + e.getMessage());
            if (!isForceDrainContext) {
                PrismDatabaseHandler.handleDatabaseException(e);
            }
            e.printStackTrace();
            if (conn != null) {
                try {
                    if (!conn.isClosed() && !conn.getAutoCommit()) {
                        Prism.log("Rolling back transaction due to error.");
                        conn.rollback();
                    }
                } catch (final SQLException ex) {
                    Prism.log("SQLException during rollback: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        } finally {
            if (generatedKeys != null) try { generatedKeys.close(); } catch (final SQLException ignored) {}
            if (psData != null) try { psData.close(); } catch (final SQLException ignored) {}
            if (psExtraData != null) try { psExtraData.close(); } catch (final SQLException ignored) {}
            if (conn != null) {
                try {
                    if (!conn.isClosed()) {
                        conn.setAutoCommit(true);
                    }
                } catch (final SQLException ex) { }
                try {
                    conn.close();
                } catch (final SQLException ignored) {}
            }
        }
    }

    @Override
    public void run() {
        if (!plugin.isEnabled()) {
            Prism.log("RecordingTask: Plugin is not enabled at start of run(), aborting.");
            return;
        }
        if (RecordingManager.failedDbConnectionCount > plugin.getConfig().getInt("prism.database.max-failures-before-wait", 5)) {
            Prism.log("Attempting to rebuild database connection pool due to " + RecordingManager.failedDbConnectionCount + " repeated failures.");
            PrismDatabaseHandler.rebuildPool();
            RecordingManager.failedDbConnectionCount = 0;
        }
        save();
        scheduleNextRecording();
    }

    protected int getTickDelayForNextBatch() {
        if (RecordingManager.failedDbConnectionCount > plugin.getConfig().getInt("prism.database.max-failures-before-wait", 5)) {
            int delayFactor = Math.min(RecordingManager.failedDbConnectionCount - plugin.getConfig().getInt("prism.database.max-failures-before-wait", 5), 30);
            return (delayFactor + 1) * 20;
        }
        int recorder_tick_delay = plugin.getConfig().getInt("prism.queue-empty-tick-delay", 3);
        return Math.max(recorder_tick_delay, 1);
    }

    protected void scheduleNextRecording() {
        if (!plugin.isEnabled()) {
            Prism.log("Plugin disabled, not scheduling new recording task.");
            return;
        }
        plugin.recordingTask = plugin.getServer().getScheduler()
                .runTaskLaterAsynchronously(plugin, new RecordingTask(plugin), getTickDelayForNextBatch());
    }
}
