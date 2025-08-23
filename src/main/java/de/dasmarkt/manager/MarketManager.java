package de.dasmarkt.manager;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.gui.MarketGUI;
import de.dasmarkt.util.ItemSerializer;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MarketManager {

    private final DasMarkt plugin;
    private final Map<String, Map<Integer, MarketItem>> marketItems = new ConcurrentHashMap<>();
    private final Map<UUID, MarketGUI.FilterType> playerFilters = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerGuiState> playerGuiStates = new ConcurrentHashMap<>();

    private static final Map<Integer, String> CATEGORY_SLOTS = new HashMap<>();
    static {
        CATEGORY_SLOTS.put(0, "aktionen");
        CATEGORY_SLOTS.put(9, "miner");
        CATEGORY_SLOTS.put(18, "holzfaeller");
        CATEGORY_SLOTS.put(27, "jaeger");
        CATEGORY_SLOTS.put(36, "graeber");
        CATEGORY_SLOTS.put(45, "farmer");
    }

    public MarketManager(DasMarkt plugin) {
        this.plugin = plugin;
        loadMarketConfigs();
    }

    public void loadMarketConfigs() {
        marketItems.clear();
        for (String category : getCategories()) {
            File marketFile = new File(plugin.getDataFolder(), category + ".yml");
            if (!marketFile.exists()) {
                plugin.saveResource(category + ".yml", false);
            }
            FileConfiguration marketConfig = YamlConfiguration.loadConfiguration(marketFile);
            Map<Integer, MarketItem> categoryItems = new HashMap<>();
            if (marketConfig.contains("items")) {
                for (String key : marketConfig.getConfigurationSection("items").getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        String itemData = marketConfig.getString("items." + key + ".item");
                        double sellPrice = marketConfig.getDouble("items." + key + ".sell-price", 0.0);
                        double buyPrice = marketConfig.getDouble("items." + key + ".buy-price", 0.0);

                        ItemStack item;
                        if (category.equals("aktionen")) {
                            item = ItemSerializer.itemStackFromBase64(itemData);
                        } else {
                            item = new ItemStack(Material.getMaterial(itemData.toUpperCase()));
                        }
                        if (item != null) {
                            categoryItems.put(slot, new MarketItem(item, sellPrice, buyPrice));
                        }
                    } catch (Exception e) {
                        DasMarkt.getPluginLogger().warning("Fehler beim Laden von Item im Markt-Config: " + category + ".yml -> Slot " + key);
                        e.printStackTrace();
                    }
                }
            }
            marketItems.put(category, categoryItems);
        }
    }

    public void setMarketItem(String category, ItemStack item, double sellPrice, double buyPrice) {
        if (!getCategories().contains(category.toLowerCase())) {
            DasMarkt.getPluginLogger().severe("Ungültige Kategorie: " + category);
            return;
        }

        File marketFile = new File(plugin.getDataFolder(), category + ".yml");
        FileConfiguration marketConfig = YamlConfiguration.loadConfiguration(marketFile);

        int nextSlot = 0;
        if (marketConfig.contains("items")) {
            Set<Integer> existingKeys = new HashSet<>();
            for(String key : marketConfig.getConfigurationSection("items").getKeys(false)) {
                try {
                    existingKeys.add(Integer.parseInt(key));
                } catch (NumberFormatException e) {
                }
            }
            if(!existingKeys.isEmpty()) {
                nextSlot = Collections.max(existingKeys) + 1;
            }
        }

        String itemData;
        if (category.equalsIgnoreCase("aktionen")) {
            itemData = ItemSerializer.itemStackToBase64(item);
        } else {
            itemData = item.getType().name();
        }

        marketConfig.set("items." + nextSlot + ".item", itemData);
        marketConfig.set("items." + nextSlot + ".sell-price", sellPrice);
        marketConfig.set("items." + nextSlot + ".buy-price", buyPrice);

        try {
            marketConfig.save(marketFile);
            loadMarketConfigs();
        } catch (IOException e) {
            e.printStackTrace();
            DasMarkt.getPluginLogger().severe("Fehler beim Speichern der Markt-Konfiguration.");
        }
    }

    public boolean removeMarketItem(String category, int slot) {
        if (!getCategories().contains(category.toLowerCase())) return false;

        File marketFile = new File(plugin.getDataFolder(), category + ".yml");
        FileConfiguration marketConfig = YamlConfiguration.loadConfiguration(marketFile);

        for (String key : marketConfig.getConfigurationSection("items").getKeys(false)) {
            if (key.equalsIgnoreCase(String.valueOf(slot))) {
                marketConfig.set("items." + key, null);
                try {
                    marketConfig.save(marketFile);
                    loadMarketConfigs();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    DasMarkt.getPluginLogger().severe("Fehler beim Speichern der Markt-Konfiguration.");
                }
            }
        }
        return false;
    }

    public boolean isSellable(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return false;
        return marketItems.values().stream()
                .flatMap(map -> map.values().stream())
                .anyMatch(marketItem -> marketItem.getItemStack().getType() == itemStack.getType() && marketItem.getSellPrice() > 0);
    }

    public Double getSellPrice(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return null;
        for (Map<Integer, MarketItem> categoryItems : marketItems.values()) {
            for (MarketItem marketItem : categoryItems.values()) {
                if (marketItem.getItemStack().isSimilar(itemStack) && marketItem.getSellPrice() > 0) {
                    return marketItem.getSellPrice();
                }
            }
        }
        return null;
    }

    public Map<Integer, MarketItem> getMarketItems(String category) {
        return marketItems.getOrDefault(category, new HashMap<>());
    }

    public ItemStack getMarketItem(String category, int slot) {
        MarketItem item = marketItems.getOrDefault(category, new HashMap<>()).get(slot);
        return item != null ? item.getItemStack() : null;
    }

    public Set<String> getCategories() {
        return CATEGORY_SLOTS.values().stream().collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    public static String getCategoryBySlot(int slot) {
        return CATEGORY_SLOTS.get(slot);
    }

    public void setPlayerFilter(UUID playerUUID, MarketGUI.FilterType filter) {
        playerFilters.put(playerUUID, filter);
    }

    public MarketGUI.FilterType getPlayerFilter(UUID playerUUID) {
        return playerFilters.getOrDefault(playerUUID, MarketGUI.FilterType.ONE);
    }

    public void removePlayerFilter(UUID playerUUID) {
        playerFilters.remove(playerUUID);
    }

    public void setPlayerGuiState(UUID playerUUID, String category, int page) {
        playerGuiStates.put(playerUUID, new PlayerGuiState(category, page));
    }

    public PlayerGuiState getPlayerGuiState(UUID playerUUID) {
        return playerGuiStates.getOrDefault(playerUUID, new PlayerGuiState("aktionen", 0));
    }

    public void removePlayerGuiState(UUID playerUUID) {
        playerGuiStates.remove(playerUUID);
    }

    // Inner class for market item data
    public static class MarketItem {
        private final ItemStack itemStack;
        private final double sellPrice;
        private final double buyPrice;

        public MarketItem(ItemStack itemStack, double sellPrice, double buyPrice) {
            this.itemStack = itemStack;
            this.sellPrice = sellPrice;
            this.buyPrice = buyPrice;
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        public double getSellPrice() {
            return sellPrice;
        }

        public double getBuyPrice() {
            return buyPrice;
        }
    }

    public static class PlayerGuiState {
        private final String category;
        private final int page;

        public PlayerGuiState(String category, int page) {
            this.category = category;
            this.page = page;
        }

        public String getCategory() {
            return category;
        }

        public int getPage() {
            return page;
        }
    }
}