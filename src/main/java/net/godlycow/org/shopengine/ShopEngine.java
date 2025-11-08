package net.godlycow.org.shopengine;

import net.godlycow.org.shopengine.commands.ShopCommand;
import net.godlycow.org.shopengine.config.ConfigManager;
import net.godlycow.org.shopengine.config.ItemManager;
import net.godlycow.org.shopengine.config.MessageManager;
import net.godlycow.org.shopengine.config.SectionManager;
import net.godlycow.org.shopengine.economy.EconomyManager;
import net.godlycow.org.shopengine.listeners.ShopListener;
import net.godlycow.org.shopengine.player.PlayerDataManager;
import net.godlycow.org.shopengine.shop.DynamicPricingManager;
import net.godlycow.org.shopengine.shop.ShopManager;
import net.godlycow.org.shopengine.shop.StockManager;
import net.godlycow.org.shopengine.utils.SignInputManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

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

    @Override
    public void onEnable() {
        instance = this;
        miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();
        configManager = new ConfigManager(this);

        messageManager = new MessageManager(this);
        sectionManager = new SectionManager(this);
        itemManager = new ItemManager(this);
        playerDataManager = new PlayerDataManager(this);
        signInputManager = new SignInputManager(this);  // NEW
        stockManager = new StockManager();


        if (!setupEconomy()) {
            getLogger().severe("No Vault-supported economy plugin found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveResource("help.yml", false);


        dynamicPricingManager = new DynamicPricingManager(this);
        shopManager = new ShopManager(this);

        getCommand("shop").setExecutor(new ShopCommand(this));
        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        getLogger().info("ShopEngine has been enabled!");
        getLogger().info("Dynamic Pricing: " + (configManager.isDynamicPricingEnabled() ? "ENABLED" : "DISABLED"));

    }

    @Override
    public void onDisable() {
        if (dynamicPricingManager != null) dynamicPricingManager.savePrices();
        if (economyManager != null) economyManager.cleanup();
        if (playerDataManager != null) playerDataManager.saveAll();
        getLogger().info("ShopEngine has been disabled!");
    }

    private boolean setupEconomy() {
        economyManager = new EconomyManager(this);
        return economyManager.setup();
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