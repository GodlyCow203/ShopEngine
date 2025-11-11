package net.godlycow.org.shopengine.shop;

import net.godlycow.org.shopengine.ShopEngine;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DynamicPricingManager {
    private final ShopEngine plugin;
    private final File pricesFile;
    private final Map<String, Double> dynamicMultipliers = new HashMap<>();
    private long lastUpdate;

    public DynamicPricingManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.pricesFile = new File(plugin.getDataFolder(), "dynamic-prices.yml");
        loadPrices();
        startSaveTask();
    }

    public double getDynamicPrice(ShopItem item, boolean isBuy) {
        if (!plugin.getConfigManager().isDynamicPricingEnabled()) {
            return isBuy ? item.getBuyPrice() : item.getSellPrice();
        }

        String key = item.getKey();
        double basePrice = isBuy ? item.getBuyPrice() : item.getSellPrice();
        double multiplier = dynamicMultipliers.getOrDefault(key, 1.0);

        return basePrice * multiplier;
    }

    public void recordTransaction(ShopItem item, boolean isBuy, int amount) {
        if (!plugin.getConfigManager().isDynamicPricingEnabled()) return;

        String key = item.getKey();
        double currentMultiplier = dynamicMultipliers.getOrDefault(key, 1.0);
        double fluctuation = plugin.getConfigManager().getDynamicFluctuationPercent() / 100.0;

        double change = isBuy ? fluctuation * amount * 0.01 : -fluctuation * amount * 0.01;
        double newMultiplier = Math.max(0.1, Math.min(3.0, currentMultiplier + change));

        dynamicMultipliers.put(key, newMultiplier);

        plugin.getLogger().fine(String.format("Price for %s changed from %.2fx to %.2fx (%s %d)",
                key,
                currentMultiplier,
                newMultiplier,
                isBuy ? "bought" : "sold",
                amount));
    }

    public void dailyUpdate() {
        if (!plugin.getConfigManager().isDynamicPricingEnabled()) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdate < 86400000) return; // 24 hours

        double fluctuation = plugin.getConfigManager().getDynamicFluctuationPercent() / 100.0;
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (String key : dynamicMultipliers.keySet()) {
            double current = dynamicMultipliers.get(key);
            double change = (random.nextDouble() - 0.5) * fluctuation * 2;
            double newMultiplier = Math.max(0.1, Math.min(3.0, current + change));
            dynamicMultipliers.put(key, newMultiplier);
        }

        lastUpdate = currentTime;
        plugin.getLogger().info("Daily dynamic price update completed!");
    }

    public void loadPrices() {
        if (!pricesFile.exists()) {
            plugin.getLogger().info("No dynamic prices file found, creating new one...");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(pricesFile);
        for (String key : config.getKeys(false)) {
            dynamicMultipliers.put(key, config.getDouble(key, 1.0));
        }
        lastUpdate = config.getLong("last-update", System.currentTimeMillis());
    }

    public void savePrices() {
        FileConfiguration config = new YamlConfiguration();

        for (Map.Entry<String, Double> entry : dynamicMultipliers.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }
        config.set("last-update", lastUpdate);

        try {
            config.save(pricesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save dynamic prices: " + e.getMessage());
        }
    }

    private void startSaveTask() {
        long interval = plugin.getConfigManager().getSaveIntervalMinutes() * 1200L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::savePrices, interval, interval);

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::dailyUpdate, 72000L, 72000L);
    }

    public Map<String, Double> getMultipliers() {
        return new HashMap<>(dynamicMultipliers);
    }

    public void resetPrice(String itemKey) {
        dynamicMultipliers.put(itemKey, 1.0);
    }
}