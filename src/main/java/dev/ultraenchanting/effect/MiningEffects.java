package dev.ultraenchanting.effect;

import dev.ultraenchanting.item.ItemData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.inventory.*;
import java.util.*;

public final class MiningEffects implements Listener {
    private final EffectContext c;
    private final Set<UUID> mining=new HashSet<>();
    private final Map<Material,Material> smelting=new EnumMap<>(Material.class);
    public MiningEffects(EffectContext c){this.c=c;refreshRecipes();}
    private void refreshRecipes(){
        for(Iterator<Recipe> iterator=Bukkit.recipeIterator();iterator.hasNext();){
            Recipe recipe=iterator.next();
            if(recipe instanceof FurnaceRecipe furnace && furnace.getResult().getAmount()==1){
                if(furnace.getInputChoice() instanceof RecipeChoice.MaterialChoice choice)
                    for(Material material:choice.getChoices())smelting.putIfAbsent(material,furnace.getResult().getType());
            }
        }
    }
    private boolean ore(Material material){return material.name().endsWith("_ORE") || material==Material.ANCIENT_DEBRIS;}
    private boolean log(Material material){return material.name().endsWith("_LOG") || material.name().endsWith("_STEM");}
    private boolean acceptable(Block block,Player p){
        return !block.getType().isAir() && block.getType().getHardness()>=0 && !(block.getState() instanceof org.bukkit.block.TileState) &&
            block.getWorld().getWorldBorder().isInside(block.getLocation()) && block.getLocation().distanceSquared(p.getLocation())<=256;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void mined(BlockBreakEvent e){
        Player p=e.getPlayer();if(!c.enabled(p) || mining.contains(p.getUniqueId()) || p.getGameMode()==GameMode.CREATIVE)return;
        ItemStack item=p.getInventory().getItemInMainHand();if(!c.tool(item))return;
        Block origin=e.getBlock();Material type=origin.getType();
        boolean vein=ore(type) && item.getType().name().endsWith("_PICKAXE") && c.power(item,"vein_miner")>0;
        boolean tree=log(type) && item.getType().name().endsWith("_AXE") && c.power(item,"tree_feller")>0;
        boolean area=c.power(item,"excavator")>0 || c.power(item,"mega_excavator")>0;
        if(!vein && !tree && !area)return;
        ItemStack expected=item.clone();Location center=origin.getLocation();
        Set<Location> targets=new LinkedHashSet<>();int cap=c.plugin.settings.integer("max-blocks",48,1,128);
        if(vein || tree){
            Deque<Location> queue=new ArrayDeque<>();Set<Location> visited=new HashSet<>();queue.add(center);visited.add(center);
            while(!queue.isEmpty() && targets.size()<cap && visited.size()<cap*12){
                Location current=queue.removeFirst();
                for(int dx=-1;dx<=1;dx++)for(int dy=tree?0:-1;dy<=1;dy++)for(int dz=-1;dz<=1;dz++){
                    if(dx==0 && dy==0 && dz==0)continue;
                    Location at=current.clone().add(dx,dy,dz);
                    if(!visited.add(at) || visited.size()>cap*12 || !at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))continue;
                    if(at.distanceSquared(center)>144 || at.getBlock().getType()!=type || targets.size()>=cap)continue;
                    targets.add(at);queue.addLast(at);
                }
            }
        }else{
            int radius=c.power(item,"mega_excavator")>0?2:1;var direction=p.getEyeLocation().getDirection();
            boolean horizontal=Math.abs(direction.getY())>0.6;boolean xPlane=Math.abs(direction.getX())>Math.abs(direction.getZ());
            for(int a=-radius;a<=radius;a++)for(int b=-radius;b<=radius;b++){
                if(a==0 && b==0)continue;
                Location at=horizontal?center.clone().add(a,0,b):xPlane?center.clone().add(0,a,b):center.clone().add(a,b,0);
                if(targets.size()<cap && at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4) && at.getBlock().getType()==type)targets.add(at);
            }
        }
        Bukkit.getScheduler().runTask(c.plugin,()->{
            if(!p.isOnline() || !c.enabled(p) || p.getWorld()!=center.getWorld() || (p.getInventory().getItemInMainHand().getType()!=expected.getType() || !c.plugin.items.levels(p.getInventory().getItemInMainHand()).equals(c.plugin.items.levels(expected))))return;
            mining.add(p.getUniqueId());try{
                int count=0;
                for(Location at:targets){
                    if(++count>cap || !at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4))break;
                    ItemStack held=p.getInventory().getItemInMainHand();
                    // Durability can change after each break, but the enchantment metadata and material must remain intact.
                    if(held.getType()!=expected.getType() || !c.tool(held) || !c.plugin.items.levels(held).equals(c.plugin.items.levels(expected)))break;
                    Block block=at.getBlock();if(block.getType()!=type || !acceptable(block,p))continue;
                    p.breakBlock(block); // Calls normal break events, protection hooks, drops and tool durability.
                }
            }finally{mining.remove(p.getUniqueId());}
        });
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void drops(BlockDropItemEvent e){
        Player p=e.getPlayer();if(!c.enabled(p))return;ItemStack tool=p.getInventory().getItemInMainHand();if(!c.tool(tool))return;
        boolean smelt=c.power(tool,"auto_smelt")>0,tele=c.power(tool,"telekinesis")>0;
        if(!smelt && !tele)return;
        var drops=new ArrayList<>(e.getItems());
        // Wait until event cancellation and entity spawning have finished. Never transfer a cancelled drop.
        Bukkit.getScheduler().runTask(c.plugin,()->{
            if(e.isCancelled() || !p.isOnline() || !c.enabled(p))return;
            for(var entity:drops){
                if(!entity.isValid() || entity.isDead())continue;
                ItemStack stack=entity.getItemStack();
                if(smelt && smelting.containsKey(stack.getType()))stack=new ItemStack(smelting.get(stack.getType()),stack.getAmount());
                if(tele){
                    var leftovers=p.getInventory().addItem(stack);
                    if(leftovers.isEmpty())entity.remove();
                    else entity.setItemStack(leftovers.values().iterator().next());
                }else entity.setItemStack(stack);
            }
        });
    }
}