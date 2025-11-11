package net.godlycow.org.shopengine.shop;

import org.bukkit.Material;
import java.util.List;

public class ShopSection {
    private final String key;
    private final String displayName;
    private final Material icon;
    private final int slot;
    private final int page;
    private final List<ShopItem> items;

    public ShopSection(String key, String displayName, Material icon, int slot, int page, List<ShopItem> items) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.slot = slot;
        this.page = page;
        this.items = items;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public int getSlot() {
        return slot;
    }

    public int getPage() {
        return page;
    }

    public List<ShopItem> getItems() {
        return items;
    }

    public ShopItem getItem(int slot, int page) {
        for (ShopItem item : items) {
            if (item.getSlot() == slot && item.getPage() == page) {
                return item;
            }
        }
        return null;
    }
}