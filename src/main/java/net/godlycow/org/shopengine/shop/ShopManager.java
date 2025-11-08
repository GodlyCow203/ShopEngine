package net.godlycow.org.shopengine.shop;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.gui.ShopGUI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private final ShopEngine plugin;
    private final Map<String, ShopSection> sections = new HashMap<>();

    public ShopManager(ShopEngine plugin) {
        this.plugin = plugin;
        loadShop();
    }

    public void loadShop() {
        sections.clear();
        var sectionMap = plugin.getSectionManager().getSections();

        for (Map.Entry<String, ShopSection> entry : sectionMap.entrySet()) {
            String sectionKey = entry.getKey();
            ShopSection section = entry.getValue();

            // Load ALL items for this section
            List<ShopItem> items = plugin.getItemManager().getItemsForSection(sectionKey);
            section.getItems().clear();
            section.getItems().addAll(items); // Add all items to list

            sections.put(sectionKey, section);
        }

        plugin.getLogger().info("Loaded " + sections.size() + " shop sections with items!");
    }

    public void openShop(Player player) {
        new ShopGUI(plugin, player, null, 0).open();
    }

    public void openSection(Player player, String sectionKey) {
        ShopSection section = sections.get(sectionKey);
        if (section != null) {
            new ShopGUI(plugin, player, section, 0).open();
        }
    }

    public Map<String, ShopSection> getSections() {
        return new HashMap<>(sections);
    }

    // FIXED: Changed from .values() to direct addAll since getItems() returns List
    public List<ShopItem> getAllItems() {
        List<ShopItem> allItems = new ArrayList<>();
        for (ShopSection section : sections.values()) {
            allItems.addAll(section.getItems()); // Directly add the List
        }
        return allItems;
    }

    public ShopSection getSection(String key) {
        return sections.get(key);
    }

    public boolean hasSection(String key) {
        return sections.containsKey(key);
    }
}