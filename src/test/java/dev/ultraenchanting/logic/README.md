# Rules test suite

`RulesTest.java` exercises `dev.ultraenchanting.logic.Rules` using JUnit Jupiter, including parameterized inputs.

## Coverage

- Case, punctuation, whitespace, and Hebrew combining-mark normalization.
- Required English/Hebrew Unbreaking names, prefix matching, substrings, minor typos, unrelated queries, and result ranking.
- Both accepted level endpoints, null, zero, negative values, overflow, decimals, signs, spaces, exponent notation, and non-ASCII digits.
- Symmetric explicit conflict lookup, self-replacement, and absence of imported native restrictions.
- All seven requested native compatibility pairs against an empty explicit graph.
- Replacing one level while retaining other entries and leaving the input map unchanged.
- Finite clamping and safe handling of NaN and infinity.

The compatibility tests verify the pure conflict policy; they do not prove that native enchantments can be applied together through Paper metadata. Alias cases use local test terms and do not load `aliases.yml`. Configuration parsing, PDC serialization, GUI cleanup, effects, and protection-plugin compatibility remain outside this suite.

## Run and extend

From the project root, with an existing JDK 25 and Gradle 9.1+:

```powershell
.\gradlew.bat test --tests dev.ultraenchanting.logic.RulesTest
```

Add boundary and regression cases when changing `Rules`. Prefer assertions about externally observable results, including preservation of inputs, over tests that simply repeat implementation steps.

These tests are present as source and have not yet been executed in the current delivery.
