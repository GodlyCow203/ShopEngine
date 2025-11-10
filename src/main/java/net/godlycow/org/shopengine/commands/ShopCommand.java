package net.godlycow.org.shopengine.commands;

import net.godlycow.org.shopengine.ShopEngine;
import net.godlycow.org.shopengine.gui.HistoryGUI;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class ShopCommand implements CommandExecutor, TabCompleter {

    private final ShopEngine plugin;
    private final List<String> mainArgs = Arrays.asList(
            "reload", "resetprice", "balance", "help", "history"
    );

    private final List<String> reloadArgs = Arrays.asList(
            "all", "config", "messages", "sections", "items"
    );

    public ShopCommand(ShopEngine plugin) {
        this.plugin = plugin;

        PluginCommand shop = plugin.getCommand("shop");
        if (shop != null) shop.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "player-only");
            return true;
        }

        if (args.length == 0) {
            plugin.getShopManager().openShop(player);
            return true;
        }
        plugin.incrementShopCommand();

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender, args);
            case "resetprice" -> handleResetPrice(sender, args);
            case "balance" -> handleBalance(sender, player);
            case "help" -> handleHelp(sender);
            case "history" -> {
                if (!sender.hasPermission("shopengine.history")) {
                    sendMessage(sender, "no-permission");
                    return true;
                }
                new HistoryGUI(plugin, player).open();
            }
            default -> sendMessage(sender, "commands.unknown");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            for (String s : mainArgs) {
                if (s.startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("reload")) {
            for (String r : reloadArgs) {
                if (r.startsWith(args[1].toLowerCase())) {
                    completions.add(r);
                }
            }
            return completions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("resetprice")) {
            return completions;
        }
        return Collections.emptyList();
    }


    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shopengine.reload")) {
            sendMessage(sender, "no-permission");
            return;
        }

        String target = args.length > 1 ? args[1].toLowerCase() : "all";

        switch (target) {
            case "all" -> {
                plugin.getConfigManager().reload();
                plugin.getMessageManager().reload();
                plugin.getSectionManager().reload();
                plugin.getItemManager().reload();
                plugin.getShopManager().loadShop();
                sendMessage(sender, "commands.reload-success");
            }
            case "config" -> {
                plugin.getConfigManager().reload();
                sendMessage(sender, "commands.reload-config");
            }
            case "messages" -> {
                plugin.getMessageManager().reload();
                sendMessage(sender, "commands.reload-messages");
            }
            case "sections" -> {
                plugin.getSectionManager().reload();
                plugin.getShopManager().loadShop();
                sendMessage(sender, "commands.reload-sections");
            }
            case "items" -> {
                plugin.getItemManager().reload();
                plugin.getShopManager().loadShop();
                sendMessage(sender, "commands.reload-items");
            }
            default -> sendMessage(sender, "commands.reload-unknown");
        }
    }

    private void handleResetPrice(CommandSender sender, String[] args) {
        if (!sender.hasPermission("shopengine.resetprice")) {
            sendMessage(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            sendMessage(sender, "commands.resetprice-usage");
            return;
        }

        plugin.getDynamicPricingManager().resetPrice(args[1]);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item", args[1]);
        sendMessage(sender, "commands.resetprice-success", placeholders);
    }

    private void handleBalance(CommandSender sender, Player player) {
        if (!sender.hasPermission("shopengine.balance")) {
            sendMessage(sender, "no-permission");
            return;
        }

        double balance = plugin.getEconomyManager().getBalance(player);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("balance", String.valueOf(balance));
        sendMessage(sender, "economy.balance", placeholders);
    }

    private void handleHelp(CommandSender sender) {
        sendMessage(sender, "commands.help.header");
        sendMessage(sender, "commands.help.shop");
        sendMessage(sender, "commands.help.balance");
        sendMessage(sender, "commands.help.reload");
        sendMessage(sender, "commands.help.resetprice");
        sendMessage(sender, "commands.help.history");
        sendMessage(sender, "commands.help.help");
    }

    private void sendMessage(CommandSender sender, String key) {
        String msg = plugin.getMessageManager().getMessage(key);
        sender.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }

    private void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        String msg = plugin.getMessageManager().getMessage(key, placeholders);
        sender.sendMessage(plugin.getMiniMessage().deserialize(msg));
    }
}
