# UltraEnchanting

Source package for an intentionally overpowered Paper plugin with a Hebrew inventory interface, official Paper text-input dialogs, all registered vanilla enchantments, and 53 custom enchantments.

## Verification status

This package has **not been compiled or tested on a Minecraft server**. Java, javac and Gradle were unavailable on PATH in the authoring environment. The owner requested a source-only delivery without installing tools. No compiled JAR is included and no successful build is claimed. Review and test on a separate development server before use on a valued world.

The pure logic JUnit tests are included but have not been executed. A PowerShell source/data consistency check is included, along with a manual gameplay verification plan. Static checking cannot establish Java compilation, server behavior, protection-plugin compatibility, or freedom from duplication bugs.

## Target and prerequisites

- Minecraft Java / Paper **26.3**, compile API pinned to **26.3.build.230-stable**.
- **JDK 25**, including javac, already installed by the operator.
- **Gradle 9.1.0 or later**, already available on PATH.
- Compatible Java Edition clients; no client mod and no resource pack.
- Bukkit/Paper main-thread scheduling; this plugin does not target Folia.

Official references checked during implementation:

- https://docs.papermc.io/paper/dev/project-setup/
- https://jd.papermc.io/paper/26.3/
- https://docs.papermc.io/paper/dev/dialogs/
- https://docs.gradle.org/current/userguide/compatibility.html

## Build without installing software

Open a terminal in this project folder after supplying an existing JDK 25 and Gradle 9.1+ on PATH:

```powershell
.\gradlew.bat clean build
```

On an existing Unix development environment:

```sh
sh ./gradlew clean build
```

`gradlew` and `gradlew.bat` are small launchers for an **existing Gradle installation**, not the standard auto-downloading Gradle Wrapper. They never install or download Java or Gradle. Gradle toolchain auto-download is disabled. Resolving the Paper API, its dependencies and JUnit requires network access and downloads project dependencies into `.gradle-user-home/` in this project. Project-local Gradle state goes into `.gradle/` and outputs into `build/`.

Expected output **after a successful build**:

```
build/libs/UltraEnchanting-1.0.0.jar
```

JUnit reports will be in `build/reports/tests/test/index.html`. Production runtime has no bundled third-party dependencies; Paper provides the public APIs used here.

Run the source/data check without Java:

```powershell
.\Verify-Source.ps1
```

## Server setup (operator steps; not performed by the authoring agent)

1. Stop an existing Paper 26.3 server normally.
2. Copy the successfully built JAR into that server's `plugins` directory.
3. Start the existing server with Java 25, for example `java -jar paper.jar --nogui` from its own server directory. The server operator manages the Paper download and EULA separately.
4. Check the console for the plugin's successful load message. Runtime defaults are created under `plugins/UltraEnchanting/`.
5. An operator can use `/ultraenchant give` to receive a special table.

Nothing in this source package installs a server, opens a server directory, starts Minecraft, or copies files into a server automatically.

## Crafting and identity

```
N N N
N E N
O O O
```

N = Netherite Block, E = Enchanting Table, O = Obsidian: five Netherite Blocks, one table and three Obsidian.

The item has a plugin namespaced PDC marker; placement copies it to the public `TileState` PDC and updates the state. Names and lore never establish identity. Normal enchanting tables are unaffected. Survival breaking produces a marked special item, including when the wrong tool is used; Creative breaking follows Creative no-drop behavior. Special tables survive explosions by default. Breaking a table or unloading its chunk closes its sessions and returns their items.

## GUI

Right-click a special table with permission `ultraenchanting.use`. Put an item in the empty slot near the top center, use search, click an enchantment result, choose a level, and click the enchant button. The menu displays the current search, selection and exact level. Arrows navigate the results.

Only ordinary left/right mouse transfers are enabled for the input slot and the player's own inventory. Shift transfers, hotbar swaps, double-click collection, creative cloning, item-dropping keys and drags are blocked while the menu is open. Decorative items cannot be taken. Cursor items retain normal player-inventory ownership.

Search and level entry use Paper's public Dialog API. The input item stays in the per-player session during the dialog. Confirm or cancel returns to the same inventory. Escape is disabled for these dialogs. An abandoned dialog expires after the configured timeout and returns the held input. Callbacks have one use, an expiry, an owner check and a session token. Opening another inventory, disconnecting, closing, reloading or disabling releases the item once. Inventory overflow is dropped at the player's location. Death places the session item in the normal death drops, or returns it when keepInventory is active.

Search examples:

- Unbreaking, unbreak, אמבריקינג, אנברייקינג, עמידות, אמברי
- sharpness, שארפנס, חדות
- efficiency, אפישנסי, יעילות
- mending, מנדינג, תיקון

