# Dynamische Survival-Bedrock-Risse

## Ziel

Ein Riss ist keine feste 3×3-, 5×5- oder 7×7-Struktur mehr. Er entsteht direkt
aus der tatsächlich zerstörten Bedrock-Fläche einer registrierten
Bedrock-to-Bedrock-Grenze. Jeder weitere angrenzende zerstörte Grenzblock
vergrößert dieselbe logische Apertur.

Die Runtime entscheidet ausschließlich anhand des aktiven Universe-Manifests,
welche Dimension, Grenzseite und Skalierung zur getroffenen Bedrock-Ebene
gehören. Gewöhnlicher Bedrock außerhalb einer registrierten Grenzebene wird von
diesem System nicht verändert.

## Neue Öffnung

Beim ersten zerstörten Grenzblock:

1. wird die zugehörige `DimensionConnection` aufgelöst;
2. wird die Quellkoordinate im toroidalen Layer kanonisiert;
3. bestimmt die Stack-Skalierung genau einmal den Gegenanker;
4. wird geprüft, dass der Gegenblock noch Bedrock ist;
5. werden Quell- und Gegenblock in einer serverautoritiven Transaktion geöffnet;
6. wird eine `PairedBoundaryAperture` mit der lokalen Zelle `(0,0)` persistiert;
7. wird die Portalansicht daraus materialisiert.

## Vergrößerung

Sobald eine Apertur existiert, wird ihre Form lokal behandelt. Die globale
Dimensionsskalierung wird für weitere Zellen nicht erneut angewendet.

Beispiel bei Faktor 8:

```text
Nether-Anker 10  <->  Außen-Anker 80
Nether +1        <->  Außen +1
11               <->  81
```

Nicht `11 <-> 88`.

Die Form ist eine Menge lokaler Blockzellen. Dadurch können konkave, L-förmige
oder anderweitig unregelmäßige Löcher exakt beschrieben werden. Immersive
Portals materialisiert in der ersten Implementierung jede Zelle als eigenes
1×1-Portalpaar. Die logische Form ist davon unabhängig und kann später ohne
Save-Migration zu einem optimierten Portalmesh zusammengefasst werden.

## Toroidale Nähte

Nachbarschaft wird über den `HorizontalPeriod` der betreffenden Dimension
berechnet. Zwei Zellen an gegenüberliegenden X- oder Z-Kanten können deshalb
Teil derselben Apertur sein. Die Portalmaterialisierung kanonisiert auch die
Weltposition jeder lokalen Zelle, sodass ein Riss über eine Torusnaht nicht in
eine zweite logische Öffnung zerfällt.

## Zusammenwachsende Risse

Berührt ein neu zerstörter Block mehrere bestehende Aperturen derselben
Verbindung, vergleicht der Server deren lokale Gegenabbildung für diese Zelle.
Nur wenn alle denselben Gegenblock ergeben, werden sie zu einer Apertur
vereinigt. Andernfalls wird die Bedrock-Zerstörung abgelehnt. Damit kann ein
Spieler zwei global verschieden verankerte lokale Räume nicht versehentlich zu
einem widersprüchlichen Portal zusammenschweißen.

## Planetenkern

Die Verbindung zum Planetenkern ist die Ausnahme. Der tiefe Nether beziehungsweise
die erste Nicht-Kern-Schicht ist alleinige persistente Wahrheit.

Gespeichert werden dort nur:

- Apertur-ID und Erstellungsreihenfolge;
- Planet und Connection-ID;
- Deep-Layer-Dimension und Grenzseite;
- kanonischer Deep-Layer-Anker;
- lokale Aperturform.

Es werden keine Kernkoordinaten gespeichert. Der
`PlanetCoreProjectionResolver` ordnet alle Deep-Layer-Aperturen deterministisch
einer der sechs Würfelflächen zu. Eine Projektion ist nur zulässig, wenn die
vollständige Form mit Sicherheitsrand auf genau einer Fläche liegt und keine
bereits zugewiesene Kernöffnung schneidet oder direkt berührt.

Ist keine kantenfreie Projektion möglich, bleibt der auslösende Deep-Layer-
Bedrockblock erhalten. Bedrock auf der Kernseite selbst kann keine neue
Apertur erzeugen oder vergrößern.

## Persistenz und Wiederaufbau

`UniverseSaveData` speichert weiterhin nur die statische Universe-Definition und
die generatorseitig ermittelten Bedrock-Ebenen. Spieleränderungen liegen in der
separaten `BoundaryApertureSaveData`.

Beim Serverstart:

1. wird das Universe-Manifest rekonstruiert;
2. werden Deep-Layer-Aperturen geladen;
3. werden Planetenkern-Projektionen erneut deterministisch berechnet;
4. werden die entsprechenden Kernblöcke geöffnet;
5. werden Portalansichten aus dem logischen Aperturzustand abgeglichen.

Immersive-Portals-UUIDs sind ausdrücklich kein persistentes Domainmodell.
Portal-Entities tragen stattdessen `dynamicuniverse:aperture:<id>`-Tags; bereits
persistierte Entities mit derselben Apertur-ID werden vor einer erneuten
Materialisierung entfernt.

## Transaktionsregeln

Für jede Grenzmutation gilt:

- nur der Server entscheidet über Quelle, Ziel und Aperturzuordnung;
- die Vanilla-Bedrock-Zerstörung wird bei einer verwalteten Grenze abgebrochen;
- alle benötigten Gegenblöcke und Projektionen werden vor der Mutation geprüft;
- Quell-, Ziel- beziehungsweise Kernblöcke werden gemeinsam geändert;
- schlägt eine Blockmutation fehl, werden bereits geänderte Blockzustände
  zurückgerollt;
- Portalmaterialisierung ist eine abgeleitete Darstellung und darf die
  persistente logische Apertur nicht zur Hälfte zurückrollen;
- ein vorhandener Nicht-Bedrock-/Nicht-Luft-Spielerblock wird nicht blind
  überschrieben.

## Aktueller Scope

Diese Implementierung deckt Erzeugen, Vergrößern, kompatibles Zusammenführen,
Persistenz, Neustart-Rekonstruktion und Planetenkern-Projektion ab. Ein eigenes
Versiegelungswerkzeug, Schutzgebietsregeln und administratives Apertur-Editing
bleiben separate Features; sie müssen auf `BoundaryApertureSaveData` aufsetzen
und dürfen keine zweite Riss-Wahrheit neben dem dynamischen Aperturmodell
einführen.
