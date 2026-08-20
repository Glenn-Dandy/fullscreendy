# FullScreendy auf F-Droid veröffentlichen

FullScreendy ist FOSS (MIT) und nutzt ausschließlich freie Abhängigkeiten:
AndroidX/Compose und CameraX (Apache 2.0), DataStore, Eclipse Paho MQTT (EPL/EDL).
Kein Google Play Services, kein Firebase, keine Tracker.

## Das `fdroid`-Flavor

F-Droid baut und signiert selbst und nimmt keine Apps, die sich über einen eigenen
APK-Download aktualisieren. Deshalb gibt es zwei Varianten (Dimension `distribution`):

| Flavor | In-App-Update | `REQUEST_INSTALL_PACKAGES` |
|---|---|---|
| `github` | Prüfen, Herunterladen, Installieren | ja (`src/github/AndroidManifest.xml`) |
| `fdroid` | nur Prüfen + Link zur Release-Seite | **nein** |

Gesteuert über `BuildConfig.UPDATER`; die Berechtigung liegt ausschließlich im
Manifest des `github`-Flavors. Bauen:

```bash
./gradlew assembleFdroidRelease    # das baut F-Droid (unsigniert, ohne keystore.properties)
./gradlew assembleGithubRelease    # das APK für die GitHub-Releases
./gradlew assembleGithubDev -PdevNum=1
```

Ohne `keystore.properties` wird gar keine Signier-Konfiguration angelegt – der Build
läuft durch und liefert ein unsigniertes APK, genau wie auf dem Buildserver.

## Schritte zur Aufnahme

1. **Store-Texte** liegen als Fastlane-Metadaten im Repo unter
   `fastlane/metadata/android/{en-US,de-DE}/` (Titel, Kurz-/Langbeschreibung,
   `changelogs/<versionCode>.txt`, `images/phoneScreenshots/`). F-Droid liest sie
   automatisch aus dem getaggten Commit.
2. **Rezept einreichen:** [`de.kewl.fullscreendy.yml`](de.kewl.fullscreendy.yml) als
   Merge-Request bei <https://gitlab.com/fdroid/fdroiddata> unter
   `metadata/de.kewl.fullscreendy.yml`. Vorher optional ein
   "Request For Packaging"-Issue anlegen.
3. `UpdateCheckMode: Tags ^v[0-9]+\.[0-9]+\.[0-9]+$` baut jeden neuen stabilen Tag
   automatisch und ignoriert die `-dev.N`-Pre-Releases.

## Hinweise

- **Signatur:** F-Droid signiert mit eigenem Schlüssel. Der Wechsel zwischen einem
  GitHub-APK und dem F-Droid-APK erfordert deshalb ein Deinstallieren.
- **Reproducible Builds** sind vorbereitet (`vcsInfo { include = false }`), aber noch
  nicht gegen einen F-Droid-Build verifiziert. Erst wenn das nachgewiesen ist, sollten
  `Binaries:` und `AllowedAPKSigningKeys:` ins Rezept – sonst schlägt die Verifikation
  fehl. Zertifikat der GitHub-Releases:
  `a3c699c2ed75997a5dfdd7694529bfc18bc672e99ed8883a3ff51c439a5a82d7`
- **Berechtigungen erklären:** Kamera (Bewegungserkennung), Mikrofon (Wecken bei Ton
  bzw. Sprachsteuerung), Geräteadmin (Bildschirm sperren) und "Über anderen Apps
  anzeigen" (Auto-Wiederherstellung) stehen prominent in der F-Droid-App-Ansicht. Die
  Langbeschreibung begründet jede einzelne – sonst wirkt die Liste abschreckend.
