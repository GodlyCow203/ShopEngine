package net.godlycow.org.shopengine.shop;

import java.util.HashMap;
import java.util.Map;

public class StockManager {
    private final Map<String, Integer> currentStock = new HashMap<>();
    private final Map<String, Integer> configStock = new HashMap<>();

    public void initializeStock(ShopItem item) {
        String key = item.getKey();
        int configuredStock = item.getStock();

        if (!configStock.containsKey(key)) {
            configStock.put(key, configuredStock);
            if (configuredStock >= 0) {
                currentStock.putIfAbsent(key, configuredStock);
            } else {
                currentStock.put(key, -1);
            }
        }
    }

    public int getCurrentStock(ShopItem item) {
        return currentStock.getOrDefault(item.getKey(), item.getStock());
    }

    public void removeStock(ShopItem item, int amount) {
        String key = item.getKey();
        if (configStock.getOrDefault(key, -1) >= 0) {
            int stock = currentStock.getOrDefault(key, 0);
            currentStock.put(key, Math.max(0, stock - amount));
        }
    }


    public void addStock(ShopItem item, int amount) {
        String key = item.getKey();
        if (configStock.getOrDefault(key, -1) >= 0) {
            int current = currentStock.getOrDefault(key, 0);
            currentStock.put(key, current + amount);
        }
    }

    public boolean hasStock(ShopItem item, int requestedAmount) {
        int stock = getCurrentStock(item);
        return stock < 0 || stock >= requestedAmount;
    }
}