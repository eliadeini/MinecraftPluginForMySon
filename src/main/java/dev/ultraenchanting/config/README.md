# Configuration access

`Settings.java` loads the definition, alias, and language data used by the rest of the plugin.

## Files and methods

The constructor creates missing `enchants.yml`, `aliases.yml`, and `messages.yml` files from bundled resources, then loads them from the plugin data directory. A YAML load failure throws an exception so initialization or reload can reject the data.

| Member | Use |
| --- | --- |
| `enchants` | Definition metadata, categories, tuning, and explicit opposite IDs |
| `aliases` | Additional English/Hebrew searchable names by short enchantment ID |
| `messages` | Primary player-facing text |
| `text(key)` | Message lookup; falls back to the key when absent |
| `number(...)` | Read main config value and constrain it to finite bounds |
| `integer(...)` | Bounded numeric access converted to an integer |
| `flag(...)` | Boolean setting with a caller-provided default |

The main `config.yml` is owned by Paper's `JavaPlugin` configuration mechanism. `Settings` reads it through `plugin.getConfig()` rather than holding a separate copy.

## Editing and extending

Defaults are in [resources](../../../../resources/README.md). Add new player-facing strings to `messages.yml` and aliases to `aliases.yml`. For new numeric options, choose explicit default, minimum, and hard maximum values at each call site. Missing YAML files are created; existing user configuration files are not automatically merged with new defaults.

Loading YAML is not complete semantic validation. The registry also checks conflict targets, while effect code applies additional runtime limits. Test malformed YAML, missing values, unknown conflict IDs, and out-of-range numeric settings when validating reload behavior.
