package de.dasmarkt.gui;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.manager.MarketManager;
import de.dasmarkt.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MarketGUI {

    private final DasMarkt plugin;
    private final Player player;
    private static final Component GUI_TITLE = MessageUtils.convert("<red>Markt");

    public static final NamespacedKey CATEGORY_KEY = new NamespacedKey(DasMarkt.getInstance(), "category");
    public static final NamespacedKey PAGE_KEY = new NamespacedKey(DasMarkt.getInstance(), "page");
    public static final NamespacedKey SELL_PRICE_KEY = new NamespacedKey(DasMarkt.getInstance(), "sell-price");
    public static final NamespacedKey BUY_PRICE_KEY = new NamespacedKey(DasMarkt.getInstance(), "buy-price");
    public static final NamespacedKey SLOT_KEY = new NamespacedKey(DasMarkt.getInstance(), "slot");
    public static final NamespacedKey MORE_SALE_KEY = new NamespacedKey(DasMarkt.getInstance(), "more-sale-button");
    public static final NamespacedKey FILTER_BUTTON_KEY = new NamespacedKey(DasMarkt.getInstance(), "filter-button");

    public enum FilterType {
        ONE("1x"),
        SIXTEEN("16x"),
        THIRTY_TWO("32x"),
        SIXTY_FOUR("64x"),
        INVENTORY("Inventar");

        private final String amountString;
        FilterType(String amountString) {
            this.amountString = amountString;
        }
        public String getAmountString() {
            return amountString;
        }
    }

    public MarketGUI(DasMarkt plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openInventory() {
        openCategory("aktionen", 0);
    }

    public void openCategory(String category, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        for (int i : new int[]{1, 2, 3, 4, 5, 6, 7, 10, 17, 19, 26, 28, 35, 37, 44, 46, 47, 51, 52}) {
            inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "<reset>"));
        }

        inv.setItem(0, createGuiItem(Material.NETHER_STAR, "<gradient:#FF6840:#731701><b>Aktionen</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("aktionen")));
        inv.setItem(9, createGuiItem(Material.DIAMOND_PICKAXE, "<gradient:#FF6840:#731701><b>Miner</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("miner")));
        inv.setItem(18, createGuiItem(Material.DIAMOND_AXE, "<gradient:#FF6840:#731701><b>Holzfäller</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("holzfaeller")));
        inv.setItem(27, createGuiItem(Material.DIAMOND_SWORD, "<gradient:#FF6840:#731701><b>Jäger</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("jaeger")));
        inv.setItem(36, createGuiItem(Material.DIAMOND_SHOVEL, "<gradient:#FF6840:#731701><b>Gräber</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("graeber")));
        inv.setItem(45, createGuiItem(Material.DIAMOND_HOE, "<gradient:#FF6840:#731701><b>Farmer</gradient>", "<gradient:gray:white>Klicke um die Seite zu wechseln</gradient>", category.equals("farmer")));

        ItemStack mehrverkaufItem = createGuiItem(Material.CHEST, "<gradient:#FF6840:#731701><b>Mehrverkauf</gradient>", "<gradient:gray:white>Klicke um den Mehrverkauf zu öffnen</gradient>");
        ItemMeta mehrverkaufMeta = mehrverkaufItem.getItemMeta();
        mehrverkaufMeta.getPersistentDataContainer().set(MORE_SALE_KEY, PersistentDataType.STRING, "mehrverkauf-button");
        mehrverkaufItem.setItemMeta(mehrverkaufMeta);
        inv.setItem(8, mehrverkaufItem);

        MarketManager marketManager = plugin.getMarketManager();
        List<Map.Entry<Integer, MarketManager.MarketItem>> items = new ArrayList<>(marketManager.getMarketItems(category).entrySet());
        Collections.sort(items, Map.Entry.comparingByKey());

        int startSlot = 11;
        int maxItemsPerPage = 24;
        int startItemIndex = page * maxItemsPerPage;

        for (int i = 0; i < maxItemsPerPage; i++) {
            int itemIndex = startItemIndex + i;
            if (itemIndex >= items.size()) break;

            int invSlot;
            if (i < 6) invSlot = startSlot + i;
            else if (i < 12) invSlot = startSlot + i + 3;
            else if (i < 18) invSlot = startSlot + i + 6;
            else invSlot = startSlot + i + 9;

            Map.Entry<Integer, MarketManager.MarketItem> entry = items.get(itemIndex);
            inv.setItem(invSlot, createMarketItem(entry.getValue(), entry.getKey(), category));
        }

        boolean hasPreviousPage = page > 0;
        boolean hasNextPage = items.size() > (page + 1) * maxItemsPerPage;

        ItemStack backItem = createNavigationItem(Material.ARROW, "<gray>Zurück", category, page);
        ItemStack nextItem = createNavigationItem(Material.ARROW, "<gray>Weiter", category, page);

        inv.setItem(48, hasPreviousPage ? backItem : createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "<reset>"));
        inv.setItem(50, hasNextPage ? nextItem : createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "<reset>"));
        inv.setItem(53, createGuiItem(Material.BARRIER, "<red>Schließen"));
        FilterType currentFilter = marketManager.getPlayerFilter(player.getUniqueId());
        if (currentFilter == null) {
            currentFilter = FilterType.ONE;
            marketManager.setPlayerFilter(player.getUniqueId(), currentFilter);
        }
        inv.setItem(49, createFilterItem(currentFilter));

        player.openInventory(inv);
        plugin.getMarketManager().setPlayerGuiState(player.getUniqueId(), category, page);
    }

    public void openCategory(String category) {
        openCategory(category, 0);
    }

    private ItemStack createGuiItem(Material material, String name, String lore, boolean isSelected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtils.convert(name));
        if (lore != null) meta.lore(Collections.singletonList(MessageUtils.convert(lore)));
        if (isSelected) meta.setEnchantmentGlintOverride(true);
        item.setItemMeta(meta);
        return item;
    }
    private ItemStack createGuiItem(Material material, String name) {
        return createGuiItem(material, name, null, false);
    }
    private ItemStack createGuiItem(Material material, String name, String lore) {
        return createGuiItem(material, name, lore, false);
    }

    private ItemStack createMarketItem(MarketManager.MarketItem marketItem, int slot, String category) {
        ItemStack item = marketItem.getItemStack().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>();
        lore.add("");

        if (marketItem.getSellPrice() > 0) {
            lore.add("<gradient:gray:white>Links Klick Verkaufen -</gradient> <green>" + marketItem.getSellPrice() + plugin.getConfigManager().getCurrency());
        } else {
            lore.add("<gradient:gray:white>Links Klick Verkaufen -</gradient> <red>Nicht möglich");
        }

        if (marketItem.getBuyPrice() > 0) {
            lore.add("<gradient:gray:white>Rechts Klick Kaufen -</gradient> <green>" + marketItem.getBuyPrice() + plugin.getConfigManager().getCurrency());
        } else {
            lore.add("<gradient:gray:white>Rechts Klick Kaufen -</gradient> <red>Nicht möglich");
        }

        meta.lore(lore.stream().map(MessageUtils::convert).collect(Collectors.toList()));

        meta.getPersistentDataContainer().set(SELL_PRICE_KEY, PersistentDataType.DOUBLE, marketItem.getSellPrice());
        meta.getPersistentDataContainer().set(BUY_PRICE_KEY, PersistentDataType.DOUBLE, marketItem.getBuyPrice());
        meta.getPersistentDataContainer().set(CATEGORY_KEY, PersistentDataType.STRING, category);
        meta.getPersistentDataContainer().set(SLOT_KEY, PersistentDataType.INTEGER, slot);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNavigationItem(Material material, String name, String category, int page) {
        ItemStack item = createGuiItem(material, name);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(CATEGORY_KEY, PersistentDataType.STRING, category);
        meta.getPersistentDataContainer().set(PAGE_KEY, PersistentDataType.INTEGER, page);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFilterItem(FilterType filter) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtils.convert("<gradient:#FF6840:#731701><b>Filter</gradient> <gradient:gray:white><b>" + filter.getAmountString()));

        meta.lore(Collections.singletonList(MessageUtils.convert("<gradient:gray:white>Klicke um den Filter zu ändern</gradient>")));

        meta.getPersistentDataContainer().set(FILTER_BUTTON_KEY, PersistentDataType.STRING, "filter-button");
        item.setItemMeta(meta);
        return item;
    }
}