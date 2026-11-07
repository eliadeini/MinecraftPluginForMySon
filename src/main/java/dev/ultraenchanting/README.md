# Plugin architecture

`UltraEnchantingPlugin.java` is the Paper entry point. It also implements command execution and tab completion for `/ultraenchant`.

## Startup and shutdown

`onEnable()` saves the default config, loads settings and enchantment definitions, creates item and GUI services, registers the recipe, starts periodic effect work, registers listeners, and connects the command executor. Initialization failures are logged and disable the plugin.

`onDisable()` closes GUI sessions, removes plugin-owned attribute and flight changes, cancels scheduled tasks, and removes the crafting recipe.

## Packages

| Package | Responsibility |
| --- | --- |
| [config](config/README.md) | YAML loading, translated messages, bounded configuration access |
| [enchant](enchant/README.md) | Definitions, registry discovery, ranked search, explicit conflicts |
| [item](item/README.md) | Special item markers, exact enchantment levels, lore, safe item return |
| [gui](gui/README.md) | Per-player inventories, text dialogs, application flow, session cleanup |
| [listener](listener/README.md) | Crafting and placed-table lifecycle |
| [effect](effect/README.md) | Combat, mining, equipment, movement, and curse behavior |
| [logic](logic/README.md) | Server-independent rules covered by JUnit source tests |

## Runtime flow

A recipe or admin command creates a marked table item. Placement copies its marker to the block's tile PDC. Interacting with that block opens a GUI session. Search uses the registry, level input uses pure validation, and applying an enchantment checks explicit conflicts before replacing the input with a modified clone. Effect listeners read persisted levels when appropriate gameplay events occur.

`give`, `open`, and `reload` require `ultraenchanting.admin`; `info` is available without that admin check. The normal table interface separately requires `ultraenchanting.use`.

Reload prepares new definition data and checks config syntax before closing sessions, cleaning passive changes, reloading the config, swapping the registry/settings, and registering the recipe again. The existing periodic scheduler interval is retained; changing `period-ticks` requires restart.

## Development boundaries

Perform Bukkit world and inventory operations on the server thread. Preserve exact stored levels independently of runtime caps. Keep search and conflict rules in the pure logic layer, identity in PDC, and configuration-driven aliases outside GUI handlers.

The current source has passed structural checks only. Use the root README for the remaining compilation, automated-test, and gameplay verification steps.
