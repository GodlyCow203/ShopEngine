package net.godlycow.org.shopengine.gui;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.player.Transaction;
import net.godlycow.org.shopengine.shop.ShopError;
import net.godlycow.org.shopengine.shop.ShopItem;
import net.godlycow.org.shopengine.shop.ShopSection;
import net.godlycow.org.shopengine.shop.StockManager;
import net.godlycow.org.shopengine.utils.ItemBuilder;
import net.godlycow.org.shopengine.utils.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopGUI implements InventoryHolder {
    private final ShopEngine plugin;
    private final Player player;
    private final ShopSection section;
    private final int currentPage;
    private final Inventory inventory;
    private final Map<Integer, ShopItem> clickableItems = new HashMap<>();
    private static final int CONTENT_SLOTS = 45;
    private static final int NAV_ROW_START = 45;

    private final Map<Integer, ShopError> errorItems = new HashMap<>();


    public ShopGUI(ShopEngine plugin, Player player, ShopSection section, int page) {
        this.plugin = plugin;
        this.player = player;
        this.section = section;
        this.currentPage = page;

        String titleKey = section == null ? "gui.titles.main-shop" : "gui.titles.section";
        String title = plugin.getMessageManager().getMessage(titleKey);
        if (section != null) {
            title = title.replace("<section>", section.getDisplayName());
        }
        title = title + " <dark_gray>(Page " + (page + 1) + ")";

        this.inventory = Bukkit.createInventory(this, 54, plugin.getMiniMessage().deserialize(title));
        initialize();
    }

    public ShopGUI(ShopEngine plugin, Player player, ShopSection section) {
        this(plugin, player, section, 0);
    }

    private void initialize() {
        clickableItems.clear();
        errorItems.clear();

        if (section == null) {
            for (ShopSection shopSection : plugin.getShopManager().getSections().values()) {
                if (shopSection.getPage() == currentPage) {
                    ItemBuilder icon = new ItemBuilder(plugin, shopSection.getIcon())
                            .name(shopSection.getDisplayName())
                            .lore(plugin.getMessageManager().getMessage("gui.lore.click-to-view"))
                            .glow(true);
                    inventory.setItem(shopSection.getSlot(), icon.build());
                }
            }
        } else {
            List<ShopItem> items = section.getItems();
            List<ShopError> errors = plugin.getItemManager().getErrorsForSection(section.getKey()); // Get errors

            for (ShopItem item : items) {
                if (item.getPage() == currentPage) {
                    plugin.getStockManager().initializeStock(item);
                    double buyPrice = plugin.getDynamicPricingManager().getDynamicPrice(item, true);
                    double sellPrice = plugin.getDynamicPricingManager().getDynamicPrice(item, false);
                    int currentStock = plugin.getStockManager().getCurrentStock(item);

                    ItemBuilder itemBuilder = new ItemBuilder(plugin, item.getMaterial())
                            .name(item.getDisplayName().isEmpty() ?
                                    "<white>" + NumberUtils.formatMaterialName(item.getMaterial()) :
                                    item.getDisplayName())
                            .amount(item.getAmount());

                    item.getLore().forEach(itemBuilder::lore);

                    if (item.canBuy()) {
                        String buyMsg = plugin.getMessageManager().getMessage("gui.lore.buy-price");
                        itemBuilder.lore(buyMsg.replace("<price>", NumberUtils.format(buyPrice)));
                    }
                    if (item.canSell()) {
                        String sellMsg = plugin.getMessageManager().getMessage("gui.lore.sell-price");
                        itemBuilder.lore(sellMsg.replace("<price>", NumberUtils.format(sellPrice)));
                    }

                    if (item.getStock() > 0) {
                        String stockMsg = plugin.getMessageManager().getMessage("gui.lore.stock");
                        itemBuilder.lore(stockMsg.replace("<current>", String.valueOf(currentStock)));
                    }

                    if (plugin.getConfigManager().isDynamicPricingEnabled()) {
                        double multiplier = plugin.getDynamicPricingManager().getMultipliers()
                                .getOrDefault(item.getKey(), 1.0);
                        if (multiplier > 1.0) {
                            itemBuilder.lore(plugin.getMessageManager().getMessage("dynamic-prices.high"));
                        } else if (multiplier < 1.0) {
                            itemBuilder.lore(plugin.getMessageManager().getMessage("dynamic-prices.low"));
                        }
                    }

                    String balanceMsg = plugin.getMessageManager().getMessage("gui.lore.your-balance");
                    itemBuilder.lore("");
                    itemBuilder.lore(balanceMsg.replace("<balance>",
                            NumberUtils.format(plugin.getEconomyManager().getBalance(player))));

                    itemBuilder.lore("");
                    if (item.canBuy() && item.canSell()) {
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.left-click-buy"));
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.right-click-sell"));
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.shift-left-buy"));
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.shift-right-sell"));
                    } else if (item.canBuy()) {
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.left-click-buy"));
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.shift-left-buy"));
                    } else if (item.canSell()) {
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.right-click-sell"));
                        itemBuilder.lore(plugin.getMessageManager().getMessage("gui.lore.shift-right-sell"));
                    }

                    if (item.getCustomModelData() > 0) {
                        itemBuilder.customModelData(item.getCustomModelData());
                    }

                    inventory.setItem(item.getSlot(), itemBuilder.build());
                    clickableItems.put(item.getSlot(), item);
                }
            }

            for (ShopError error : errors) {
                if (error.getPage() == currentPage) {
                    ItemBuilder errorBuilder = new ItemBuilder(plugin, Material.BARRIER)
                            .name(error.getDisplayName())
                            .amount(1);

                    for (String loreLine : error.getLore()) {
                        errorBuilder.lore(loreLine);
                    }

                    inventory.setItem(error.getSlot(), errorBuilder.build());
                    errorItems.put(error.getSlot(), error);
                }
            }
        }

        addNavigationButtons();
        fillEmptySlots();
    }

    private void addNavigationButtons() {
        int totalPages = calculateTotalPages();

        boolean hasPrev = currentPage > 0;
        ItemBuilder prevButton = new ItemBuilder(plugin, hasPrev ? Material.ARROW : Material.BARRIER)
                .name(hasPrev ?
                        plugin.getMessageManager().getMessage("gui.navigation.previous") :
                        plugin.getMessageManager().getMessage("gui.navigation.no-previous"));
        if (hasPrev) {
            prevButton.lore(plugin.getMessageManager().getMessage("gui.navigation.previous-lore")
                    .replace("<page>", String.valueOf(currentPage)));
        }
        inventory.setItem(NAV_ROW_START, prevButton.build());

        ItemBuilder pageInfo = new ItemBuilder(plugin, Material.PAPER)
                .name(plugin.getMessageManager().getMessage("gui.navigation.page-info")
                        .replace("<current>", String.valueOf(currentPage + 1))
                        .replace("<total>", String.valueOf(totalPages)));
        inventory.setItem(NAV_ROW_START + 4, pageInfo.build());

        if (section != null) {
            ItemBuilder backButton = new ItemBuilder(plugin, Material.ARROW)
                    .name(plugin.getMessageManager().getMessage("gui.lore.back-title"))
                    .lore(plugin.getMessageManager().getMessage("gui.lore.back-button"));
            inventory.setItem(NAV_ROW_START + 3, backButton.build());
        }

        if (section == null) {
            ItemBuilder searchItem = new ItemBuilder(plugin, Material.OAK_SIGN)
                    .name(plugin.getMessageManager().getMessage("gui.search.sign-title"))
                    .lore(plugin.getMessageManager().getMessage("gui.search.sign-lore"));
            inventory.setItem(NAV_ROW_START + 6, searchItem.build());
        }

        boolean hasNext = currentPage < totalPages - 1;
        ItemBuilder nextButton = new ItemBuilder(plugin, hasNext ? Material.ARROW : Material.BARRIER)
                .name(hasNext ?
                        plugin.getMessageManager().getMessage("gui.navigation.next") :
                        plugin.getMessageManager().getMessage("gui.navigation.no-next"));
        if (hasNext) {
            nextButton.lore(plugin.getMessageManager().getMessage("gui.navigation.next-lore")
                    .replace("<page>", String.valueOf(currentPage + 2)));
        }
        inventory.setItem(NAV_ROW_START + 8, nextButton.build());
    }

    private int calculateTotalPages() {
        if (section == null) {
            return plugin.getShopManager().getSections().values().stream()
                    .mapToInt(ShopSection::getPage)
                    .max().orElse(0) + 1;
        } else {
            return section.getItems().stream()
                    .mapToInt(ShopItem::getPage)
                    .max().orElse(0) + 1;
        }
    }

    private void fillEmptySlots() {
        ItemBuilder filler = new ItemBuilder(plugin, Material.GRAY_STAINED_GLASS_PANE)
                .name(plugin.getMessageManager().getMessage("gui.lore.filler"));
        for (int i = 0; i < CONTENT_SLOTS; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler.build());
            }
        }
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();


        ShopError error = errorItems.get(slot);
        if (error != null) {
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<red>Configuration Error in " + error.getConfigFile() +
                            "<gray>Line " + error.getLineNumber() +
                            "<red>: " + error.getErrorMessage()
            ));
            player.sendMessage(plugin.getMiniMessage().deserialize(
                    "<yellow>Please fix the config and run /shopadmin reload"
            ));
            return;
        }

        if (slot == NAV_ROW_START) {
            if (currentPage > 0) {
                new ShopGUI(plugin, player, section, currentPage - 1).open();
            }
            return;
        }

        if (slot == NAV_ROW_START + 8) {
            int totalPages = calculateTotalPages();
            if (currentPage < totalPages - 1) {
                new ShopGUI(plugin, player, section, currentPage + 1).open();
            }
            return;
        }

        if (section == null && slot == NAV_ROW_START + 6) {
            player.closeInventory();
            plugin.getSignInputManager().openSearch(player, term -> {
                if (!term.isEmpty()) {
                    new SearchResultsGUI(plugin, player, term).open();
                    player.sendMessage(plugin.getMiniMessage().deserialize(
                            plugin.getMessageManager().getMessage("shop.search-starting")
                                    .replace("<term>", term)
                    ));
                }
            });
            return;
        }

        if (section != null && slot == NAV_ROW_START + 3) {
            new ShopGUI(plugin, player, null, 0).open();
            return;
        }

        if (section == null) {
            for (ShopSection shopSection : plugin.getShopManager().getSections().values()) {
                if (shopSection.getSlot() == slot && shopSection.getPage() == currentPage) {
                    new ShopGUI(plugin, player, shopSection, 0).open();
                    return;
                }
            }
        } else {
            ShopItem item = clickableItems.get(slot);
            if (item != null) {
                handleItemClick(item, event);
            }
        }
    }

    private void handleItemClick(ShopItem item, InventoryClickEvent event) {
        boolean isBuy = event.isLeftClick();
        boolean isShift = event.isShiftClick();

        if (isBuy && isShift) {
            buyItem(item, 64);
        } else if (isBuy) {
            buyItem(item, 1);
        } else if (!isBuy && isShift) {
            sellItem(item, -1);
        } else {
            sellItem(item, 1);
        }
    }

    private void buyItem(ShopItem item, int quantity) {
        if (!item.canBuy()) {
            sendMessage("shop.cannot-buy");
            return;
        }

        StockManager stockManager = plugin.getStockManager();
        if (!stockManager.hasStock(item, quantity)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("current", String.valueOf(stockManager.getCurrentStock(item)));
            sendMessage("shop.not-enough-stock", placeholders);
            return;
        }

        double price = plugin.getDynamicPricingManager().getDynamicPrice(item, true) * quantity;

        if (!plugin.getEconomyManager().hasEnough(player, price)) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", NumberUtils.format(price));
            sendMessage("economy.not-enough-money", placeholders);
            return;
        }

        ItemStack itemStack = new ItemStack(item.getMaterial(), quantity);
        if (!player.getInventory().addItem(itemStack).isEmpty()) {
            sendMessage("shop.inventory-full");
            return;
        }

        if (!plugin.getEconomyManager().withdraw(player, price)) {
            player.getInventory().removeItem(itemStack);
            sendMessage("economy.transaction-failed");
            return;
        }

        if (item.getStock() > 0) {
            stockManager.removeStock(item, quantity);
        }

        plugin.getDynamicPricingManager().recordTransaction(item, true, quantity);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(quantity));
        placeholders.put("item", NumberUtils.formatMaterialName(item.getMaterial()));
        placeholders.put("price", NumberUtils.format(price));
        sendMessage("shop.purchase-success", placeholders);

        playSound("buy");
        refreshGUI();

        plugin.getPlayerDataManager().recordTransaction(player,
                new Transaction(item.getMaterial(), quantity, price, true)
        );
    }

    private void sellItem(ShopItem item, int quantity) {
        if (!item.canSell()) {
            sendMessage("shop.cannot-sell");
            return;
        }

        int itemCount = countItems(player, item.getMaterial());
        if (quantity == -1) quantity = itemCount;

        if (itemCount < quantity) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(itemCount));
            sendMessage("shop.not-enough-items", placeholders);
            return;
        }

        double price = plugin.getDynamicPricingManager().getDynamicPrice(item, false) * quantity;
        removeItems(player, item.getMaterial(), quantity);

        if (!plugin.getEconomyManager().deposit(player, price)) {
            player.getInventory().addItem(new ItemStack(item.getMaterial(), quantity));
            sendMessage("economy.transaction-failed");
            return;
        }

        if (item.getStock() > 0) {
            plugin.getStockManager().addStock(item, quantity);
        }

        plugin.getDynamicPricingManager().recordTransaction(item, false, quantity);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", String.valueOf(quantity));
        placeholders.put("item", NumberUtils.formatMaterialName(item.getMaterial()));
        placeholders.put("price", NumberUtils.format(price));
        sendMessage("shop.sell-success", placeholders);

        playSound("sell");
        refreshGUI();

        plugin.getPlayerDataManager().recordTransaction(player,
                new Transaction(item.getMaterial(), quantity, price, false)
        );
    }

    private void sendMessage(String key) {
        String msg = plugin.getMessageManager().getMessage(key);
        player.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String msg = plugin.getMessageManager().getMessage(key, placeholders);
        player.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }

    private void playSound(String type) {
        if (!plugin.getConfig().getBoolean("gui.sound-effects.enabled", true)) return;

        String soundPath = "gui.sound-effects." + type;
        String sound = plugin.getConfig().getString(soundPath, "UI_BUTTON_CLICK");

        try {
            org.bukkit.Sound soundEnum = org.bukkit.Sound.valueOf(sound);
            player.playSound(player.getLocation(), soundEnum, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + sound);
        }
    }

    private void refreshGUI() {
        plugin.getServer().getScheduler().runTaskLater(plugin, this::open, 2L);
    }

    private int countItems(Player player, Material material) {
        return player.getInventory().all(material).values().stream()
                .mapToInt(ItemStack::getAmount)
                .sum();
    }

    private void removeItems(Player player, Material material, int quantity) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material) continue;

            int amount = item.getAmount();
            if (amount > quantity) {
                item.setAmount(amount - quantity);
                break;
            } else {
                quantity -= amount;
                player.getInventory().remove(item);
                if (quantity == 0) break;
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}