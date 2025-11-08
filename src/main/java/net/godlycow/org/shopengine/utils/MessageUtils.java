package net.godlycow.org.shopengine.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageUtils {
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static Component format(String message) {
        if (message == null) return Component.empty();
        return miniMessage.deserialize(message);
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        Component component = miniMessage.deserialize(message);
        if (sender instanceof Player player) {
            player.sendMessage(component);
        } else {
            sender.sendMessage(miniMessage.stripTags(message));
        }
    }

    public static void sendWithPrefix(CommandSender sender, String prefix, String message) {
        if (message == null) return;
        send(sender, prefix + " " + message);
    }
}