package dev.ultraenchanting.item;

import dev.ultraenchanting.UltraEnchantingPlugin;
import dev.ultraenchanting.enchant.Definition;
import dev.ultraenchanting.logic.Rules;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.persistence.*;
import java.util.*;

public final class ItemData {
    private final UltraEnchantingPlugin plugin;
    public final NamespacedKey tableKey;
    private final NamespacedKey levelsKey,loreKey;
    public ItemData(UltraEnchantingPlugin plugin) {
        this.plugin=plugin; tableKey=new NamespacedKey(plugin,"ultimate_table");
        levelsKey=new NamespacedKey(plugin,"levels"); loreKey=new NamespacedKey(plugin,"original_lore");
    }
    public ItemStack table() {
        ItemStack item=new ItemStack(Material.ENCHANTING_TABLE);
        item.editMeta(meta->{meta.displayName(Component.text(plugin.settings.text("title"),NamedTextColor.LIGHT_PURPLE));
            meta.getPersistentDataContainer().set(tableKey,PersistentDataType.BYTE,(byte)1);meta.setEnchantmentGlintOverride(true);});
        return item;
    }
    public boolean isTable(ItemStack item) {
        return item!=null && item.getType()==Material.ENCHANTING_TABLE && item.hasItemMeta() &&
            item.getItemMeta().getPersistentDataContainer().has(tableKey,PersistentDataType.BYTE);
    }
    public Map<String,Integer> levels(ItemStack item) {
        Map<String,Integer> values=new TreeMap<>();
        if(item==null || item.getType().isAir()) return values;
        item.getEnchantments().forEach((e,l)->values.put(e.getKey().toString(),l));
        ItemMeta meta=item.getItemMeta();
        if(meta instanceof EnchantmentStorageMeta book) book.getStoredEnchants().forEach((e,l)->values.put(e.getKey().toString(),l));
        PersistentDataContainer container=meta.getPersistentDataContainer().get(levelsKey,PersistentDataType.TAG_CONTAINER);
        if(container!=null) for(NamespacedKey key:container.getKeys()) {
            Integer value=container.get(key,PersistentDataType.INTEGER);
            if(value!=null && value>0 && value<=Rules.MAX_LEVEL) values.put(key.toString(),value);
        }
        return values;
    }
    public int level(ItemStack item,String id) { return levels(item).getOrDefault("ultraenchanting:"+id,0); }
    public ItemStack apply(ItemStack original,Definition selected,int level) {
        Rules.level(Integer.toString(level));
        ItemStack result=original.clone();
        Map<String,Integer> values=Rules.replace(levels(result),selected.id(),level);
        result.editMeta(meta->{
            PersistentDataContainer pdc=meta.getPersistentDataContainer();
            PersistentDataContainer container=pdc.getAdapterContext().newPersistentDataContainer();
            values.forEach((id,l)->{NamespacedKey key=NamespacedKey.fromString(id);if(key!=null)container.set(key,PersistentDataType.INTEGER,l);});
            pdc.set(levelsKey,PersistentDataType.TAG_CONTAINER,container);
            if(selected.vanilla()!=null) {
                int engine=Math.min(level,plugin.settings.integer("vanilla-engine-level-cap",255,1,255));
                if(meta instanceof EnchantmentStorageMeta book) book.addStoredEnchant(selected.vanilla(),engine,true);
                else meta.addEnchant(selected.vanilla(),engine,true);
            }
            // Save the original lore once as serialized Adventure components, never parse lore as enchantment data.
            var serializer=net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson();
            if(!pdc.has(loreKey,PersistentDataType.LIST.strings())) {
                List<String> originalLore=meta.lore()==null?List.of():meta.lore().stream().map(serializer::serialize).toList();
                pdc.set(loreKey,PersistentDataType.LIST.strings(),originalLore);
            }
            List<Component> lore=new ArrayList<>();
            for(String line:pdc.getOrDefault(loreKey,PersistentDataType.LIST.strings(),List.of())) lore.add(serializer.deserialize(line));
            values.forEach((id,l)->{
                Definition d=plugin.registry.get(id);
                lore.add(Component.text((d==null?id:d.hebrew()+" ("+d.english()+")")+" "+l,
                    d!=null && d.classification().equals("bad")?NamedTextColor.RED:NamedTextColor.AQUA));
            });
            meta.lore(lore);meta.setEnchantmentGlintOverride(true);
        });
        return result;
    }
    public static void give(org.bukkit.entity.Player player,ItemStack item) {
        if(item==null || item.getType().isAir())return;
        player.getInventory().addItem(item).values().forEach(left->player.getWorld().dropItemNaturally(player.getLocation(),left));
    }
}