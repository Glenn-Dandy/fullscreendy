# tools

Hilfsmittel rund um Dashboards in der WebView – nichts davon wird mitgebaut.

- **`webcheck.js`** – Diagnose-Panel für FHEMWEB. Einbinden mit
  `attr <FHEMWEB-Device> JavaScripts www/pgm2/webcheck.js`. Zeigt an, welche
  Sprach-/Mikrofon-APIs der anzeigende Browser wirklich hat (`SpeechRecognition`,
  `speechSynthesis`, `getUserMedia`, Secure Context, JS-Brücken) und testet
  Mikrofon-Anfrage, Sprachausgabe und Spracherkennung per Knopfdruck. Gedacht für
  den Vergleich WebView ↔ Chrome auf demselben Gerät.

- **`voicecontrol-fullscreendy.patch`** – Patch für `contrib/voicecontrol.js` aus dem
  FHEM-SVN (Wiki: *FHEMWEB/VoiceControl: Web-STT & Hardware-Wakeword*). Das Skript
  spricht bisher nur über `fully.textToSpeech`; der Patch nimmt zusätzlich
  `fullscreendy.textToSpeech` an, damit die Sprachausgabe auch in FullScreendy läuft
  (die WebView hat kein `speechSynthesis`).

  ```bash
  patch www/pgm2/voicecontrol.js < voicecontrol-fullscreendy.patch
  ```
