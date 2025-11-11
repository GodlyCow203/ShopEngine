package net.godlycow.org.shopengine.updaters.spigotmc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.Plugin;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class SpigotMCUpdateChecker {

    private final Plugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final int resourceId = 130075;
    private boolean updateAvailable = false;
    private String latestVersion = null;

    public SpigotMCUpdateChecker(Plugin plugin) {
        this.plugin = plugin;
    }

    public void checkForUpdates() {
        try {
            URL url = new URL("https://api.spiget.org/v2/resources/" + resourceId + "/versions/latest");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            JsonObject obj = JsonParser.parseReader(
                    new InputStreamReader(conn.getInputStream())
            ).getAsJsonObject();

            latestVersion = obj.get("name").getAsString();
            String currentVersion = plugin.getDescription().getVersion();

            updateAvailable = isNewerVersion(latestVersion, currentVersion);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to check for updates: " + e.getMessage());
        }
    }

    public boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public Component getUpdateMessage() {
        if (latestVersion == null) return Component.text("Update available!");

        String msg = """
            <#ff9666>ShopEngine update available!</#ff9666>
            <#C4C4C4>Current version: <#FF3636>""" + plugin.getDescription().getVersion() + """
            </#FF3636></#C4C4C4>
            <#C4C4C4>Latest version: <#3EA800>""" + latestVersion + """
            </#3EA800></#C4C4C4>
            <#2263BD><click:open_url:'https://www.spigotmc.org/resources/shopengine.130075/'>Click here to download</click></#2263BD>
            """;

        return mm.deserialize(msg);
    }

    private boolean isNewerVersion(String latest, String current) {
        try {
            String[] c = current.split("\\.");
            String[] l = latest.split("\\.");

            int max = Math.max(c.length, l.length);
            for (int i = 0; i < max; i++) {
                int cv = i < c.length ? parse(c[i]) : 0;
                int lv = i < l.length ? parse(l[i]) : 0;
                if (lv > cv) return true;
                if (lv < cv) return false;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private int parse(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception ignored) { return 0; }
    }
}
