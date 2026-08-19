package de.kewl.fullscreendy.kiosk

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Befehle, die der Dienst an die Kiosk-Activity (WebView / Bildschirm) sendet. */
sealed interface KioskCommand {
    data class LoadUrl(val url: String) : KioskCommand
    data object Reload : KioskCommand

    /** Auf das Dashboard mit diesem Index (0-basiert) umschalten. */
    data class SelectDashboard(val index: Int) : KioskCommand

    /** Bildschirm "an" (Overlay weg, Helligkeit zurück) oder "aus" (schwarzes Overlay). */
    data class Screen(val on: Boolean) : KioskCommand

    /** Helligkeit 0f..1f, oder -1f für Systemautomatik. */
    data class Brightness(val level: Float) : KioskCommand

    /** Browser-Cache leeren. */
    data object ClearCache : KioskCommand

    /** Bildschirm wecken und (unsicheren) Sperrbildschirm lösen. */
    data object Unlock : KioskCommand
}

/**
 * Einfacher prozessweiter Event-Bus. Der [de.kewl.fullscreendy.service.KioskService]
 * sendet Befehle, die [de.kewl.fullscreendy.MainActivity] konsumiert.
 */
object KioskBus {
    private val _commands = MutableSharedFlow<KioskCommand>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val commands = _commands.asSharedFlow()

    fun send(command: KioskCommand) {
        _commands.tryEmit(command)
    }
}
