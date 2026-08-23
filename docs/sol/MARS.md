# Mars

Status: **Planetprofil-Plan**

## Zielbild

Mars soll als kalter, trockener Gesteinsplanet mit dünner CO₂-Atmosphäre, Staubstürmen, Wassereis und stark oxidierter Oberfläche funktionieren. Der Untergrund ist spielmechanisch wichtiger als bei Terra, weil geschützte Basen, Eisvorkommen und alte Lava-/Hohlraumsysteme dort plausibel zusammenkommen.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `16` |
| Gravitation | ca. `0,38 g` |
| Atmosphäre | dünn, überwiegend CO₂ |
| Oberfläche | Basalt, Eisenoxide, Staub, Eis |

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:mars_core` | CORE | `null -> BEDROCK` | metallischer Kern, deutlich weniger thermisch dominant als Terra |
| 1 | `dynamicuniverse:mars_mantle` | SHELL | `BEDROCK -> BEDROCK` | Mantel, tiefe Kruste, alte Magmazonen und Hohlräume |
| 2 | `dynamicuniverse:mars_surface` | SURFACE | `BEDROCK -> AIR` | kalte Wüstenoberfläche, Vulkane, Canyons und Krater |
| 3 | `dynamicuniverse:mars_atmosphere` | SKY | `AIR -> AIR` | dünne Atmosphäre und Übergang in `UniverseSpace` |

## Kern und Mantel

- Kern mit Eisen, Nickel und Schwefelanteilen.
- Weniger „Lavaozean“-Charakter als Terra.
- Mantel/Kruste kann große erkaltete Vulkanstrukturen und Lava-Tubes enthalten.
- Unterirdische Räume sind bevorzugte Standorte für druckgeschützte Kolonien.

## Oberfläche

- Eisenoxidreicher roter Regolith.
- Große Höhenunterschiede, Canyons, Schildvulkane und Einschlagbecken.
- Polare und unterirdische Wassereisvorkommen.
- Sehr wenig natürliche flüssige Oberflächenflüssigkeit.
- Staub soll Maschinen, Solarpaneele und Sicht beeinflussen können.

## Atmosphäre

- Dünn und nicht atembar.
- CO₂ dominiert; Argon und Stickstoff als Nebenbestandteile.
- Druck fällt schnell mit der Höhe ab.
- Staubstürme sind ein lokales Atmosphärenereignis und keine eigene Dimension.
- Sky ↔ UniverseSpace soll deutlich kürzer wirken als bei Terra, weil die Atmosphäre dünner ist.

## Umweltmechaniken

- **Niedrige Gravitation:** vergleichbar mit Merkur, aber mit dünner Atmosphäre statt Vakuum.
- **Kälte:** Basen benötigen Wärmehaushalt.
- **Druck:** ungeschützte Spieler benötigen geschlossene Systeme.
- **Staub:** reduziert Solarleistung, Sicht und kann Wartung erzwingen.
- **Wasserknappheit:** Eis ist strategisch wertvoll.

## Ressourcen und Chemie

- Eisenoxide
- Silikate
- Kohlendioxid
- Argon
- Wasser-/CO₂-Eis
- Schwefel- und Perchloratchemie als spätere Spezialressource

Mars soll sich besonders für Treibstoff-, Sauerstoff- und Wasserproduktion aus lokalen Rohstoffen eignen, sofern entsprechende Maschinen vorhanden sind.

## Darstellung im All

- Rötliche Oberfläche mit dunkleren Basaltregionen.
- Sichtbare Polkappen.
- Dünne Atmosphärenkante.
- Regionale Staubstürme können als dynamische LOD-/Wolkenschicht erscheinen.

## Erforderliche Technik

Das bestehende CORE/SHELL/SURFACE/SKY-Modell reicht topologisch. Zusätzlich benötigt Mars:

- geringe Gravitation,
- dünnes Atmosphären-/Druckmodell,
- Temperaturmodell,
- Staubablagerung/-stürme,
- Eisvorkommen mit geografischer/unterirdischer Logik.

## Offene Entscheidungen

- Werden Olympus Mons, Valles Marineris und große Becken als makroskopische feste Planetmerkmale in die Generatorlogik eingebaut?
- Wie stark soll Staub technische Systeme tatsächlich verschleißen?
- Soll Terraforming langfristig die Atmosphärenparameter eines existierenden lokalen Profils verändern können?
