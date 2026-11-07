# Bundled plugin resources

Gradle includes these files as main resources. At runtime, the plugin copies missing defaults into its own Paper data directory.

| File | Purpose |
| --- | --- |
| `plugin.yml` | Plugin name, version, main class, API version, command declaration, and permission defaults |
| `config.yml` | Feature switches, engine representation cap, work budgets, effect safety caps, scheduler interval, and dialog timeout |
| `enchants.yml` | 53 custom definitions, known native Hebrew translations, and explicit opposite edges |
| `aliases.yml` | Additional searchable names keyed by short enchantment ID |
| `messages.yml` | Primary Hebrew GUI labels and action messages |

## Definition schema

A custom definition under `custom.<id>` includes `english`, `hebrew`, `categories`, `description`, `classification`, `enabled`, `scale`, `cap`, `chance`, `cooldown-seconds`, and `conflicts`. Native overrides live under `vanilla.<id>`. Conflicts use complete namespaced IDs and work in either direction.

Categories are `any`, `weapon`, `tool`, `armor`, or `helmet`. Runtime strength is based on scale and cap, with extra hard bounds in handlers. The requested item level remains exact. Aliases and descriptions are metadata; adding a definition does not automatically implement a new effect.

## Editing rules

Use UTF-8, valid YAML indentation, stable IDs, and correctly quoted strings where punctuation needs it. Preserve the descriptor's main class and API version unless changing the implementation deliberately. Existing user files are not automatically merged with revised defaults.

The selected-level range is fixed by `Rules.MAX_LEVEL`; `max-selectable-level` documents that contract. The engine cap limits native representation only. Changing `period-ticks` requires restart, because reload retains the existing scheduled interval.

The root `Verify-Source.ps1` checks definition structure and known references, but does not perform a full YAML parse or runtime configuration test. Invalid YAML and unknown opposite IDs should also be exercised through the reload command on a development server.
