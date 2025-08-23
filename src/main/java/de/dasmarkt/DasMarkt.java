package de.dasmarkt;

import de.dasmarkt.command.MarketCommand;
import de.dasmarkt.listener.MarketListener;
import de.dasmarkt.manager.ConfigManager;
import de.dasmarkt.manager.MarketManager;
import de.dasmarkt.manager.PlayerDataManager;
import de.dasmarkt.util.MessageUtils;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class DasMarkt extends JavaPlugin {

    private static DasMarkt instance;
    private static Economy economy = null;
    private static final Logger LOGGER = Bukkit.getLogger();

    private ConfigManager configManager;
    private MarketManager marketManager;
    private PlayerDataManager playerDataManager;
    private boolean marketActive = true;

    @Override
    public void onEnable() {
        instance = this;
        LOGGER.info("DasMarkt Plugin wird gestartet...");
        this.configManager = new ConfigManager(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.marketManager = new MarketManager(this);
        this.configManager.loadConfig();

        if (!setupEconomy()) {
            LOGGER.severe("Vault-Economy-API wurde nicht gefunden! Plugin wird deaktiviert.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("markt")).setExecutor(new MarketCommand(this));
        Objects.requireNonNull(getCommand("markt")).setTabCompleter(new MarketCommand(this));
        getServer().getPluginManager().registerEvents(new MarketListener(this), this);

        LOGGER.info("DasMarkt Plugin wurde erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        LOGGER.info("DasMarkt Plugin wird deaktiviert...");
        this.playerDataManager.saveAllData();
        instance = null;
        LOGGER.info("DasMarkt Plugin wurde erfolgreich deaktiviert.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public static DasMarkt getInstance() {
        return instance;
    }

    public static Economy getEconomy() {
        return economy;
    }

    public static Logger getPluginLogger() {
        return LOGGER;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MarketManager getMarketManager() {
        return marketManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public boolean isMarketActive() {
        return marketActive;
    }

    public void setMarketActive(boolean marketActive) {
        this.marketActive = marketActive;
    }

    public List<String> getDisabledWorlds() {
        return configManager.getDisabledWorlds();
    }
}