package de.dasmarkt.manager;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.util.TimeUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final DasMarkt plugin;
    private File playerDataFile;
    private FileConfiguration playerDataConfig;
    private final Map<UUID, Double> dailyMoneyGain = new ConcurrentHashMap<>();
    private LocalDateTime nextResetTime;

    public PlayerDataManager(DasMarkt plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        loadPlayerData();
    }

    public void loadPlayerData() {
        dailyMoneyGain.clear();
        nextResetTime = TimeUtils.getNextResetTime();

        if (playerDataConfig.contains("players")) {
            for (String uuidStr : playerDataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    double gain = playerDataConfig.getDouble("players." + uuidStr + ".daily-gain", 0.0);
                    dailyMoneyGain.put(uuid, gain);
                } catch (IllegalArgumentException e) {
                    DasMarkt.getPluginLogger().severe("Ungültige UUID in playerdata.yml: " + uuidStr);
                }
            }
        }
        if (playerDataConfig.contains("reset-time")) {
            long lastResetMillis = playerDataConfig.getLong("reset-time");
            long nextResetMillis = TimeUtils.toMillis(nextResetTime);
            if (lastResetMillis > nextResetMillis) {
                dailyMoneyGain.clear();
                saveAllData();
            }
        } else {
            saveAllData();
        }
    }

    public void saveAllData() {
        playerDataConfig.set("reset-time", TimeUtils.toMillis(nextResetTime));
        for (Map.Entry<UUID, Double> entry : dailyMoneyGain.entrySet()) {
            playerDataConfig.set("players." + entry.getKey().toString() + ".daily-gain", entry.getValue());
        }
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
            DasMarkt.getPluginLogger().severe("Fehler beim Speichern der Spielerdaten!");
        }
    }

    public double getDailyGain(UUID playerUUID) {
        return dailyMoneyGain.getOrDefault(playerUUID, 0.0);
    }

    public void addDailyGain(UUID playerUUID, double amount) {
        dailyMoneyGain.put(playerUUID, getDailyGain(playerUUID) + amount);
        saveAllData();
    }

    public boolean hasReachedDailyLimit(UUID playerUUID) {
        return getDailyGain(playerUUID) >= plugin.getConfigManager().getMaxMoneyGain();
    }

    public void resetDailyGain(UUID playerUUID) {
        dailyMoneyGain.put(playerUUID, 0.0);
        saveAllData();
    }

    public LocalDateTime getResetTime() {
        return nextResetTime;
    }
}