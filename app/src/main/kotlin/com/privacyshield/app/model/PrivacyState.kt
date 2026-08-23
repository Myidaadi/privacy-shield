package com.privacyshield.app.model

/**
 * All possible states of the privacy protection system.
 */
enum class PrivacyState {
    /** Protection is disabled. */
    DISABLED,

    /** 1 face (owner) detected — all clear. */
    NORMAL,

    /** 0 faces for several frames — owner away, countdown started. */
    OWNER_AWAY,

    /** 2+ faces detected — privacy overlay active. */
    PEEKER_ALERT,

    /** Owner away for [AppSettings.autoLockSeconds] — full lock. */
    LOCKED;

    val isOverlayActive: Boolean
        get() = this == PEEKER_ALERT || this == LOCKED

    val isProtectionEnabled: Boolean
        get() = this != DISABLED

    val displayLabel: String
        get() = when (this) {
            DISABLED     -> "Protection Off"
            NORMAL       -> "Protected"
            OWNER_AWAY   -> "Owner Away"
            PEEKER_ALERT -> "Intruder Detected!"
            LOCKED       -> "Screen Locked"
        }

    val statusMessage: String
        get() = when (this) {
            DISABLED     -> "Enable Privacy Shield to protect your screen."
            NORMAL       -> "AI monitoring active · On-device · No data sent"
            OWNER_AWAY   -> "You've stepped away. Locking screen soon..."
            PEEKER_ALERT -> "Someone else is looking at your screen!"
            LOCKED       -> "Authenticate to resume viewing."
        }
}
