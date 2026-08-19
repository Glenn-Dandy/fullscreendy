# FullScreendy

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

[English](README.md) · **Deutsch**

Ein Vollbild-Kiosk-Browser (Chromium/WebView) für Android-Wandtablets, der ein
FHEM-Dashboard anzeigt und das Tablet über **MQTT** als FHEM-Gerät anbindet.

- 🔋 **Akku & Laden** – Ladestand, Ladezustand, Temperatur
- 🔊 **Text-to-Speech** – FHEM schickt Text, das Tablet spricht ihn
- 🔈 **Töne** – gespeicherte Sounddateien im Hintergrund abspielen
- 🖥️ **Bildschirm-Steuerung** – an/aus (Overlay) und Helligkeit
- 🚶 **Präsenz / Bewegung** – Kamera-Bewegungserkennung, weckt das Display (kein Bild verlässt das Gerät)
- 🗂️ **Bis zu 3 Dashboards** – benannte Reiter, im Menü umschaltbar; das
  **Standard-Dashboard** wird beim Aufwecken immer wieder angezeigt
- 🌐 **Fernsteuerung** – Dashboard oder URL wechseln, neu laden, Cache leeren – alles per MQTT
- ☰ **Menü** – von links einwischen: MQTT-Status, Dashboard-Liste, Aktionen,
  Einstellungen (PIN optional), Über, App beenden
- 🌍 **Zweisprachig** – Englisch (Standard) / Deutsch

Alles läuft in einem dauerhaften Foreground-Service mit Auto-Reconnect zum MQTT-Broker.

---

## Bauen

Voraussetzung: **Android Studio** (Ladybug o. neuer) mit Android SDK 35.

1. In Android Studio: *File → Open* → diesen Ordner (`fullscreendy`) wählen.
2. Gradle-Sync abwarten (lädt AGP 8.7, Kotlin 2.1, Abhängigkeiten).
3. *Run* auf ein Gerät/Emulator (**Android 9+**, API 28 – läuft auch auf Fire OS 7 /
   Fire-HD-Tablets), oder APK bauen:
   *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

```bash
gradle wrapper            # einmalig, legt ./gradlew an
./gradlew assembleDebug    # APK: app/build/outputs/apk/debug/app-debug.apk
```

