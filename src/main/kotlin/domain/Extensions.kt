package com.mathias8dev.simplefiledownloader.domain

import kotlin.math.roundToInt


fun Number.asHumanReadableSize(): String {
    val length = this.toLong()
    val kbLimit = 1024F
    val moLimit = 1024F * 1024F
    val goLimit = 1024F * 1024F * 1024F

    fun formatSize(size: Float, unit: String): String {
        return if (size.toInt().times(100) == size.times(100).roundToInt()) {
            // No decimal part, remove dot
            "${size.toInt()}$unit"
        } else {
            // Format with 2 decimal places
            String.format("%.2f", size) + unit
        }
    }

    return when {
        length > goLimit -> formatSize(length / goLimit, "Go")
        length > moLimit -> formatSize(length / moLimit, "Mo")
        length > kbLimit -> formatSize(length / kbLimit, "Kb")
        else -> "$length Octets"
    }
}