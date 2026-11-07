# Structural checks only. This does not compile Java, run JUnit or start Paper.
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
function Require([bool]$condition, [string]$message) {
    if (-not $condition) { throw $message }
}
$required = @('build.gradle.kts','settings.gradle.kts','gradle.properties','gradlew','gradlew.bat','README.md',
    'src/main/resources/plugin.yml','src/main/resources/config.yml','src/main/resources/messages.yml',
    'src/main/resources/aliases.yml','src/main/resources/enchants.yml',
    'src/main/java/dev/ultraenchanting/UltraEnchantingPlugin.java',
    'src/test/java/dev/ultraenchanting/logic/RulesTest.java')
foreach ($relative in $required) { Require (Test-Path -LiteralPath (Join-Path $projectRoot $relative) -PathType Leaf) "Missing $relative" }
$yaml = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/enchants.yml'))
$custom = [regex]::Match($yaml, '(?ms)^custom:\r?\n(.*?)^vanilla:').Groups[1].Value
$vanilla = [regex]::Match($yaml, '(?ms)^vanilla:\r?\n(.*)$').Groups[1].Value
$sections = [regex]::Matches($custom, '(?ms)^  ([a-z][a-z0-9_]*):\r?\n(.*?)(?=^  [a-z][a-z0-9_]*:|\z)')
Require ($sections.Count -eq 53) "Expected 53 custom definitions, found $($sections.Count)"
$ids = @{}
foreach ($section in $sections) {
    $id = $section.Groups[1].Value
    Require (-not $ids.ContainsKey($id)) "Duplicate custom ID $id"
    $ids[$id] = $true
    foreach ($field in @('english','hebrew','categories','description','classification','enabled','scale','cap','chance','cooldown-seconds','conflicts')) {
        Require ([regex]::IsMatch($section.Groups[2].Value, "(?m)^    ${field}:")) "Missing $field for $id"
    }
}
$vanillaIds = @{}
foreach ($match in [regex]::Matches($vanilla, '(?m)^  ([a-z][a-z0-9_]*):')) { $vanillaIds[$match.Groups[1].Value] = $true }
foreach ($match in [regex]::Matches($custom, "'(minecraft|ultraenchanting):([a-z_]+)'")) {
    $id = $match.Groups[2].Value
    Require (($match.Groups[1].Value -eq 'minecraft' -and $vanillaIds.ContainsKey($id)) -or
        ($match.Groups[1].Value -eq 'ultraenchanting' -and $ids.ContainsKey($id))) "Unknown conflict reference: $($match.Value)"
}
$javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Filter '*.java' -File -Recurse)
$effectFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src/main/java/dev/ultraenchanting/effect') -Filter '*.java' -File)
$effects = ($effectFiles | ForEach-Object { [IO.File]::ReadAllText($_.FullName) }) -join "`n"
foreach ($id in $ids.Keys) { Require ($effects.Contains('"' + $id + '"')) "No effect reference for $id" }
# Lexically remove comments and string/character literals before checking delimiters.
$tokenPattern = '(?s)/\*.*?\*/|//[^\r\n]*|"(?:\\.|[^"\\])*"|''(?:\\.|[^''\\])*'''
$closing = @{ '}' = '{'; ')' = '('; ']' = '[' }
foreach ($file in $javaFiles) {
    $text = [IO.File]::ReadAllText($file.FullName)
    Require (-not [regex]::IsMatch($text, '\bTODO\b|UnsupportedOperationException')) "Unfinished implementation marker in $($file.Name)"
    $clean = [regex]::Replace($text, $tokenPattern, '')
    $stack = [System.Collections.Generic.Stack[char]]::new()
    foreach ($character in $clean.ToCharArray()) {
        if ('{(['.Contains([string]$character)) { $stack.Push($character) }
        elseif ('})]'.Contains([string]$character)) {
            Require ($stack.Count -gt 0) "Unmatched delimiter in $($file.Name)"
            Require ([string]$stack.Pop() -eq $closing[[string]$character]) "Mismatched delimiter in $($file.Name)"
        }
    }
    Require ($stack.Count -eq 0) "Unclosed delimiter in $($file.Name)"
}
$build = [IO.File]::ReadAllText((Join-Path $projectRoot 'build.gradle.kts'))
Require ($build.Contains('26.3.build.230-stable')) 'Pinned Paper API is missing'
Require ($build.Contains('JavaLanguageVersion.of(25)')) 'Java 25 target is missing'
$aliases = [IO.File]::ReadAllText((Join-Path $projectRoot 'src/main/resources/aliases.yml'))
foreach ($term in @('אמבריקינג','אנברייקינג','עמידות','אמברי','שארפנס','אפישנסי','מנדינג')) {
    Require ($aliases.Contains($term)) "Missing required search alias $term"
}
Write-Output "PASS: required files; 53 complete custom definition records; conflict references; effect references; $($javaFiles.Count) Java files with balanced delimiters; target versions; required Hebrew aliases."
Write-Output 'NOT CHECKED: Java compilation, JUnit execution, full YAML parsing, runtime behavior or server integration.'