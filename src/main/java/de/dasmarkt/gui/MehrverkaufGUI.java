package de.dasmarkt.gui;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.util.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class MehrverkaufGUI {

    private final DasMarkt plugin;
    private final Player player;
    private static final Component GUI_TITLE = MessageUtils.convert("<red>Mehrverkauf");

    public MehrverkaufGUI(DasMarkt plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    public void openInventory() {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
        player.openInventory(inv);
        MessageUtils.send(player, "<green>Du hast das <red>Mehrverkauf<green>-Menü geöffnet. Lege hier Items zum Verkaufen ab.");
    }
}