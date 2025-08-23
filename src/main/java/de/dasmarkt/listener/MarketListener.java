package de.dasmarkt.listener;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.gui.FilterGUI;
import de.dasmarkt.gui.MarketGUI;
import de.dasmarkt.gui.MehrverkaufGUI;
import de.dasmarkt.manager.MarketManager;
import de.dasmarkt.manager.PlayerDataManager;
import de.dasmarkt.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MarketListener implements Listener {

    private final DasMarkt plugin;
    private static final Component GUI_TITLE = MessageUtils.convert("<red>Markt");
    private static final Component MEHRVERKAUF_TITLE = MessageUtils.convert("<red>Mehrverkauf");
    private static final Component FILTER_TITLE = MessageUtils.convert("<red>Filter");

    public MarketListener(DasMarkt plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (event.getView().title().equals(GUI_TITLE) || event.getView().title().equals(FILTER_TITLE)) {
            event.setCancelled(true);
        }

        if (clickedInventory != null && event.getView().title().equals(GUI_TITLE)) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) return;
            if (handleControlButtons(player, clickedItem, event.getSlot())) return;
            handleMarketItemInteraction(player, clickedItem, event.getClick().isRightClick(), event.isShiftClick());
        } else if (clickedInventory != null && event.getView().title().equals(FILTER_TITLE)) {
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType().isAir()) return;
            handleFilterMenuInteraction(player, clickedItem, event.getSlot());
        }
    }

    private boolean handleControlButtons(Player player, ItemStack item, int slot) {
        switch (slot) {
            case 0:
            case 9:
            case 18:
            case 27:
            case 36:
            case 45:
                String category = MarketManager.getCategoryBySlot(slot);
                if (category != null) {
                    new MarketGUI(plugin, player).openCategory(category, 0);
                    plugin.getMarketManager().setPlayerGuiState(player.getUniqueId(), category, 0);
                }
                return true;
            case 53:
                player.closeInventory();
                return true;
            case 48:
            case 50:
                if (item.getType() == Material.ARROW) {
                    MarketGUI gui = new MarketGUI(plugin, player);
                    ItemMeta meta = item.getItemMeta();
                    PersistentDataContainer data = meta.getPersistentDataContainer();
                    String currentCategory = data.getOrDefault(MarketGUI.CATEGORY_KEY, PersistentDataType.STRING, "aktionen");
                    int currentPage = data.getOrDefault(MarketGUI.PAGE_KEY, PersistentDataType.INTEGER, 0);

                    if (slot == 48) {
                        if (currentPage > 0) {
                            gui.openCategory(currentCategory, currentPage - 1);
                            plugin.getMarketManager().setPlayerGuiState(player.getUniqueId(), currentCategory, currentPage - 1);
                        }
                    } else {
                        gui.openCategory(currentCategory, currentPage + 1);
                        plugin.getMarketManager().setPlayerGuiState(player.getUniqueId(), currentCategory, currentPage + 1);
                    }
                }
                return true;
            case 49:
                ItemMeta meta = item.getItemMeta();
                if (meta != null && meta.getPersistentDataContainer().has(MarketGUI.FILTER_BUTTON_KEY)) {
                    new FilterGUI(plugin, player).openInventory();
                    return true;
                }
            case 8:
                new MehrverkaufGUI(plugin, player).openInventory();
                return true;
        }
        return false;
    }

    private void handleFilterMenuInteraction(Player player, ItemStack item, int slot) {
        MarketGUI.FilterType newFilter = null;

        switch (slot) {
            case 11:
                newFilter = MarketGUI.FilterType.ONE;
                break;
            case 12:
                newFilter = MarketGUI.FilterType.SIXTEEN;
                break;
            case 13:
                newFilter = MarketGUI.FilterType.THIRTY_TWO;
                break;
            case 14:
                newFilter = MarketGUI.FilterType.SIXTY_FOUR;
                break;
            case 15:
                newFilter = MarketGUI.FilterType.INVENTORY;
                break;
            case 18:
                player.closeInventory();
                String lastCategory = plugin.getMarketManager().getPlayerGuiState(player.getUniqueId()).getCategory();
                int lastPage = plugin.getMarketManager().getPlayerGuiState(player.getUniqueId()).getPage();
                new MarketGUI(plugin, player).openCategory(lastCategory, lastPage);
                return;
            case 26:
                player.closeInventory();
                return;
        }

        if (newFilter != null) {
            plugin.getMarketManager().setPlayerFilter(player.getUniqueId(), newFilter);
            player.closeInventory();
            MessageUtils.send(player, "<green>Filter wurde auf " + newFilter.getAmountString() + " geändert.");
            String lastCategory = plugin.getMarketManager().getPlayerGuiState(player.getUniqueId()).getCategory();
            int lastPage = plugin.getMarketManager().getPlayerGuiState(player.getUniqueId()).getPage();
            new MarketGUI(plugin, player).openCategory(lastCategory, lastPage);
        }
    }
    private void handleMarketItemInteraction(Player player, ItemStack item, boolean isRightClick, boolean isShiftClick) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer data = meta.getPersistentDataContainer();

        double sellPrice = data.getOrDefault(MarketGUI.SELL_PRICE_KEY, PersistentDataType.DOUBLE, 0.0);
        double buyPrice = data.getOrDefault(MarketGUI.BUY_PRICE_KEY, PersistentDataType.DOUBLE, 0.0);
        String category = data.get(MarketGUI.CATEGORY_KEY, PersistentDataType.STRING);
        Integer slot = data.get(MarketGUI.SLOT_KEY, PersistentDataType.INTEGER);

        if (category == null || slot == null) return;

        ItemStack marketItem = plugin.getMarketManager().getMarketItem(category, slot);
        if (marketItem == null) return;

        UUID playerUUID = player.getUniqueId();
        MarketGUI.FilterType filter = plugin.getMarketManager().getPlayerFilter(playerUUID);
        int amount;
        switch (filter) {
            case ONE:
                amount = 1;
                break;
            case SIXTEEN:
                amount = 16;
                break;
            case THIRTY_TWO:
                amount = 32;
                break;
            case SIXTY_FOUR:
                amount = 64;
                break;
            case INVENTORY:
            default:
                amount = isRightClick ? -1 : -2;
                break;
        }

        if (isRightClick) {
            handleBuy(player, marketItem, buyPrice, amount);
        } else {
            handleSell(player, marketItem, sellPrice, amount);
        }
    }

    private void handleBuy(Player player, ItemStack item, double price, int amount) {
        if (price <= 0) {
            MessageUtils.send(player, "<red>Dieses Item kann nicht gekauft werden.");
            return;
        }

        double totalCost;
        if (amount == -1) {
            int emptySlots = player.getInventory().getSize() - player.getInventory().getContents().length + (int) java.util.Arrays.stream(player.getInventory().getContents()).filter(Objects::isNull).count();
            int maxPossibleBuy = emptySlots * item.getMaxStackSize();
            int canAfford = (int) Math.floor(DasMarkt.getEconomy().getBalance(player) / price);
            amount = Math.min(maxPossibleBuy, canAfford);
            if (amount <= 0) {
                MessageUtils.send(player, "<red>Du hast nicht genug Geld oder keinen Platz im Inventar, um dieses Item zu kaufen.");
                return;
            }
            totalCost = amount * price;
        } else {
            totalCost = amount * price;
            if (DasMarkt.getEconomy().getBalance(player) < totalCost) {
                MessageUtils.send(player, String.format("<red>Du hast nicht genug Geld, um %d von diesem Item zu kaufen.", amount));
                return;
            }
        }

        if (DasMarkt.getEconomy().withdrawPlayer(player, totalCost).transactionSuccess()) {
            ItemStack toGive = item.clone();
            toGive.setAmount(amount);
            player.getInventory().addItem(toGive);
            MessageUtils.send(player, String.format("<green>Du hast %d %s<green> für %s%s gekauft.", amount, item.getType().name(), String.format("%.2f", totalCost), plugin.getConfigManager().getCurrency()));
        } else {
            MessageUtils.send(player, "<red>Fehler beim Kaufen des Items. Bitte versuche es erneut.");
        }
    }

    private void handleSell(Player player, ItemStack item, double price, int amount) {
        if (price <= 0) {
            MessageUtils.send(player, "<red>Dieses Item kann nicht verkauft werden.");
            return;
        }

        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();
        if (playerDataManager.hasReachedDailyLimit(player.getUniqueId())) {
            LocalDateTime resetTime = playerDataManager.getResetTime();
            MessageUtils.send(player, String.format("<red>Du hast dein tägliches Verkaufslimit erreicht. Du kannst wieder verkaufen am %s um %s Uhr.",
                    resetTime.atZone(ZoneId.of("Europe/Berlin")).toLocalDate().toString(),
                    resetTime.atZone(ZoneId.of("Europe/Berlin")).toLocalTime().toString()));
            return;
        }

        ItemStack playerItem = null;
        for (ItemStack invItem : player.getInventory().getContents()) {
            if (invItem != null && invItem.isSimilar(item)) {
                playerItem = invItem;
                break;
            }
        }

        if (playerItem == null) {
            MessageUtils.send(player, "<red>Du hast das Item nicht in deinem Inventar.");
            return;
        }

        double totalGain;
        int itemsToSell;

        if (amount == -2) {
            int totalItemsInInventory = 0;
            for (ItemStack invItem : player.getInventory().getContents()) {
                if (invItem != null && invItem.isSimilar(item)) {
                    totalItemsInInventory += invItem.getAmount();
                }
            }
            itemsToSell = totalItemsInInventory;
        } else {
            itemsToSell = amount;
            if (playerItem.getAmount() < itemsToSell) {
                MessageUtils.send(player, String.format("<red>Du hast nicht genug von diesem Item, um %d zu verkaufen.", itemsToSell));
                return;
            }
        }

        totalGain = itemsToSell * price;
        double currentGain = playerDataManager.getDailyGain(player.getUniqueId());
        double maxGain = plugin.getConfigManager().getMaxMoneyGain();
        if (currentGain + totalGain > maxGain) {
            totalGain = maxGain - currentGain;
            itemsToSell = (int) Math.floor(totalGain / price);
            if (itemsToSell <= 0) {
                MessageUtils.send(player, "<red>Du hast dein tägliches Verkaufslimit erreicht.");
                return;
            }
            MessageUtils.send(player, "<yellow>Dein Verkauf wurde auf dein verbleibendes Tageslimit angepasst.");
        }

        if (DasMarkt.getEconomy().depositPlayer(player, totalGain).transactionSuccess()) {
            ItemStack toRemove = item.clone();
            toRemove.setAmount(itemsToSell);
            player.getInventory().removeItem(toRemove);

            playerDataManager.addDailyGain(player.getUniqueId(), totalGain);
            MessageUtils.send(player, String.format("<green>Du hast %d %s<green> für %s%s verkauft.", itemsToSell, item.getType().name(), String.format("%.2f", totalGain), plugin.getConfigManager().getCurrency()));
        } else {
            MessageUtils.send(player, "<red>Fehler beim Verkaufen des Items. Bitte versuche es erneut.");
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        if (event.getView().title().equals(MEHRVERKAUF_TITLE)) {
            handleMehrverkaufClose(player, event.getInventory());
        }

        if (event.getView().title().equals(GUI_TITLE)) {
            plugin.getMarketManager().removePlayerGuiState(event.getPlayer().getUniqueId());
        }
    }

    private void handleMehrverkaufClose(Player player, Inventory inventory) {
        MarketManager marketManager = plugin.getMarketManager();
        PlayerDataManager playerDataManager = plugin.getPlayerDataManager();

        double totalGain = 0;
        List<ItemStack> shulkerBoxesToReturn = new ArrayList<>();

        for (ItemStack item : inventory.getContents()) {
            if (item == null || item.getType().isAir()) {
                continue;
            }

            if (playerDataManager.hasReachedDailyLimit(player.getUniqueId())) {
                LocalDateTime resetTime = playerDataManager.getResetTime();
                MessageUtils.send(player, String.format("<red>Du hast dein tägliches Verkaufslimit erreicht. Du kannst wieder verkaufen am %s um %s Uhr.",
                        resetTime.atZone(ZoneId.of("Europe/Berlin")).toLocalDate().toString(),
                        resetTime.atZone(ZoneId.of("Europe/Berlin")).toLocalTime().toString()));
                Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory), 1L);
                return;
            }

            if (item.getItemMeta() instanceof BlockStateMeta meta && meta.getBlockState() instanceof ShulkerBox shulkerBox) {
                shulkerBoxesToReturn.add(item);
                for (ItemStack shulkerItem : shulkerBox.getInventory().getContents()) {
                    if (shulkerItem == null || shulkerItem.getType().isAir()) continue;

                    Double sellPrice = marketManager.getSellPrice(shulkerItem);
                    if (sellPrice == null || sellPrice <= 0) {
                        MessageUtils.send(player, String.format("<red>Das Item %s in einer Shulkerbox kann nicht verkauft werden. Verkauf abgebrochen.", shulkerItem.getType().name()));
                        Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory), 1L);
                        return;
                    }
                    totalGain += sellPrice * shulkerItem.getAmount();
                }
            } else {
                Double sellPrice = marketManager.getSellPrice(item);
                if (sellPrice == null || sellPrice <= 0) {
                    MessageUtils.send(player, String.format("<red>Das Item %s kann nicht verkauft werden. Verkauf abgebrochen.", item.getType().name()));
                    Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory), 1L);
                    return;
                }
                totalGain += sellPrice * item.getAmount();
            }
        }

        if (totalGain <= 0) {
            MessageUtils.send(player, "<red>Keine verkaufbaren Gegenstände im Inventar gefunden.");
            return;
        }

        double currentGain = playerDataManager.getDailyGain(player.getUniqueId());
        double maxGain = plugin.getConfigManager().getMaxMoneyGain();
        if (currentGain + totalGain > maxGain) {
            totalGain = maxGain - currentGain;
            if (totalGain <= 0) {
                MessageUtils.send(player, "<red>Du hast dein tägliches Verkaufslimit erreicht.");
                return;
            }
            MessageUtils.send(player, "<yellow>Dein Verkauf wurde auf dein verbleibendes Tageslimit angepasst.");
        }

        if (DasMarkt.getEconomy().depositPlayer(player, totalGain).transactionSuccess()) {
            playerDataManager.addDailyGain(player.getUniqueId(), totalGain);
            MessageUtils.send(player, String.format("<green>Du hast Items für insgesamt %s%s verkauft.", String.format("%.2f", totalGain), plugin.getConfigManager().getCurrency()));

            for (ItemStack shulker : shulkerBoxesToReturn) {
                BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
                if (meta.getBlockState() instanceof ShulkerBox shulkerBox) {
                    shulkerBox.getInventory().clear();
                    meta.setBlockState(shulkerBox);
                    shulker.setItemMeta(meta);
                }
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItemNaturally(player.getLocation(), shulker);
                } else {
                    player.getInventory().addItem(shulker);
                }
            }
        } else {
            MessageUtils.send(player, "<red>Fehler beim Verkaufen der Items. Bitte versuche es erneut.");
        }
    }
}