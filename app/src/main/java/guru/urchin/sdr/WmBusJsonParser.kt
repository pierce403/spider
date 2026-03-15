package guru.urchin.sdr

import org.json.JSONObject

/**
 * Parses JSON lines from the wmbus_json_bridge TCP stream.
 *
 * Expected format:
 * ```
 * {"type":"wmbus","manufacturer":"KAM","serial":"12345678","version":1,"device_type":"water","rssi":-65,"freq":868.95}
 * ```
 */
object WmBusJsonParser {
  fun parse(jsonLine: String): SdrReading.WmBus? {
    return try {
      val json = JSONObject(jsonLine)
      if (json.optString("type") != "wmbus") return null

      val manufacturer = json.optStringOrNull("manufacturer") ?: return null
      val serialNumber = json.optStringOrNull("serial") ?: return null

      SdrReading.WmBus(
        manufacturer = manufacturer,
        serialNumber = serialNumber,
        meterVersion = json.optIntOrNull("version"),
        meterType = json.optStringOrNull("device_type"),
        rssi = json.optDoubleOrNull("rssi"),
        snr = json.optDoubleOrNull("snr"),
        frequencyMhz = json.optDoubleOrNull("freq"),
        rawJson = jsonLine
      )
    } catch (_: Exception) {
      null
    }
  }
}
