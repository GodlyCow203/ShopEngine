package net.godlycow.org.shopengine.config;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.shop.ShopItem;
import net.godlycow.org.shopengine.shop.ShopSection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {
    private final ShopEngine plugin;
    private FileConfiguration config;

    private static final String DYNAMIC_PRICING_ENABLED = "dynamic-pricing.enabled";
    private static final String DYNAMIC_PRICING_FLUCTUATION = "dynamic-pricing.daily-fluctuation-percent";
    private static final String DYNAMIC_PRICING_SAVE_INTERVAL = "dynamic-pricing.save-interval-minutes";
    private static final String SHOP_SECTIONS = "shop-sections";
    private static final String MESSAGES_PREFIX = "messages.prefix";

    public ConfigManager(ShopEngine plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public boolean isDynamicPricingEnabled() {
        return config.getBoolean(DYNAMIC_PRICING_ENABLED, false);
    }

    public double getDynamicFluctuationPercent() {
        return config.getDouble(DYNAMIC_PRICING_FLUCTUATION, 5.0);
    }

    public long getSaveIntervalMinutes() {
        return config.getLong(DYNAMIC_PRICING_SAVE_INTERVAL, 10);
    }

    public String getPrefix() {
        return config.getString(MESSAGES_PREFIX, "<dark_gray>[<gradient:#00ff00:#00ffff>ShopEngine</gradient>]</dark_gray>");
    }

    public Map<String, ShopSection> loadShopSections() {
        Map<String, ShopSection> sections = new HashMap<>();
        ConfigurationSection sectionConfig = config.getConfigurationSection(SHOP_SECTIONS);

        if (sectionConfig == null) {
            plugin.getLogger().warning("No shop sections found in config!");
            return sections;
        }

        for (String sectionKey : sectionConfig.getKeys(false)) {
            ConfigurationSection sectionData = sectionConfig.getConfigurationSection(sectionKey);
            if (sectionData == null) continue;

            try {
                ShopSection section = loadSection(sectionKey, sectionData);
                sections.put(sectionKey, section);
                plugin.getLogger().info("Loaded shop section: " + sectionKey);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load section '" + sectionKey + "': " + e.getMessage());
            }
        }

        return sections;
    }

    private ShopSection loadSection(String key, ConfigurationSection sectionData) {
        String displayName = sectionData.getString("display-name", key);
        Material icon = Material.matchMaterial(sectionData.getString("icon", "CHEST"));
        int slot = sectionData.getInt("slot", 0);
        int page = sectionData.getInt("page", 1) - 1;
        List<ShopItem> items = new ArrayList<>();

        ConfigurationSection itemsConfig = sectionData.getConfigurationSection("items");
        if (itemsConfig != null) {
            for (String itemKey : itemsConfig.getKeys(false)) {
                ConfigurationSection itemData = itemsConfig.getConfigurationSection(itemKey);
                if (itemData == null) continue;

                try {
                    ShopItem shopItem = loadItem(itemKey, itemData);
                    items.add(shopItem);
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to load item '" + itemKey + "' in section '" + key + "': " + e.getMessage());
                }
            }
        }

        return new ShopSection(key, displayName, icon, slot, page, items);
    }

    private ShopItem loadItem(String key, ConfigurationSection itemData) {
        Material material = Material.matchMaterial(itemData.getString("material"));
        if (material == null) {
            throw new IllegalArgumentException("Invalid material: " + itemData.getString("material"));
        }

        String displayName = itemData.getString("display-name", "");
        int slot = itemData.getInt("slot");
        int page = itemData.getInt("page", 1) - 1;
        int amount = itemData.getInt("amount", 1);
        double buyPrice = itemData.getDouble("buy-price", -1);
        double sellPrice = itemData.getDouble("sell-price", -1);
        int stock = itemData.getInt("stock", -1);

        java.util.List<String> lore = itemData.getStringList("lore");

        int customModelData = itemData.getInt("custom-model-data", -1);

        return new ShopItem(
                key,
                material,
                displayName,
                lore,
                slot,
                page,
                amount,
                buyPrice,
                sellPrice,
                stock,
                customModelData
        );
    }

    public Set<String> getSectionKeys() {
        ConfigurationSection sectionConfig = config.getConfigurationSection(SHOP_SECTIONS);
        return sectionConfig != null ? sectionConfig.getKeys(false) : java.util.Collections.emptySet();
    }

    public void addItemToSection(String sectionKey, String itemKey, ShopItem item) {
        String path = SHOP_SECTIONS + "." + sectionKey + ".items." + itemKey;
        config.set(path + ".material", item.getMaterial().name());
        config.set(path + ".display-name", item.getDisplayName());
        config.set(path + ".slot", item.getSlot());
        config.set(path + ".page", item.getPage() + 1);
        config.set(path + ".amount", item.getAmount());
        config.set(path + ".buy-price", item.getBuyPrice());
        config.set(path + ".sell-price", item.getSellPrice());
        config.set(path + ".stock", item.getStock());
        config.set(path + ".custom-model-data", item.getCustomModelData());
        config.set(path + ".lore", item.getLore());
        plugin.saveConfig();
    }
}