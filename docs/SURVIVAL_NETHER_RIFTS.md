# Survival-Nether-Risse und Debug-Werkzeuge

## Ziel

Spieler sollen an der unteren Bedrock-Grenze eines gültigen
Universe-Planetenstacks kontrolliert einen lokalen, horizontalen Übergang vom
Overworld in den Nether öffnen können. Die Funktion nutzt die durch
`LocalEuclideanPortalGraph` ermittelte Verbindung
`minecraft:overworld.BOTTOM -> minecraft:the_nether.TOP`; sie erfindet keine
Zieldimension und keine Richtung.

Der Übergang bleibt lokal und reversibel. Ein zweiter Wurftrank versiegelt ihn
und stellt die ersetzte Grenzfläche wieder her.

## Survival-Gegenstände

### Nether-Riss-Trank

Ein geworfener Trank, der beim Aufprall auf eine gültige Bedrock-Untergrenze
einen Riss öffnet. Es gibt drei feste Varianten:

| Variante | Öffnung | Zweck |
| --- | --- | --- |
| klein | 3 × 3 Blöcke | Einzelspieler und enge Schächte |
| mittel | 5 × 5 Blöcke | Standardzugang |
| groß | 7 × 7 Blöcke | Fahrzeuge und größere Gruppen |

Der Trank darf ausschließlich für Spieler im Survival-Modus funktionieren.
Creative-/Spectator-Spieler verwenden stattdessen die Debug-Befehle.

### Rissversiegler

Ein geworfener Trank, der einen getroffenen bestehenden Riss schließt. Dabei
entfernt er die zugehörigen Portalinstanzen und stellt die bei der Öffnung
gesicherten Blockzustände wieder her. Der Versiegler darf keinen Riss anhand
einer bloßen Position erraten: Er benötigt die gespeicherte Riss-ID des
getroffenen Portals beziehungsweise dessen Rissfläche.

## Zulässigkeitsprüfung beim Öffnen

Vor einer Änderung prüft der Server vollständig autoritativ:

1. Der Werfer ist ein Survival-Spieler.
2. Die Quell-Dimension ist `minecraft:overworld`.
3. Der Treffer liegt auf der unteren Bedrock-Grenze des aktiven planetaren
   Dimensionsstacks ("Niedriglandschaft"), nicht nur auf einem beliebigen
   niedrigen Y-Wert oder künstlich platziertem Bedrock.
4. `LocalEuclideanPortalGraph` liefert für `overworld.BOTTOM` ein Portal zum
   `nether.TOP`.
5. Die gesamte Zielgröße besteht aus zulässigen Bedrock-Grenzblöcken und liegt
   innerhalb geladener Chunks.
6. Die Fläche überschneidet keinen vorhandenen Riss und verletzt weder einen
   Schutzbereich noch eine konfigurierbare Spieler-/Weltgrenze.

Bei einer fehlgeschlagenen Prüfung verändert der Trank keine Blöcke und wird
mit einer verständlichen Rückmeldung abgewiesen.

## Persistenter Risszustand

Jeder geöffnete Riss benötigt eine persistierte, serverautoritativ vergebene
ID. Mindestens folgende Daten werden gespeichert:

- Riss-ID und Ersteller-UUID;
- Quelle, Ziel, Größe und Begrenzungsfläche;
- die vom Portalgraphen gelieferte Skalierung und Grenzart;
- vollständige Blockzustände der ersetzten Bedrock-Fläche;
- IDs aller materialisierten Portalinstanzen;
- Öffnungszeit und optional eine Ablaufzeit.

Beim Laden einer Welt wird der Zustand gegen den aktuellen Portalgraphen
validiert. Nicht mehr gültige Risse werden nicht blind rekonstruiert, sondern
als reparaturbedürftig protokolliert.

## Debug-Befehle

