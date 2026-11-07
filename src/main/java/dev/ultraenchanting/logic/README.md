# Server-independent rules

`Rules.java` contains pure static operations without Bukkit, filesystem, or world access. Keep this package independent of Paper so its tests can run without a Minecraft server.

| Method | Contract |
| --- | --- |
| `level(input)` | Accept ASCII digits representing 1 through 100000000; reject null, signs, whitespace, decimals, overflow, and values outside the range |
| `normalize(text)` | Apply NFKD normalization, lowercase with a stable locale, strip combining marks, then retain letters and digits |
| `score(query, terms)` | Rank exact matches at 100, prefix at 80, substring at 60, and tolerated typos at 30; blank query scores 1 |
| `distance(a, b)` | Iterative Levenshtein edit distance using one working row |
| `conflict(selected, existing, graph)` | Return the first sorted explicit opposite, checking edges in both directions and excluding self-replacement |
| `replace(old, id, value)` | Return a copied, sorted map with one validated level replaced; leave the original map unchanged |
| `bounded(value, min, max)` | Clamp a finite value; return the minimum for non-finite input |

Fuzzy matching starts at query length four: one edit is tolerated for shorter queries and two for length eight or greater. Normalization enables Hebrew niqqud-insensitive matching, while aliases supply alternative names and transliterations. It is not a general transliteration algorithm.

The corresponding tests are in the mirrored `src/test/java/dev/ultraenchanting/logic` package. Add cases there for changed boundary, normalization, ranking, conflict, or copy semantics. Do not add server-dependent policy or effect handlers here.
