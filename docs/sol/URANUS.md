# Uranus

Status: **Planetprofil-Plan / benötigt Eisriesen-Erweiterung**

## Zielbild

Uranus soll als Eisriese mit ungewöhnlich starker Achsneigung, kalter äußerer Atmosphäre und einem tiefen Wasser-/Ammoniak-/Methan-reichen Hochdruckmantel funktionieren. Der Planet besitzt keine feste Oberfläche. Sein auffälligstes kosmisches Merkmal ist die fast auf der Seite liegende Rotation, wodurch Jahreszeiten und Beleuchtung extrem von Terra abweichen.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `128` |
| Gravitation an Wolkenoberkante | ca. `0,89 g` |
| Hauptatmosphäre | Wasserstoff, Helium, Methan |
| tiefer Mantel | Hochdruckgemisch aus Wasser, Ammoniak, Methan und gelösten Stoffen |
| feste Oberfläche | keine |
| Achsneigung | extrem, etwa `98°` |

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | geplante Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:uranus_core` | CORE | `null -> BEDROCK` | kleiner dichter Gesteins-/Metallkern |
| 1 | `dynamicuniverse:uranus_ice_mantle` | SHELL | `BEDROCK -> BEDROCK` | heißer Hochdruck-„Eis“-/Fluidmantel aus H₂O/NH₃/CH₄-Systemen |
| 2 | `dynamicuniverse:uranus_molecular_envelope` | SHELL | `BEDROCK -> BEDROCK` | dichter Wasserstoff-/Helium-/Methan-Übergangsbereich |
| 3 | `dynamicuniverse:uranus_deep_atmosphere` | ENVELOPE | `BEDROCK -> AIR` | tiefe Atmosphäre ohne festen Boden |
| 4 | `dynamicuniverse:uranus_atmosphere` | SKY | `AIR -> AIR` | kalte äußere Atmosphäre bis `UniverseSpace` |

## Eisriesen-Semantik

„Eis“ bedeutet hier keine gigantische gefrorene Minecraft-Eiswand. In der Planetologie bezeichnet es die Zusammensetzung aus Stoffen wie Wasser, Ammoniak und Methan, die im Inneren unter hohem Druck und hoher Temperatur als Fluide, ionische oder andere Hochdruckphasen auftreten können.

Die geplante `ice_mantle`-Dimension soll daher:

- keinen normalen Wasser-/Eisbiom-Look verwenden;
- hohen Druck und hohe Temperatur abbilden;
- Materialphasen über Umweltwerte statt nur Blocknamen definieren;
- spätere exotische Leitfähigkeits-/Chemieprozesse ermöglichen.

## Atmosphäre

- Wasserstoff und Helium dominieren.
- Methan absorbiert rotes Licht und trägt zur blaugrünen Erscheinung bei.
- Wolken- und Dunstzonen liegen innerhalb einer Atmosphärendimension.
- Nach unten steigen Druck und Temperatur kontinuierlich.
- Kein begehbarer Boden; ein fallendes Objekt gelangt in tiefere Envelope-/Mantelbereiche.

## Achsneigung und Jahreszeiten

Uranus benötigt eine echte Rotationsachse im Kosmosmodell:

- lokale Tagesrichtung folgt der Planetrotation;
- Sonnenstand wird aus Orbit und Achsenorientierung berechnet;
- Pole können über lange Zeiträume stark beleuchtet oder stark verdunkelt sein;
- Atmosphäre und lokale Dimensionen müssen dieselbe Körperorientierung verwenden.

Damit wird Uranus der wichtigste Testkörper dafür, dass „oben“ im lokalen Planetenraum nicht heimlich mit einer festen UniverseSpace-Y-Achse verwechselt wird.

## Umweltmechaniken

- Gravitation nahe Terra-Niveau;
- extrem kalte obere Atmosphäre;
- Methan-/Wasserstoffatmosphäre nicht atembar;
- steigender Druck nach innen;
- hohe Windgeschwindigkeiten in Atmosphärenzonen;
- keine feste Landung;
- extreme saisonale Beleuchtung durch Achsneigung.

## Ressourcen und Chemie

- Wasserstoff
- Helium
- Methan
- Ammoniak
- Wasser in tiefen Hochdruckphasen
- Kohlenstoffchemie unter hohem Druck

Diamantbildung/-regen kann als **optionale, physikalisch motivierte Hochdruckmechanik** geplant werden, sollte aber nicht als bereits gesicherter normaler Oberflächenprozess behandelt werden.

## Ringe und Monde

Uranus besitzt ein Ringsystem, dieses gehört wie bei Saturn in `UniverseSpace` und nicht in den radialen Stack. Die großen Monde werden später als eigene `MOON`-Körper mit Faktor 2 geplant.

## Darstellung im All

- blass cyan/blaugrüne Wolkendecke;
- geringe sichtbare Bandstruktur im Vergleich zu Jupiter;
- extreme Rotationsachsenlage klar sichtbar;
- schmale dunkle Ringe bei passender LOD-Stufe;
- saisonale Polbeleuchtung aus realer Körperorientierung.

## Erforderliche Technik

Zusätzlich zur Gasriesen-`ENVELOPE`-Semantik:

- Hochdruck-Ice-/Fluidmantel;
- kosmische Rotationsachse und Körperorientierung;
- Beleuchtung der lokalen Dimension aus dieser Orientierung;
- Ringobjekte im UniverseSpace;
- Druck-/Temperaturphasen für H₂O/NH₃/CH₄-Gemische.

## Offene Entscheidungen

- Wie detailliert sollen Hochdruckphasen wirklich chemisch simuliert werden?
- Soll die Achsneigung unmittelbar auf Sable-Fahrzeugnavigation und künstlichen Horizont wirken?
- Werden die schwachen Ringe schon mit dem ersten Uranus-Profil implementiert oder später mit dem allgemeinen Ringsystem?
- Wie werden jahrzehntelange reale Jahreszeiten sinnvoll auf Spielzeit skaliert?
