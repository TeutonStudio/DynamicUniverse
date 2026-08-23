# Sol – Planeten- und Dimensionsplanung

Status: **Planung**

Dieses Verzeichnis beschreibt die acht Planeten des Sonnensystems als Zielprofile für DynamicUniverse. Jede Datei plant den Himmelskörper selbst, seinen radialen Dimensionsstack, Umweltmechaniken, Ressourcen sowie notwendige Engine-Erweiterungen.

## Kanonische Planetennamen

Für Sol verwendet DynamicUniverse die lateinischen Planetennamen als Anzeigenamen:

| Reihenfolge | Lateinischer Name | gebräuchlicher deutscher Name |
| ---: | --- | --- |
| 1 | **Mercurius** | Merkur |
| 2 | **Venus** | Venus |
| 3 | **Terra** | Erde |
| 4 | **Mars** | Mars |
| 5 | **Iuppiter** | Jupiter |
| 6 | **Saturnus** | Saturn |
| 7 | **Uranus** | Uranus |
| 8 | **Neptunus** | Neptun |

Die lateinischen Namen sind die kanonischen Profil- und UI-Namen. Bestehende technische IDs und Dateipfade wie `dynamicuniverse:jupiter_core` oder `JUPITER.md` bleiben vorerst stabil und werden nicht allein wegen der Anzeige umbenannt.

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
| Mercurius | 16 | 8 |
| Venus | 32 | 8 |
| Terra | 32 | 8 |
| Mars | 16 | 8 |
| Iuppiter | 360 | 8 |
| Saturnus | 304 | 8 |
| Uranus | 128 | 8 |
| Neptunus | 128 | 8 |

Langfristig sollten **physischer Körperradius**, **lokaler Pseudoradius** und **Planetenkern-Größe** getrennte Werte werden. Sonst muss die Anzahl der Dimensionen absurderweise mitentscheiden, wie groß ein Planet aussieht. Menschen haben schon kompliziertere Messsysteme erfunden, aber man muss es ihnen nicht nachmachen.

## Planungsdateien

- [Mercurius](MERCURY.md)
- [Venus](VENUS.md)
- [Terra](TERRA.md)
- [Mars](MARS.md)
- [Iuppiter](JUPITER.md)
- [Saturnus](SATURN.md)
- [Uranus](URANUS.md)
- [Neptunus](NEPTUNE.md)

## Empfohlene Implementierungsreihenfolge

1. **Terra** als Referenzprofil vollständig stabilisieren.
2. **Mercurius, Venus und Mars** als weitere terrestrische Templates implementieren.
3. Geometrische Rolle und semantische Planetenschicht im Stackmodell voneinander trennen.
4. `ENVELOPE`/Gasriesen-Validierung und druckabhängige Übergänge implementieren.
5. **Iuppiter und Saturnus** implementieren.
6. Hochdruck-Eis-/Fluidmantel für **Uranus und Neptunus** ergänzen.
7. Monde und Ringsysteme separat planen; sie gehören nicht als radiale Dimensionen in den Planetenstack.
