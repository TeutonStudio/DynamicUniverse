# Saturnus

Status: **Planetprofil-Plan / benötigt Gasriesen-Erweiterung**

## Zielbild

Saturnus soll sich als leichterer, stark abgeplatteter Gasriese mit ausgeprägtem Ringsystem spielen. Wie Iuppiter besitzt er keine feste Oberfläche. Sein Profil soll aber nicht bloß „Iuppiter mit gelberem Shader“ sein: geringere Dichte, andere Wolkenstruktur, auffällige Ringumgebung und der polare Hexagon-Sturm geben ihm eigene Mechaniken und Ziele.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `304` |
| Gravitation an Wolkenoberkante | ca. `1,1 g` |
| Hauptbestandteile | Wasserstoff, Helium |
| feste Oberfläche | keine |
| Ringsystem | ja, externes kosmisches Objekt / Ringstruktur |

`coreSize = 304` ist vorläufig ein relativer Geometrieanker und kein physischer Kernradius.

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | geplante Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:saturn_core` | CORE | `null -> BEDROCK` | dichter Kernbereich aus schweren Elementen und Hochdruckphasen |
| 1 | `dynamicuniverse:saturn_metallic_hydrogen` | SHELL | `BEDROCK -> BEDROCK` | leitfähiger Hochdruck-Wasserstoff |
| 2 | `dynamicuniverse:saturn_molecular_envelope` | SHELL | `BEDROCK -> BEDROCK` | Wasserstoff-/Heliumhülle |
| 3 | `dynamicuniverse:saturn_deep_atmosphere` | ENVELOPE | `BEDROCK -> AIR` | tiefe befliegbare Atmosphäre ohne feste Oberfläche |
| 4 | `dynamicuniverse:saturn_atmosphere` | SKY | `AIR -> AIR` | Wolken, Stürme und Übergang in `UniverseSpace` |

Wie bei Iuppiter ist `ENVELOPE` eine semantische Rolle. Die Boundary-Geometrie kann `BEDROCK -> AIR` bleiben, darf aber nicht als begehbare feste Oberfläche interpretiert werden.

## Innere Schichten

- dichter Kern-/Hochdruckbereich;
- metallischer Wasserstoff;
- molekularer Wasserstoff und Helium;
- mögliche Heliumentmischung („Heliumregen“) kann später als Energie-/Wärmetransportmechanik genutzt werden.

Die innere Physik soll sich über Druck und Phase erklären, nicht über willkürliche Steinschichten.

## Atmosphäre

Eine Atmosphärendimension mit vertikalen Zonen:

1. obere H₂/He-Schicht;
2. Ammoniakwolken;
3. Ammoniumhydrogensulfid-/Schwefelchemie;
4. tiefere Wasserwolken;
5. Hochdruck-/Hochtemperaturzone;
6. Übergang in die molekulare Hülle.

Der Nordpol-Hexagonsturm ist ein persistentes Großwetter-Feature und eignet sich als eindeutige Navigationsmarke.

## Ringsystem

Die Ringe gehören **nicht** in den radialen Planetendimensionsstack.

Geplant:

- eigenes `CosmicSpatialObject` beziehungsweise Ringobjekt, an Saturnus gebunden;
- Partikel-/LOD-Darstellung aus der Ferne;
- lokale Sable-/Sublevel-Cluster oder physische Ringfragmente bei Annäherung;
- überwiegend Wassereis, dazu Gesteins-/Staubanteile;
- Lücken und Ringbänder als echte räumliche Struktur, nicht als flache Dekortextur.

So kann man durch die Ringe fliegen, ohne Saturnus selbst topologisch in eine Scheibe zu verwandeln. Das wäre zwar bemerkenswert, aber aus den falschen Gründen.

## Umweltmechaniken

- Gravitation an der Wolkenoberkante näher an Terra als bei Iuppiter;
- dennoch zunehmender Druck und Temperatur nach innen;
- starke Winde;
- Auftrieb und Atmosphärenflug;
- kein Landen;
- ringnahe Mikrometeoriten-/Kollisionsgefahr;
- mögliche elektrische Stürme.

## Ressourcen und Chemie

Atmosphäre:

- Wasserstoff
- Helium
- Ammoniak
- Wasser in tieferen Wolken
- Schwefel-/Ammoniumverbindungen

Ringe:

- Wassereis als dominante Ressource
- Staub und Gestein
- seltene eingetragene Metalle

## Darstellung im All

- deutlich abgeplattete Planetenkugel;
- blasse gelblich-beige Wolkenbänder;
- sehr dominantes Ringsystem mit korrekter Neigung;
- Schatten der Ringe auf der Atmosphäre und Planetenschatten auf den Ringen;
- Hexagon am Nordpol bei ausreichender LOD-Stufe.

## Erforderliche Technik

- gleiche `ENVELOPE`-/Gasriesen-Unterstützung wie Iuppiter;
- ringförmige `CosmicSpatialObject`-Geometrie;
- Ring-LOD und lokale Materialisierung;
- Ring-/Planetenschatten;
- optional Helium-Entmischungs-/Wärmeflussmodell.

## Offene Entscheidungen

- Werden Ringpartikel physikalisch einzeln nur in Simulationsblasen materialisiert oder in größeren aggregierten Clustern?
- Wie werden Lücken und Ringresonanzen gespeichert, ohne Milliarden Einzelkörper zu simulieren?
- Ist der Hexagon-Sturm fest an den Planeten gekoppelt oder Teil einer separat rotierenden Atmosphärenschicht?
- Soll Ringabbau langfristig die sichtbare Ringmasse verändern können?
