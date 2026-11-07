# Enchantment model and registry

This package separates enchantment metadata and discovery from GUI and gameplay event handling.

## Files

`Definition.java` is an immutable record containing the stable namespaced ID, English and Hebrew names, aliases, description, categories, classification, conflict IDs, enable flag, scaling/cap/chance/cooldown parameters, and optional native Paper enchantment.

- `terms()` assembles the searchable names and ID.
- `effective(level)` computes a bounded runtime strength from the requested level.
- `accepts(material)` interprets the supported item categories: `any`, `weapon`, `tool`, `armor`, and `helmet`.

`EnchantRegistry.java` discovers native enchantments through Paper's enchantment registry, then loads custom definitions from `enchants.yml`. Native IDs retain their namespace; custom IDs use `ultraenchanting:`. Definitions are held in stable ID order.

- `get(id)` resolves one definition; unknown IDs return null.
- `all()` exposes the current definitions.
- `search(query)` filters disabled entries, ranks matches through `Rules.score`, and breaks ties by English name.
- `conflict(id, existing)` checks only configured opposite edges through the pure rules layer.

## Data contracts

Definition aliases come from `aliases.yml` using the short ID. Custom metadata uses `custom.<short-id>` and native metadata uses `vanilla.<short-id>` in `enchants.yml`. Unknown conflict targets cause registry construction to fail. An edge declared on either side rejects the pair.

Selected levels are held by GUI sessions and item PDC rather than inside shared definitions. Native incompatibilities are not imported into the custom conflict graph.

## Adding an enchantment

Add its definition and aliases, implement meaningful effect handling in the effect package, and define any true opposites using complete namespaced IDs. A YAML entry alone supplies searchable metadata and storage; it does not create new physical behavior. Update the source checker if the intentional number of custom definitions changes from 53.

New native registry entries appear automatically, with generated English names when no translation is provided. Test search and conflict behavior in the logic test package, then exercise the effect on a development server.
