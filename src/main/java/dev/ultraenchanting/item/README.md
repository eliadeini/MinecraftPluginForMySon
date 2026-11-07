# Item identity and enchantment persistence

`ItemData.java` creates the special table item, reads enchantment levels, applies a selected definition, and returns items to players.

## Persistent keys

| Key | Value and purpose |
| --- | --- |
| `ultraenchanting:ultimate_table` | Byte marker identifying a special table item; copied to placed tile PDC |
| `ultraenchanting:levels` | Nested PDC containing integer levels keyed by complete enchantment IDs |
| `ultraenchanting:original_lore` | List of serialized Adventure components preserving lore from before the first plugin application |

Identity and levels come from metadata, never from display names or lore. Treat stable keys and their value types as a persistence format: changing them requires a migration strategy for existing items.

## Read and write flow

`levels(item)` merges native item enchantments, stored book enchantments, and valid plugin PDC entries. PDC entries override native levels, preserving exact values through 100000000. `level(item, shortId)` retrieves a custom value using the plugin namespace.

`apply(original, definition, level)` validates the level and clones the stack. It replaces only the selected entry in the merged map and writes the result to nested PDC. For native enchantments, the native representation is limited by the configured engine cap, with a hard maximum of 255. Books use stored-enchantment metadata.

The method saves original lore once, rebuilds presentation lore from the exact levels, and enables the public glint override. Native level caps affect engine behavior, not the persisted/displayed requested value. This does not emulate every native enchantment beyond that cap.

`give(player, item)` adds to the player inventory and drops leftovers at the player's position. Callers must clear their old ownership before invoking it, otherwise returning an item could duplicate it.

## Verification

Check native enchantments and enchanted books, reapplication, preservation of unrelated levels and pre-existing lore, null/air inputs, high-level round trips, stack amounts, and inventory overflow. Conflict rejection happens in the GUI before this writer is called; this class does not independently enforce the conflict graph.