Normalization ignores case, whitespace, punctuation and combining marks. Exact, prefix and substring matches are ranked before small typo matches. Add aliases to `aliases.yml`; the GUI contains no Hebrew alias list.

## Levels and conflicts

Input accepts ASCII digits representing integers from **1 through 100000000**, inclusive. Signs, spaces, decimals, exponent notation, overflow and larger values are rejected. An invalid value produces a Hebrew message and reopens the level dialog.

Applying an enchantment clones the item and replaces just that enchantment's level. Other enchantments and original lore remain. Exact requested levels are stored as integer entries in a nested namespaced item PDC. Lore is generated from PDC and original lore is saved separately. Glint uses the public ItemMeta override. Enchanted books use stored-enchantment metadata.

Vanilla enchantments are enumerated from the Paper enchantment registry at startup, so new registered vanilla entries are included even if no Hebrew translation has been supplied. A vanilla engine level is bounded by `vanilla-engine-level-cap` (default and hard maximum 255); the requested level remains exact in PDC and lore. **This does not emulate every vanilla enchantment at its requested extended level.** For example, an extended vanilla level above the engine cap has only the bounded native effect. Unsafe levels can still produce very strong vanilla behavior; tune the cap for the server.

Normal vanilla incompatibilities are deliberately ignored: Mending/Infinity, Sharpness/Smite/Bane, Silk Touch/Fortune, Multishot/Piercing and Riptide/Loyalty/Channeling can coexist.

Only the explicit conflict edges in `enchants.yml` reject combinations. Edges work in both directions even if configured on only one side. Rejection leaves the original item untouched, plays an error sound and explains the opposite enchantment in red Hebrew text. Conflicts are checked per item; different equipment pieces can carry opposing effects.

## Custom effects

Each definition includes a stable ID, English and Hebrew names, description, item categories, classification, aliases, conflicts, enable flag, scale, cap, chance and cooldown. Selected levels live in session state and item PDC. Runtime effect strength uses `min(cap, level * scale)` plus independent hard safety limits. Chance effects scale their probability logarithmically with level, capped at 100 percent.

- Combat: Life Steal, Vampire, Lightning, Explosive, Freeze, Poison, Wither, Critical, Double Strike, Triple Strike, Executioner, Giant Slayer, Shockwave, Earthquake, Chain Lightning, Wildfire, Launch, Gravity and Overpower.
- Mining: Telekinesis, Vein Miner, Excavator, Mega Excavator, Tree Feller and Auto Smelt.
- Equipment and utility: Magnet, Speed, Jump Boost, Health Boost, Regeneration, Fire Resistance, Night Vision, Water Breathing, Haste, Flight, Dash, Blink, Magic Shield and Resurrection.
- Negative: Curse of Fragility, Decay, Weakness, Mining Fatigue, Vulnerability, Heavy Falling, Slowness, Heaviness, Frailty, Fire Vulnerability, Blindness, Drowning and Grounding, plus Repulsion.

Combat enchantments activate on swords, axes, mace, trident, bow and crossbow. Projectile weapon metadata is captured at firing. Secondary damage has a synchronous recursion guard and uses attributed public damage calls. Lightning uses a harmless visual strike plus attributed bounded damage. Explosions do not ignite terrain and block damage defaults to false. Wildfire ignites nearby hostile mobs; chain lightning has a separate target cap.

Mining effects activate on appropriate tools, not arbitrary stored items such as a carrot. Vein Miner requires a pickaxe and Tree Feller requires an axe. Connected mining uses a bounded iterative queue and only loaded chunks. Excavators target matching materials in a facing-dependent 3x3 or 5x5 plane, protecting unrelated nearby block types. Extra breaks use `Player.breakBlock` so ordinary break events, tool durability and drops are processed. Cancelled breaks do not get forcibly removed. Auto Smelt uses public material-choice furnace recipes; exact-item recipes are intentionally excluded. Telekinesis transfers drops and leaves overflow in the world.

Armor effects use the strongest applicable worn value, not a sum across all pieces. Night Vision and Water Breathing require a helmet. Attribute modifiers are namespaced and transient. Short potion effects expire after equipment removal rather than deleting effects another plugin may have applied. Armor refresh runs once per configured interval, not every tick. This means flight and armor changes can take up to that interval to update. Existing Creative/Spectator flight is respected. Plugin flight saves the previous allowFlight/flying state in player PDC and restores it when removed, on disconnect or disable.

