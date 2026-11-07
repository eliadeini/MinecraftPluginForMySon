# Special-table lifecycle

`Tables.java` is the event listener connecting special item identity to crafting and world blocks.

## Recipe and permissions

`registerRecipe()` replaces the plugin's namespaced recipe with the exact `NNN / NEN / OOO` layout: five Netherite Blocks, one Enchanting Table, and three Obsidian. The output is created by `ItemData.table()` with its persistent marker.

Preparation and craft handlers enforce `ultraenchanting.craft`. Right-click handling checks `ultraenchanting.use` before opening the custom GUI. The main hand is processed once to avoid duplicate offhand opens.

## Placement, breaking, and explosions

Placement copies the item marker onto the supported tile-state PDC and updates the block state. `special(block)` requires both the Enchanting Table material and the marker.

For uncancelled special-table breaks, the listener suppresses normal drops, closes associated GUI sessions, and schedules one marked item drop outside Creative mode. A drop-event handler also cancels a marked block's normal drop event, preventing a second normal-table output if another handler re-enables it. Survival recovery does not require the vanilla correct tool.

Entity and block explosion handlers remove marked tables from their affected block lists. Ordinary tables pass through to vanilla behavior.

## Integration checks

Test cancelled placements and breaks, a full player inventory, wrong-tool Survival breaks, Creative breaks, explosions, other plugins changing drop behavior, and multiple viewers of the same table. Confirm one special Survival drop and no additional normal table drop. Test persistence through a normal save/restart. These scenarios have not yet been exercised on a server.
