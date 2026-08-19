/*
 * webcheck.js – Diagnose-Panel für FHEMWEB
 *
 * Zeigt an, welche Sprach-/Mikrofon-APIs der anzeigende Browser wirklich kann.
 * Gedacht zum Vergleich WebView (FullScreendy/Fully) gegen Chrome auf demselben
 * Tablet – Grundlage für github.com/Glenn-Dandy/fullscreendy Issue #1.
 *
 * Einbinden:  attr <FHEMWEB-Device> JavaScripts www/pgm2/webcheck.js
 */
(function () {
  "use strict";

  // FHEMWEB bindet JavaScripts im <head> ein – da gibt es document.body noch
  // nicht. Deshalb erst nach dem Aufbau des DOM starten.
  function start() {
  var PANEL_ID = "fsdy-webcheck";
  if (document.getElementById(PANEL_ID)) return;

  // ---- kleine Helfer -------------------------------------------------------
  function el(tag, css, text) {
    var e = document.createElement(tag);
    if (css) e.setAttribute("style", css);
    if (text != null) e.textContent = text;
    return e;
  }
  function typeOf(obj, name) {
    try { return typeof obj[name]; } catch (e) { return "n/a"; }
  }

  var lines = [];   // für "Ergebnis kopieren"
  var rows = [];    // Zeilen, die nachträglich aktualisiert werden

  // ---- Panel ---------------------------------------------------------------
  var box = el("div", [
    "position:fixed;right:12px;top:12px;z-index:99999;width:min(94vw,460px)",
    "max-height:92vh;overflow:auto;background:#12141a;color:#e8eaed",
    "font:14px/1.45 system-ui,sans-serif;border:1px solid #3a3f4b",
    "border-radius:14px;box-shadow:0 8px 30px rgba(0,0,0,.5);padding:14px"
  ].join(";"));
  box.id = PANEL_ID;

  var head = el("div", "display:flex;align-items:center;gap:8px;margin-bottom:10px");
  head.appendChild(el("div", "font-weight:700;font-size:16px;flex:1", "WebCheck – Sprache & Mikrofon"));
  var close = el("button", "background:#2a2f3a;color:#e8eaed;border:0;border-radius:8px;padding:6px 10px;font-size:15px", "✕");
  close.onclick = function () { box.remove(); };
  head.appendChild(close);
  box.appendChild(head);

  var list = el("div");
  box.appendChild(list);

  /** Eine Ergebniszeile; gibt eine set(ok, text)-Funktion zurück. */
  function row(label, ok, value) {
    var r = el("div", "display:flex;gap:8px;align-items:baseline;padding:4px 0;border-bottom:1px solid #23262e");
    var mark = el("span", "width:1.2em;flex:0 0 auto");
    var name = el("span", "flex:0 0 46%;color:#9aa0aa");
    name.textContent = label;
    var val = el("span", "flex:1;word-break:break-word");
    r.appendChild(mark); r.appendChild(name); r.appendChild(val);
    list.appendChild(r);

    function set(ok, value) {
      mark.textContent = ok === null ? "•" : (ok ? "✓" : "✗");
      mark.style.color = ok === null ? "#9aa0aa" : (ok ? "#4caf50" : "#ef5350");
      val.textContent = String(value);
      var i = rows.indexOf(set);
      lines[i] = (ok === null ? "-" : (ok ? "OK " : "FEHLT")) + "\t" + label + ": " + value;
    }
    rows.push(set);
    lines.push("");
    set(ok, value);
    return set;
  }

  // ---- 1. Kontext ----------------------------------------------------------
  list.appendChild(el("div", "margin:8px 0 2px;color:#7aa7ff;font-weight:600", "Kontext"));
  var secure = window.isSecureContext === true;
  row("Protokoll", secure, location.protocol + "//" + location.host);
  row("isSecureContext", secure, secure ? "ja (getUserMedia erlaubt)" : "NEIN – ohne HTTPS gibt es kein Mikrofon");
  var ua = navigator.userAgent;
  row("WebView?", null, /\bwv\b|Version\/[\d.]+ Chrome/.test(ua) ? "sieht nach WebView aus" : "eher echter Browser");
  row("UserAgent", null, ua);

  // ---- 2. APIs -------------------------------------------------------------
  list.appendChild(el("div", "margin:12px 0 2px;color:#7aa7ff;font-weight:600", "APIs"));
  var hasSR = typeOf(window, "SpeechRecognition") !== "undefined" ||
              typeOf(window, "webkitSpeechRecognition") !== "undefined";
  row("SpeechRecognition (STT)", hasSR,
      "SpeechRecognition=" + typeOf(window, "SpeechRecognition") +
      ", webkit=" + typeOf(window, "webkitSpeechRecognition"));

  var hasTTS = typeOf(window, "speechSynthesis") !== "undefined";
  var voicesRow = row("speechSynthesis (TTS)", hasTTS, hasTTS ? "vorhanden, Stimmen werden geprüft…" : "nicht vorhanden");

  var md = navigator.mediaDevices;
  row("navigator.mediaDevices", !!md, md ? "vorhanden" : "fehlt (meist wegen fehlendem HTTPS)");
  row("getUserMedia", !!(md && md.getUserMedia), (md && md.getUserMedia) ? "vorhanden" : "fehlt");
  row("JS-Brücke fully.*", typeOf(window, "fully") !== "undefined", typeOf(window, "fully"));
  row("JS-Brücke fullscreendy.*", typeOf(window, "fullscreendy") !== "undefined", typeOf(window, "fullscreendy"));

  // Stimmen kommen oft verzögert – bis zu 3 s nachsehen.
  if (hasTTS) {
    var tries = 0;
    var poll = setInterval(function () {
      var v = [];
      try { v = window.speechSynthesis.getVoices() || []; } catch (e) {}
      if (v.length || ++tries > 12) {
        clearInterval(poll);
        voicesRow(v.length > 0, v.length + " Stimme(n)" + (v.length ? ": " + v[0].name + " …" : " – TTS läuft so nicht"));
      }
    }, 250);
  }

  // ---- 3. Aktive Tests -----------------------------------------------------
  list.appendChild(el("div", "margin:12px 0 2px;color:#7aa7ff;font-weight:600", "Tests (antippen)"));
  var micRow = row("Mikrofon-Anfrage", null, "noch nicht getestet");
  var ttsRow = row("Sprachausgabe", null, "noch nicht getestet");
  var sttRow = row("Spracherkennung", null, "noch nicht getestet");

  var bar = el("div", "display:flex;flex-wrap:wrap;gap:8px;margin-top:12px");
  function button(text, fn) {
    var b = el("button", [
      "flex:1 1 46%;background:#2a2f3a;color:#e8eaed;border:0;border-radius:10px",
      "padding:12px 10px;font-size:15px"
    ].join(";"), text);
    b.onclick = fn;
    bar.appendChild(b);
    return b;
  }

  button("🎤 Mikrofon anfragen", function () {
    micRow(null, "frage an…");
    if (!md || !md.getUserMedia) { micRow(false, "getUserMedia gibt es hier nicht"); return; }
    md.getUserMedia({ audio: true }).then(function (stream) {
      var t = stream.getAudioTracks()[0];
      micRow(true, "erlaubt – Track: " + (t ? t.label || "(ohne Namen)" : "?"));
      stream.getTracks().forEach(function (x) { x.stop(); });
    }).catch(function (err) {
      // NotAllowedError in einer WebView = die App hat onPermissionRequest nicht beantwortet.
      micRow(false, err.name + ": " + (err.message || ""));
    });
  });

  button("🔊 Sprachausgabe", function () {
    if (!hasTTS) { ttsRow(false, "speechSynthesis fehlt"); return; }
    ttsRow(null, "spreche…");
    try {
      var u = new SpeechSynthesisUtterance("FullScreendy Test eins zwei drei");
      u.lang = "de-DE";
      u.onstart = function () { ttsRow(true, "onstart – es wird gesprochen"); };
      u.onend = function () { ttsRow(true, "fertig gesprochen"); };
      u.onerror = function (e) { ttsRow(false, "onerror: " + (e.error || "?")); };
      window.speechSynthesis.speak(u);
      setTimeout(function () {
        var i = rows.indexOf(ttsRow);
        if (lines[i].indexOf("spreche") > -1) ttsRow(false, "keine Reaktion – kein TTS-Backend");
      }, 4000);
    } catch (e) { ttsRow(false, e.name + ": " + e.message); }
  });

  button("🗣️ Spracherkennung", function () {
    var SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) { sttRow(false, "SpeechRecognition fehlt – in einer WebView nicht vorhanden"); return; }
    sttRow(null, "starte…");
    try {
      var r = new SR();
      r.lang = "de-DE";
      r.onstart = function () { sttRow(true, "gestartet – jetzt sprechen"); };
      r.onresult = function (e) { sttRow(true, "erkannt: " + e.results[0][0].transcript); };
      r.onerror = function (e) { sttRow(false, "onerror: " + (e.error || "?")); };
      r.start();
    } catch (e) { sttRow(false, e.name + ": " + e.message); }
  });

  button("📋 Ergebnis kopieren", function () {
    var text = "WebCheck " + new Date().toISOString() + "\n" + lines.join("\n");
    var done = function () { alert("Ergebnis kopiert"); };
    if (navigator.clipboard && navigator.clipboard.writeText) {
      navigator.clipboard.writeText(text).then(done, function () { window.prompt("Kopieren:", text); });
    } else {
      window.prompt("Kopieren:", text);
    }
  });

  box.appendChild(bar);
  (document.body || document.documentElement).appendChild(box);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", start);
  } else {
    start();
  }
})();
