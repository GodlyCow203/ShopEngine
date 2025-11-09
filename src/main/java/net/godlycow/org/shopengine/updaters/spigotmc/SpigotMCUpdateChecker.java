package net.godlycow.org.shopengine.updaters.spigotmc;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class SpigotMCUpdateChecker {

    private final Plugin plugin;
    private final int resourceId;

    public SpigotMCUpdateChecker(Plugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }


    public void getLatestVersion(Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                InputStream inputStream = connection.getInputStream();
                String latest = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
                consumer.accept(latest);

            } catch (Exception e) {
                plugin.getLogger().warning("[UpdateChecker:SpigotMC] Failed: " + e.getMessage());
            }
        });
    }
}
