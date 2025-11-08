package net.godlycow.org.shopengine.player;

import org.bukkit.Material;

public class Transaction {
    private final long timestamp;
    private final Material material;
    private final int amount;
    private final double price;
    private final boolean isBuy;

    public Transaction(Material material, int amount, double price, boolean isBuy) {
        this.timestamp = System.currentTimeMillis();
        this.material = material;
        this.amount = amount;
        this.price = price;
        this.isBuy = isBuy;
    }

    public long getTimestamp() { return timestamp; }
    public Material getMaterial() { return material; }
    public int getAmount() { return amount; }
    public double getPrice() { return price; }
    public boolean isBuy() { return isBuy; }

    public String getFormattedTime() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }

    public String getAction() {
        return isBuy ? "PURCHASE" : "SALE";
    }
}