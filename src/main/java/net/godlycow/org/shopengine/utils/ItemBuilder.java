package net.godlycow.org.shopengine.utils;

import net.godlycow.org.shopengine.ShopEngine;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemBuilder {
    private final ShopEngine plugin;
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;
    private final List<Component> lore = new ArrayList<>();

    public ItemBuilder(ShopEngine plugin, Material material) {
        this.plugin = plugin;
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        itemMeta.displayName(MessageUtils.format(name));
        return this;
    }

    public ItemBuilder lore(String line) {
        lore.add(MessageUtils.format(line));
        return this;
    }

    public ItemBuilder lore(String... lines) {
        Arrays.stream(lines).forEach(line -> lore.add(MessageUtils.format(line)));
        return this;
    }

    public ItemBuilder lore(List<String> lines) {
        lines.forEach(line -> lore.add(MessageUtils.format(line)));
        return this;
    }

    public ItemBuilder amount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public ItemBuilder glow(boolean glow) {
        if (glow) {
            itemMeta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            itemMeta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (data > 0) {
            itemMeta.setCustomModelData(data);
        }
        return this;
    }

    public ItemStack build() {
        if (!lore.isEmpty()) {
            itemMeta.lore(lore);
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}