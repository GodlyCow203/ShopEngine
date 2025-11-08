package net.godlycow.org.shopengine.shop;

import java.util.HashMap;
import java.util.Map;

public class StockManager {
    private final Map<String, Integer> currentStock = new HashMap<>();
    private final Map<String, Integer> configStock = new HashMap<>();

    public void initializeStock(ShopItem item) {
        String key = item.getKey();
        int configuredStock = item.getStock();

        // Only initialize once per item
        if (!configStock.containsKey(key)) {
            configStock.put(key, configuredStock);
            // Set current stock to config value if not already tracked
            if (configuredStock >= 0) {
                currentStock.putIfAbsent(key, configuredStock);
            } else {
                // For unlimited stock (-1), store -1
                currentStock.put(key, -1);
            }
        }
    }

    public int getCurrentStock(ShopItem item) {
        return currentStock.getOrDefault(item.getKey(), item.getStock());
    }

    public void removeStock(ShopItem item, int amount) {
        String key = item.getKey();
        // Only remove if stock is limited (not -1)
        if (configStock.getOrDefault(key, -1) >= 0) {
            int stock = currentStock.getOrDefault(key, 0);
            currentStock.put(key, Math.max(0, stock - amount));
        }
    }

    /**
     * FIXED: Stock can exceed config value when selling
     */
    public void addStock(ShopItem item, int amount) {
        String key = item.getKey();
        // Only add if stock is limited (not -1)
        if (configStock.getOrDefault(key, -1) >= 0) {
            int current = currentStock.getOrDefault(key, 0);
            currentStock.put(key, current + amount);
        }
    }

    public boolean hasStock(ShopItem item, int requestedAmount) {
        int stock = getCurrentStock(item);
        return stock < 0 || stock >= requestedAmount; // -1 = unlimited
    }
}