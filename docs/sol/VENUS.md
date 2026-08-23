# Venus

Status: **Planetprofil-Plan**

## Zielbild

Venus soll kein „heißes Terra“ sein, sondern ein eigener Überlebensraum: dichte CO₂-Atmosphäre, sehr hoher Oberflächendruck, extreme Hitze und Schwefelsäurewolken. Die interessanteste sichere Bauzone liegt langfristig eher in der Atmosphäre als am Boden.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `32` |
| Gravitation | ca. `0,90 g` |
| Atmosphäre | sehr dicht, überwiegend CO₂, Stickstoffanteil |
| Wolken | Schwefelsäuretröpfchen |
| Oberfläche | vulkanische Ebenen, Hochländer, Basalt |

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:venus_core` | CORE | `null -> BEDROCK` | heißer metallischer Kern |
| 1 | `dynamicuniverse:venus_mantle` | SHELL | `BEDROCK -> BEDROCK` | Mantel, Magmakammern, tiefe Kruste |
| 2 | `dynamicuniverse:venus_surface` | SURFACE | `BEDROCK -> AIR` | extrem heiße, druckreiche Oberfläche |
| 3 | `dynamicuniverse:venus_atmosphere` | SKY | `AIR -> AIR` | komplette Atmosphäre mit vertikalen Druck-, Temperatur- und Wolkenzonen |

Die Wolkenschichten werden **nicht** in mehrere Minecraft-Dimensionen zerlegt. Druck, Temperatur, Sichtweite, Chemie und Wolkentyp ändern sich innerhalb einer Atmosphärendimension über die Höhe. Das vermeidet Portalmatroschka nur deshalb, weil eine Atmosphäre mehrere Schichten besitzt.

## Kern und Mantel

- Metallischer Kern mit hohen Temperaturen.
- Vulkanisch aktiver oder geologisch heißer Mantel als Gameplay-Schwerpunkt.
- Schwefelhaltige und basaltische Rohstoffe.
- Magma- und Gasaufstiegszonen können Oberflächenvulkanismus speisen.

## Oberfläche

- Basaltische Ebenen und große vulkanische Strukturen.
- Keine offenen Wassermeere.
- Dauerhaft sehr hohe Temperatur.
- Sehr hoher Druck mit Belastung für Spieler, Maschinen, Tanks und Fahrzeuge.
- Dichte Atmosphäre reduziert Sichtweite und verändert Flug-/Fallverhalten.

## Atmosphäre

Eine Dimension, intern in Höhenbänder gegliedert:

1. **untere Atmosphäre:** extrem heiß und dicht;
2. **Wolkenunterseite:** zunehmend schwefelsäurehaltig;
3. **Wolkenzone:** starke Säurebelastung, hohe Albedo, gute Zone für schwebende Basen;
4. **obere Wolken-/Atmosphärenschicht:** sinkender Druck und Temperatur;
5. **Exosphärenübergang:** Übergang zu `UniverseSpace`.

Ein späteres Atmosphärenmodell soll lokale Werte statt binärer „Luft vorhanden“-Flags liefern: Druck, Dichte, Temperatur, Sauerstoffanteil, Korrosivität und Wind.

## Umweltmechaniken

- **Druck:** ungeschützte Hohlräume und schwache Fahrzeuge werden belastet.
- **Hitze:** Oberflächenbetrieb benötigt aktive Kühlung.
- **Korrosion:** Schwefelsäurewolken greifen ungeeignete Materialien an.
- **Wind:** für Luftschiffe und Sable-Fahrzeuge relevant.
- **Auftrieb:** dichte CO₂-Atmosphäre macht schwebende Plattformen und Ballons spielmechanisch besonders interessant.

## Ressourcen und Chemie

- Kohlendioxid als dominantes Atmosphärengas
- Stickstoff
- Schwefeldioxid und schwefelhaltige Mineralien
- Schwefelsäure in Wolken
- Basalt/Silikate
- Metalle im Untergrund

Venus eignet sich damit als später Hochtemperatur-, Säure- und Atmosphärenchemie-Planet.

## Darstellung im All

- Helle gelblich-weiße Wolkendecke dominiert die sichtbare Kugel.
- Oberfläche aus dem All normalerweise nicht direkt sichtbar.
- LOD-Modell sollte daher primär die Wolkenoberseite darstellen und erst bei Annäherung atmosphärische Tiefe offenlegen.

## Erforderliche Technik

Das bestehende CORE/SHELL/SURFACE/SKY-Modell reicht topologisch. Benötigt werden zusätzlich:

- höhenabhängige Atmosphärenparameter,
- Druck-/Temperatur-/Korrosionssystem,
- Auftrieb für Sable-Sublevels und Fluggeräte,
- Wolken als volumetrische oder zonenbasierte Umwelt.

## Offene Entscheidungen

- Welche Höhe bildet die optimale „habitable“ Schwebebasis-Zone im Gameplay?
- Soll Säure echte Blockkorrosion verursachen oder nur Maschinen-/Schutzsysteme belasten?
- Wie stark beeinflusst die dichte Atmosphäre Projektile, Flugzeuge und Create-Contraptions?
