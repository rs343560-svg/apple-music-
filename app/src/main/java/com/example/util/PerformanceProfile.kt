package com.example.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build

enum class PerformanceMode(val displayName: String, val description: String) {
    HIGH("High Quality", "Full liquid glass, animated ambient glow & video canvas"),
    BALANCED("Balanced", "Smooth 60/120 FPS with optimized glass highlights"),
    BATTERY_SAVER("Performance / Power Save", "Minimal GPU overhead for low-end devices")
}

object PerformanceManager {
    private var detectedMode: PerformanceMode? = null

    fun detectDefaultMode(context: Context): PerformanceMode {
        detectedMode?.let { return it }

        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val isLowRam = actManager?.isLowRamDevice ?: false

        val memoryInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memoryInfo)
        val totalMemGb = memoryInfo.totalMem / (1024.0 * 1024.0 * 1024.0)

        val mode = when {
            isLowRam || totalMemGb < 3.0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> {
                PerformanceMode.BATTERY_SAVER
            }
            totalMemGb < 5.5 -> {
                PerformanceMode.BALANCED
            }
            else -> {
                PerformanceMode.HIGH
            }
        }
        detectedMode = mode
        return mode
    }
}
