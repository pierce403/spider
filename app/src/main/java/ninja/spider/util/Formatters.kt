package ninja.spider.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Formatters {
  fun formatTimestamp(timestamp: Long): String {
    return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
      .withLocale(Locale.getDefault())
      .format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))
  }

  fun formatRssi(rssi: Int): String {
    return if (rssi <= -100) {
      "Signal unavailable"
    } else {
      "RSSI: $rssi dBm"
    }
  }

  fun formatPressure(kpa: Double): String {
    return String.format(Locale.US, "%.1f kPa / %.1f PSI", kpa, kpa * 0.145038)
  }

  fun formatTemperature(celsius: Double): String {
    return String.format(Locale.US, "%.1f C / %.1f F", celsius, celsius * 9.0 / 5.0 + 32.0)
  }

  fun formatGain(gain: Int?): String {
    return gain?.let { "$it dB" } ?: "Auto"
  }

  fun formatSightingsCount(count: Int): String {
    return if (count == 1) {
      "1 sighting"
    } else {
      "$count sightings"
    }
  }
}
