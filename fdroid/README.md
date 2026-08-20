# FullScreendy auf F-Droid veröffentlichen

FullScreendy ist FOSS (MIT) und nutzt ausschließlich freie Abhängigkeiten:
AndroidX/Compose und CameraX (Apache 2.0), DataStore, Eclipse Paho MQTT (EPL/EDL).
Kein Google Play Services, kein Firebase, keine Tracker.

## Ein Build für alles

Die App lädt und installiert selbst keine APKs – die *Über*-Seite prüft nur auf eine
neuere Version und verlinkt die Release-Seite. Damit fällt `REQUEST_INSTALL_PACKAGES`
weg, und es gibt genau einen Build für GitHub wie F-Droid:

```bash
./gradlew assembleRelease   # ohne keystore.properties: unsigniert, genau wie beim Buildserver
```

Reproducible Builds sind vorbereitet: `dependenciesInfo` und `vcsInfo` sind abgeschaltet
(die beiden bekannten Störenfriede), zwei aufeinanderfolgende `clean`-Builds liefern
byte-identische APKs. `Binaries:` im Rezept zeigt auf das APK des GitHub-Releases,
`AllowedAPKSigningKeys:` auf unser Zertifikat.

## Vor dem Einreichen

1. **Ein Tag, der alles enthaelt.** F-Droid baut genau den angegebenen Commit und liest
   auch die Fastlane-Texte daraus – nicht aus `HEAD`. `v0.4.6` und `v0.4.7` scheiden aus: dort fehlen
   Wrapper, Metadaten und den abgeschalteten `dependenciesInfo`. Die erste
   F-Droid-Version ist deshalb **0.4.9 (versionCode 22)**.
2. **Screenshots** nach `fastlane/metadata/android/*/images/phoneScreenshots/` legen,
   siehe [`../fastlane/README.md`](../fastlane/README.md).
3. **Vollen Commit-Hash eintragen**, keinen Tag-Namen:
   ```bash
   git rev-list -n1 v0.4.9
   ```
   Der Wert ersetzt `FULL_COMMIT_HASH_OF_TAG_v0.4.9` im Rezept. Tags koennen verschoben
   werden, Hashes nicht – F-Droid besteht darauf.
4. **Gegenprobe**, dass der getaggte Commit wirklich baut:
   ```bash
   git checkout v0.4.9 && ./gradlew assembleRelease   # ohne keystore.properties
   ```

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
