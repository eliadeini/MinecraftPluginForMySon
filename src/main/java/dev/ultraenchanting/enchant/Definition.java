package dev.ultraenchanting.enchant;

import java.util.*;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public record Definition(String id, String english, String hebrew, List<String> aliases,
        String description, Set<String> categories, String classification, Set<String> conflicts,
        boolean enabled, double scale, double cap, double chance, int cooldown, Enchantment vanilla) {
    public List<String> terms() {
        List<String> all=new ArrayList<>(aliases); all.add(id); all.add(english); all.add(hebrew); return all;
    }
    public double effective(int level) { return Math.min(cap,Math.max(0,level*scale)); }
    public boolean accepts(Material type) {
        if (categories.contains("any")) return true;
        String n=type.name();
        return categories.stream().anyMatch(c -> switch(c) {
            case "weapon" -> n.endsWith("_SWORD") || n.endsWith("_AXE") || n.equals("MACE") || n.equals("TRIDENT") || n.equals("BOW") || n.equals("CROSSBOW");
            case "tool" -> n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") || n.endsWith("_HOE") || n.equals("SHEARS");
            case "armor" -> n.endsWith("_HELMET") || n.endsWith("_CHESTPLATE") || n.endsWith("_LEGGINGS") || n.endsWith("_BOOTS") || n.equals("ELYTRA");
            case "helmet" -> n.endsWith("_HELMET");
            default -> false;
        });
    }
}