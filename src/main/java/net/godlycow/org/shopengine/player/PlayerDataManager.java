package net.godlycow.org.shopengine.player;

import net.godlycow.org.shopengine.ShopEngine;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private final ShopEngine plugin;
    private final File dataFolder;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        plugin.getLogger().info("PlayerDataManager initialized!");
    }

    public PlayerData getPlayerData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> loadData(player));
    }

    private PlayerData loadData(Player player) {
        File file = new File(dataFolder, player.getUniqueId() + ".yml");
        PlayerData data = new PlayerData(player.getUniqueId());

        if (!file.exists()) return data;

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            if (config.contains("transactions")) {
                List<Map<?, ?>> transactions = config.getMapList("transactions");
                for (Map<?, ?> map : transactions) {
                    try {
                        Material material = Material.valueOf((String) map.get("material"));
                        int amount = (Integer) map.get("amount");
                        double price = (Double) map.get("price");
                        boolean isBuy = (Boolean) map.get("isBuy");
                        long timestamp = ((Number) map.get("timestamp")).longValue();
                        Transaction transaction = new Transaction(material, amount, price, isBuy);
                        data.addTransaction(transaction);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to load transaction for " + player.getName());
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player data for " + player.getName());
        }

        return data;
    }

    public void saveData(Player player) {
        PlayerData data = cache.get(player.getUniqueId());
        if (data == null) return;

        CompletableFuture.runAsync(() -> {
            File file = new File(dataFolder, player.getUniqueId() + ".yml");
            FileConfiguration config = new YamlConfiguration();
            List<Map<String, Object>> transactions = new ArrayList<>();
            for (Transaction transaction : data.getTransactions()) {
                Map<String, Object> map = new HashMap<>();
                map.put("material", transaction.getMaterial().name());
                map.put("amount", transaction.getAmount());
                map.put("price", transaction.getPrice());
                map.put("isBuy", transaction.isBuy());
                map.put("timestamp", transaction.getTimestamp());
                transactions.add(map);
            }
            config.set("transactions", transactions);

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to save data for " + player.getName());
            }
        });
    }

    public void saveAll() {
        cache.keySet().forEach(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            if (player != null && player.isOnline()) {
                saveData(player);
            }
        });
    }

    public void recordTransaction(Player player, Transaction transaction) {
        PlayerData data = getPlayerData(player);
        data.addTransaction(transaction);
        saveData(player);
    }

    public static class PlayerData {
        private final UUID uuid;
        private final List<Transaction> transactions = new ArrayList<>();

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
        }

        public List<Transaction> getTransactions() {
            return new ArrayList<>(transactions);
        }

        public void addTransaction(Transaction transaction) {
            transactions.add(transaction);
            if (transactions.size() > 100) {
                transactions.remove(0);
            }
        }

        public List<Transaction> getRecentTransactions(int limit) {
            List<Transaction> recent = new ArrayList<>(transactions);
            Collections.reverse(recent);
            return recent.subList(0, Math.min(limit, recent.size()));
        }
    }
}