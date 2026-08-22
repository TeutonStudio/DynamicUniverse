# Lokale Portalgrenzen

## Ursache

Der Universe-Welttyp speicherte Schichten ausschließlich als `inner -> outer`
und wandelte sie in ungerichtete Koordinatenrouten um. Dabei ging verloren,
welche Seite einer Dimension unten oder oben liegt. Der bisherige
`DimensionConnectionGraph` enthält deshalb keine Information, mit der ein
Portaladapter das Bedrock am Boden des Overworlds eindeutig dem oberen Rand des
Nethers zuordnen kann. Es gab folglich auch keinen Plan für ein lokales,
euklidisches Portal.

## Korrektur

Die planetenbasierte Auswahlliste enthält nun außerdem eine verpflichtende,
feste `minecraft:the_nether`-Schicht zwischen Untergrund und Oberfläche. Damit
zeigt bereits ein Planet ohne zusätzliche Zwischenschichten den Zielraum, den
seine untere Overworld-Grenze erreichen soll.

`UniverseWorldType.localEuclideanPortalGraph()` leitet jetzt für jede valide
gemeinsame Schichtgrenze zwei gerichtete Portale ab:

- `inner.UPPER -> outer.LOWER`
- `outer.LOWER -> inner.UPPER`

Die Grenzart (`BEDROCK` oder `AIR`) und die horizontale Skalierung bleiben am
Portal erhalten. Für `minecraft:overworld` an seiner Unterseite wird damit bei
einer passenden Bedrock-Grenze automatisch `minecraft:the_nether` an dessen
Oberseite gefunden. Eine Dimension darf nur noch einer vertikalen Position
zugeordnet werden; mehrdeutige Planet-/Stack-Zuordnungen werden beim Erzeugen
des Modells abgelehnt.

Die Spezifikation ist absichtlich adapterneutral. Ein optionaler
Immersive-Portals-Adapter kann die vollständigen `LocalEuclideanPortal`-Daten
direkt als horizontale Portale materialisieren, ohne Reihenfolge oder Richtung
erneut erraten zu müssen.
