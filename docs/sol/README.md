# Sol – Planeten- und Dimensionsplanung

Status: **Planung**

Dieses Verzeichnis beschreibt die acht Planeten des Sonnensystems als Zielprofile für DynamicUniverse. Jede Datei plant den Himmelskörper selbst, seinen radialen Dimensionsstack, Umweltmechaniken, Ressourcen sowie notwendige Engine-Erweiterungen.

## Gemeinsame Regeln

- Alle acht Körper sind `CelestialBodyKind.PLANET` und verwenden standardmäßig den radialen Faktor **1:8**.
- `UniverseSpace` ist keine normale Stack-Dimension, sondern der kontinuierliche Außenraum, an den die äußerste Himmels-/Atmosphärenschicht bindet.
- Der `coreSize` ist ein Geometrieanker der lokalen Dimensionsgeometrie und **kein** kosmischer Kollisionsradius.
- Planetenprofile sind Vorlagen. Sobald der Spieler sie verändert, entsteht wie vorgesehen ein lokales Custom-Profil.
- Dimensionen sollen nur getrennt werden, wenn Topologie, Generation oder Gameplay tatsächlich einen eigenen Raum rechtfertigen. Atmosphärische Schichten, die nur Druck, Temperatur oder Wolkentyp ändern, sollen bevorzugt als vertikale Zonen innerhalb einer Dimension modelliert werden.

## Boundary-Grammatik

Die derzeitige alpha0-Grammatik bleibt Grundlage:

| Geometrie | Bedeutung |
| --- | --- |
| `null -> BEDROCK` | Kern |
| `BEDROCK -> BEDROCK` | innere Schale |
| `BEDROCK -> AIR` | äußerer Übergang zur freien Atmosphäre |
| `AIR -> AIR` | Himmel / Atmosphäre |

Für terrestrische Planeten entspricht `BEDROCK -> AIR` einer echten festen Oberfläche. Für Gas- und Eisriesen darf dieselbe Geometrie künftig **nicht** automatisch eine begehbare Oberfläche bedeuten. Dafür wird eine semantische Rolle `ENVELOPE` beziehungsweise eine äquivalente Trennung von Geometrie und Planetenschicht benötigt.

## Größenanker

Die vorgeschlagenen Werte orientieren die sichtbare relative Planetengröße grob an Terra, solange die Pseudoradius-Geometrie noch direkt von `coreSize` und Stacktiefe abhängt. Sie sind ausdrücklich keine Aussage über die reale Größe des physikalischen Planetenkerns.

| Planet | `coreSize`-Planwert | Faktor |
| --- | ---: | ---: |
| Merkur | 16 | 8 |
| Venus | 32 | 8 |
| Terra | 32 | 8 |
| Mars | 16 | 8 |
| Jupiter | 360 | 8 |
| Saturn | 304 | 8 |
| Uranus | 128 | 8 |
| Neptun | 128 | 8 |

Langfristig sollten **physischer Körperradius**, **lokaler Pseudoradius** und **Planetenkern-Größe** getrennte Werte werden. Sonst muss die Anzahl der Dimensionen absurderweise mitentscheiden, wie groß ein Planet aussieht. Menschen haben schon kompliziertere Messsysteme erfunden, aber man muss es ihnen nicht nachmachen.

## Planungsdateien

- [Merkur](MERCURY.md)
- [Venus](VENUS.md)
- [Terra](TERRA.md)
- [Mars](MARS.md)
- [Jupiter](JUPITER.md)
- [Saturn](SATURN.md)
- [Uranus](URANUS.md)
- [Neptun](NEPTUNE.md)

## Empfohlene Implementierungsreihenfolge

1. **Terra** als Referenzprofil vollständig stabilisieren.
2. **Merkur, Venus und Mars** als weitere terrestrische Templates implementieren.
3. Geometrische Rolle und semantische Planetenschicht im Stackmodell voneinander trennen.
4. `ENVELOPE`/Gasriesen-Validierung und druckabhängige Übergänge implementieren.
5. **Jupiter und Saturn** implementieren.
6. Hochdruck-Eis-/Fluidmantel für **Uranus und Neptun** ergänzen.
7. Monde und Ringsysteme separat planen; sie gehören nicht als radiale Dimensionen in den Planetenstack.
