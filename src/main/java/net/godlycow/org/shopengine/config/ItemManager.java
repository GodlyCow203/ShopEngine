package net.godlycow.org.shopengine.config;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.shop.ShopError;
import net.godlycow.org.shopengine.shop.ShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class ItemManager {
    private final ShopEngine plugin;
    private final File itemsFolder;
    private final Map<String, List<ShopItem>> itemCache = new HashMap<>();
    private final Map<String, List<ShopError>> errorCache = new HashMap<>();

    public ItemManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
            createDefaultItems();
        }
        loadItems();
    }

    private void createDefaultItems() {
        File blocksItems = new File(itemsFolder, "blocks.yml");
        if (!blocksItems.exists()) {
            plugin.saveResource("items/blocks.yml", false);
        }

        File mineralsItems = new File(itemsFolder, "minerals.yml");
        if (!mineralsItems.exists()) {
            plugin.saveResource("items/minerals.yml", false);
        }

        File foodItems = new File(itemsFolder, "food.yml");
        if (!foodItems.exists()) {
            plugin.saveResource("items/food.yml", false);
        }
    }

    public void loadItems() {
        itemCache.clear();
        errorCache.clear();

        File[] files = itemsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No item files found in /items/ folder!");
            return;
        }

        for (File file : files) {
            try {
                String fileName = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                Map<String, Object> results = loadItemsFromFile(config, fileName);
                List<ShopItem> items = (List<ShopItem>) results.get("items");
                List<ShopError> errors = (List<ShopError>) results.get("errors");

                itemCache.put(fileName, items);
                errorCache.put(fileName, errors);

                plugin.getLogger().info("Loaded " + items.size() + " items and " + errors.size() + " errors from " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load item file: " + file.getName(), e);
            }
        }
    }


    private Map<String, Object> loadItemsFromFile(FileConfiguration config, String fileName) {
        List<ShopItem> items = new ArrayList<>();
        List<ShopError> errors = new ArrayList<>();

        for (String key : config.getKeys(false)) {
            ConfigurationSection itemData = config.getConfigurationSection(key);
            if (itemData == null) {
                errors.add(new ShopError(key, "Missing configuration section", fileName, -1));
                continue;
            }

            try {
                ShopItem item = loadItem(key, itemData);
                items.add(item);
            } catch (Exception e) {
                int line = extractLineNumber(e, fileName);
                errors.add(new ShopError(key, e.getMessage(), fileName, line));
                plugin.getLogger().warning("Failed to load item '" + key + "' in file '" + fileName + "': " + e.getMessage());
            }
        }

        Map<String, Object> results = new HashMap<>();
        results.put("items", items);
        results.put("errors", errors);
        return results;
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
        int customModelData = itemData.getInt("custom-model-data", -1);

        java.util.List<String> lore = itemData.getStringList("lore");

        return new ShopItem(key, material, displayName, lore, slot, page, amount,
                buyPrice, sellPrice, stock, customModelData);
    }


    private int extractLineNumber(Exception e, String fileName) {
        String msg = e.getMessage();
        if (msg != null && msg.contains("line")) {
            try {
                return Integer.parseInt(msg.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        return -1;
    }

    public List<ShopItem> getItemsForSection(String sectionFileName) {
        return itemCache.getOrDefault(sectionFileName, new ArrayList<>());
    }

    public List<ShopError> getErrorsForSection(String sectionFileName) {
        return errorCache.getOrDefault(sectionFileName, new ArrayList<>());
    }

    public void reload() {
        loadItems();
    }
}