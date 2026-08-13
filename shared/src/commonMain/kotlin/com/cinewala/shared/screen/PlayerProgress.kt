package com.cinewala.shared.screen

/**
 * Progress data emitted by the VideoEasy player via postMessage events.
 */
data class PlayerProgress(
    val progress: Double = 0.0,      // Watch progress percentage (0.0 - 1.0)
    val duration: Long = 0,          // Total duration in seconds
    val currentTime: Long = 0        // Current position in seconds
)