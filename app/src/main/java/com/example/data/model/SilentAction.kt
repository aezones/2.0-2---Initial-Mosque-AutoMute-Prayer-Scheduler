package com.example.data.model

enum class SilentAction(val displayName: String, val description: String) {
    SILENT("Mute (Silent)", "Turns off ringtone and media sounds completely"),
    VIBRATE("Vibrate", "Sets phone to vibrate-only mode"),
    DND("Do Not Disturb (DND)", "Activates system DND to block all notifications except priority alarms")
}
