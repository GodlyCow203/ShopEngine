package net.godlycow.org.shopengine.utils;

import net.godlycow.org.shopengine.ShopEngine;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class SignInputManager implements Listener {
    private final ShopEngine plugin;
    private final Map<UUID, Consumer<String>> waitingPlayers = new HashMap<>();

    public SignInputManager(ShopEngine plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openSearch(Player player, Consumer<String> callback) {
        waitingPlayers.put(player.getUniqueId(), callback);

        player.sendMessage(plugin.getMiniMessage().deserialize(
                plugin.getMessageManager().getMessage("shop.search-prompt")
        ));
        player.sendMessage(plugin.getMiniMessage().deserialize(
                plugin.getMessageManager().getMessage("shop.search-cancel")
        ));
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = waitingPlayers.get(player.getUniqueId());
        if (callback == null) return;
        event.setCancelled(true);
        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        waitingPlayers.remove(player.getUniqueId());
        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    plugin.getMessageManager().getMessage("shop.search-cancelled")
            ));
            return;
        }
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            callback.accept(message);
        });
    }

    public void cancelSearch(UUID playerId) {
        waitingPlayers.remove(playerId);
    }
}