Dash trigger: **sneak + swap-hands key (default F)** with an applicable enchanted held item or armor. The hand swap is consumed. Blink trigger: **sneak + right-click air** with an applicable enchanted held item or armor. Blink checks loaded chunks, the world border, a clear ray, a two-block landing column and a solid floor. The public teleport event can still cancel it.

Magic Shield blocks an attack once per cooldown. Resurrection uses the native resurrection event, allowing normal totems to work first, and uses a persistent player-PDC cooldown. All effect cooldowns persist with ordinary player saves, so relogging and plugin reloads do not reset them.

## Configuration and permissions

- `config.yml`: any-item storage, engine representation cap, global effect enable, explosions, scan radius, work budgets, velocity, damage and health caps, periodic refresh and dialog timeout.
- `enchants.yml`: every custom definition, translations for known vanilla enchantments and explicit opposites.
- `aliases.yml`: additional searchable names.
- `messages.yml`: Hebrew menu and primary action messages.

`allow-enchant-on-any-item: true` permits enchantment storage on almost any non-air item. Physical effects only activate for meaningful equipment. Setting this false filters and validates using vanilla item applicability or custom categories. The stored maximum is always exactly 100000000; `max-selectable-level` documents that contract and cannot raise or lower it.

`/ultraenchant reload` validates definition and YAML syntax before replacing live state, returns all GUI items and removes plugin attribute/flight changes before refreshing. Changes to `period-ticks` require a normal server restart because the existing scheduler interval is retained. Do not use the server's global `/reload` command.

| Permission | Default | Purpose |
| --- | --- | --- |
| ultraenchanting.use | true | Use tables and activate custom effects |
| ultraenchanting.craft | true | Craft the special table |
| ultraenchanting.admin | op | Give tables, open the debug GUI and reload |

Commands with tab completion:

```
/ultraenchant give [online-player]
/ultraenchant open
/ultraenchant reload
/ultraenchant info
```

## Safety and limitations

Hard implementation ceilings still apply even if configuration is excessive: scans have radius at most 12 blocks, entity output at most 64, connected mining at most 128 blocks, extra hits at most two, chain targets at most twelve, velocity at most three, explosions at most strength four, plugin health bonus at most 100, primary custom damage at most 1000 and potion amplifiers at most four. Lower defaults are supplied. Selected/stored levels never shrink because of these ceilings.

Entity queries are spatially bounded but an extremely dense entity pile can still make a local query costly. There is no full-world scan. Bukkit world and inventory operations run on the server thread. No NMS is used.

Other plugins can change damage, death, drops, flight and inventory behavior. Public break and damage events are used where available, but direct knockback, potion effects and visual lightning are not universal region-protection integrations. Test with the server's own protection plugins. Another plugin changing allowFlight during plugin-granted flight cannot be distinguished perfectly from the saved baseline.

GUI state is in memory. Normal close, disconnect, death, reload and disable have cleanup handlers, but an abrupt process crash or machine power loss during a GUI session is not transactionally recoverable. Do not promise crash-proof item storage or zero duplication risk without live integration tests.

## Gameplay verification plan

On an isolated Paper 26.3 development server after compilation:

1. Craft the exact recipe with and without craft permission. Confirm normal tables still open vanilla enchanting and marked tables reopen after server restart.
2. Break marked tables in Survival (including with the wrong tool) and Creative. Test Silk Touch and explosions; there should be one special Survival drop and no normal-table duplicate.
3. Open two players' independent sessions. Transfer items using ordinary clicks, then try shift-click, double-click, number keys, offhand swaps, middle-click, drag and drop keys. Verify decoration cannot move.
4. Close menus, quit during dialogs, wait for dialog timeout, die with keepInventory on/off, reload the plugin and stop the server normally. Count items before and after, including full inventories and cursor stacks.
5. Verify every listed Hebrew search example and a small typo. Apply level 1 and 100000000; reject zero, negatives, decimals and oversized values. Reapply one enchantment and check unrelated enchantments and original lore.
6. Test all seven vanilla compatibility pairs listed above, then try each configured true opposite in both directions. Confirm rejection preserves the item byte-for-byte.
7. Exercise every custom effect at low and maximum stored levels. Confirm bounded explosions, entity work, velocity, chain targets and health; verify generated hits never recursively retrigger combat effects.
8. Mine protected and unprotected ore, trees and area targets. Confirm protection cancellation, normal drops, overflow, smelting, finite durability and stopping on tool break. Verify a carrot with Vein Miner does nothing.
9. Equip/remove armor and change game mode. Verify attributes, potion expiry, flight restoration and native totems/resurrection cooldown across reconnects and plugin reload.
10. Test Dash and Blink near walls, borders, hazardous floor blocks and unloaded chunks. Recheck behavior with other plugins enabled.