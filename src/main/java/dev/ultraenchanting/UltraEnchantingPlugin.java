package dev.ultraenchanting;

import dev.ultraenchanting.config.Settings;
import dev.ultraenchanting.enchant.EnchantRegistry;
import dev.ultraenchanting.effect.*;
import dev.ultraenchanting.gui.EnchantGui;
import dev.ultraenchanting.item.ItemData;
import dev.ultraenchanting.listener.Tables;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class UltraEnchantingPlugin extends JavaPlugin implements TabExecutor {
    public Settings settings;
    public EnchantRegistry registry;
    public ItemData items;
    public EnchantGui gui;
    private PassiveEffects passive;
    private Tables tables;
    @Override public void onEnable(){
        saveDefaultConfig();
        try{
            settings=new Settings(this);registry=new EnchantRegistry(settings);items=new ItemData(this);gui=new EnchantGui(this);
            tables=new Tables(this);tables.registerRecipe();
            EffectContext context=new EffectContext(this);passive=new PassiveEffects(context);
            for(var listener:List.of(gui,tables,passive,new CombatEffects(context),new MiningEffects(context)))getServer().getPluginManager().registerEvents(listener,this);
            PluginCommand command=Objects.requireNonNull(getCommand("ultraenchant"));command.setExecutor(this);command.setTabCompleter(this);
            getLogger().info("Loaded "+registry.all().size()+" enchantments. Exact stored level limit: 100000000.");
        }catch(Exception ex){getLogger().log(java.util.logging.Level.SEVERE,"Cannot initialize UltraEnchanting",ex);getServer().getPluginManager().disablePlugin(this);}
    }
    @Override public void onDisable(){
        if(gui!=null)gui.closeAll();if(passive!=null)passive.cleanAll();Bukkit.getScheduler().cancelTasks(this);
        Bukkit.removeRecipe(new NamespacedKey(this,"ultimate_table"));
    }
    public void message(CommandSender sender,String key){sender.sendMessage(Component.text(settings.text(key),NamedTextColor.AQUA));}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args){
        String action=args.length==0?"info":args[0].toLowerCase(Locale.ROOT);
        if(action.equals("info")){sender.sendMessage(Component.text("UltraEnchanting "+getPluginMeta().getVersion()+" | Paper 26.3 | Java 25 | 1–100000000"));return true;}
        if(!sender.hasPermission("ultraenchanting.admin")){message(sender,"no-permission");return true;}
        switch(action){
            case "give"->{
                Player target=args.length>=2?Bukkit.getPlayerExact(args[1]):sender instanceof Player p?p:null;
                if(target==null){sender.sendMessage(Component.text("/ultraenchant give <online-player>"));return true;}
                ItemData.give(target,items.table());
            }
            case "open"->{if(sender instanceof Player p)gui.open(p,null);}
            case "reload"->{
                try{
                    // Parse and validate definition files before replacing live state.
                    Settings nextSettings=new Settings(this);EnchantRegistry nextRegistry=new EnchantRegistry(nextSettings);
                    var configCheck=new org.bukkit.configuration.file.YamlConfiguration();
                    configCheck.load(new java.io.File(getDataFolder(),"config.yml"));
                    gui.closeAll();passive.cleanAll();reloadConfig();settings=nextSettings;registry=nextRegistry;tables.registerRecipe();message(sender,"reloaded");
                }catch(Exception ex){sender.sendMessage(Component.text("Reload failed: "+ex.getMessage(),NamedTextColor.RED));getLogger().log(java.util.logging.Level.WARNING,"Reload rejected",ex);}
            }
            default->{return false;}
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args){
        List<String> options=args.length==1?(sender.hasPermission("ultraenchanting.admin")?List.of("give","open","reload","info"):List.of("info")):
            args.length==2 && args[0].equalsIgnoreCase("give") && sender.hasPermission("ultraenchanting.admin")?Bukkit.getOnlinePlayers().stream().map(Player::getName).toList():List.of();
        String prefix=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);return options.stream().filter(s->s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}