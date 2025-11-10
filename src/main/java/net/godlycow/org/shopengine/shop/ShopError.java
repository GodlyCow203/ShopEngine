package net.godlycow.org.shopengine.shop;

import org.bukkit.Material;
import java.util.List;


public class ShopError extends ShopItem {
    private final String errorMessage;
    private final String configFile;
    private final int lineNumber;

    public ShopError(String key, String errorMessage, String configFile, int lineNumber) {
        super(key, Material.BARRIER, "<red><bold>⚠ CONFIG ERROR",
                List.of(
                        "<gray>File: <yellow>" + configFile,
                        "<gray>Line: <yellow>" + lineNumber,
                        "<gray>Error: <red>" + errorMessage,
                        "",
                        "<gray>Click to see details"
                ),
                0, 0, 1, -1, -1, -1, -1);
        this.errorMessage = errorMessage;
        this.configFile = configFile;
        this.lineNumber = lineNumber;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getConfigFile() {
        return configFile;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean canBuy() {
        return false;
    }

    @Override
    public boolean canSell() {
        return false;
    }
}
