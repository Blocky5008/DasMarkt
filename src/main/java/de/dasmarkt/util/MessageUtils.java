package de.dasmarkt.util;

import de.dasmarkt.DasMarkt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final DasMarkt plugin = DasMarkt.getInstance();

    public static void send(CommandSender sender, String message) {
        String prefixedMessage = plugin.getConfigManager().getPrefix() + message;
        Component component = miniMessage.deserialize(prefixedMessage);
        sender.sendMessage(component);
    }

    public static Component convert(String message) {
        return miniMessage.deserialize(message);
    }
}