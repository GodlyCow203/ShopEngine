package net.godlycow.org.shopengine;

import net.godlycow.org.shopengine.commands.ShopCommand;
import net.godlycow.org.shopengine.config.ConfigManager;
import net.godlycow.org.shopengine.config.ItemManager;
import net.godlycow.org.shopengine.config.MessageManager;
import net.godlycow.org.shopengine.config.SectionManager;
import net.godlycow.org.shopengine.economy.EconomyManager;
import net.godlycow.org.shopengine.listeners.ShopListener;
import net.godlycow.org.shopengine.listeners.UpdateNotifyListener;
import net.godlycow.org.shopengine.metrics.Metrics;
import net.godlycow.org.shopengine.player.PlayerDataManager;
import net.godlycow.org.shopengine.shop.DynamicPricingManager;
import net.godlycow.org.shopengine.shop.ShopManager;
import net.godlycow.org.shopengine.shop.StockManager;
import net.godlycow.org.shopengine.updaters.spigotmc.SpigotMCUpdateChecker;
import net.godlycow.org.shopengine.utils.SignInputManager;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class ShopEngine extends JavaPlugin {
    private static ShopEngine instance;
    private MiniMessage miniMessage;

    private ConfigManager configManager;
    private MessageManager messageManager;
    private SectionManager sectionManager;
    private ItemManager itemManager;
    private ShopManager shopManager;
    private DynamicPricingManager dynamicPricingManager;
    private EconomyManager economyManager;
    private PlayerDataManager playerDataManager;
    private SignInputManager signInputManager;
    private StockManager stockManager;
    private SpigotMCUpdateChecker updateChecker;


    private Metrics metrics;

    private final AtomicInteger shopCommandAtomic = new AtomicInteger(0);

    @Override
    public void onEnable() {
        instance = this;
        miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();
        saveResource("help.yml", false);

        CompletableFuture.runAsync(() -> new SpigotMCUpdateChecker(this).checkForUpdates());

        try {
            configManager = new ConfigManager(this);
            messageManager = new MessageManager(this);
            sectionManager = new SectionManager(this);

            itemManager = new ItemManager(this);
            shopManager = new ShopManager(this);
            stockManager = new StockManager();

            playerDataManager = new PlayerDataManager(this);
            signInputManager = new SignInputManager(this);

        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Critical failure while loading core managers", t);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (!setupEconomy()) {
            getLogger().severe("No Vault-supported economy plugin found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        updateChecker = new SpigotMCUpdateChecker(this);

        CompletableFuture.runAsync(this::initMetricsSafe);
        CompletableFuture.runAsync(() -> {
            updateChecker.checkForUpdates();
        });
        Objects.requireNonNull(getCommand("shop")).setExecutor(new ShopCommand(this));
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getLogger().info("ShopEngine has been enabled (optimized startup)");
        getLogger().info("Dynamic Pricing: " + (configManager.isDynamicPricingEnabled() ? "ENABLED" : "DISABLED"));

        Bukkit.getPluginManager().registerEvents(
                new UpdateNotifyListener(updateChecker),
                this
        );

    }

    @Override
    public void onDisable() {
        try {
            if (dynamicPricingManager != null) dynamicPricingManager.savePrices();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Error while saving dynamic prices", t);
        }

        try {
            if (economyManager != null) economyManager.cleanup();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Error while cleaning up economy manager", t);
        }

        try {
            if (playerDataManager != null) playerDataManager.saveAll();
        } catch (Throwable t) {
            getLogger().log(Level.WARNING, "Error while saving player data", t);
        }

        getLogger().info("ShopEngine has been disabled");
    }




    private boolean setupEconomy() {
        economyManager = new EconomyManager(this);
        return economyManager.setup();
    }

    private void initMetricsSafe() {
        try {
            int pluginId = 27920;
            Metrics localMetrics = new Metrics(this, pluginId);
            localMetrics.addCustomChart(new Metrics.SingleLineChart("shop_command_usage", shopCommandAtomic::get));
            this.metrics = localMetrics;
        } catch (Throwable t) {
            Bukkit.getScheduler().runTask(this, () -> getLogger().log(Level.WARNING, "Failed to initialize metrics", t));
        }
    }

    public void deferredLoad(Runnable task) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                task.run();
            } catch (Throwable t) {
                Bukkit.getScheduler().runTask(this, () -> getLogger().log(Level.WARNING, "Deferred load task failed", t));
            }
        });
    }

    public void incrementShopCommand() {
        shopCommandAtomic.incrementAndGet();
    }


    public static ShopEngine getInstance() { return instance; }
    public MiniMessage getMiniMessage() { return miniMessage; }
    public ConfigManager getConfigManager() { return configManager; }
    public MessageManager getMessageManager() { return messageManager; }
    public SectionManager getSectionManager() { return sectionManager; }
    public ItemManager getItemManager() { return itemManager; }
    public ShopManager getShopManager() { return shopManager; }
    public DynamicPricingManager getDynamicPricingManager() { return dynamicPricingManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public SignInputManager getSignInputManager() { return signInputManager; }
    public StockManager getStockManager() { return stockManager; }
}
