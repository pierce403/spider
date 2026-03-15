package guru.urchin.sdr

import org.json.JSONObject

/**
 * Parses JSON lines from the zwave_json_bridge TCP stream.
 *
 * Expected format:
 * ```
 * {"type":"zwave","home_id":"AABBCCDD","node_id":5,"frame_type":"singlecast","rssi":-72,"freq":908.42}
 * ```
 */
object ZwaveJsonParser {
  fun parse(jsonLine: String): SdrReading.Zwave? {
    return try {
      val json = JSONObject(jsonLine)
      if (json.optString("type") != "zwave") return null

      val homeId = json.optStringOrNull("home_id") ?: return null
      val nodeId = json.optIntOrNull("node_id") ?: return null

      SdrReading.Zwave(
        homeId = homeId,
        nodeId = nodeId,
        frameType = json.optStringOrNull("frame_type"),
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