Alle Befehle liegen unter `/dynamicuniverse debug` und benötigen
Administratorrechte. Lesende Befehle dürfen keinen Weltzustand verändern;
schreibende Befehle müssen Ziel, Größe und Resultat protokollieren.

### Stack- und Grenzdiagnose

| Befehl | Wirkung |
| --- | --- |
| `stacks inspect <dimension>` | Zeigt Planet, Stack, Schicht sowie obere und untere Nachbarn der Dimension. |
| `stacks validate [planetId]` | Prüft eindeutige Dimensionszuordnung, Grenzmaterial und Skalierungen. |
| `stacks route <sourceDimension> <targetDimension> [x y z]` | Gibt den Übergangspfad und die transformierten Koordinaten aus. |
| `boundaries scan <dimension> <bottom|top> [radius]` | Prüft eine Grenzfläche auf Bedrock/Luft, Lücken und falsche Blöcke. |
| `portals inspect <dimension> <bottom|top>` | Zeigt Zielraum, Grenzart und Skalierung des abgeleiteten lokalen Portals. |
| `portals list [dimension]` | Listet geplante und materialisierte lokale Portale. |

### Rissverwaltung

| Befehl | Wirkung |
| --- | --- |
| `rifts validate <x> <y> <z> <small|medium|large>` | Führt alle Öffnungsprüfungen aus, ohne Blöcke oder Portale zu ändern. |
| `rifts create <small|medium|large> <x> <y> <z>` | Öffnet für Debug-Zwecke einen Riss nach erfolgreicher Validierung. |
| `rifts inspect <id>` | Zeigt gespeicherten Zustand, Besitzer, Fläche und Portalinstanzen. |
| `rifts list [dimension]` | Listet alle Risse, optional gefiltert nach Quelle. |
| `rifts reevaluate [dimension] [x z] [radius]` | Bewertet Grenzflächen und Rissgültigkeit im Bereich erneut, ohne automatisch neue Risse zu öffnen. |
| `rifts seal <id> [restoreBlocks]` | Entfernt einen Riss; `restoreBlocks` ist standardmäßig aktiv. |
| `rifts reconcile` | Vergleicht persistierte Risse, Portalgraph und materialisierte Portale; meldet oder repariert nur explizit bestätigte Abweichungen. |
| `rifts trace <id>` | Aktiviert detaillierte Serverprotokolle für Validierung, Materialisierung und Versiegelung eines Risses. |
| `rifts give <player> <opener|sealer> [small|medium|large]` | Gibt einen Debug-Gegenstand aus. |

### Weltweite Wartung

| Befehl | Wirkung |
| --- | --- |
| `portals reevaluate [dimension] [x z] [radius]` | Aktualisiert die Portalpläne für einen Bereich, ohne ungeprüfte Portal-Entities zu erzeugen. |
| `portals materialize <portalId>` | Erzwingt die Materialisierung eines bereits validierten Portalplans. |
| `portals remove <portalId>` | Entfernt ausschließlich die benannte Portalinstanz. |
| `reload universe` | Lädt die Universe-/Stack-Konfiguration neu; eine anschließende `rifts reconcile`-Prüfung wird empfohlen. |

## Sicherheits- und UX-Regeln

- Nur der Server entscheidet über Öffnen, Schließen, Blockwiederherstellung und
  Zielkoordinaten.
- Ein Riss darf niemals ersetzt werden, solange dessen gespeicherte Fläche
  nicht vollständig wiederherstellbar ist.
- Materialisierung erfolgt erst nach vollständiger Prüfung und gespeicherter
  Riss-Metadaten; bei einem Fehler muss der Vorgang atomar zurückrollen.
- Die effektiven Größen, der erlaubte Bereich und Schutzgebietsregeln gehören
  in eine Serverkonfiguration.
- Der erste Prototyp sollte Risse nicht automatisch ablaufen lassen. Eine
  Ablaufzeit ist erst sinnvoll, wenn ihre Block- und Spielerfolgen eindeutig
  geregelt sind.
