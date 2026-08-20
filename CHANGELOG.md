# Changelog

Alle nennenswerten Änderungen. Versionsschema: `MAJOR.MINOR.PATCH`,
Vorabversionen als `-dev.N`.

## 0.4.8 – 2026-08-20

### Für F-Droid
- Der von AGP eingebettete, Google-signierte Abhängigkeits-Metadatenblock wird nicht mehr
  ins APK geschrieben (`dependenciesInfo`). Er unterscheidet sich bei jedem Build und
  verhinderte damit reproduzierbare Builds. An der App selbst ändert sich nichts.

## 0.4.7 – 2026-08-20

### Geändert
- **PIN-Schutz ist standardmäßig aus.** Auf einem Wandtablet zuhause ist die Abfrage bei
  jedem Handgriff lästiger als nützlich; wer sie braucht, schaltet sie unter *System* ein.
  Wirkt nur bei Neuinstallationen.

### Für F-Droid
- Neue Flavor-Dimension `distribution`: **github** mit In-App-Update, **fdroid** ohne
  Updater und ohne `REQUEST_INSTALL_PACKAGES` – F-Droid aktualisiert selbst.
- Ohne `keystore.properties` wird keine Signier-Konfiguration mehr angelegt, der Build
  liefert dann ein unsigniertes APK statt zu scheitern.
- Gradle-Wrapper im Repo, Distribution über Prüfsumme festgenagelt; `vcsInfo` bleibt aus
  dem Release-APK (reproduzierbare Builds).

## 0.4.6 – 2026-08-20

### Neu
- **Mehrere Dashboards (bis zu 3)** mit eigenem Namen, URL, Login und
  Zertifikats-Einstellung je Dashboard. Ein Dashboard ist das **Standard-Dashboard**
  und wird nach jedem Aufwecken wieder angezeigt; das Seitenmenü listet alle zum
  Umschalten auf. Neu dazu der MQTT-Befehl `cmd/dashboard` sowie die Readings
  `dashboard` und `dashboardName`.
- **Sprachsteuerung im Dashboard:** `getUserMedia`-Anfragen der geladenen Seite können
  freigegeben werden (Auswahl unter *Verhalten → Mikrofon*), und die JS-Brücke
  `window.fullscreendy.textToSpeech()` ersetzt das in der Android-WebView fehlende
  `speechSynthesis`.
- **PIN-Schutz abschaltbar.**

### Geändert
- Einstellungen neu sortiert (*Dashboards* zuerst, *Verbindung* nur noch MQTT/Topics),
  Menüs und Über-Seite überarbeitet; Update-Download zeigt den Fortschritt in Prozent.
- Mikrofon-Nutzung ist eine Auswahl statt zweier Schalter – *Wecken bei Ton* und
  *Sprachsteuerung* schließen sich physikalisch aus.

### Behoben
- **Pull-to-Refresh löste beim Hochscrollen aus.** Jetzt lädt nur ein Zug nach unten
  neu, der am Seitenanfang beginnt. ([#1](https://github.com/Glenn-Dandy/fullscreendy/issues/1))

## 0.4.5 – 2026-07-21
- Android 9 (API 28), Dashboard-Login (HTTP Basic Auth), selbst-signierte Zertifikate
  zulassen, In-App-Update, Auto-Speichern der Einstellungen sowie zahlreiche
  Verbesserungen für den Dauerbetrieb (LOW_MEMORY, Auto-Wiederherstellung der UI).
