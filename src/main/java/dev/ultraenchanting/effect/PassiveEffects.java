package dev.ultraenchanting.effect;

import dev.ultraenchanting.item.ItemData;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;

public final class PassiveEffects implements Listener {
    private final EffectContext c;
    private final NamespacedKey flightKey,speedKey,healthKey;
    public PassiveEffects(EffectContext c){
        this.c=c;flightKey=new NamespacedKey(c.plugin,"flight_before");speedKey=new NamespacedKey(c.plugin,"speed");healthKey=new NamespacedKey(c.plugin,"health");
        int period=c.plugin.settings.integer("period-ticks",20,10,100);
        Bukkit.getScheduler().runTaskTimer(c.plugin,()->{for(Player p:Bukkit.getOnlinePlayers())refresh(p);},1,period);
    }
    private double active(Player p,String id){
        ItemStack hand=p.getInventory().getItemInMainHand();
        return Math.max(c.armor(p,id),c.weapon(hand)||c.tool(hand)?c.power(hand,id):0);
    }
    private void modifier(Player p,Attribute attribute,NamespacedKey key,double amount){
        AttributeInstance instance=p.getAttribute(attribute);if(instance==null)return;
        AttributeModifier old=instance.getModifiers().stream().filter(m->m.getKey().equals(key)).findFirst().orElse(null);
        if(old!=null && Math.abs(old.getAmount()-amount)<0.00001)return;
        if(old!=null)instance.removeModifier(old);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(key,amount,AttributeModifier.Operation.ADD_NUMBER,EquipmentSlotGroup.ANY));
        if(attribute==Attribute.MAX_HEALTH && !p.isDead() && p.getHealth()>instance.getValue())p.setHealth(Math.max(1,instance.getValue()));
    }
    public void refresh(Player p){
        if(!c.enabled(p)){clean(p);return;}
        double speed=Math.min(0.2,c.armor(p,"speed")*0.02+active(p,"overpower")*0.01)-Math.min(0.08,c.armor(p,"curse_of_slowness")*0.02);
        modifier(p,Attribute.MOVEMENT_SPEED,speedKey,speed);
        double health=Math.min(c.limit("max-health-bonus",80,0,100),c.armor(p,"health_boost"))-Math.min(18,c.armor(p,"curse_of_frailty"));
        var attr=p.getAttribute(Attribute.MAX_HEALTH);
        if(attr!=null){double without=attr.getValue();for(var m:attr.getModifiers())if(m.getKey().equals(healthKey))without-=m.getAmount();health=Math.max(1-without,health);}
        modifier(p,Attribute.MAX_HEALTH,healthKey,health);
        c.potion(p,PotionEffectType.JUMP_BOOST,c.armor(p,"jump_boost"));
        c.potion(p,PotionEffectType.SLOWNESS,c.armor(p,"curse_of_heaviness"));
        // Negative jump strength enforces the curse rather than merely slowing walking.
        NamespacedKey jumpKey=new NamespacedKey(c.plugin,"jump_curse");
        modifier(p,Attribute.JUMP_STRENGTH,jumpKey,-Math.min(0.4,c.armor(p,"curse_of_heaviness")*0.1));
        c.potion(p,PotionEffectType.REGENERATION,c.armor(p,"regeneration"));
        c.potion(p,PotionEffectType.FIRE_RESISTANCE,c.armor(p,"fire_resistance"));
        c.potion(p,PotionEffectType.NIGHT_VISION,c.power(p.getInventory().getHelmet(),"night_vision"));
        c.potion(p,PotionEffectType.WATER_BREATHING,c.power(p.getInventory().getHelmet(),"water_breathing"));
        c.potion(p,PotionEffectType.HASTE,Math.max(c.armor(p,"haste"),active(p,"overpower")));
        c.potion(p,PotionEffectType.MINING_FATIGUE,active(p,"curse_of_mining_fatigue"));
        c.potion(p,PotionEffectType.BLINDNESS,c.armor(p,"curse_of_blindness"));
        if(c.armor(p,"curse_of_drowning")>0 && p.getEyeLocation().getBlock().isLiquid())p.setRemainingAir(Math.max(-20,p.getRemainingAir()-40));
        boolean flight=c.armor(p,"flight")>0 && c.armor(p,"curse_of_grounding")==0;
        if(p.getGameMode()==GameMode.CREATIVE || p.getGameMode()==GameMode.SPECTATOR){p.getPersistentDataContainer().remove(flightKey);}
        else if(flight){
            if(!p.getPersistentDataContainer().has(flightKey,PersistentDataType.STRING))p.getPersistentDataContainer().set(flightKey,PersistentDataType.STRING,p.getAllowFlight()+":"+p.isFlying());
            p.setAllowFlight(true);
        }else restoreFlight(p);
        double magnet=active(p,"magnet"),repulsion=active(p,"repulsion");
        if(magnet>0 || repulsion>0)for(Entity e:c.nearby(p.getLocation())) {
            if(!(e instanceof Item) && !(e instanceof ExperienceOrb))continue;
            Vector direction=p.getLocation().toVector().add(new Vector(0,0.5,0)).subtract(e.getLocation().toVector());
            if(direction.lengthSquared()<0.04)continue;
            c.velocity(e,direction.normalize().multiply(repulsion>0?-0.4:0.4));
        }
    }
    private void restoreFlight(Player p){
        String original=p.getPersistentDataContainer().get(flightKey,PersistentDataType.STRING);if(original==null)return;
        p.getPersistentDataContainer().remove(flightKey);
        if(p.getGameMode()==GameMode.CREATIVE || p.getGameMode()==GameMode.SPECTATOR)return;
        boolean allow=original.startsWith("true:");p.setFlying(false);p.setAllowFlight(allow);
        if(allow && original.endsWith(":true"))p.setFlying(true);
    }
    public void clean(Player p){
        modifier(p,Attribute.MOVEMENT_SPEED,speedKey,0);modifier(p,Attribute.MAX_HEALTH,healthKey,0);
        modifier(p,Attribute.JUMP_STRENGTH,new NamespacedKey(c.plugin,"jump_curse"),0);restoreFlight(p);
    }
    public void cleanAll(){for(Player p:Bukkit.getOnlinePlayers())clean(p);}
    @EventHandler public void join(PlayerJoinEvent e){clean(e.getPlayer());}
    @EventHandler public void quit(PlayerQuitEvent e){clean(e.getPlayer());}
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void damage(EntityDamageEvent e){
        if(!(e.getEntity() instanceof Player p) || !c.enabled(p))return;
        double factor=1+Math.min(3,c.armor(p,"curse_of_vulnerability"));
        if(e.getCause()==EntityDamageEvent.DamageCause.FALL)factor+=Math.min(4,c.armor(p,"curse_of_heavy_falling"));
        if(Set.of(EntityDamageEvent.DamageCause.FIRE,EntityDamageEvent.DamageCause.FIRE_TICK,EntityDamageEvent.DamageCause.LAVA,EntityDamageEvent.DamageCause.HOT_FLOOR).contains(e.getCause()))factor+=Math.min(4,c.armor(p,"curse_of_fire_vulnerability"));
        e.setDamage(Math.min(c.limit("max-damage",100,1,1000),e.getDamage()*factor));
        if(c.armor(p,"magic_shield")>0 && c.ready(p,"magic_shield")){e.setCancelled(true);p.getWorld().playSound(p.getLocation(),Sound.ITEM_SHIELD_BLOCK,1,1);}
    }
    // Native resurrection event respects totems and ensures only genuinely lethal damage consumes the cooldown.
    @EventHandler(priority=EventPriority.HIGHEST)
    public void resurrect(EntityResurrectEvent e){
        if(!(e.getEntity() instanceof Player p) || !e.isCancelled() || !c.enabled(p) || c.armor(p,"resurrection")<=0)return;
        if(c.ready(p,"resurrection")){
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(c.plugin,()->{if(p.isOnline() && !p.isDead())c.heal(p,Math.min(10,c.armor(p,"resurrection")));});
        }
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void durability(PlayerItemDamageEvent e){
        if(!c.enabled(e.getPlayer()))return;
        double curse=c.power(e.getItem(),"curse_of_fragility");if(curse>0)e.setDamage((int)Math.min(1000,Math.ceil(e.getDamage()*(1+curse))));
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void mend(PlayerItemMendEvent e){if(c.power(e.getItem(),"curse_of_decay")>0)e.setCancelled(true);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void dash(PlayerSwapHandItemsEvent e){
        Player p=e.getPlayer();if(!c.enabled(p) || !p.isSneaking() || active(p,"dash")<=0)return;e.setCancelled(true);
        if(c.ready(p,"dash"))c.velocity(p,p.getLocation().getDirection().multiply(Math.min(2,active(p,"dash"))).setY(0.2));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void blink(PlayerInteractEvent e){
        Player p=e.getPlayer();if(!c.enabled(p) || e.getHand()!=EquipmentSlot.HAND || !p.isSneaking() || e.getAction()!=org.bukkit.event.block.Action.RIGHT_CLICK_AIR || active(p,"blink")<=0)return;
        e.setCancelled(true);if(!c.ready(p,"blink"))return;
        Location start=p.getLocation(),best=null;Vector direction=start.getDirection().normalize();
        double length=Math.min(8,active(p,"blink"));
        for(double step=0.25;step<=length;step+=0.25){
            Location at=start.clone().add(direction.clone().multiply(step));
            if(at.getY()<at.getWorld().getMinHeight()+1 || at.getY()>at.getWorld().getMaxHeight()-2 || !at.getWorld().getWorldBorder().isInside(at) || !at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))break;
            // Require a clear two-block column along the entire ray and a solid landing surface.
            if(!at.getBlock().isPassable() || !at.clone().add(0,1,0).getBlock().isPassable())break;
            Material floor=at.clone().subtract(0,1,0).getBlock().getType();
            if(floor.isSolid() && floor!=Material.MAGMA_BLOCK && floor!=Material.CACTUS && !at.getBlock().isLiquid())best=at;
        }
        if(best!=null && best.distanceSquared(start)>0.5)p.teleport(best,PlayerTeleportEvent.TeleportCause.PLUGIN);
    }
}