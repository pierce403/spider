package guru.urchin.ui

import guru.urchin.data.DeviceEntity
import org.json.JSONObject
import org.json.JSONTokener

data class DeviceJsonExport(
  val fileName: String,
  val json: String
)

object DeviceDetailExport {
  fun build(device: DeviceEntity): DeviceJsonExport {
    return DeviceJsonExport(
      fileName = defaultFileName(device.lastAddress),
      json = buildJson(device)
    )
  }

  fun buildJson(device: DeviceEntity): String {
    val root = JSONObject()
    root.put("device", deviceToJson(device))
    root.put("lastMetadata", parseJsonValue(device.lastMetadataJson) ?: JSONObject.NULL)
    return root.toString(2)
  }

  fun defaultFileName(sensorId: String?): String {
    val suffix = sensorId
      ?.trim()
      ?.uppercase()
      ?.replace(Regex("[^A-Z0-9:_-]"), "_")
      ?.takeUnless { it.isBlank() }
      ?: "UNKNOWN-SENSOR"
    return "urchin-$suffix.txt"
  }

  private fun deviceToJson(device: DeviceEntity): JSONObject {
    return JSONObject().apply {
      put("deviceKey", device.deviceKey)
      put("displayName", device.displayName ?: JSONObject.NULL)
      put("lastAddress", device.lastAddress ?: JSONObject.NULL)
      put("firstSeen", device.firstSeen)
      put("lastSeen", device.lastSeen)
      put("lastSightingAt", device.lastSightingAt)
      put("sightingsCount", device.sightingsCount)
      put("observationCount", device.observationCount)
      put("lastRssi", device.lastRssi)
      put("rssiMin", device.rssiMin)
      put("rssiMax", device.rssiMax)
      put("rssiAvg", device.rssiAvg)
      put("lastMetadataJson", device.lastMetadataJson ?: JSONObject.NULL)
      put("starred", device.starred)
      put("userCustomName", device.userCustomName ?: JSONObject.NULL)
    }
  }

  private fun parseJsonValue(rawJson: String?): Any? {
    if (rawJson.isNullOrBlank()) {
      return null
    }
    return runCatching { JSONTokener(rawJson).nextValue() }.getOrNull() ?: rawJson
  }
}
