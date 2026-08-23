# Jupiter

Status: **Planetprofil-Plan / benötigt Gasriesen-Erweiterung**

## Zielbild

Jupiter soll als echter Gasriese funktionieren: keine feste Oberfläche, sondern ein kontinuierlicher Übergang von dünner oberer Atmosphäre über immer dichtere und heißere Wasserstoffschichten bis zu metallischem Wasserstoff und einem tiefen Kernbereich. Der Spieler kann also nicht einfach „auf Jupiter landen“. Er kann in die Atmosphäre eintauchen, schweben, fliegen oder so lange fallen, bis Druck und Temperatur jede ungeschützte Konstruktion in eine schlechte technische Anekdote verwandeln.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `360` |
| Gravitation an Wolkenoberkante | ca. `2,5 g` |
| Hauptbestandteile | Wasserstoff, Helium |
| feste Oberfläche | keine |

`coreSize = 360` ist vorläufig ein Geometrieanker für die relative sichtbare Größe. Er darf nicht als physischer Kernradius interpretiert werden.

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | geplante Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:jupiter_core` | CORE | `null -> BEDROCK` | dichter Kern-/Übergangsbereich aus schweren Elementen |
| 1 | `dynamicuniverse:jupiter_metallic_hydrogen` | SHELL | `BEDROCK -> BEDROCK` | leitfähiger metallischer Wasserstoff, extreme Drücke |
| 2 | `dynamicuniverse:jupiter_molecular_envelope` | SHELL | `BEDROCK -> BEDROCK` | dichter molekularer Wasserstoff/Helium, Übergang zur Atmosphäre |
| 3 | `dynamicuniverse:jupiter_deep_atmosphere` | ENVELOPE | `BEDROCK -> AIR` | technischer Übergang vom dichten Fluid in frei befliegbare Atmosphäre; **keine feste Oberfläche** |
| 4 | `dynamicuniverse:jupiter_atmosphere` | SKY | `AIR -> AIR` | Wolken, Stürme und äußerer Übergang zu `UniverseSpace` |

## Notwendige Modelländerung: ENVELOPE

Die aktuelle Boundary-Klassifikation würde `BEDROCK -> AIR` automatisch als `SURFACE` behandeln. Für Jupiter ist dieselbe Geometrie nur ein technischer radialer Übergang und darf keine begehbare Oberfläche oder Oberflächenlogik implizieren.

Geplant ist deshalb:

- geometrische Boundary-Klasse und semantische Planetenschicht trennen;
- terrestrische `SURFACE` und gasförmige `ENVELOPE` dürfen beide `BEDROCK -> AIR` verwenden;
- Validierung verlangt dann genau **einen äußeren Übergang**, nicht genau eine feste Oberfläche;
- Gasriesenprofile deklarieren `hasSolidSurface = false` beziehungsweise eine äquivalente Capability.

## Kern und metallischer Wasserstoff

- Der innerste Bereich enthält schwere Elemente und Hochdruckmaterialien.
- Die metallische Wasserstoffschicht ist elektrisch leitfähig und eignet sich als Quelle für extreme Energie-/Magnetfeldmechaniken.
- Direkter Aufenthalt benötigt Hochdrucktechnik weit jenseits normaler Anzüge.
- Keine künstlichen großen Höhlen, sofern nicht bewusst als seltene exotische Strukturen geplant.

## Molekulare Hülle

- Wasserstoff und Helium dominieren.
- Dichte, Druck und Temperatur steigen nach innen kontinuierlich.
- Der Übergang zu metallischem Wasserstoff soll spielmechanisch über Druck-/Temperaturwerte erfolgen, nicht durch einen sichtbaren „Steinboden“.

## Tiefe Atmosphäre

- Startet dort, wo freies Fliegen/Schweben noch sinnvoll modellierbar ist.
- Nach unten rasch steigender Druck und Temperatur.
- Kein Landen; Fahrzeuge benötigen Auftrieb oder permanenten Schub.
- Ein Fall nach innen soll über den radialen Stack weitergehen, nicht auf einer unsichtbaren Barriere enden.

## Obere Atmosphäre

Vertikale Zonen innerhalb **einer** Dimension:

1. obere H₂/He-Atmosphäre;
2. Ammoniak-Wolken;
3. Ammoniumhydrogensulfid-/Schwefelzonen;
4. tiefere Wasserwolken;
5. Sturm- und Blitzregionen;
6. Übergang in `UniverseSpace`.

Der Große Rote Fleck und andere langlebige Wirbel sind makroskopische Wetterobjekte, keine eigenen Dimensionen.

## Umweltmechaniken

- hohe Gravitation;
- extremer Druckgradient;
- Temperaturanstieg nach innen;
- starke Winde und Scherkräfte;
- Gewitter und Blitz;
- Auftrieb in dichter Atmosphäre;
- Magnetosphäre/Strahlung als spätere Gefahr;
- keine normale Bodenhaftung oder bodengebundene Basen.

## Ressourcen und Chemie

- Wasserstoff
- Helium
- Ammoniak
- Methan in Spuren
- Schwefel-/Ammoniumverbindungen
- Wasser in tieferen Wolken
- metallischer Wasserstoff als sehr späte Hochtechnologie-Ressource

## Darstellung im All

- breite Wolkenbänder mit differentieller Bewegung;
- Großer Roter Fleck als persistentes planetenfestes beziehungsweise atmosphärisch wanderndes Feature;
- deutliche Abplattung durch Rotation langfristig wünschenswert;
- keine sichtbare feste Oberfläche unter den Wolken.

## Erforderliche Technik

Jupiter ist der erste Planet, der zwingend braucht:

- `ENVELOPE`-Semantik oder gleichwertige Trennung von Boundary-Geometrie und Layer-Rolle;
- druck-/dichteabhängige Atmosphärenphysik;
- Auftrieb und Strömung für Spielerfahrzeuge/Sable-Sublevels;
- radialen Fall ohne feste Oberfläche;
- Hochdruckphasen für Fluide;
- Gasriesen-LOD ohne Oberflächentextur.

## Offene Entscheidungen

- Wie tief darf ein Spieler praktisch gelangen, bevor Materialgrenzen den Fortschritt blockieren?
- Wird metallischer Wasserstoff als abbaubares Fluid, Prozesszustand oder nur als Umweltphase dargestellt?
- Wie werden langlebige Stürme persistent gespeichert und mit der rotierenden Atmosphäre bewegt?
- Soll die starke Magnetosphäre bereits in der ersten Jupiter-Version Gameplay-Relevanz erhalten?
