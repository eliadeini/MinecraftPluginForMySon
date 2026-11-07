# Plugin tests

The current automated suite resides in [logic](logic/README.md), mirroring the production rules package. It exercises text matching, numeric boundaries, explicit conflicts, level replacement, and numeric safety without Bukkit runtime behavior.

There are currently no automated GUI, item-metadata, event, or world integration tests. Use the project README's gameplay verification plan for those areas, and record actual results when a development server becomes available. A structural source check is not equivalent to either compilation or JUnit execution.
