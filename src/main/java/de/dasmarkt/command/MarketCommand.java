package de.dasmarkt.command;

import de.dasmarkt.DasMarkt;
import de.dasmarkt.gui.MarketGUI;
import de.dasmarkt.manager.MarketManager;
import de.dasmarkt.util.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final DasMarkt plugin;

    public MarketCommand(DasMarkt plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return true;
        }

        if (plugin.getDisabledWorlds().contains(player.getWorld().getName())) {
            MessageUtils.send(player, "<red>Du kannst den Markt in dieser Welt nicht benutzen.");
            return true;
        }

        if (args.length == 0) {
            if (!player.hasPermission("dasmarkt.use")) {
                MessageUtils.send(player, "<red>Du hast keine Berechtigung, den Markt zu nutzen.");
                return true;
            }

            if (!plugin.isMarketActive()) {
                MessageUtils.send(player, "<red>Der Markt ist derzeit deaktiviert.");
                return true;
            }

            new MarketGUI(plugin, player).openInventory();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload":
                handleReloadCommand(sender);
                break;
            case "deaktivieren":
                handleDeactivateCommand(sender);
                break;
            case "aktivieren":
                handleActivateCommand(sender);
                break;
            case "aktion":
                handleAktionCommand(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            default:
                MessageUtils.send(sender, "<red>Unbekannter Befehl. Benutze <gray>/markt</gray> um den Markt zu öffnen.");
                return true;
        }
        return true;
    }

    private void handleReloadCommand(@NotNull CommandSender sender) {
        if (!sender.hasPermission("dasmarkt.reload")) {
            MessageUtils.send(sender, "<red>Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return;
        }
        plugin.getConfigManager().reloadAllConfigs();
        MessageUtils.send(sender, "<green>Konfigurationen neu geladen.");
    }

    private void handleDeactivateCommand(@NotNull CommandSender sender) {
        if (!sender.hasPermission("dasmarkt.admin")) {
            MessageUtils.send(sender, "<red>Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return;
        }
        plugin.setMarketActive(false);
        MessageUtils.send(sender, "<green>Der Markt wurde deaktiviert.");
    }

    private void handleActivateCommand(@NotNull CommandSender sender) {
        if (!sender.hasPermission("dasmarkt.admin")) {
            MessageUtils.send(sender, "<red>Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return;
        }
        plugin.setMarketActive(true);
        MessageUtils.send(sender, "<green>Der Markt wurde aktiviert.");
    }

    private void handleAktionCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dasmarkt.admin")) {
            MessageUtils.send(sender, "<red>Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return;
        }
        if (!(sender instanceof Player player)) {
            MessageUtils.send(sender, "<red>Dieser Befehl kann nur von einem Spieler ausgeführt werden.");
            return;
        }
        if (args.length < 2) {
            MessageUtils.send(player, "<red>Nutzung: /markt aktion <set|remove> ...");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "set":
                handleSetCommand(player, args);
                break;
            case "remove":
                handleRemoveCommand(player, args);
                break;
            default:
                MessageUtils.send(player, "<red>Unbekannter Befehl. Benutze <gray>/markt aktion <set|remove></gray>.");
                break;
        }
    }

    private void handleSetCommand(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 4) {
            MessageUtils.send(player, "<red>Nutzung: /markt aktion set <Verkaufspreis> <Kaufpreis>");
            return;
        }

        double sellPrice, buyPrice;
        try {
            sellPrice = Double.parseDouble(args[2]);
            buyPrice = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            MessageUtils.send(player, "<red>Ungültige Preisangaben. Bitte gib nur Zahlen ein.");
            return;
        }

        if (sellPrice < 0 || buyPrice < 0) {
            MessageUtils.send(player, "<red>Preise können nicht negativ sein.");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            MessageUtils.send(player, "<red>Du musst ein Item in der Hand halten.");
            return;
        }

        plugin.getMarketManager().setMarketItem("aktionen", itemInHand, sellPrice, buyPrice);
        MessageUtils.send(player, String.format("<green>Item wurde in die Kategorie <gold>Aktionen</gold> mit den Preisen <green>%s</green> (verkaufen) und <red>%s</red> (kaufen) hinzugefügt.", sellPrice, buyPrice));
    }

    private void handleRemoveCommand(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 3) {
            MessageUtils.send(player, "<red>Nutzung: /markt aktion remove <Slot>");
            return;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtils.send(player, "<red>Ungültiger Slot. Bitte gib eine Zahl ein.");
            return;
        }

        if (plugin.getMarketManager().removeMarketItem("aktionen", slot)) {
            MessageUtils.send(player, String.format("<green>Item an Slot %d in der Kategorie <gold>Aktionen</gold> wurde entfernt.", slot));
        } else {
            MessageUtils.send(player, String.format("<red>Kein Item an Slot %d in der Kategorie <gold>Aktionen</gold> gefunden.", slot));
        }
    }

    private void handleResetCommand(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission("dasmarkt.admin")) {
            MessageUtils.send(sender, "<red>Du hast keine Berechtigung, diesen Befehl auszuführen.");
            return;
        }

        if (args.length < 2) {
            MessageUtils.send(sender, "<red>Nutzung: /markt reset <Spieler>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtils.send(sender, "<red>Dieser Spieler wurde nicht gefunden.");
            return;
        }

        plugin.getPlayerDataManager().resetDailyGain(target.getUniqueId());
        MessageUtils.send(sender, String.format("<green>Der tägliche Verkaufsgewinn von <yellow>%s<green> wurde zurückgesetzt.", target.getName()));
        MessageUtils.send(target, "<green>Dein täglicher Verkaufsgewinn wurde von einem Administrator zurückgesetzt.");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        MarketManager marketManager = plugin.getMarketManager();

        if (args.length == 1) {
            completions.add("reload");
            completions.add("aktivieren");
            completions.add("deaktivieren");
            completions.add("aktion");
            completions.add("reset");
            return completions.stream().filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "aktion":
                    completions.add("set");
                    completions.add("remove");
                    return completions.stream().filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT))).collect(Collectors.toList());
                case "reset":
                    return Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                            .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("aktion") && args[1].equalsIgnoreCase("remove")) {
            return marketManager.getMarketItems("aktionen").keySet().stream()
                    .map(String::valueOf)
                    .filter(s -> s.startsWith(args[2]))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}