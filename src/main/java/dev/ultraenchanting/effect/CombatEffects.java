package dev.ultraenchanting.effect;

import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import java.util.*;

public final class CombatEffects implements Listener {
    private final EffectContext c;
    private boolean generatedDamage=false;
    private final NamespacedKey shotKey;
    public CombatEffects(EffectContext c){this.c=c;shotKey=new NamespacedKey(c.plugin,"shot_item");}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void bow(EntityShootBowEvent e){if(e.getEntity() instanceof Player && e.getBow()!=null)e.getProjectile().getPersistentDataContainer().set(shotKey,PersistentDataType.BYTE_ARRAY,e.getBow().serializeAsBytes());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void launch(ProjectileLaunchEvent e){
        if(e.getEntity() instanceof Trident trident)e.getEntity().getPersistentDataContainer().set(shotKey,PersistentDataType.BYTE_ARRAY,trident.getItemStack().serializeAsBytes());
    }
    private Player attacker(Entity entity){return entity instanceof Player p?p:entity instanceof Projectile shot && shot.getShooter() instanceof Player p?p:null;}
    private ItemStack weapon(Entity entity,Player p){
        if(entity instanceof Projectile){byte[] data=entity.getPersistentDataContainer().get(shotKey,PersistentDataType.BYTE_ARRAY);return data==null?null:ItemStack.deserializeBytes(data);}
        return p.getInventory().getItemInMainHand();
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void offense(EntityDamageByEntityEvent e){
        if(!(e.getEntity() instanceof LivingEntity target) || generatedDamage)return;
        Player p=attacker(e.getDamager());if(p==null || !c.enabled(p) || target.equals(p))return;
        ItemStack item=weapon(e.getDamager(),p);if(!c.weapon(item))return;
        double damage=e.getDamage();
        damage*=1+Math.min(3,c.power(item,"overpower"));
        if(c.roll(item,"critical"))damage*=1+Math.min(3,c.power(item,"critical"));
        if(target.getHealth()/c.maxHealth(target)<0.3)damage*=1+Math.min(3,c.power(item,"executioner"));
        if(c.maxHealth(target)>40)damage*=1+Math.min(3,c.power(item,"giant_slayer"));
        damage*=Math.max(0.05,1-Math.min(0.95,c.power(item,"curse_of_weakness")));
        e.setDamage(Math.min(c.limit("max-damage",100,1,1000),damage));
    }
    // Effects run only after protection plugins have had their opportunity to cancel damage.
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void impact(EntityDamageByEntityEvent e){
        if(!(e.getEntity() instanceof LivingEntity target) || generatedDamage || e.getFinalDamage()<=0)return;
        Player p=attacker(e.getDamager());if(p==null || !c.enabled(p) || target.equals(p))return;
        ItemStack item=weapon(e.getDamager(),p);if(!c.weapon(item))return;
        double dealt=Math.min(target.getHealth()+target.getAbsorptionAmount(),e.getFinalDamage());
        double healing=dealt*(Math.min(0.5,c.power(item,"life_steal"))+Math.min(0.5,c.power(item,"vampire"))*(1-target.getHealth()/c.maxHealth(target)));
        int extras=c.roll(item,"triple_strike")?2:c.roll(item,"double_strike")?1:0;
        extras=Math.min(extras,c.plugin.settings.integer("max-extra-hits",2,0,2));final int hits=extras;
        Bukkit.getScheduler().runTask(c.plugin,()->{
            if(!p.isOnline() || !c.enabled(p))return;
            c.heal(p,healing);
            if(!target.isValid() || target.isDead() || target.getWorld()!=p.getWorld())return;
            for(int i=0;i<hits && !target.isDead();i++)damage(target,p,Math.min(20,dealt*0.5));
            if(c.roll(item,"freeze"))c.potion(target,PotionEffectType.SLOWNESS,c.power(item,"freeze"));
            if(c.roll(item,"poison"))c.potion(target,PotionEffectType.POISON,c.power(item,"poison"));
            if(c.roll(item,"wither"))c.potion(target,PotionEffectType.WITHER,c.power(item,"wither"));
            if(c.roll(item,"lightning") && c.ready(p,"lightning")) {
                target.getWorld().strikeLightningEffect(target.getLocation());damage(target,p,Math.min(20,c.power(item,"lightning")));
            }
            if(c.roll(item,"explosive") && c.ready(p,"explosive")) {
                float strength=(float)Math.min(c.limit("max-explosion-strength",3,0,4),c.power(item,"explosive"));
                generatedDamage=true;try{target.getWorld().createExplosion(target.getLocation(),strength,false,c.plugin.settings.flag("explosion-block-damage",false),p);}finally{generatedDamage=false;}
            }
            if(c.power(item,"launch")>0)c.velocity(target,new Vector(0,Math.min(2,c.power(item,"launch")),0));
            boolean shock=c.power(item,"shockwave")>0 && c.ready(p,"shockwave");
            boolean quake=(item.getType()==Material.MACE || item.getType().name().endsWith("_AXE")) && c.power(item,"earthquake")>0 && c.ready(p,"earthquake");
            boolean chain=c.roll(item,"chain_lightning") && c.ready(p,"chain_lightning");
            boolean wildfire=c.roll(item,"wildfire") && c.ready(p,"wildfire");
            boolean gravity=c.power(item,"gravity")>0 && c.ready(p,"gravity");
            int chainTargets=0;
            if(shock || quake || chain || wildfire || gravity)for(Entity near:c.nearby(target.getLocation())) {
                if(!(near instanceof LivingEntity victim) || victim.equals(p) || victim.equals(target) || victim instanceof ArmorStand)continue;
                if(shock)c.velocity(victim,victim.getLocation().toVector().subtract(p.getLocation().toVector()).setY(0.3));
                if(quake)damage(victim,p,Math.min(20,c.power(item,"earthquake")));
                if(chain && victim instanceof Monster && chainTargets++<c.plugin.settings.integer("max-chain-targets",6,1,12)){
                    victim.getWorld().strikeLightningEffect(victim.getLocation());damage(victim,p,Math.min(20,c.power(item,"chain_lightning")));
                }
                if(wildfire && victim instanceof Monster)victim.setFireTicks(80);
                if(gravity)c.velocity(victim,target.getLocation().toVector().subtract(victim.getLocation().toVector()).multiply(0.2));
            }
        });
    }
    private void damage(LivingEntity target,Player source,double amount){
        if(target.isDead() || amount<=0)return; int invulnerability=target.getNoDamageTicks();target.setNoDamageTicks(0);
        generatedDamage=true;try{target.damage(amount,source);}finally{target.setNoDamageTicks(invulnerability);generatedDamage=false;}
    }
}