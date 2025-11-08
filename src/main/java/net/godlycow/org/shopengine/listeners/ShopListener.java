package net.godlycow.org.shopengine.listeners;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.gui.HistoryGUI;
import net.godlycow.org.shopengine.gui.SearchResultsGUI;
import net.godlycow.org.shopengine.gui.ShopGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.InventoryHolder;

public class ShopListener implements Listener {
    private final ShopEngine plugin;

    public ShopListener(ShopEngine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof ShopGUI gui) {
            gui.handleClick(event);
        } else if (holder instanceof HistoryGUI historyGUI) {
            historyGUI.handleClick(event);
        } else if (holder instanceof SearchResultsGUI searchGUI) {
            searchGUI.handleClick(event);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    }
}