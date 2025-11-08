package net.godlycow.org.shopengine.gui;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.player.Transaction;
import net.godlycow.org.shopengine.shop.ShopItem;
import net.godlycow.org.shopengine.utils.ItemBuilder;
import net.godlycow.org.shopengine.utils.NumberUtils; // ADDED IMPORT
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
import java.util.stream.Collectors;

public class SearchResultsGUI implements InventoryHolder {
    private final ShopEngine plugin;
    private final Player player;
    private final String searchTerm;
    private final List<ShopItem> results;
    private final Inventory inventory;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public SearchResultsGUI(ShopEngine plugin, Player player, String searchTerm) {
        this.plugin = plugin;
        this.player = player;
        this.searchTerm = searchTerm;
        this.results = plugin.getShopManager().getAllItems().stream()
                .filter(item -> matchesSearch(item, searchTerm))
                .collect(Collectors.toList());
        this.inventory = Bukkit.createInventory(this, 54,
                plugin.getMiniMessage().deserialize(
                        plugin.getMessageManager().getMessage("gui.search.title")
                                .replace("<term>", searchTerm)
                )
        );
        initialize();
    }

    private boolean matchesSearch(ShopItem item, String term) {
        String search = term.toLowerCase();
        return item.getKey().toLowerCase().contains(search) ||
                item.getMaterial().name().toLowerCase().contains(search) ||
                item.getDisplayName().toLowerCase().contains(search);
    }

    private void initialize() {
        inventory.clear();

        if (results.isEmpty()) {
            ItemBuilder empty = new ItemBuilder(plugin, Material.BARRIER)
                    .name(plugin.getMessageManager().getMessage("shop.search-no-results")
                            .replace("<term>", searchTerm));
            inventory.setItem(22, empty.build());
            setupCloseButton();
            return;
        }

        int totalPages = (int) Math.ceil(results.size() / (double) ITEMS_PER_PAGE);
        page = Math.min(page, Math.max(0, totalPages - 1));

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && (startIndex + i) < results.size(); i++) {
            ShopItem item = results.get(startIndex + i);
            displayItem(i, item);
        }

        setupNavigation(totalPages);
    }

    private void displayItem(int slot, ShopItem item) {
        double buyPrice = plugin.getDynamicPricingManager().getDynamicPrice(item, true);
        double sellPrice = plugin.getDynamicPricingManager().getDynamicPrice(item, false);

        ItemBuilder builder = new ItemBuilder(plugin, item.getMaterial())
                .name(item.getDisplayName().isEmpty() ?
                        "<white>" + item.getMaterial().name() : item.getDisplayName())
                .amount(item.getAmount())
                .lore(
                        plugin.getMessageManager().getMessage("gui.lore.buy-price")
                                .replace("<price>", NumberUtils.format(buyPrice)),
                        plugin.getMessageManager().getMessage("gui.lore.sell-price")
                                .replace("<price>", NumberUtils.format(sellPrice)),
                        "",
                        plugin.getMessageManager().getMessage("gui.search.click-to-buy"),
                        plugin.getMessageManager().getMessage("gui.search.click-to-sell")
                );

        if (item.getCustomModelData() > 0) {
            builder.customModelData(item.getCustomModelData());
        }

        inventory.setItem(slot, builder.build());
    }

    private void setupNavigation(int totalPages) {
        if (page > 0) {
            ItemBuilder prev = new ItemBuilder(plugin, Material.ARROW)
                    .name(plugin.getMessageManager().getMessage("gui.search.previous"));
            inventory.setItem(48, prev.build());
        }

        if (page < totalPages - 1) {
            ItemBuilder next = new ItemBuilder(plugin, Material.ARROW)
                    .name(plugin.getMessageManager().getMessage("gui.search.next"));
            inventory.setItem(50, next.build());
        }

        ItemBuilder searchAgain = new ItemBuilder(plugin, Material.OAK_SIGN)
                .name(plugin.getMessageManager().getMessage("gui.search.search-again"))
                .lore(plugin.getMessageManager().getMessage("gui.search.search-lore"));
        inventory.setItem(49, searchAgain.build());
    }

    private void setupCloseButton() {
        ItemBuilder close = new ItemBuilder(plugin, Material.BARRIER)
                .name(plugin.getMessageManager().getMessage("gui.search.close"));
        inventory.setItem(49, close.build());
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();
        int clickedItemIndex = page * ITEMS_PER_PAGE + slot;

        if (slot < 45 && clickedItemIndex < results.size()) {
            ShopItem item = results.get(clickedItemIndex);
            if (event.isLeftClick()) {
                executeTransaction(item, true, 1);
            } else {
                executeTransaction(item, false, 1);
            }
        } else if (slot == 48 && page > 0) {
            page--;
            initialize();
        } else if (slot == 50) {
            page++;
            initialize();
        } else if (slot == 49) {
            if (results.isEmpty()) {
                player.closeInventory();
            } else {
                plugin.getSignInputManager().openSearch(player, term -> {
                    if (!term.isEmpty()) {
                        new SearchResultsGUI(plugin, player, term).open();
                    }
                });
            }
        }
    }

    private void executeTransaction(ShopItem item, boolean isBuy, int amount) {
        if (isBuy && !item.canBuy()) {
            sendMessage("shop.cannot-buy");
            return;
        }
        if (!isBuy && !item.canSell()) {
            sendMessage("shop.cannot-sell");
            return;
        }

        double price = plugin.getDynamicPricingManager().getDynamicPrice(item, isBuy) * amount;

        if (isBuy) {
            if (!plugin.getEconomyManager().hasEnough(player, price)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", NumberUtils.format(price));
                sendMessage("economy.not-enough-money", placeholders);
                return;
            }

            ItemStack itemStack = new ItemStack(item.getMaterial(), amount);
            if (!player.getInventory().addItem(itemStack).isEmpty()) {
                sendMessage("shop.inventory-full");
                return;
            }

            plugin.getEconomyManager().withdraw(player, price);
            plugin.getDynamicPricingManager().recordTransaction(item, true, amount);
            plugin.getPlayerDataManager().recordTransaction(player,
                    new Transaction(item.getMaterial(), amount, price, true));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(amount));
            placeholders.put("item", item.getKey());
            placeholders.put("price", NumberUtils.format(price));
            sendMessage("shop.purchase-success", placeholders);
        } else {
            int itemCount = countItems(player, item.getMaterial());
            if (itemCount < amount) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", String.valueOf(itemCount));
                sendMessage("shop.not-enough-items", placeholders);
                return;
            }

            removeItems(player, item.getMaterial(), amount);
            plugin.getEconomyManager().deposit(player, price);
            plugin.getDynamicPricingManager().recordTransaction(item, false, amount);
            plugin.getPlayerDataManager().recordTransaction(player,
                    new Transaction(item.getMaterial(), amount, price, false));

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", String.valueOf(amount));
            placeholders.put("item", item.getKey());
            placeholders.put("price", NumberUtils.format(price));
            sendMessage("shop.sell-success", placeholders);
        }

        initialize();
    }

    private void sendMessage(String key) {
        String msg = plugin.getMessageManager().getMessage(key);
        player.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String msg = plugin.getMessageManager().getMessage(key, placeholders);
        player.sendMessage(plugin.getMiniMessage().deserialize(msg));
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