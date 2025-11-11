package net.godlycow.org.shopengine.player;

import net.godlycow.org.shopengine.ShopEngine;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class PlayerDataManager {

    private final ShopEngine plugin;
    private final File dataFolder;

    private final ConcurrentHashMap<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    private final ExecutorService saveExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ShopEngine-PlayerDataSave");
                t.setDaemon(true);
                return t;
            });

    public PlayerDataManager(ShopEngine plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!dataFolder.exists())
            dataFolder.mkdirs();

        plugin.getLogger().info("PlayerDataManager initialized!");
    }



    public PlayerData getPlayerData(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), uuid -> loadData(uuid));
    }

    private PlayerData loadData(UUID uuid) {
        File file = new File(dataFolder, uuid + ".yml");
        PlayerData data = new PlayerData(uuid);

        if (!file.exists())
            return data;

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            List<Map<?, ?>> rawTx = config.getMapList("transactions");
            if (rawTx != null) {
                for (Map<?, ?> map : rawTx) {
                    try {
                        Material material = Material.valueOf((String) map.get("material"));
                        int amount = ((Number) map.get("amount")).intValue();
                        double price = ((Number) map.get("price")).doubleValue();
                        boolean isBuy = (Boolean) map.get("isBuy");
                        long timestamp = ((Number) map.get("timestamp")).longValue();
                        data.addTransaction(new Transaction(material, amount, price, isBuy ));
                    } catch (Exception ignored) {}
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load player data for " + uuid);
        }

        return data;
    }



    public void saveData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = cache.get(uuid);
        if (data == null) return;

        saveExecutor.submit(() -> saveToFile(uuid, data));
    }

    private void saveToFile(UUID uuid, PlayerData data) {
        File file = new File(dataFolder, uuid + ".yml");
        FileConfiguration config = new YamlConfiguration();

        List<Map<String, Object>> txList = new ArrayList<>(data.transactions.size());
        for (Transaction t : data.transactions) {
            Map<String, Object> map = new HashMap<>();
            map.put("material", t.getMaterial().name());
            map.put("amount", t.getAmount());
            map.put("price", t.getPrice());
            map.put("isBuy", t.isBuy());
            map.put("timestamp", t.getTimestamp());
            txList.add(map);
        }
        config.set("transactions", txList);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save data for " + uuid);
        }
    }


    public void saveAll() {
        CountDownLatch latch = new CountDownLatch(cache.size());
        for (UUID uuid : cache.keySet()) {
            PlayerData data = cache.get(uuid);
            if (data != null) {
                saveExecutor.submit(() -> {
                    saveToFile(uuid, data);
                    latch.countDown();
                });
            } else {
                latch.countDown();
            }
        }

        try {
            latch.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }



    public void recordTransaction(Player player, Transaction transaction) {
        PlayerData data = getPlayerData(player);
        data.addTransaction(transaction);
        saveData(player);
    }

    public static class PlayerData {
        private final UUID uuid;

        private final Deque<Transaction> transactions = new ArrayDeque<>();

        private static final int MAX = 100;

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
        }

        public List<Transaction> getTransactions() {
            return new ArrayList<>(transactions);
        }

        public void addTransaction(Transaction t) {
            if (transactions.size() >= MAX)
                transactions.removeFirst();
            transactions.addLast(t);
        }

        public List<Transaction> getRecentTransactions(int limit) {
            List<Transaction> reversed = new ArrayList<>(transactions);
            Collections.reverse(reversed);
            return reversed.subList(0, Math.min(limit, reversed.size()));
        }
    }
}
