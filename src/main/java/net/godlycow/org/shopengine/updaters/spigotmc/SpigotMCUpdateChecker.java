package net.godlycow.org.shopengine.updaters.spigotmc;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.function.Consumer;

public class SpigotMCUpdateChecker {

    private final Plugin plugin;
    private final int resourceId;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SpigotMCUpdateChecker(Plugin plugin, int resourceId) {
        this.plugin = plugin;
        this.resourceId = resourceId;
    }

    public void checkForUpdates() {
        getLatestVersion(latest -> {
            if (latest == null || latest.isBlank()) {
                plugin.getLogger().warning("[UpdateChecker] Could not check SpigotMC for updates.");
                return;
            }

            String current = plugin.getDescription().getVersion();

            if (isNewerVersion(latest, current)) {
                alertUpdateAvailable(current, latest);
            } else {
                alertUpToDate(current);
            }
        });
    }

    public void getLatestVersion(Consumer<String> consumer) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=" + resourceId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                InputStream inputStream = conn.getInputStream();
                String latest = new Scanner(inputStream, "UTF-8").useDelimiter("\\A").next();
                consumer.accept(latest.trim());

            } catch (Exception e) {
                plugin.getLogger().warning("[UpdateChecker:SpigotMC] Failed: " + e.getMessage());
            }
        });
    }

    private boolean isNewerVersion(String latest, String current) {
        return !latest.equalsIgnoreCase(current);
    }

    private void alertUpdateAvailable(String current, String latest) {
        String msg = "<#ff9666>ShopEngine update available!</#ff9666>\n" +
                "<#C4C4C4>Current version: <#FF3636>" + current + "</#FF3636></#C4C4C4>\n" +
                "<#C4C4C4>Latest version: <#3EA800>" + latest + "</green></#C4C4C4>\n" +
                "<#2263BD><click:open_url:'https://www.spigotmc.org/resources/" + resourceId + "'>Click here to download</click></#2263BD>";

        Bukkit.getConsoleSender().sendMessage(mm.deserialize(msg));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("shopengine.update")) {
                p.sendMessage(mm.deserialize(msg));
            }
        }
    }

    private void alertUpToDate(String current) {
        String msg = "<#98F527>You are running the <#5B9418>latest version<#98F527> of ShopEngine! <gray>(<white>" + current + "</white>)</gray>";

        Bukkit.getConsoleSender().sendMessage(mm.deserialize(msg));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("shopengine.update")) {
                p.sendMessage(mm.deserialize(msg));
            }
        }
    }
}
