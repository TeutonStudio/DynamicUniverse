# Mercurius

Status: **Planetprofil-Plan**

## Zielbild

Mercurius soll sich wie ein fast atmosphärenloser, stark verkraterter Gesteinsplanet mit ungewöhnlich großem metallischem Kern spielen. Das zentrale Gameplay entsteht aus extremer Sonneneinstrahlung, sehr starken Temperaturunterschieden zwischen Tag und Nacht, geringer Gravitation und einer nur hauchdünnen Exosphäre.

## Planetparameter

| Parameter | Planwert |
| --- | --- |
| Körperart | `PLANET` |
| radialer Faktor | `8` |
| `coreSize` | `16` |
| Gravitation | ca. `0,38 g` |
| Atmosphäre | praktisch keine; Exosphäre |
| dominante Oberfläche | Regolith, Basalt, Krater, Metall-/Silikatgestein |

`coreSize = 16` dient nur als lokaler Geometrieanker. Der kosmische Radius bleibt davon getrennt.

## Geplanter Dimensionsstack

Von innen nach außen:

| # | Template-ID | Rolle | Grenzen | Zweck |
| ---: | --- | --- | --- | --- |
| 0 | `dynamicuniverse:mercury_core` | CORE | `null -> BEDROCK` | eisen-/nickelreicher Kern, sehr heiß, teilweise flüssige Zonen |
| 1 | `dynamicuniverse:mercury_mantle` | SHELL | `BEDROCK -> BEDROCK` | silikatischer Mantel und tiefe Kruste |
| 2 | `dynamicuniverse:mercury_surface` | SURFACE | `BEDROCK -> AIR` | Kraterlandschaft, Regolith, Klippen und Einschlagbecken |
| 3 | `dynamicuniverse:mercury_exosphere` | SKY | `AIR -> AIR` | nahezu Vakuum; Übergang in `UniverseSpace` |

### Kern

- Schwerpunkt auf Eisen und Nickel.
- Lava darf vorkommen, soll aber nicht das gesamte Volumen wie bei Terra dominieren.
- Hohe Temperatur und hoher technischer Aufwand für direkten Abbau.

### Mantel

- Trockene, kompakte Gesteinsschale.
- Große erkaltete Magmakörper und Metalladern.
- Keine Nether-Kopie. Mercurius soll nicht lediglich Terra mit anderer Textur sein.

### Oberfläche

- Große Krater und Überlappungen alter Einschläge.
- Starke Reliefkanten und Becken.
- Keine natürliche Vegetation oder offenen Flüssigkeitsmeere.
- In dauerhaft verschatteten Polkratern kann Wassereis als seltene strategische Ressource vorkommen.

### Exosphäre

- Fast vakuumartig.
- Kein normaler Minecraft-Himmel mit dichter Luft.
- Kleine Mengen Natrium, Kalium, Sauerstoff-/Helium-Spuren können später als Umwelt-/Chemiesystem abgebildet werden.
- Fahrzeuge und Spieler gehen bei ausreichender Höhe ohne harte Portalwand in `UniverseSpace` über.

## Umweltmechaniken

- **Sonnenseite:** starke Wärmebelastung abhängig von Sonnenwinkel und Tageszeit.
- **Nachtseite:** starke Abkühlung.
- **Vakuum:** ohne geschlossenen Anzug kein normaler Atem-/Druckzustand.
- **Geringe Gravitation:** höhere Sprünge, langsamere Fallbeschleunigung und andere Fahrzeugphysik.
- **Solarstrahlung:** später als zusätzlicher Schutzparameter geeignet.

## Ressourcen und Technik

Priorisierte natürliche Ressourcen:

- Eisen
- Nickel
- Silikate
- Schwefel
- seltenes Wassereis an den Polen

Der Planet soll damit für Metallgewinnung attraktiv sein, während Wasser und lebensfreundliche Betriebsbedingungen teuer bleiben.

## Darstellung im All

- Dunkelgraue, stark verkraterte Kugel ohne sichtbare Wolkendecke.
- Tagseite sehr hell, Nachtseite fast schwarz.
- Terminator deutlich sichtbar.
- Keine künstliche Atmosphärenkorona außer einer sehr schwachen Exosphärenkante.

## Erforderliche Technik

Für eine erste Version reicht das bestehende CORE/SHELL/SURFACE/SKY-Modell. Zusätzliche Enginearbeit ist vor allem für:

- echte geringe Gravitation,
- Sonneneinstrahlungs-/Temperaturmodell,
- Vakuum- und Drucksystem,
- polare Kältefallen.

## Offene Entscheidungen

- Soll der sehr lange reale Mercurius-Tag spielmechanisch stark verkürzt werden oder direkt aus der Orbit-/Rotationssimulation folgen?
- Soll Wassereis nur in vorab berechneten Polregionen oder dynamisch anhand dauerhafter Verschattung entstehen?
- Wie stark soll Wärme in Blöcke, Contraptions und Sable-Sublevels übertragen werden?
