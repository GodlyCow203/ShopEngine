package net.godlycow.org.shopengine.config;

import net.godlycow.org.shopengine.ShopEngine;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class MessageManager {
    private final ShopEngine plugin;
    private final File messagesFile;
    private FileConfiguration messagesConfig;
    private final Map<String, String> messageCache = new HashMap<>();

    public MessageManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "shopmessages.yml");
        loadMessages();
    }

    private void loadMessages() {
        if (!messagesFile.exists()) {
            plugin.saveResource("shopmessages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        cacheMessages();
        plugin.getLogger().info("Loaded " + messageCache.size() + " messages!");
    }

    private void cacheMessages() {
        messageCache.clear();

        cacheSection(messagesConfig, "");

        ensureDefaults();

        save();
    }

    private void cacheSection(ConfigurationSection section, String parentKey) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            if (section.isConfigurationSection(key)) {
                cacheSection(section.getConfigurationSection(key), fullKey);
            } else if (section.isString(key)) {
                messageCache.put(fullKey, section.getString(key));
            }
        }
    }

    private void ensureDefaults() {
        ensureDefault("prefix", "<dark_gray>[<gradient:#00ff00:#00ffff>ShopEngine</gradient>]</dark_gray>");
        ensureDefault("no-permission", "<red>You don't have permission to use this!");
        ensureDefault("player-only", "<red>This command can only be used by players!");

        ensureDefault("economy.not-enough-money", "<red>You don't have enough money! <gray>(Need: <gold>$<amount></gold>)");
        ensureDefault("economy.balance", "<green>Your balance: <gold>$<balance>");
        ensureDefault("economy.transaction-failed", "<red>Transaction failed! Please contact an admin.");

        ensureDefault("shop.cannot-buy", "<red>You cannot buy this item!");
        ensureDefault("shop.cannot-sell", "<red>You cannot sell this item!");
        ensureDefault("shop.not-enough-stock", "<red>Not enough stock! <gray>(Available: <yellow><stock></yellow>)");
        ensureDefault("shop.inventory-full", "<red>Your inventory is full!");
        ensureDefault("shop.not-enough-items", "<red>You don't have enough items! <gray>(You have: <white><amount></white>)");
        ensureDefault("shop.purchase-success", "<green>✓ Purchased <white><amount>x <item> <green>for <gold>$<price>!");
        ensureDefault("shop.sell-success", "<green>✓ Sold <white><amount>x <item> <green>for <gold>$<price>!");

        ensureDefault("dynamic-prices.high", "<gold>▲ Prices are high");
        ensureDefault("dynamic-prices.low", "<aqua>▼ Prices are low");

        ensureDefault("gui.titles.main-shop", "<gradient:#00ff00:#00ffff>Shop</gradient>");
        ensureDefault("gui.titles.section", "<section>");

        ensureDefault("gui.lore.click-to-view", "<gray>Click to view items!");
        ensureDefault("gui.lore.your-balance", "<gray>Your balance: <gold>$<balance>");
        ensureDefault("gui.lore.buy-price", "<green>Buy: <white>$<price>");
        ensureDefault("gui.lore.sell-price", "<red>Sell: <white>$<price>");
        ensureDefault("gui.lore.stock", "<gray>Stock: <yellow><stock>");
        ensureDefault("gui.lore.back-title", "<red>← Back");
        ensureDefault("gui.lore.back-button", "<gray>Return to main menu");
        ensureDefault("gui.lore.filler", "<gray>•");
        ensureDefault("gui.lore.left-click-buy", "<yellow>Left-Click <gray>to buy 1");
        ensureDefault("gui.lore.right-click-sell", "<yellow>Right-Click <gray>to sell 1");
        ensureDefault("gui.lore.shift-left-buy", "<yellow>Shift-Left <gray>to buy 64");
        ensureDefault("gui.lore.shift-right-sell", "<yellow>Shift-Right <gray>to sell all");

        ensureDefault("commands.reload-success", "<green>ShopEngine configuration reloaded!");
        ensureDefault("commands.reload-config", "<green>Configuration reloaded!");
        ensureDefault("commands.reload-messages", "<green>Messages reloaded!");
        ensureDefault("commands.reload-sections", "<green>Sections reloaded!");
        ensureDefault("commands.reload-items", "<green>Items reloaded!");
        ensureDefault("commands.reload-unknown", "<red>Unknown reload target! Use: <white>all|config|messages|sections|items");
        ensureDefault("commands.resetprice-success", "<green>Price multiplier reset for <white><item>!");
        ensureDefault("commands.resetprice-usage", "<red>Usage: /shop resetprice <item-key>");
        ensureDefault("commands.help.header", "<gold>ShopEngine Commands:");
        ensureDefault("commands.help.shop", "<yellow>/shop <gray>- Open the shop");
        ensureDefault("commands.help.balance", "<yellow>/shop balance <gray>- Check your balance");
        ensureDefault("commands.help.reload", "<yellow>/shop reload <all|config|messages|sections|items> <gray>- Reload configuration");
        ensureDefault("commands.help.resetprice", "<yellow>/shop resetprice <item> <gray>- Reset dynamic price");
        ensureDefault("commands.help.help", "<yellow>/shop help <gray>- Show this help");
        ensureDefault("commands.unknown", "<red>Unknown subcommand! Use: <white>/shop help");
    }

    private void ensureDefault(String key, String defaultValue) {
        if (!messageCache.containsKey(key)) {
            messageCache.put(key, defaultValue);
            messagesConfig.set(key, defaultValue);
        }
    }

    public String getMessage(String key) {
        return messageCache.getOrDefault(key, "<red>Message not found: " + key);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("<" + entry.getKey() + ">", entry.getValue());
        }
        return message;
    }

    public void reload() {
        loadMessages();
        plugin.getLogger().info("Messages reloaded!");
    }

    public void save() {
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save shopmessages.yml!", e);
        }
    }
}