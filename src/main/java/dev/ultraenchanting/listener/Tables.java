package dev.ultraenchanting.listener;

import dev.ultraenchanting.UltraEnchantingPlugin;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;

public final class Tables implements Listener {
    private final UltraEnchantingPlugin plugin;
    public Tables(UltraEnchantingPlugin plugin) {this.plugin=plugin;}
    public void registerRecipe() {
        NamespacedKey key=new NamespacedKey(plugin,"ultimate_table");Bukkit.removeRecipe(key);
        ShapedRecipe recipe=new ShapedRecipe(key,plugin.items.table());
        recipe.shape("NNN","NEN","OOO");recipe.setIngredient('N',Material.NETHERITE_BLOCK);
        recipe.setIngredient('E',Material.ENCHANTING_TABLE);recipe.setIngredient('O',Material.OBSIDIAN);Bukkit.addRecipe(recipe);
    }
    public boolean special(Block block) {return block!=null && block.getType()==Material.ENCHANTING_TABLE &&
        block.getState() instanceof TileState tile && tile.getPersistentDataContainer().has(plugin.items.tableKey,PersistentDataType.BYTE);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void place(BlockPlaceEvent e) {
        if(plugin.items.isTable(e.getItemInHand()) && e.getBlockPlaced().getState() instanceof TileState tile) {
            tile.getPersistentDataContainer().set(plugin.items.tableKey,PersistentDataType.BYTE,(byte)1);tile.update();
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void use(PlayerInteractEvent e) {
        if(e.getAction()!=Action.RIGHT_CLICK_BLOCK || !special(e.getClickedBlock()))return;
        e.setCancelled(true);
        if(e.getHand()!=EquipmentSlot.HAND)return;
        if(e.getPlayer().hasPermission("ultraenchanting.use"))plugin.gui.open(e.getPlayer(),e.getClickedBlock().getLocation());
        else plugin.message(e.getPlayer(),"no-permission");
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void suppressNormalDrop(BlockBreakEvent e) {if(special(e.getBlock()))e.setDropItems(false);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void broken(BlockBreakEvent e) {
        if(!special(e.getBlock()))return;
        Location location=e.getBlock().getLocation();plugin.gui.closeAt(location);
        if(e.getPlayer().getGameMode()!=GameMode.CREATIVE)Bukkit.getScheduler().runTask(plugin,()->{
            if(!e.isCancelled())location.getWorld().dropItemNaturally(location.clone().add(0.5,0.5,0.5),plugin.items.table());
        });
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drops(BlockDropItemEvent e) {
        if(e.getBlockState() instanceof TileState tile && tile.getPersistentDataContainer().has(plugin.items.tableKey,PersistentDataType.BYTE))e.setCancelled(true);
    }
    // Special stations survive explosions; normal tables retain normal behavior.
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void explosion(EntityExplodeEvent e){e.blockList().removeIf(this::special);}
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void explosion(BlockExplodeEvent e){e.blockList().removeIf(this::special);}
    @EventHandler public void prepare(PrepareItemCraftEvent e) {
        if(plugin.items.isTable(e.getInventory().getResult()) && e.getView().getPlayer() instanceof Player p && !p.hasPermission("ultraenchanting.craft"))e.getInventory().setResult(null);
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void craft(CraftItemEvent e) {
        if(e.getRecipe().getResult()!=null && plugin.items.isTable(e.getRecipe().getResult()) && !e.getWhoClicked().hasPermission("ultraenchanting.craft"))e.setCancelled(true);
    }
}