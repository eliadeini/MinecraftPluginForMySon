package dev.ultraenchanting.config;

import dev.ultraenchanting.logic.Rules;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.util.*;

public final class Settings {
    private final JavaPlugin plugin;
    public final YamlConfiguration enchants, aliases, messages;
    public Settings(JavaPlugin plugin) {
        this.plugin=plugin;
        for(String name:List.of("enchants.yml","aliases.yml","messages.yml"))
            if(!new File(plugin.getDataFolder(),name).exists()) plugin.saveResource(name,false);
        enchants=read("enchants.yml"); aliases=read("aliases.yml"); messages=read("messages.yml");
    }
    private YamlConfiguration read(String name) {
        YamlConfiguration yaml=new YamlConfiguration();
        try { yaml.load(new File(plugin.getDataFolder(),name)); }
        catch(Exception ex) { throw new IllegalArgumentException("Invalid "+name,ex); }
        return yaml;
    }
    public String text(String key) { return messages.getString(key,key); }
    public double number(String key,double fallback,double min,double hardMax) {
        return Rules.bounded(plugin.getConfig().getDouble(key,fallback),min,hardMax);
    }
    public int integer(String key,int fallback,int min,int max) { return (int)number(key,fallback,min,max); }
    public boolean flag(String key,boolean fallback) { return plugin.getConfig().getBoolean(key,fallback); }
}