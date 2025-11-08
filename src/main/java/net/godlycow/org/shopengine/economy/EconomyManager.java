package net.godlycow.org.shopengine.economy;

import net.godlycow.org.shopengine.ShopEngine;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.logging.Level;

public class EconomyManager {
    private final ShopEngine plugin;
    private Economy economy;
    private String economyName;

    public EconomyManager(ShopEngine plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault")) {
            plugin.getLogger().severe("Vault plugin not found!");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager()
                .getRegistration(Economy.class);

        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found!");
            return false;
        }

        economy = rsp.getProvider();
        economyName = economy.getName();

        plugin.getLogger().info("Economy hooked: " + economyName);
        return true;
    }

    public boolean hasEnough(Player player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (economy == null) return false;

        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to withdraw $" + amount + " from " + player.getName() + ": " + response.errorMessage);
            return false;
        }
        return true;
    }

    public boolean deposit(Player player, double amount) {
        if (economy == null) return false;

        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to deposit $" + amount + " to " + player.getName() + ": " + response.errorMessage);
            return false;
        }
        return true;
    }

    public double getBalance(Player player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }

    public String format(double amount) {
        if (economy == null) return String.format("$%.2f", amount);
        return economy.format(amount);
    }

    public String getEconomyName() {
        return economyName;
    }

    public Economy getEconomy() {
        return economy;
    }

    public void cleanup() {
    }
}