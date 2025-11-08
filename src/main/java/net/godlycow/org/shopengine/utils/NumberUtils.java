package net.godlycow.org.shopengine.utils;

import org.bukkit.Material;

public class NumberUtils {
    public static String format(double number) {
        return String.format("%,.2f", number);
    }
    public static String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }
}