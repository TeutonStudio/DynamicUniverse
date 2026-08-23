# Neptunus

Status: **Planetprofil-Plan / benötigt Eisriesen-Erweiterung**

## Zielbild

Neptunus soll als dunklerer, dynamischerer Eisriese mit sehr starken Winden, Methanatmosphäre und tiefem Hochdruckmantel funktionieren. Wie Uranus besitzt er keine feste Oberfläche. Im Gameplay liegt sein Schwerpunkt stärker auf extremer Atmosphärendynamik, tiefen Druckzonen und schwer erreichbaren Hochdruckressourcen.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `128` |
| Gravitation an Wolkenoberkante | ca. `1,14 g` |
| Hauptatmosphäre | Wasserstoff, Helium, Methan |
| tiefer Mantel | Hochdruckgemisch aus Wasser, Ammoniak, Methan und schweren Bestandteilen |
| feste Oberfläche | keine |

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | geplante Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:neptune_core` | CORE | `null -> BEDROCK` | dichter Gesteins-/Metallkern |
| 1 | `dynamicuniverse:neptune_ice_mantle` | SHELL | `BEDROCK -> BEDROCK` | heißer Hochdruck-Ice-/Fluidmantel |
| 2 | `dynamicuniverse:neptune_molecular_envelope` | SHELL | `BEDROCK -> BEDROCK` | dichter H₂/He/CH₄-Übergangsbereich |
| 3 | `dynamicuniverse:neptune_deep_atmosphere` | ENVELOPE | `BEDROCK -> AIR` | tiefe Atmosphäre ohne festen Boden |
| 4 | `dynamicuniverse:neptune_atmosphere` | SKY | `AIR -> AIR` | kalte, stürmische äußere Atmosphäre bis `UniverseSpace` |

Wie bei Uranus ist `ice_mantle` kein gefrorener Ozean im üblichen Sinn. Die Stoffe befinden sich bei hohen Drücken und Temperaturen in dichten Fluid-/Hochdruckphasen.

## Innere Schichten

- dichter Kern aus Gestein, Metallen und schweren Bestandteilen;
- darüber Wasser-/Ammoniak-/Methan-reicher Hochdruckmantel;
- weiter außen zunehmend wasserstoff-/heliumreiche Hülle;
- keine klar definierte feste Oberfläche zwischen Atmosphäre und Innerem.

Die Schichten sollen gameplayseitig über Druck, Dichte, Temperatur und Materialphase verbunden sein. Sichtbare Bedrock-Grenzen sind technische Dimensionsgrenzen und dürfen nicht als geologische Realität verkauft werden.

## Atmosphäre

Eine Atmosphärendimension mit mehreren Höhenzonen:

1. obere kalte H₂/He/CH₄-Atmosphäre;
2. Methanwolken und Dunst;
3. stärkere Sturm-/Jetstream-Zonen;
4. tiefere Wolken aus weiteren kondensierbaren Stoffen;
5. Übergang zur dichten Envelope.

Neptunus erhält im Vergleich zu Uranus höhere Windgeschwindigkeiten und stärkere großräumige Wetterdynamik.

## Umweltmechaniken

- Gravitation leicht über Terra-Niveau;
- sehr kalte obere Atmosphäre;
- nicht atembare Wasserstoff-/Helium-/Methanatmosphäre;
- extrem starke Winde;
- dynamische Stürme und dunkle Wirbel;
- steigender Druck und Temperatur nach innen;
- kein Landen;
- Auftrieb, Schub und aerodynamische Stabilität sind zentrale Fahrzeugmechaniken.

## Ressourcen und Chemie

- Wasserstoff
- Helium
- Methan
- Ammoniak
- Wasser in tiefen Hochdruckphasen
- Kohlenstoffverbindungen

Wie bei Uranus kann Diamantbildung/-regen als spätere Hochdruckmechanik untersucht werden. Der Plan behandelt dies bewusst als modellierbare Hypothese und nicht als garantierte alltägliche Ressourcendusche aus dem Himmel.

## Wetter und persistente Strukturen

- Dunkle Flecken können als langlebige, aber nicht notwendigerweise permanente Wirbelobjekte gespeichert werden.
- Stürme bewegen sich relativ zur Planetenrotation.
- Windfelder sollten großräumige Zonen besitzen, nicht pro Block zufällig würfeln.
- Atmosphären-Sable-Sublevels müssen Windkräfte aus der lokalen Simulation übernehmen können.

## Darstellung im All

- kräftiger blau wirkende Kugel als Uranus;
- sichtbare helle Wolken und dunkle Sturmstrukturen;
- dynamische Atmosphärenbänder;
- schwaches Ringsystem nur bei ausreichender LOD/Beleuchtung;
- keine feste Oberflächentextur unter der Atmosphäre.

## Ringe und Monde

Das schwache Ringsystem gehört in `UniverseSpace`. Triton und weitere Monde werden separat als `MOON`-Körper geplant; Triton ist wegen seiner retrograden Bahn und kryogenen Oberfläche ein eigenes späteres Designproblem und nicht Teil dieser Datei.

## Erforderliche Technik

Zusätzlich zur allgemeinen Gas-/Eisriesen-Unterstützung:

- `ENVELOPE`-Semantik ohne feste Oberfläche;
- Hochdruck-Ice-/Fluidmantel;
- persistente großräumige Wind- und Sturmfelder;
- Windkopplung an Sable-/Fahrzeugphysik;
- dynamische Atmosphären-LOD;
- Ringobjekte im UniverseSpace.

## Offene Entscheidungen

- Wie stark darf Wind Fahrzeuge versetzen, bevor Gameplay nur noch aus unfreiwilligem Tourismus besteht?
- Werden Sturmzellen rein atmosphärisch simuliert oder als persistente planetare Wetterobjekte gespeichert?
- Welche Hochdruckchemie wird tatsächlich spielmechanisch zugänglich?
- Wie wird Neptunus visuell klar von Uranus unterschieden, ohne lediglich die Farbsättigung hochzudrehen?
