package com.cinewala.shared.util

import kotlinx.datetime.Clock

/**
 * Returns the current epoch time in milliseconds.
 * Uses kotlinx-datetime for multiplatform compatibility.
 */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()