package dev.ultraenchanting.effect;

import dev.ultraenchanting.UltraEnchantingPlugin;
import dev.ultraenchanting.enchant.Definition;
import dev.ultraenchanting.logic.Rules;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public final class EffectContext {
    public final UltraEnchantingPlugin plugin;

    public EffectContext(UltraEnchantingPlugin plugin){this.plugin=plugin;}
    public boolean enabled(Player p){return plugin.settings.flag("custom-effects-enabled",true) && p.hasPermission("ultraenchanting.use") && !p.isDead();}
    public double power(ItemStack item,String id){
        Definition d=plugin.registry.get("ultraenchanting:"+id);
        return d==null || !d.enabled()?0:d.effective(plugin.items.level(item,id));
    }
    public double armor(Player p,String id){
        double max=0;Definition d=plugin.registry.get("ultraenchanting:"+id);
        if(d==null || !d.enabled())return 0;
        for(ItemStack item:p.getInventory().getArmorContents())if(item!=null && d.accepts(item.getType()))max=Math.max(max,power(item,id));return max;
    }
    public boolean weapon(ItemStack item){return item!=null && (item.getType().name().endsWith("_SWORD") || item.getType().name().endsWith("_AXE") || Set.of(Material.MACE,Material.TRIDENT,Material.BOW,Material.CROSSBOW).contains(item.getType()));}
    public boolean tool(ItemStack item){return item!=null && (item.getType().name().endsWith("_PICKAXE") || item.getType().name().endsWith("_AXE") || item.getType().name().endsWith("_SHOVEL") || item.getType().name().endsWith("_HOE") || item.getType()==Material.SHEARS);}
    public boolean roll(ItemStack item,String id){
        if(power(item,id)<=0)return false;
        Definition d=plugin.registry.get("ultraenchanting:"+id);
        return ThreadLocalRandom.current().nextDouble()<Math.min(1,d.chance()*Math.max(1,Math.log10(plugin.items.level(item,id)+1.0)));
    }
    public boolean ready(Player p,String id){
        Definition d=plugin.registry.get("ultraenchanting:"+id);if(d==null)return false;
        NamespacedKey key=new NamespacedKey(plugin,"cooldown_"+id);
        long now=System.currentTimeMillis();
        if(p.getPersistentDataContainer().getOrDefault(key,org.bukkit.persistence.PersistentDataType.LONG,0L)>now)return false;
        p.getPersistentDataContainer().set(key,org.bukkit.persistence.PersistentDataType.LONG,now+d.cooldown()*1000L);return true;
    }
    public double limit(String key,double fallback,double min,double max){return plugin.settings.number(key,fallback,min,max);}
    public double maxHealth(LivingEntity entity){var a=entity.getAttribute(Attribute.MAX_HEALTH);return a==null?20:a.getValue();}
    public void heal(Player p,double amount){if(!p.isDead())p.setHealth(Math.max(0,Math.min(maxHealth(p),p.getHealth()+Rules.bounded(amount,0,20))));}
    public void potion(LivingEntity entity,PotionEffectType type,double power){
        if(power>0)entity.addPotionEffect(new PotionEffect(type,Math.max(60,plugin.settings.integer("period-ticks",20,10,100)+40),(int)Math.min(4,Math.max(0,power-1)),true,false,true));
    }
    public void velocity(Entity entity,Vector vector){
        double cap=limit("max-velocity",2,0.1,3);
        if(!Double.isFinite(vector.lengthSquared()))return;
        if(vector.lengthSquared()>cap*cap)vector.normalize().multiply(cap);entity.setVelocity(vector);
    }
    public List<Entity> nearby(Location location){
        double radius=limit("scan-radius",6,1,12);
        // A bounding box query plus an output cap: no global scans or chunk loads.
        return location.getWorld().getNearbyEntities(location,radius,radius,radius).stream()
            .filter(e->e.getLocation().distanceSquared(location)<=radius*radius)
            .sorted(Comparator.comparingDouble(e->e.getLocation().distanceSquared(location)))
            .limit(plugin.settings.integer("max-nearby-entities",24,1,64)).toList();
    }
}