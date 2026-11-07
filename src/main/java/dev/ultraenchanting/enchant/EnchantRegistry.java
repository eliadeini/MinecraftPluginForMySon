package dev.ultraenchanting.enchant;

import dev.ultraenchanting.config.Settings;
import dev.ultraenchanting.logic.Rules;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import java.util.*;

public final class EnchantRegistry {
    private final Map<String,Definition> definitions=new TreeMap<>();
    private final Map<String,Set<String>> conflicts=new HashMap<>();
    public EnchantRegistry(Settings settings) {
        for(Enchantment enchant:RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
            String id=enchant.getKey().toString();
            String name=Arrays.stream(enchant.getKey().getKey().split("_")).map(s -> s.substring(0,1).toUpperCase(Locale.ROOT)+s.substring(1)).reduce((a,b)->a+" "+b).orElse(id);
            add(settings,id,name,enchant);
        }
        ConfigurationSection custom=settings.enchants.getConfigurationSection("custom");
        if(custom!=null) for(String id:custom.getKeys(false)) add(settings,"ultraenchanting:"+id,id.replace('_',' '),null);
        for(Definition d:definitions.values()) {
            for(String other:d.conflicts()) if(!definitions.containsKey(other)) throw new IllegalArgumentException("Unknown conflict: "+other);
            conflicts.put(d.id(),d.conflicts());
        }
    }
    private void add(Settings settings,String id,String fallback,Enchantment vanilla) {
        String shortId=id.substring(id.indexOf(':')+1);
        String path=(vanilla==null?"custom.":"vanilla.")+shortId;
        var y=settings.enchants;
        Definition d=new Definition(id,y.getString(path+".english",fallback),y.getString(path+".hebrew",fallback),
            settings.aliases.getStringList(shortId),y.getString(path+".description",fallback),
            Set.copyOf(y.getStringList(path+".categories").isEmpty()?List.of("any"):y.getStringList(path+".categories")),
            y.getString(path+".classification","neutral"),Set.copyOf(y.getStringList(path+".conflicts")),
            y.getBoolean(path+".enabled",true),Rules.bounded(y.getDouble(path+".scale",0.1),0,100),
            Rules.bounded(y.getDouble(path+".cap",2),0,100),Rules.bounded(y.getDouble(path+".chance",0.2),0,1),
            (int)Rules.bounded(y.getInt(path+".cooldown-seconds",3),1,86400),vanilla);
        definitions.put(id,d);
    }
    public Definition get(String id) { return definitions.get(id); }
    public Collection<Definition> all() { return definitions.values(); }
    public Optional<String> conflict(String id,Set<String> existing) { return Rules.conflict(id,existing,conflicts); }
    public List<Definition> search(String query) {
        return definitions.values().stream().filter(Definition::enabled).filter(d->Rules.score(query,d.terms())>0)
            .sorted(Comparator.<Definition>comparingInt(d->Rules.score(query,d.terms())).reversed().thenComparing(Definition::english)).toList();
    }
}