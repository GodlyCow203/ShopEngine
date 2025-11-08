package net.godlycow.org.shopengine.config;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.shop.ShopSection;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class SectionManager {
    private final ShopEngine plugin;
    private final File sectionsFolder;
    private final Map<String, ShopSection> sectionCache = new HashMap<>();

    public SectionManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.sectionsFolder = new File(plugin.getDataFolder(), "sections");
        if (!sectionsFolder.exists()) {
            sectionsFolder.mkdirs();
            createDefaultSections();
        }
        loadSections();
    }

    private void createDefaultSections() {
        File blocksFile = new File(sectionsFolder, "blocks.yml");
        if (!blocksFile.exists()) {
            plugin.saveResource("sections/blocks.yml", false);
        }

        File mineralsFile = new File(sectionsFolder, "minerals.yml");
        if (!mineralsFile.exists()) {
            plugin.saveResource("sections/minerals.yml", false);
        }

        File foodFile = new File(sectionsFolder, "food.yml");
        if (!foodFile.exists()) {
            plugin.saveResource("sections/food.yml", false);
        }
    }

    public void loadSections() {
        sectionCache.clear();

        File[] files = sectionsFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No section files found in /sections/ folder!");
            return;
        }

        for (File file : files) {
            try {
                String sectionKey = file.getName().replace(".yml", "");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                ShopSection section = loadSectionFromConfig(sectionKey, config);
                sectionCache.put(sectionKey, section);

                plugin.getLogger().info("Loaded section: " + sectionKey + " from " + file.getName());
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load section file: " + file.getName(), e);
            }
        }
    }

    private ShopSection loadSectionFromConfig(String key, FileConfiguration config) {
        String displayName = config.getString("display-name", key);
        Material icon = Material.matchMaterial(config.getString("icon", "CHEST"));
        int slot = config.getInt("slot", 0);
        int page = config.getInt("page", 1) - 1;

        return new ShopSection(key, displayName, icon, slot, page, new ArrayList<>());
    }

    public ShopSection getSection(String key) {
        return sectionCache.get(key);
    }

    public Map<String, ShopSection> getSections() {
        return new HashMap<>(sectionCache);
    }

    public boolean hasSection(String key) {
        return sectionCache.containsKey(key);
    }

    public void reload() {
        loadSections();
    }
}