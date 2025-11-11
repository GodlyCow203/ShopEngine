package net.godlycow.org.shopengine.listeners;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.updaters.spigotmc.SpigotMCUpdateChecker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateNotifyListener implements Listener {

    private final SpigotMCUpdateChecker checker;

    public UpdateNotifyListener(SpigotMCUpdateChecker checker) {
        this.checker = checker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!player.isOp()) return;
        if (!checker.isUpdateAvailable()) return;

        player.sendMessage(checker.getUpdateMessage());
    }
}
