package de.dasmarkt.manager;

import de.dasmarkt.DasMarkt;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {

    private final DasMarkt plugin;
    private FileConfiguration config;
    private File configFile;
    private String prefix;
    private String currency;
    private double maxMoneyGain;
    private List<String> disabledWorlds;

    public ConfigManager(DasMarkt plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        this.prefix = config.getString("prefix", "<gradient:#FF6840:#731701><b>DasMarkt</gradient> <gray>>> ");
        this.currency = config.getString("currency", "€");
        this.maxMoneyGain = config.getDouble("max-money-gain", 100000.0);
        this.disabledWorlds = config.getStringList("disabled-worlds");

        plugin.getMarketManager().loadMarketConfigs();
        plugin.getPlayerDataManager().loadPlayerData();
    }

    public void reloadAllConfigs() {
        loadConfig();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCurrency() {
        return currency;
    }

    public double getMaxMoneyGain() {
        return maxMoneyGain;
    }

    public List<String> getDisabledWorlds() {
        return disabledWorlds;
    }
}