package dev.ultraenchanting.gui;

import java.util.*;
import org.bukkit.*;
import org.bukkit.inventory.*;
import dev.ultraenchanting.enchant.Definition;

public final class GuiSession implements InventoryHolder {
    public final UUID owner;
    public final Location table;
    private final Inventory inventory;
    public String search="", selected="";
    public int level=1,page=0;
    public boolean dialog=false, closed=false;
    public long deadline=0;
    public UUID token=UUID.randomUUID();
    public List<Definition> results=List.of();
    public GuiSession(UUID owner,Location table,net.kyori.adventure.text.Component title) {
        this.owner=owner;this.table=table;this.inventory=Bukkit.createInventory(this,54,title);
    }
    @Override public Inventory getInventory(){return inventory;}
}