# Gameplay effects

These listeners translate persisted custom levels into bounded gameplay behavior. All world and inventory operations use server-thread events or synchronous scheduled tasks.

## Files

| File | Responsibility |
| --- | --- |
| `EffectContext.java` | Shared level lookup, applicability helpers, chance rolls, cooldowns, healing, potions, velocity, and local entity queries |
| `CombatEffects.java` | Primary damage modifiers and delayed on-hit effects; bow/trident metadata snapshots |
| `MiningEffects.java` | Connected/area mining, furnace-result conversion, and inventory delivery of drops |
| `PassiveEffects.java` | Periodic armor/utility effects, attribute/flight cleanup, defensive effects, movement triggers, and item curses |

## Strength, ownership, and cooldowns

`EffectContext.enabled(player)` checks the global feature toggle, use permission, and whether the player is alive. `power(item, id)` resolves enabled custom metadata and calculates effective strength from exact item levels. `armor(...)` takes the strongest applicable worn value. Effect handlers add independent hard limits to definition caps.

Chance rolls scale logarithmically with stored level and stop at 100 percent. `ready(...)` both checks and consumes a cooldown. Expiry timestamps are stored under `ultraenchanting:cooldown_<id>` in player PDC and persist with normal player saves.

Local entity queries limit radius and returned results. The underlying query still examines entities within its spatial area, so a dense pile can remain costly.

## Combat

`offense()` adjusts the primary damage at HIGH priority. `impact()` observes uncancelled, positive final damage at MONITOR priority and schedules healing, extra strikes, status effects, lightning, explosions, knockback, and nearby effects.

Bow and trident attacks use serialized weapon metadata captured on launch rather than the player's later held item. Generated damage and explosions set a synchronous recursion guard so they do not schedule another round of combat effects. The helper temporarily clears and then restores invulnerability ticks for secondary damage.

Lightning is a visual strike plus attributed damage. Explosions do not ignite terrain; block destruction is configurable and disabled by default. Chain targets, extra hits, primary damage, explosion strength, and velocity are independently limited.

## Mining

Connected ore/log searches use a queue and visited set, loaded chunks, bounded distance, and a block budget. Excavators select a matching-material plane based on player facing. Additional breaks use `Player.breakBlock`, preserving ordinary break events and normal tool processing. A per-player guard prevents recursive expansion. Processing stops if the held material or enchantment map changes.

Auto Smelt builds a material mapping from public material-choice furnace recipes at listener construction. Exact-item recipes are excluded. Reload does not reconstruct that mapping; recipe changes require listener recreation or restart.

Drop handling waits until the event finishes and checks cancellation and entity validity before transforming or transferring drops. Telekinesis leaves inventory overflow on the drop entity.

## Equipment, movement, and cleanup

Passive refresh applies the strongest eligible worn values and relevant held-item utility effects at the configured interval. Speed, health, and jump changes use namespaced transient attribute modifiers. Short potion effects expire naturally after equipment removal. Do not remove unrelated effects or modifiers owned by other plugins.

Flight records the previous allowFlight/flying state in player PDC and restores it when plugin flight ends. Creative and Spectator are respected. Join, quit, reload, and disable paths perform cleanup. Another plugin changing flight state during this period can conflict with the saved baseline.

Dash consumes sneak + swap-hands when available. Blink consumes sneak + right-click air and checks loaded chunks, border, a clear two-block path, and a solid landing floor. Magic Shield uses a cooldown, while Resurrection participates in the native resurrection event after ordinary totem handling. Durability loss and mending repair have separate curse handlers.

## Verification

Test recursion, cancelled damage/break/drop events, tool exhaustion, overflow, maximum levels, cooldown persistence, equipment removal, and mode changes on a development server. Damage and block-break events offer integration points; direct velocity and potion effects are not universal region-protection hooks. No server behavior has yet been verified for this source delivery.
