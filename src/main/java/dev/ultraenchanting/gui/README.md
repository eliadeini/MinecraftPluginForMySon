# Inventory GUI and dialogs

This package owns the per-player enchanting workflow and the input item while a session is open.

## Files and layout

`GuiSession.java` implements `InventoryHolder`. It stores the owner UUID, optional station location, 54-slot inventory, search text, selected ID, exact selected level, result page/list, dialog state, timeout, and callback token. An admin debug session can have no physical table location.

`EnchantGui.java` renders the inventory and handles clicks, drags, closes, disconnects, death, other inventories, dialog input, application, expiry, and cleanup.

| Slot | Purpose |
| --- | --- |
| 0 | Search dialog and current query |
| 4 | Input item |
| 8 | Input hint |
| 10 | Current selected enchantment |
| 12 | Level dialog and chosen level |
| 18–44 | Up to 27 search results |
| 45 / 53 | Previous / next page |
| 48 | Apply selected enchantment |
| 49 | Close |

## Session and item lifecycle

Opening creates one session per player and closes any previous session. Ordinary left/right transfers are allowed for the input and player inventory; other interaction paths and all drags are blocked. UI controls and result icons are decorative, not transferable items.

A dialog temporarily closes the inventory while the session retains its input. Paper dialog callbacks have a single use, owner checks, an expiry, and a session token. Callback work is scheduled onto the server thread. Confirm or cancel reopens the same inventory; invalid level input shows a message and reopens level entry. Escape is disabled for these dialogs.

A periodic task closes expired dialogs and sessions whose table marker is no longer available, including unloaded station chunks. Other-inventory opening, ordinary close, disconnect, table break, reload, and disable also release ownership. Cleanup marks the session closed and clears the input before returning it, making repeated cleanup calls harmless. Inventory overflow drops normally. Death routes the item to death drops or the inventory according to keepInventory.

## Application contract

`apply()` verifies use permission, input, enabled selection, optional item applicability, and explicit opposite conflicts. On success it replaces the input with `ItemData.apply()`'s clone. Rejection must leave the existing item untouched.

Search and validation belong in the registry and pure rules layer. Translation strings come from `messages.yml`. Keep callback tokens invalidated whenever ownership ends, and keep Bukkit operations out of asynchronous callback work.

## Tests still needed

Exercise all click types, cursor stacks, simultaneous players, callback expiry, disconnect during a dialog, full inventory, death with either keepInventory setting, and station destruction. Session state is in memory and is not transactionally recoverable after abrupt process failure. The existing static checker does not prove duplication resistance.
