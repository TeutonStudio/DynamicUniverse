# Terra

Status: **Referenzprofil / alpha0-Ziel**

## Zielbild

Terra ist das Referenzprofil für den radialen Planetendimensionsstack. Es verbindet das bestehende Minecraft-Spielgefühl mit der DynamicUniverse-Geometrie und dient als Kontrollfall für alle späteren Planetenprofile.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `32` |
| Gravitation | `1,0 g` |
| Atmosphäre | N₂/O₂-dominiert, lebensfreundlich |
| Oberfläche | Ozeane, Kontinente und Vanilla-/Mod-Biome |

## Geplanter Dimensionsstack

Der bestehende Standardstack bleibt verbindliche Referenz:

| # | Template-ID | Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:planet_core` | CORE | `null -> BEDROCK` | Planetenkern |
| 1 | `dynamicuniverse:deep_nether` | SHELL | `BEDROCK -> BEDROCK` | tiefer heißer Mantel-/Netherbereich |
| 2 | `minecraft:the_nether` | SHELL | `BEDROCK -> BEDROCK` | Nether |
| 3 | `minecraft:overworld` | SURFACE | `BEDROCK -> AIR` | Erdoberfläche |
| 4 | `dynamicuniverse:sky` | SKY | `AIR -> AIR` | Atmosphäre/Himmel bis `UniverseSpace` |

## Planetenkern

- Sehr heißer, überwiegend metallischer Kern.
- Minecraft-seitig kann Lava einen großen Teil der thermischen Gefährlichkeit visualisieren.
- Direkter Zugang erfolgt nur über die definierte vertikale Geometrie beziehungsweise gezielt erzeugte Bedrock-Aperturen.
- Der Kern ist nicht einfach „noch ein Nether“, sondern Endpunkt des radialen Stacks.

## Tiefer Nether

- Übergangsraum zwischen Kern und normalem Nether.
- Höhere Temperaturen, stärkere Lava-/Magma-Prägung und geringere natürliche Hohlraumdichte.
- Besonders geeignet für seltene Hochtemperaturressourcen und spätere geologische Mechaniken.

## Nether

- Vanilla-Nether bleibt als registrierte Dimension nutzbar.
- Seine obere und untere Bedrock-Grenze werden in die radiale Topologie eingebunden.
- Bedrock-Löcher verbinden deterministisch mit den korrespondierenden Koordinaten der benachbarten Schicht.

## Oberwelt

- Vollwertige Minecraft-Oberfläche.
- Horizontale X/Z-Geometrie ist periodisch/toroidal.
- Tag/Nacht soll langfristig aus der tatsächlichen Stellung von Terra und Sol im Kosmos abgeleitet werden.
- Biome, Ozeane und Mod-Weltgeneration bleiben erhalten, soweit die Generatoradapter ihre Grenzen korrekt melden.

## Himmel

- Eine eigene `AIR -> AIR`-Dimension verbindet die Oberwelt mit dem kontinuierlichen UniverseSpace.
- Sie kann Wolken, hohe Atmosphäre und später Druckabfall enthalten.
- Vollständige Sable-Fahrzeuge sollen den Übergang ohne künstliches Zerlegen des Fahrzeugs passieren können.

## Umweltmechaniken

Terra ist der Normalfall, gegen den andere Planeten abweichen:

- 1-g-Gravitation
- atembare Atmosphäre
- flüssiges Oberflächenwasser
- moderater Druck- und Temperaturbereich
- Wetter und Wolken
- Tag/Nacht aus kosmischer Beleuchtung

## Ressourcen und Chemie

Terra bleibt breit aufgestellt und bildet den Referenzraum für bestehende Modchemie, insbesondere Mekanism. Andere Planeten sollen gerade dadurch interessant werden, dass bestimmte Gase, Mineralien oder Umweltbedingungen dort leichter oder ausschließlich verfügbar sind.

## Darstellung im All

- Ozeane, Landmassen und Wolken bilden getrennte visuelle Ebenen.
- Wolken dürfen sich unabhängig von der Oberflächen-LOD bewegen.
- Nachtseite erhält später Stadt-/Spielerlicht nur aus tatsächlich geladenen oder gecachten Daten, nicht als feste Textur.
- Der sichtbare Globus ist eine Projektion der periodischen lokalen Oberfläche, nicht die reale Blockgeometrie selbst.

## Referenzkriterien

Terra gilt als stabiler Referenzplanet, wenn:

1. Weltanlage und Wiedereintritt keine ungültigen Spielerdaten erzeugen;
2. jeder vertikale Übergang in beide Richtungen deterministisch ist;
3. Nether-/Bedrock-Aperturen persistent bleiben;
4. horizontale Toroid-Nähte stabil sind;
5. Sky ↔ UniverseSpace mit Spieler und Sable-Objekten funktioniert;
6. Save/Load den vollständigen Planetstack rekonstruiert.

## Offene Entscheidungen

- Wie detailliert wird die Atmosphäre als physikalisches Medium simuliert?
- Wann ersetzt kosmische Beleuchtung den Vanilla-Tag/Nacht-Timer vollständig?
- Welche Schicht rendert die globale Wolkendecke für den Blick aus dem All?
