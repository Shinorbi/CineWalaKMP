package com.cinewala.shared.util

import kotlin.math.round

/**
 * Multiplatform-compatible formatter that rounds a Double to one decimal place
 * and returns it as a String (e.g., 7.5, 8.0).
 */
fun formatRating(value: Double): String {
    val rounded = round(value * 10) / 10
    val whole = rounded.toLong()
    val fraction = ((rounded - whole) * 10).toInt()
    return if (fraction == 0) {
        "$whole.0"
    } else {
        "$whole.$fraction"
    }
}

/**
 * Formats a duration in seconds to a human-readable string (e.g., 125 -> "2:05", 3661 -> "1:01:01").
 */
fun formatDuration(totalSeconds: Long): String {
    if (totalSeconds <= 0) return "0:00"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}:${pad2(minutes)}:${pad2(seconds)}"
    } else {
        "${minutes}:${pad2(seconds)}"
    }
}

private fun pad2(value: Long): String {
    return if (value < 10) "0$value" else "$value"
}
