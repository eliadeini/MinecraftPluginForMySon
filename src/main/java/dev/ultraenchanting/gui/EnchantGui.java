package dev.ultraenchanting.gui;

import dev.ultraenchanting.UltraEnchantingPlugin;
import dev.ultraenchanting.enchant.Definition;
import dev.ultraenchanting.item.ItemData;
import dev.ultraenchanting.logic.Rules;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.*;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import java.time.Duration;
import java.util.*;

public final class EnchantGui implements Listener {
    private static final int INPUT=4,START=18,COUNT=27;
    private final UltraEnchantingPlugin plugin;
    private final Map<UUID,GuiSession> sessions=new HashMap<>();
    public EnchantGui(UltraEnchantingPlugin plugin) {
        this.plugin=plugin;
        Bukkit.getScheduler().runTaskTimer(plugin,()->{
            for(GuiSession s:new ArrayList<>(sessions.values())) {
                boolean expired=s.dialog && System.currentTimeMillis()>s.deadline;
                boolean gone=s.table!=null && (!s.table.getWorld().isChunkLoaded(s.table.getBlockX()>>4,s.table.getBlockZ()>>4) ||
                    !(s.table.getBlock().getState() instanceof org.bukkit.block.TileState tile) ||
                    !tile.getPersistentDataContainer().has(plugin.items.tableKey,org.bukkit.persistence.PersistentDataType.BYTE));
                if(expired || gone)close(s);
            }
        },20,20);
    }
    public void open(Player player,Location table) {
        GuiSession old=sessions.get(player.getUniqueId());if(old!=null)close(old);
        GuiSession s=new GuiSession(player.getUniqueId(),table,Component.text(plugin.settings.text("title")));
        sessions.put(s.owner,s);render(s);player.openInventory(s.getInventory());
    }
    private ItemStack button(Material material,String text) {
        ItemStack item=new ItemStack(material);item.editMeta(m->m.displayName(Component.text(text,NamedTextColor.GOLD)));return item;
    }
    private void render(GuiSession s) {
        Inventory inv=s.getInventory();
        for(int i=0;i<54;i++)if(i!=INPUT)inv.setItem(i,button(Material.GRAY_STAINED_GLASS_PANE," "));
        s.results=plugin.registry.search(s.search).stream().filter(d->plugin.settings.flag("allow-enchant-on-any-item",true) ||
            inv.getItem(INPUT)==null || (d.vanilla()!=null?d.vanilla().canEnchantItem(inv.getItem(INPUT)):d.accepts(inv.getItem(INPUT).getType()))).toList();
        s.page=Math.max(0,Math.min(s.page,Math.max(0,(s.results.size()-1)/COUNT)));
        for(int i=0;i<COUNT;i++) {
            int index=s.page*COUNT+i;
            if(index<s.results.size()) {
                Definition d=s.results.get(index);ItemStack entry=button(Material.ENCHANTED_BOOK,d.hebrew()+" ("+d.english()+")");
                entry.editMeta(m->m.lore(List.of(Component.text(d.description()),Component.text(d.classification()))));inv.setItem(START+i,entry);
            }
        }
        inv.setItem(0,button(Material.OAK_SIGN,plugin.settings.text("search")+": "+s.search));
        inv.setItem(8,button(Material.PAPER,plugin.settings.text("input")));
        Definition selected=plugin.registry.get(s.selected);
        inv.setItem(10,button(Material.BOOK,plugin.settings.text("selected")+": "+(selected==null?"—":selected.hebrew())));
        inv.setItem(12,button(Material.EXPERIENCE_BOTTLE,plugin.settings.text("level")+": "+s.level));
        inv.setItem(45,button(Material.ARROW,plugin.settings.text("previous")));
        inv.setItem(48,button(Material.LIME_CONCRETE,plugin.settings.text("apply")));
        inv.setItem(49,button(Material.BARRIER,plugin.settings.text("close")));
        inv.setItem(53,button(Material.ARROW,plugin.settings.text("next")+" "+(s.page+1)));
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof GuiSession s))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(s.owner) || s.closed || s.dialog)return;
        int slot=e.getRawSlot();
        // Only ordinary mouse transfers are permitted. No shift, collect, hotbar, creative clone or drop paths.
        if((e.getClick()==ClickType.LEFT || e.getClick()==ClickType.RIGHT) && (slot==INPUT || slot>=54)) {
            e.setCancelled(false);
            Bukkit.getScheduler().runTask(plugin,()->{if(!s.closed && !s.dialog)render(s);});return;
        }
        if(slot<0 || slot>=54 || e.getClick()!=ClickType.LEFT)return;
        if(slot>=START && slot<START+COUNT) {
            int index=s.page*COUNT+slot-START;if(index<s.results.size())s.selected=s.results.get(index).id();render(s);return;
        }
        switch(slot) {
            case 0 -> Bukkit.getScheduler().runTask(plugin,()->input(p,s,false));
            case 12 -> Bukkit.getScheduler().runTask(plugin,()->input(p,s,true));
            case 45 -> {s.page--;render(s);}
            case 53 -> {s.page++;render(s);}
            case 48 -> apply(p,s);
            case 49 -> p.closeInventory();
            default -> {}
        }
    }
    @EventHandler(priority=EventPriority.HIGHEST,ignoreCancelled=true)
    public void drag(InventoryDragEvent e) {if(e.getView().getTopInventory().getHolder() instanceof GuiSession)e.setCancelled(true);}
    @EventHandler public void inventoryClose(InventoryCloseEvent e) {
        if(e.getInventory().getHolder() instanceof GuiSession s && !s.dialog)close(s);
    }
    @EventHandler public void quit(PlayerQuitEvent e) {GuiSession s=sessions.get(e.getPlayer().getUniqueId());if(s!=null)close(s);}
    @EventHandler public void otherInventory(InventoryOpenEvent e) {
        GuiSession s=sessions.get(e.getPlayer().getUniqueId());
        if(s!=null && s.dialog && e.getInventory()!=s.getInventory())close(s);
    }
    @EventHandler(priority=EventPriority.HIGHEST)
    public void death(org.bukkit.event.entity.PlayerDeathEvent e) {
        GuiSession s=sessions.remove(e.getEntity().getUniqueId());if(s==null || s.closed)return;
        s.closed=true;s.dialog=false;s.token=UUID.randomUUID();
        ItemStack item=s.getInventory().getItem(INPUT);s.getInventory().setItem(INPUT,null);
        if(item!=null){if(e.getKeepInventory())ItemData.give(e.getEntity(),item);else e.getDrops().add(item);}
    }
    private void apply(Player p,GuiSession s) {
        if(!p.hasPermission("ultraenchanting.use")){plugin.message(p,"no-permission");return;}
        ItemStack original=s.getInventory().getItem(INPUT);Definition d=plugin.registry.get(s.selected);
        if(original==null || original.getType().isAir() || d==null || !d.enabled()){plugin.message(p,"missing");return;}
        if(!plugin.settings.flag("allow-enchant-on-any-item",true) && !(d.vanilla()!=null?d.vanilla().canEnchantItem(original):d.accepts(original.getType()))) {
            plugin.message(p,"inapplicable");return;
        }
        Optional<String> conflict=plugin.registry.conflict(d.id(),plugin.items.levels(original).keySet());
        if(conflict.isPresent()) {
            Definition other=plugin.registry.get(conflict.get());
            p.sendMessage(Component.text(plugin.settings.text("conflict").replace("{selected}",d.hebrew()).replace("{existing}",other==null?conflict.get():other.hebrew()),NamedTextColor.RED));
            p.playSound(p.getLocation(),Sound.BLOCK_NOTE_BLOCK_BASS,1,0.5f);return;
        }
        s.getInventory().setItem(INPUT,plugin.items.apply(original,d,s.level));
        plugin.message(p,"success");p.playSound(p.getLocation(),Sound.BLOCK_ENCHANTMENT_TABLE_USE,1,1);
    }
    private void input(Player p,GuiSession s,boolean level) {
        if(s.closed || !p.isOnline() || sessions.get(s.owner)!=s)return;
        s.dialog=true;s.token=UUID.randomUUID();UUID token=s.token;
        int timeout=plugin.settings.integer("dialog-timeout-seconds",120,15,600);
        s.deadline=System.currentTimeMillis()+timeout*1000L;
        p.closeInventory();
        var options=ClickCallback.Options.builder().uses(1).lifetime(Duration.ofSeconds(timeout)).build();
        var confirm=DialogAction.customClick((response,audience)->{
            if(!plugin.isEnabled() || !(audience instanceof Player actor) || !actor.getUniqueId().equals(s.owner))return; String value=response.getText("value");
            Bukkit.getScheduler().runTask(plugin,()->{
                if(!valid(p,s,token))return;
                if(level) {
                    try{s.level=Rules.level(value);}catch(IllegalArgumentException ex){plugin.message(p,"invalid-level");input(p,s,true);return;}
                }else{s.search=value==null?"":value;s.page=0;}
                s.dialog=false;render(s);p.openInventory(s.getInventory());
            });
        },options);
        var cancel=DialogAction.customClick((response,audience)->{
            if(!plugin.isEnabled())return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if(audience instanceof Player actor && actor.getUniqueId().equals(s.owner) && valid(p,s,token)){s.token=UUID.randomUUID();s.dialog=false;render(s);p.openInventory(s.getInventory());}
            });
        },options);
        p.showDialog(Dialog.create(builder->builder.empty()
            .base(DialogBase.builder(Component.text(plugin.settings.text(level?"level":"search")))
                .canCloseWithEscape(false)
                .inputs(List.of(DialogInput.text("value",Component.text(plugin.settings.text(level?"level":"search")))
                    .initial(level?Integer.toString(s.level):s.search).maxLength(level?12:80).build())).build())
            .type(DialogType.confirmation(ActionButton.create(Component.text(plugin.settings.text("confirm")),null,150,confirm),
                ActionButton.create(Component.text(plugin.settings.text("cancel")),null,150,cancel)))));
    }
    private boolean valid(Player p,GuiSession s,UUID token) {
        return plugin.isEnabled() && p.isOnline() && !s.closed && s.dialog && s.token.equals(token) && sessions.get(s.owner)==s && System.currentTimeMillis()<=s.deadline;
    }
    private void close(GuiSession s) {
        if(s.closed)return;boolean wasDialog=s.dialog;s.closed=true;s.dialog=false;s.token=UUID.randomUUID();sessions.remove(s.owner,s);
        ItemStack item=s.getInventory().getItem(INPUT);s.getInventory().setItem(INPUT,null);
        Player p=Bukkit.getPlayer(s.owner);
        if(p!=null){if(p.getOpenInventory().getTopInventory()==s.getInventory())p.closeInventory();if(wasDialog)p.closeDialog();ItemData.give(p,item);}
        else if(item!=null && s.table!=null && s.table.getWorld()!=null)s.table.getWorld().dropItemNaturally(s.table,item);
    }
    public void closeAll(){for(GuiSession s:new ArrayList<>(sessions.values()))close(s);}
    public void closeAt(Location table){for(GuiSession s:new ArrayList<>(sessions.values()))if(table.equals(s.table))close(s);}
}