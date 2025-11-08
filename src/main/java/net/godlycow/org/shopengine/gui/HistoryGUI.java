package net.godlycow.org.shopengine.gui;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.player.PlayerDataManager;
import net.godlycow.org.shopengine.player.Transaction;
import net.godlycow.org.shopengine.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HistoryGUI implements InventoryHolder {
    private final ShopEngine plugin;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 45;

    public HistoryGUI(ShopEngine plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54,
                plugin.getMiniMessage().deserialize(
                        plugin.getMessageManager().getMessage("gui.history.title")
                )
        );
        initialize();
    }

    private void initialize() {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);
        List<Transaction> transactions = data.getRecentTransactions(1000);

        inventory.clear();

        if (transactions.isEmpty()) {
            ItemBuilder empty = new ItemBuilder(plugin, Material.BARRIER)
                    .name(plugin.getMessageManager().getMessage("gui.history.no-transactions"));
            inventory.setItem(22, empty.build());
            setupBackButton();
            return;
        }

        int totalPages = (int) Math.ceil(transactions.size() / (double) ITEMS_PER_PAGE);
        page = Math.min(page, Math.max(0, totalPages - 1));

        int startIndex = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int index = startIndex + i;
            if (index >= transactions.size()) break;

            Transaction transaction = transactions.get(index);
            displayTransaction(i, transaction);
        }

        setupNavigation(totalPages);
        setupInfoPanel(transactions.size());
    }

    private void displayTransaction(int slot, Transaction transaction) {
        String action = transaction.isBuy() ? "PURCHASED" : "SOLD";

        ItemBuilder builder = new ItemBuilder(plugin, transaction.getMaterial())
                .name((transaction.isBuy() ? "<green>✓ " : "<red>✗ ") + action)
                .amount(transaction.getAmount())
                .lore(
                        "<gray>Time: <white>" + transaction.getFormattedTime(),
                        "<gray>Amount: <yellow>" + transaction.getAmount() + "x",
                        "<gray>Price: <gold>$" + transaction.getPrice(),
                        "",
                        "<gray>Total: <gold>$" + (transaction.getPrice() * transaction.getAmount())
                );

        inventory.setItem(slot, builder.build());
    }

    private void setupNavigation(int totalPages) {
        if (page > 0) {
            ItemBuilder prev = new ItemBuilder(plugin, Material.ARROW)
                    .name(plugin.getMessageManager().getMessage("gui.history.previous-page"))
                    .lore(plugin.getMessageManager().getMessage("gui.history.page-info")
                            .replace("<current>", String.valueOf(page + 1))
                            .replace("<total>", String.valueOf(totalPages)));
            inventory.setItem(48, prev.build());
        }

        if (page < totalPages - 1) {
            ItemBuilder next = new ItemBuilder(plugin, Material.ARROW)
                    .name(plugin.getMessageManager().getMessage("gui.history.next-page"))
                    .lore(plugin.getMessageManager().getMessage("gui.history.page-info")
                            .replace("<current>", String.valueOf(page + 2))
                            .replace("<total>", String.valueOf(totalPages)));
            inventory.setItem(50, next.build());
        }

        setupBackButton();
    }

    private void setupBackButton() {
        ItemBuilder back = new ItemBuilder(plugin, Material.BARRIER)
                .name(plugin.getMessageManager().getMessage("gui.history.close"))
                .lore(plugin.getMessageManager().getMessage("gui.history.close-lore"));
        inventory.setItem(49, back.build());
    }

    private void setupInfoPanel(int totalTransactions) {
        PlayerDataManager.PlayerData data = plugin.getPlayerDataManager().getPlayerData(player);

        String totalMsg = plugin.getMessageManager().getMessage("gui.history.total-transactions")
                .replace("<count>", String.valueOf(totalTransactions));
        String purchasesMsg = plugin.getMessageManager().getMessage("gui.history.purchases-count")
                .replace("<count>", String.valueOf(countTransactions(data, true)));
        String salesMsg = plugin.getMessageManager().getMessage("gui.history.sales-count")
                .replace("<count>", String.valueOf(countTransactions(data, false)));

        ItemBuilder stats = new ItemBuilder(plugin, Material.BOOK)
                .name(plugin.getMessageManager().getMessage("gui.history.statistics"))
                .lore(
                        totalMsg,
                        plugin.getMessageManager().getMessage("gui.history.recent-activity"),
                        purchasesMsg,
                        salesMsg
                );
        inventory.setItem(53, stats.build());
    }

    private long countTransactions(PlayerDataManager.PlayerData data, boolean isBuy) {
        return data.getTransactions().stream()
                .filter(t -> t.isBuy() == isBuy)
                .count();
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        int slot = event.getSlot();

        if (slot == 48 && page > 0) {
            page--;
            initialize();
        } else if (slot == 50) {
            page++;
            initialize();
        } else if (slot == 49) {
            player.closeInventory();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}