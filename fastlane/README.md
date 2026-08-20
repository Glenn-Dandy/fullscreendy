# Fastlane-Metadaten (F-Droid)

F-Droid liest den Store-Eintrag aus diesem Verzeichnis — aus dem **getaggten Commit**,
nicht aus `HEAD`. Neue Texte werden also erst mit dem nächsten stabilen Tag sichtbar.

```
fastlane/metadata/android/<locale>/
├── title.txt              max. 30 Zeichen
├── short_description.txt  max. 80 Zeichen
├── full_description.txt   max. 4000 Zeichen, erlaubt <b> <i> <u> <br> <ul> <li> <a>
├── changelogs/<versionCode>.txt   max. 500 Zeichen, eine Datei je Release
└── images/phoneScreenshots/       1.png, 2.png, … (in der gewünschten Reihenfolge)
```

Gepflegt sind `en-US` und `de-DE`. Beim nächsten Release eine neue
`changelogs/<versionCode>.txt` anlegen (aktuell: `19.txt` für 0.4.6).

## Was noch fehlt: Screenshots

`images/phoneScreenshots/` ist leer — die Bilder müssen vom Gerät kommen. Sinnvoll sind
vier bis fünf PNGs, ohne Rahmen, in Gerätegröße:

1. Dashboard im Vollbild (das, was den Zweck der App auf einen Blick zeigt)
2. Seitenmenü mit der Dashboard-Liste
3. Einstellungen → Dashboards mit den Reitern
4. Einstellungen → Verhalten mit der Mikrofon-Auswahl
5. Über-Seite

Aufnehmen lassen sie sich per `adb shell screencap -p /sdcard/1.png` und
`adb pull /sdcard/1.png`. Bitte darauf achten, dass keine echten Zugangsdaten,
Tokens oder interne Adressen im Bild stehen — die Bilder landen im F-Droid-Katalog.