Fertige signierte APKs gibt es auf der [Releases-Seite](https://github.com/Glenn-Dandy/fullscreendy/releases).

---

## Erste Einrichtung (auf dem Tablet)

1. App starten. Da noch keine URL konfiguriert ist, öffnen sich direkt die
   **Einstellungen**.
2. Eintragen:
   - *Dashboards*: **Dashboard 1** gibt es immer; weitere (max. 3) über den Reiter
     **„+ Hinzufügen“**. Je Dashboard: **Anzeigename** (erscheint im Menü), **URL**
     (z. B. `http://192.168.1.10:8083/fhem/floorplan/Wohnung`), optional ein **Login**
     (Benutzer/Passwort für ein passwortgeschütztes Dashboard, z. B. FHEM `basicAuth` –
     als HTTP Basic Auth gesendet, sauberer als `user:pass@` in der URL) und
     **selbst-signierte Zertifikate erlauben** (aktivieren, wenn dieses Dashboard HTTPS
     mit selbst-signiertem/ungültigem Zertifikat nutzt, z. B. lokales FHEM –
     standardmäßig aus; nur im eigenen LAN nutzen). Ein Dashboard ist das
     **Standard-Dashboard** (Stern): Es wird beim Start und nach jedem Aufwecken
     des Bildschirms angezeigt.
   - *Verbindung*: **MQTT-Host/Port** (+ ggf. Benutzer/Passwort/TLS), **Basis-Topic**
     (Standard `fhem/tablet`) und **Geräte-ID** (Standard `tablet1`)
   - *System*: Sprache, **PIN-Schutz** (abschaltbar) und Admin-PIN (Standard `0000`)
3. Die Einstellungen speichern sich ~1 s nach der letzten Änderung selbst; zurück →
   das Dashboard wird angezeigt.

Kamera- und Benachrichtigungs-Berechtigung beim ersten Start erlauben.

**Menü / zurück in die Einstellungen:** vom **linken Bildschirmrand nach rechts
wischen** → Menü mit MQTT-Status, der **Dashboard-Liste** (zum Umschalten antippen),
*Neu laden*, *Cache leeren*, *Bildschirm aus*, *Einstellungen* (PIN, falls aktiv),
*Über* (Version, Update, GitHub), *App beenden*.

### Autostart (optional)
Die App ist bewusst **kein Launcher/Home-Ersatz** – das Tablet bleibt normal
bedienbar. Für Autostart nach dem Booten: *Einstellungen → System → „Beim Booten
starten“* aktivieren. Auf manchen Geräten (Xiaomi, Huawei, …) zusätzlich in den
Android-Einstellungen den **OEM-Autostart** für FullScreendy erlauben. Für
Vollverriegelung ggf. Android **Screen Pinning** nutzen.

---

## MQTT-Topics

Basis: `<Basis-Topic>/<Geräte-ID>`, im Beispiel `fhem/tablet/tablet1`.

**Das Tablet meldet (retained):**

| Topic | Werte |
|---|---|
| `…/status` | `online` / `offline` (Last-Will) |
| `…/battery` | `0`–`100` |
| `…/charging` | `on` / `off` |
| `…/plug` | `ac` / `usb` / `wireless` / `none` |
| `…/batteryTemp` | z. B. `24.5` |
| `…/motion` | `on` / `off` |
| `…/presence` | `present` / `absent` |
| `…/screen` | `on` / `off` |
| `…/brightness` | `0`–`100` oder `auto` |
| `…/volume` | `0`–`100` (Medienlautstärke) |
| `…/url` | aktuell geladene URL |
| `…/dashboard` | Nummer des sichtbaren Dashboards (`1`–`3`) |
| `…/dashboardName` | dessen Anzeigename |
| `…/ip` | IPv4-Adresse |
| `…/appVersion` | App-Version, z. B. `0.2.0` |
| `…/androidVersion` | z. B. `13 (SDK 33)` |

**Das Tablet empfängt Befehle (`…/cmd/…`):**

| Topic | Payload | Wirkung |
|---|---|---|
| `cmd/tts` | beliebiger Text | spricht den Text (Aliase: `say`, `speak`) |
| `cmd/mediaplay` | `ordner/sound.mp3` | spielt gespeicherte Tondatei ab (Aliase: `media`, `play`) |
| `cmd/mediastop` | (egal) | stoppt die Wiedergabe |
| `cmd/url` | URL | lädt eine andere Seite und meldet sie als `url`-Reading |
| `cmd/dashboard` | `1`–`3` oder Name | schaltet auf dieses Dashboard um (Alias: `db`) |
| `cmd/reload` | (egal) | lädt die Seite neu |
| `cmd/clearcache` | (egal) | leert den Browser-Cache |
| `cmd/screen` | `on` / `off` | weckt das Display physisch (an) bzw. schwarzes Overlay (aus) |
| `cmd/screensaver` | `on` / `off` | wie `screen`, invertiert |
| `cmd/brightness` | `0`–`100` oder `auto` | Helligkeit (voller Bereich mit „Helligkeitssteuerung erlauben") |
| `cmd/volume` | `0`–`100` | Medienlautstärke (TTS/Töne) |
| `cmd/lock` | (egal) | sperrt den Bildschirm (benötigt Geräteadmin) |
| `cmd/unlock` | (egal) | weckt & löst den (unsicheren) Sperrbildschirm |
| `cmd/vibrate` | Dauer in ms (leer = 200) | Vibrations-Feedback, max. 5000 ms |

**Töne:** Dateien in den öffentlichen Ordner **`/sdcard/FullScreendy/`** kopieren
(über Dateimanager/USB erreichbar). Vorher einmalig **Einstellungen → System →
„Dateizugriff erlauben"** erteilen. Dann per `cmd/mediaplay ding.mp3` abspielen.
`http…`-URLs werden gestreamt, Pfade mit `/` als absolute Datei behandelt.

---

## FHEM-Konfiguration

Voraussetzung: ein MQTT-Server in FHEM (`MQTT2_SERVER`) **oder** ein
`MQTT2_CLIENT`, der mit deinem Broker (z. B. mosquitto) verbunden ist.

### Gerät anlegen
Sobald das Tablet zum ersten Mal publisht, schlägt FHEM per `autocreate` ein
`MQTT2_DEVICE` vor. Alternativ manuell:

```
define fullscreendy MQTT2_DEVICE
attr fullscreendy readingList fhem/tablet/tablet1/status:.* status \
  fhem/tablet/tablet1/battery:.* battery \
  fhem/tablet/tablet1/charging:.* charging \
  fhem/tablet/tablet1/plug:.* plug \
  fhem/tablet/tablet1/batteryTemp:.* batteryTemp \
  fhem/tablet/tablet1/motion:.* motion \
  fhem/tablet/tablet1/presence:.* presence \
  fhem/tablet/tablet1/screen:.* screen \
  fhem/tablet/tablet1/brightness:.* brightness \
  fhem/tablet/tablet1/volume:.* volume \
  fhem/tablet/tablet1/url:.* url \
  fhem/tablet/tablet1/ip:.* ip \
  fhem/tablet/tablet1/appVersion:.* appVersion \
  fhem/tablet/tablet1/androidVersion:.* androidVersion
```

Set-Befehle müssen manuell gesetzt werden („fullscreendy" durch euren Gerätenamen ersetzen):

```
attr fullscreendy setList screenOn:noArg   fhem/tablet/tablet1/cmd/screen on \
  screenOff:noArg  fhem/tablet/tablet1/cmd/screen off \
  reload:noArg     fhem/tablet/tablet1/cmd/reload 1 \
  clearCache:noArg fhem/tablet/tablet1/cmd/clearcache 1 \
  lock:noArg       fhem/tablet/tablet1/cmd/lock 1 \
  unlock:noArg     fhem/tablet/tablet1/cmd/unlock 1 \
  mediaStop:noArg  fhem/tablet/tablet1/cmd/mediastop 1 \
  brightness:slider,0,1,100 fhem/tablet/tablet1/cmd/brightness $EVTPART1 \
  volume:slider,0,1,100 fhem/tablet/tablet1/cmd/volume $EVTPART1 \
  url:textField       fhem/tablet/tablet1/cmd/url $EVTPART1 \
  mediaPlay:textField fhem/tablet/tablet1/cmd/mediaplay $EVTPART1 \
  say:textField {my @a=split(" ",$EVENT);; shift(@a);; return "fhem/tablet/tablet1/cmd/tts ".join(" ",@a)}
attr fullscreendy stateFormat online battery %
```

`$EVTPART1` reicht für Werte ohne Leerzeichen (Zahl, URL, Dateipfad).

### Text-to-Speech (ganzer Satz mit Leerzeichen)
`setList` zerlegt Argumente wortweise, daher für freien Text ein Perl-Ausdruck,
der `$EVENT` ab dem ersten Leerzeichen abschneidet:

```
attr <device> setList say:textField {my @a=split(" ",$EVENT);; shift(@a);; return "fhem/tablet/tablet1/cmd/tts ".join(" ",@a)}
```

Alternativ direkt publizieren (`<IODev>` = dein MQTT2_SERVER/CLIENT):

```
set MQTT2_Broker publish fhem/tablet/tablet1/cmd/tts Guten Morgen, es sind 8 Grad
```

### Beispiele
```
# Bei Bewegung Licht an
define n_flur_licht notify tablet1:motion:.on set Flurlicht on

# Klingel-Sound abspielen (Datei im Sound-Ordner)
set MQTT2_Broker publish fhem/tablet/tablet1/cmd/mediaplay tuerklingel.mp3
```

---

## Geräte-Einstellungen & Berechtigungen

Unter *Einstellungen → Verhalten*:
- **Zum Aktualisieren nach unten ziehen**: Es lädt nur ein Zug nach unten neu, der am
  Seitenanfang *beginnt* (mit dem bekannten Lade-Kreis). Hochscrollen stoppt einfach
  oben auf der Seite – kein versehentlicher Reload beim normalen Scrollen.
- **Bewegungs-Empfindlichkeit** (Schieberegler) für die Kamera-Erkennung.
- **Wecken bei Ton (Mikrofon)** mit **Ton-Empfindlichkeit** – lauter Umgebungsschall
  weckt das Display (nur Lautstärke, keine Aufnahme).
- **Test-Indikatoren**: bei aktivierter Bewegungs-/Ton-Erkennung zeigen grüne
  Punkte live jede Erkennung an (das Gerät vibriert dabei kurz). Erst speichern,
  dann winken bzw. Geräusch machen.
- **Robust gegen Lichtwechsel**: die Bewegungserkennung zieht die globale
  Helligkeitsänderung ab und verlangt mehrere Frames – gleichmäßiges Flackern
  (Fernseher, Beleuchtung) löst nicht aus, lokale Bewegung schon. Reagiert es
  trotzdem noch, die **Bewegungs-Empfindlichkeit** etwas verringern.

Unter *Einstellungen → Anzeige*:
- **Bildschirm immer an** ist standardmäßig aus.
- **Bildschirm abdunkeln** (Standard 60 s): dunkelt nach Inaktivität per schwarzem
  Overlay ab, statt das Display per Android-Timeout auszuschalten. Der Bildschirm
  bleibt technisch an, damit **Kamera/Mikrofon weiterlaufen** und Bewegung/Ton
  **zuverlässig** wecken – und es blitzt beim Aufwecken **kein Wallpaper/Lockscreen**
  mehr auf. Berührung oder **jede** Bewegung hebt das Abdunkeln sofort auf und
  startet den Timer neu. `0 s` = nie. (Ist Wecken-auf-Bewegung/-Ton aktiv, bleibt
  der Bildschirm automatisch an.)
- **Bildschirm ausschalten** (Standard 0 = nie): nach *noch längerer* Inaktivität
  das Display ganz ausschalten (per Geräteadmin). Danach weckt nur Berührung/Power/
  `cmd/screen on` – Bewegung nicht mehr (Kamera ist dann aus). Beim echten
  Ausschalten kann das System beim Aufwecken kurz seine Aufwach-Animation zeigen;
  für **komplett flackerfreies** Aufwecken nur **Abdunkeln** nutzen (Ausschalten = 0).
  Dann bleibt das App-Fenster im Vordergrund und das Dashboard ist sofort da.
- **Periodischer Reload** (Standard 6 h, 0 = aus): lädt das Dashboard per Timer neu,
  damit der WebView-Speicher (DOM/JS/Bild-Cache) nicht über Tage anwächst – die
  wichtigste Maßnahme gegen `LOW_MEMORY`-Kills im Dauerbetrieb.

Unter *Einstellungen → System*:
- **PIN-Schutz für Einstellungen** (Standard an): Ausgeschaltet öffnen sich die
  *Einstellungen* aus dem Menü ohne PIN-Abfrage – praktisch auf einem Tablet, an das
  sonst niemand herankommt.

Unter *Einstellungen → System → Berechtigungen* (einmalig erteilen; erteilte
Berechtigungen zeigen ein „✓"):
- **Kamera erlauben** → für Bewegungserkennung (Frontkamera).
- **Mikrofon erlauben** → für Ton-Weckung.
- **Geräteadmin aktivieren** → nötig für `cmd/lock` und „Bildschirm ausschalten".
- **Helligkeitssteuerung erlauben** (WRITE_SETTINGS) → echte Hardware-Helligkeit
  über den vollen Bereich (behebt „nur bis ~60 %").
- **Dateizugriff erlauben** (All-Files-Access) → damit die App Tondateien aus
  `/sdcard/FullScreendy/` lesen kann.
- **Akku-Optimierung deaktivieren** → damit Android die App im Hintergrund nicht killt.
- **Über anderen Apps anzeigen** → lässt den Dienst das Dashboard automatisch wieder
  in den Vordergrund holen, falls Android die App je wegen Speichermangel beendet
  (siehe *Dauerbetrieb*).

Der Geräteadmin-Button zeigt den aktuellen Status („Geräteadmin aktiv ✓“) und
öffnet zur Not die Sicherheits-Einstellungen, falls der direkte Dialog auf dem
Gerät nicht verfügbar ist.

## JS-Schnittstelle fürs Dashboard

Die Android-WebView hat **kein** `speechSynthesis` – Web-Sprachausgabe gibt es dort
schlicht nicht (auf Android 15 / WebView 151 nachgemessen: `webkitSpeechRecognition`
ist vorhanden, `speechSynthesis` nicht). Für Dashboards, die sprechen wollen, bietet
FullScreendy der Seite deshalb eine kleine Brücke an:

```js
fullscreendy.textToSpeech("Der Keller ist zu kalt");  // spricht über Android-TTS
fullscreendy.stopSpeech();                             // bricht die Ausgabe ab
fullscreendy.getVersion();                             // z. B. "0.4.6"
```

`textToSpeech` beachtet *Einstellungen → Verhalten → Text-to-Speech aktiv*.

**Mikrofon für Webseiten:** Eine `getUserMedia()`-Anfrage der Seite (Sprachsteuerung,
Wakeword-Engines) wird **standardmäßig abgelehnt** – die Android-Berechtigung allein
genügt nicht, die WebView braucht eine eigene Freigabe. Dafür *Einstellungen →
Verhalten → „Mikrofon-Zugriff für Dashboard-Seiten"* aktivieren und unter *System*
die Mikrofon-Berechtigung erteilen. Freigegeben wird nur Audio und nur, solange der
Schalter an ist. `getUserMedia` verlangt außerdem einen Secure Context, das Dashboard
muss also über **HTTPS** laufen.

Ein fertiger Patch für das FHEM-Wiki-Skript
[VoiceControl: Web-STT](https://wiki.fhem.de/wiki/FHEMWEB/VoiceControl:_Web-STT_%26_Hardware-Wakeword)
liegt unter `tools/voicecontrol-fullscreendy.patch`; `tools/webcheck.js` ist ein
Diagnose-Panel (`attr WEB JavaScripts www/pgm2/webcheck.js`), das anzeigt, welche
Sprach-APIs der anzeigende Browser tatsächlich mitbringt.

---

## Architektur (Kurzüberblick)

```
MainActivity ──── WebView (Vollbild-Dashboard) + Menü/Overlay/Helligkeit
      ▲  KioskBus (Befehle)
      │
KioskService (Foreground) ── MqttManager (Paho, Auto-Reconnect, Last-Will)
      ├── BatteryMonitor   → …/battery, …/charging, …
      ├── TtsManager       ← …/cmd/tts
      ├── MediaManager     ← …/cmd/mediaplay
      ├── SoundDetector    → weckt (AudioRecord, Lautstärke)
      └── MotionDetector   → …/motion, …/presence (CameraX, lichtkompensiert)
```

Einstellungen liegen in Jetpack DataStore; alle Topics werden daraus abgeleitet.

---

## Bekannte Grenzen / To-do
- `cmd/unlock` weckt und löst nur einen **unsicheren** Sperrbildschirm; eine
  gesicherte PIN/Muster kann aus Sicherheitsgründen nicht umgangen werden.
- **Bewegungs-/Ton-Weckung** funktionieren nur, solange der Bildschirm technisch an
  ist – daher das *Abdunkeln*-Overlay statt echtem Screen-Off. Lässt man das Display
  per Android-Timeout ganz ausgehen, stoppt die Kamera und Bewegung weckt nicht mehr;
  dann hilft nur `cmd/screen on` (Wakelock).
- Kamera/Mikrofon starten nur, wenn die App im Vordergrund war; nach reinem
  Boot ohne Öffnen bleiben sie aus (Android-Hintergrund-Restriktion).
- Autostart nach Boot ist je nach Hersteller unzuverlässig (Android blockiert
  Hintergrund-Activity-Starts) – ggf. **OEM-Autostart** für die App erlauben.
- TLS nutzt den System-Truststore; selbstsignierte Broker-Zertifikate müssten
  ergänzt werden.

---

## Dauerbetrieb & Diagnose

Für 24/7-Betrieb:
1. *Einstellungen → System → Berechtigungen → **Akku-Optimierung deaktivieren*** –
   sonst kann Android die App nach Stunden beenden.
2. Der Dienst nutzt den FGS-Typ **specialUse** (Android 14+) – der frühere Typ
   `dataSync` wurde von Android 15 nach ~6 h pro Tag hart beendet.

**Gegen `LOW_MEMORY`-Kills** (App läuft morgens, ist abends weg): mehrere Maßnahmen
halten den Speicherbedarf klein und holen die Anzeige zurück, falls Android die App
doch beendet:
- **`largeHeap`** für ein größeres Heap-Limit und **`onTrimMemory`** gibt den
  WebView-Cache proaktiv frei, wenn das System Speicherdruck meldet.
- **Periodischer Reload** (siehe *Anzeige*) setzt den über Tage wachsenden
  WebView-Speicher zurück.
- Die **Bewegungs-Kamera** analysiert mit kleiner Auflösung (320×240, kleinere Puffer).
- **UI-Auto-Wiederherstellung:** nach einem Kill startet `START_STICKY` nur den
  *Dienst* neu, nicht das Dashboard – deshalb holt der Dienst die Kiosk-Activity
  selbst zurück (benötigt die Berechtigung **„Über anderen Apps anzeigen"**). Ohne sie
  bliebe der Bildschirm schwarz („nicht aufweckbar"), obwohl MQTT kurz neu verbindet.

Wenn die App dennoch „irgendwann weg" ist:
- **MQTT-Reading `lastExit`** zeigt nach dem Neustart den Grund des letzten
  Prozess-Endes (z. B. `CRASH`, `ANR`, `LOW_MEMORY`, `USER_REQUESTED`, `SIGNALED`).
- **Log-Datei** mit Heartbeat (alle 30 min, inkl. Speicher: `heap=…/… frei=… lowMemory=…`),
  Start/Stopp, Cold-Restart und Crash-Stacktraces:
  `Android/data/de.kewl.fullscreendy/files/logs/app.log` (per „Dateizugriff
  erlauben" + Dateimanager oder `adb pull` erreichbar).

**Grüner Punkt in der Statusleiste:** Das ist Androids Datenschutz-Indikator –
er zeigt an, dass die Kamera (Bewegungserkennung) aktiv ist. Er lässt sich von
Apps nicht ausblenden; er verschwindet nur, wenn die Bewegungserkennung aus ist.

---

## Updates
Die App prüft beim Öffnen von *Über* die GitHub-Releases: stabile Builds suchen das
neueste stabile Release, Dev-Builds auch Pre-Releases. Gibt es eine neuere Version,
kannst du sie direkt aus der „Über"-Seite **herunterladen und installieren** – mit
Fortschrittsanzeige in Prozent (einmalig „Unbekannte Apps installieren" erlauben).
Die „Über"-Seite zeigt außerdem App-/Android-Version, IP-Adresse und Geräte-ID und
verlinkt **Stern auf GitHub geben**, den **Quellcode** und **Projekt unterstützen**.

---

## Lizenz

[MIT](LICENSE) © Glenn-Dandy. Nutzung, Änderung und Weitergabe (auch kommerziell)
frei, solange Copyright- und Lizenzhinweis erhalten bleiben. Ohne Gewährleistung.

Genutzte Bibliotheken behalten ihre eigenen Lizenzen (u. a. Eclipse Paho,
AndroidX/Jetpack Compose – Apache-2.0).
