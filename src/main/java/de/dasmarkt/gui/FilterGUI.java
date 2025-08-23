package de.dasmarkt.gui;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Collections;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;

public class FilterGUI {

    private final DasMarkt plugin;
    private final Player player;
    private static final Component GUI_TITLE = MessageUtils.convert("<red>Filter");

    public FilterGUI(DasMarkt plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openInventory() {
        Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);

        for (int i = 0; i < inv.getSize(); i++) {
            if (i > 9 && i < 17) continue;
            inv.setItem(i, createGuiItem(Material.GRAY_STAINED_GLASS_PANE, "<reset>"));
        }

        inv.setItem(18, createGuiItem(Material.ARROW, "<gray>Zurück"));
        inv.setItem(26, createGuiItem(Material.BARRIER, "<red>Schließen"));
        inv.setItem(11, createFilterOption(MarketGUI.FilterType.ONE, Material.HOPPER));
        inv.setItem(12, createFilterOption(MarketGUI.FilterType.SIXTEEN, Material.HOPPER, 16));
        inv.setItem(13, createFilterOption(MarketGUI.FilterType.THIRTY_TWO, Material.HOPPER, 32));
        inv.setItem(14, createFilterOption(MarketGUI.FilterType.SIXTY_FOUR, Material.HOPPER, 64));
        inv.setItem(15, createFilterOption(MarketGUI.FilterType.INVENTORY, Material.GOLDEN_HORSE_ARMOR));

        player.openInventory(inv);
    }

    private ItemStack createFilterOption(MarketGUI.FilterType filterType, Material material) {
        return createFilterOption(filterType, material, 1);
    }

    private ItemStack createFilterOption(MarketGUI.FilterType filterType, Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtils.convert("<gradient:gray:white>" + filterType.getAmountString() + "</gradient> <gradient:#FF6840:#731701>Kauf/Verkauf</gradient>"));
        meta.lore(Collections.singletonList(MessageUtils.convert("<gray>Klicke um den Filter zu ändern")));

        if (plugin.getMarketManager().getPlayerFilter(player.getUniqueId()) == filterType) {
            meta.setEnchantmentGlintOverride(true);
        }

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createGuiItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MessageUtils.convert(name));
        item.setItemMeta(meta);
        return item;
    }
}