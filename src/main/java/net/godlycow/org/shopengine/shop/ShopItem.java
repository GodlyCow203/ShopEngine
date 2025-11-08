package net.godlycow.org.shopengine.shop;

import org.bukkit.Material;
import java.util.List;

public class ShopItem {
    private final String key;
    private final Material material;
    private final String displayName;
    private final List<String> lore;
    private final int slot;
    private final int amount;
    private final double buyPrice;
    private final double sellPrice;
    private final int stock;
    private final int customModelData;
    private final int page;


    public ShopItem(String key, Material material, String displayName, List<String> lore,
                    int slot, int page, int amount, double buyPrice, double sellPrice, int stock, int customModelData) {
        this.key = key;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.page = page;
        this.slot = slot;
        this.amount = amount;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.stock = stock;
        this.customModelData = customModelData;
    }

    public String getKey() { return key; }
    public Material getMaterial() { return material; }
    public String getDisplayName() { return displayName; }
    public List<String> getLore() { return lore; }
    public int getSlot() { return slot; }
    public int getAmount() { return amount; }
    public int getPage() { return page; }
    public double getBuyPrice() { return buyPrice; }
    public double getSellPrice() { return sellPrice; }
    public int getStock() { return stock; }
    public int getCustomModelData() { return customModelData; }

    public boolean canBuy() {
        return buyPrice > 0;
    }

    public boolean canSell() {
        return sellPrice > 0;
    }
}