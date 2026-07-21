# FullScreendy

![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

**English** · [Deutsch](README.de.md)

A fullscreen kiosk browser (Chromium/WebView) for Android wall tablets that shows
an FHEM dashboard and integrates the tablet as an FHEM device over **MQTT**.

- 🔋 **Battery & charging** – level, charging state, temperature
- 🔊 **Text-to-speech** – FHEM sends text, the tablet speaks it
- 🔈 **Sounds** – play stored sound files in the background
- 🖥️ **Screen control** – on/off (overlay) and brightness
- 🚶 **Presence / motion** – camera motion detection wakes the display (no image leaves the device)
- 🌐 **Remote control** – switch URL, reload, clear cache – all via MQTT
- 🗂️ **Menu** – swipe in from the left: status, actions, settings (PIN), about, exit
- 🌍 **Bilingual** – English (default) / German

Everything runs in a persistent foreground service with auto-reconnect to the MQTT broker.

---

## Build

Requires **Android Studio** (Ladybug or newer) with Android SDK 35.

1. In Android Studio: *File → Open* → select this folder (`fullscreendy`).
2. Wait for the Gradle sync (downloads AGP 8.7, Kotlin 2.1, dependencies).
3. *Run* on a device/emulator (Android 10+), or build an APK:
   *Build → Build Bundle(s)/APK(s) → Build APK(s)*.

```bash
gradle wrapper            # once, creates ./gradlew
./gradlew assembleDebug    # APK: app/build/outputs/apk/debug/app-debug.apk
```

Prebuilt signed APKs are on the [Releases page](https://github.com/Glenn-Dandy/fullscreendy/releases).

---

## First setup (on the tablet)

1. Launch the app. As no URL is configured yet, **Settings** open right away.
2. Enter:
   - *Connection*: **dashboard URL** (e.g. `http://192.168.1.10:8083/fhem/floorplan/Home`),
     **MQTT host/port** (+ optional user/password/TLS), **base topic** (default
     `fhem/tablet`) and **device ID** (default `tablet1`)
   - *System*: language and admin PIN (default `0000`)
3. Tap **Save** at the top, then go back → the dashboard is shown.

Allow the camera and notification permissions on first launch.

**Menu / back to settings:** swipe in from the **left edge** → menu with MQTT status,
*Reload*, *Clear cache*, *Screen off*, *Settings* (PIN), *About* (with GitHub link),
*Exit app*.

### Autostart (optional)
The app is deliberately **not a launcher/home replacement** – the tablet stays
usable normally. For autostart after boot: enable *Settings → System → “Start on
boot”*. On some devices (Xiaomi, Huawei, …) additionally allow the **OEM autostart**
for FullScreendy in the Android settings. For full lockdown, consider Android
**screen pinning**.

---

## MQTT topics

Base: `<base-topic>/<device-id>`, e.g. `fhem/tablet/tablet1`.

**The tablet reports (retained):**

| Topic | Values |
|---|---|
| `…/status` | `online` / `offline` (last will) |
| `…/battery` | `0`–`100` |
| `…/charging` | `on` / `off` |
| `…/plug` | `ac` / `usb` / `wireless` / `none` |
| `…/batteryTemp` | e.g. `24.5` |
| `…/motion` | `on` / `off` |
| `…/presence` | `present` / `absent` |
| `…/screen` | `on` / `off` |
| `…/brightness` | `0`–`100` or `auto` |
| `…/volume` | `0`–`100` (media volume) |
| `…/url` | currently loaded URL |
| `…/ip` | IPv4 address |
| `…/appVersion` | app version, e.g. `0.2.0` |
| `…/androidVersion` | e.g. `13 (SDK 33)` |

**The tablet accepts commands (`…/cmd/…`):**

| Topic | Payload | Effect |
|---|---|---|
| `cmd/tts` | any text | speaks the text (aliases: `say`, `speak`) |
| `cmd/mediaplay` | `folder/sound.mp3` | plays a stored sound file (aliases: `media`, `play`) |
| `cmd/mediastop` | (any) | stops playback |
| `cmd/url` | URL | loads another page and reports it as the `url` reading |
| `cmd/reload` | (any) | reloads the page |
| `cmd/clearcache` | (any) | clears the browser cache |
| `cmd/screen` | `on` / `off` | physically wakes the display (on) or black overlay (off) |
| `cmd/screensaver` | `on` / `off` | like `screen`, inverted |
| `cmd/brightness` | `0`–`100` or `auto` | brightness (full range with “Allow brightness control”) |
| `cmd/volume` | `0`–`100` | media volume (TTS/sounds) |
| `cmd/lock` | (any) | locks the screen (needs device admin) |
| `cmd/unlock` | (any) | wakes & dismisses the (insecure) lock screen |
| `cmd/vibrate` | duration in ms (empty = 200) | vibration feedback, max 5000 ms |

**Sounds:** copy files into the public folder **`/sdcard/FullScreendy/`** (accessible
via file manager/USB). Grant **Settings → System → “Allow file access”** once. Then
play via `cmd/mediaplay ding.mp3`. `http…` URLs are streamed, paths with `/` are
treated as absolute files.

---

## FHEM configuration

Requires an MQTT server in FHEM (`MQTT2_SERVER`) **or** an `MQTT2_CLIENT` connected
to your broker (e.g. mosquitto).

### Create the device
As soon as the tablet publishes for the first time, FHEM suggests an `MQTT2_DEVICE`
via `autocreate`. Alternatively, manually:

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

Set commands must be added manually (replace `fullscreendy` with your device name):

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

`$EVTPART1` is enough for values without spaces (number, URL, file path).

### Text-to-speech (full sentence with spaces)
`setList` splits arguments word by word, so for free text use a Perl expression that
takes everything after the first space of `$EVENT`:

```
attr <device> setList say:textField {my @a=split(" ",$EVENT);; shift(@a);; return "fhem/tablet/tablet1/cmd/tts ".join(" ",@a)}
```

Or publish directly (`<IODev>` = your MQTT2_SERVER/CLIENT):

```
set MQTT2_Broker publish fhem/tablet/tablet1/cmd/tts Good morning, it is 8 degrees
```

### Examples
```
# Light on when motion is detected
define n_hall_light notify tablet1:motion:.on set HallLight on

# Play a doorbell sound (file in the sound folder)
set MQTT2_Broker publish fhem/tablet/tablet1/cmd/mediaplay doorbell.mp3
```

---

## Device settings & permissions

Under *Settings → Behavior*:
- **Motion sensitivity** (slider) for the camera detection.
- **Wake on sound (microphone)** with **sound sensitivity** – loud ambient noise
  wakes the display (loudness only, no recording).
- **Test indicators**: with motion/sound detection enabled, green dots show each
  detection live (the device vibrates briefly). Save first, then wave / make noise.
- **Robust against light changes**: motion detection subtracts the global brightness
  change and requires several frames – uniform flicker (TV, lighting) does not
  trigger, local movement does. If it still reacts, lower the **motion sensitivity**.

Under *Settings → Display*:
- **Keep screen always on** is off by default.
- **Screen dimming** (default 60 s): dims to black via an overlay after inactivity
  instead of the Android timeout turning the display off. The screen stays
  technically on so **camera/microphone keep running** and motion/sound wake
  **reliably** – and there is **no wallpaper/lock screen flash** on wake. Touch or
  **any** motion clears the dim immediately and restarts the timer. `0 s` = never.
  (When wake-on-motion/sound is enabled, the screen stays on automatically.)
- **Turn screen off** (default 0 = never): after *even longer* inactivity, fully turn
  off the display (via device admin). Afterwards only touch/power/`cmd/screen on`
  wakes it – motion no longer does (the camera is off then). On a real screen-off the
  system may briefly show its wake animation on resume; for **completely flicker-free**
  wake use **dimming only** (turn off = 0). The app window then stays in the foreground
  and the dashboard is there instantly.
- **Periodic reload** (default 6 h, 0 = off): reloads the dashboard on a timer so the
  WebView’s memory (DOM/JS/image cache) does not keep growing over days – the main
  defense against Android killing the app for `LOW_MEMORY` in long-term operation.

Under *Settings → System → Permissions* (grant once; granted permissions show a “✓”):
- **Allow camera** → for motion detection (front camera).
- **Allow microphone** → for sound wake.
- **Enable device admin** → needed for `cmd/lock` and “turn screen off”.
- **Allow brightness control** (WRITE_SETTINGS) → real hardware brightness over the
  full range (fixes “only up to ~60 %”).
- **Allow file access** (all-files access) → so the app can read sound files from
  `/sdcard/FullScreendy/`.
- **Disable battery optimization** → so Android does not kill the app in the background.
- **Display over other apps** → lets the service bring the dashboard back to the front
  automatically if Android ever kills the app for memory (see *24/7 operation*).

The device-admin button shows the current status (“Device admin active ✓”) and falls
back to the security settings if the direct dialog is unavailable on the device.

## Architecture (overview)

```
MainActivity ──── WebView (fullscreen dashboard) + menu/overlay/brightness
      ▲  KioskBus (commands)
      │
KioskService (foreground) ── MqttManager (Paho, auto-reconnect, last will)
      ├── BatteryMonitor   → …/battery, …/charging, …
      ├── TtsManager       ← …/cmd/tts
      ├── MediaManager     ← …/cmd/mediaplay
      ├── SoundDetector    → wakes (AudioRecord, loudness)
      └── MotionDetector   → …/motion, …/presence (CameraX, light-compensated)
```

Settings live in Jetpack DataStore; all topics are derived from them.

---

## Known limitations / to-do
- `cmd/unlock` only wakes and dismisses an **insecure** lock screen; a secured
  PIN/pattern cannot be bypassed for security reasons.
- **Motion/sound wake** only work while the screen is technically on – hence the
  *dim* overlay instead of a real screen-off. If you let the display go off via the
  Android timeout, the camera stops and motion no longer wakes; then only
  `cmd/screen on` (wakelock) helps.
- Camera/microphone only start once the app has been in the foreground; after a plain
  boot without opening it, they stay off (Android background restriction).
- Autostart after boot is unreliable depending on the manufacturer (Android blocks
  background activity starts) – allow the **OEM autostart** for the app if needed.
- TLS uses the system trust store; self-signed broker certificates would need to be
  added.

---

## 24/7 operation & diagnostics

For continuous operation:
1. *Settings → System → Permissions → **Disable battery optimization*** – otherwise
   Android may kill the app after some hours.
2. The service uses the **specialUse** FGS type (Android 14+) – the previous
   `dataSync` type was hard-stopped by Android 15 after ~6 h per day.

**Against `LOW_MEMORY` kills** (the app runs fine in the morning but is gone by
evening): several measures keep the memory footprint low and recover the UI if Android
still reaps the app:
- **`largeHeap`** for a bigger heap ceiling, and **`onTrimMemory`** frees the WebView
  cache proactively when the system signals memory pressure.
- **Periodic reload** (see *Display*) resets the WebView memory that grows over days.
- The **motion camera** analyses at a small 320×240 resolution (smaller frame buffers).
- **UI auto-recovery:** after a kill, `START_STICKY` restarts only the *service*, not
  the dashboard – so the service relaunches the kiosk Activity itself (needs the
  **“Display over other apps”** permission). Without it the screen would stay black
  (“not wakeable”) even though MQTT briefly reconnects.

If the app still “disappears” at some point:
- The **MQTT reading `lastExit`** shows the reason of the last process death after
  restart (e.g. `CRASH`, `ANR`, `LOW_MEMORY`, `USER_REQUESTED`, `SIGNALED`).
- A **log file** with heartbeat (every 30 min, incl. memory: `heap=…/… free=… lowMemory=…`),
  start/stop, cold-restart and crash stack traces:
  `Android/data/de.kewl.fullscreendy/files/logs/app.log` (reachable via “Allow file
  access” + file manager, or `adb pull`).

**Green dot in the status bar:** that is Android’s privacy indicator – it shows the
camera (motion detection) is active. Apps cannot hide it; it only disappears when
motion detection is off.

---

## Updates
The app checks GitHub Releases when you open *About*: stable builds look for the latest
stable release, dev builds also consider pre-releases. If a newer version exists you can
**download and install** it right from the About screen (allow “install unknown apps” once).

---

## License

[MIT](LICENSE) © Glenn-Dandy. Free to use, modify and distribute (including
commercially) as long as the copyright and license notice are retained. No warranty.

Bundled libraries keep their own licenses (e.g. Eclipse Paho, AndroidX/Jetpack
Compose – Apache-2.0).
