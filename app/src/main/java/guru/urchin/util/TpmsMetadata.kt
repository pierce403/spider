package guru.urchin.util

import guru.urchin.data.DeviceEntity
import org.json.JSONObject

data class TpmsMetadata(
  val source: String? = null,
  val vendorName: String? = null,
  val vendorSource: String? = null,
  val vendorConfidence: String? = null,
  val classificationLabel: String? = null,
  val tpmsModel: String? = null,
  val tpmsSensorId: String? = null,
  val tpmsPressureKpa: Double? = null,
  val tpmsTemperatureC: Double? = null,
  val tpmsBatteryOk: Boolean? = null,
  val tpmsFrequencyMhz: Double? = null,
  val tpmsSnr: Double? = null,
  val rssi: Int? = null,
  val rawJson: String? = null
)

data class SensorPresentation(
  val title: String,
  val listSummary: String,
  val detailLines: List<String>,
  val searchText: String
)

object TpmsMetadataParser {
  fun parse(metadataJson: String?): TpmsMetadata {
    if (metadataJson.isNullOrBlank()) {
      return TpmsMetadata()
    }

    return runCatching {
      val json = JSONObject(metadataJson)
      TpmsMetadata(
        source = json.optStringOrNull("source"),
        vendorName = json.optStringOrNull("vendorName"),
        vendorSource = json.optStringOrNull("vendorSource"),
        vendorConfidence = json.optStringOrNull("vendorConfidence"),
        classificationLabel = json.optStringOrNull("classificationLabel"),
        tpmsModel = json.optStringOrNull("tpmsModel"),
        tpmsSensorId = json.optStringOrNull("tpmsSensorId"),
        tpmsPressureKpa = json.optDoubleOrNull("tpmsPressureKpa"),
        tpmsTemperatureC = json.optDoubleOrNull("tpmsTemperatureC"),
        tpmsBatteryOk = json.optBooleanOrNull("tpmsBatteryOk"),
        tpmsFrequencyMhz = json.optDoubleOrNull("tpmsFrequencyMhz"),
        tpmsSnr = json.optDoubleOrNull("tpmsSnr"),
        rssi = json.optIntOrNull("rssi"),
        rawJson = json.optStringOrNull("rawJson")
      )
    }.getOrDefault(TpmsMetadata())
  }

  private fun JSONObject.optStringOrNull(key: String): String? {
    if (!has(key) || isNull(key)) return null
    return optString(key).trim().takeIf { it.isNotEmpty() && it != "null" }
  }

  private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    return optDouble(key).takeIf { !it.isNaN() && !it.isInfinite() }
  }

  private fun JSONObject.optIntOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return optInt(key)
  }

  private fun JSONObject.optBooleanOrNull(key: String): Boolean? {
    if (!has(key) || isNull(key)) return null
    return optBoolean(key)
  }
}

object SensorPresentationBuilder {
  fun build(device: DeviceEntity): SensorPresentation {
    val metadata = TpmsMetadataParser.parse(device.lastMetadataJson)
    val preferredTitle = device.userCustomName?.takeIf(String::isNotBlank)
      ?: device.displayName?.takeIf(String::isNotBlank)
      ?: metadata.tpmsSensorId?.let { "TPMS $it" }
      ?: metadata.tpmsModel?.let { "TPMS $it" }
      ?: "Unknown TPMS sensor"

    val listSummaryParts = buildList {
      metadata.tpmsPressureKpa?.let { add(Formatters.formatPressure(it)) }
      metadata.tpmsTemperatureC?.let { add(Formatters.formatTemperature(it)) }
      metadata.tpmsBatteryOk?.let { add(if (it) "Battery OK" else "Battery low") }
      metadata.tpmsFrequencyMhz?.let { add(String.format("%.2f MHz", it)) }
    }

    val detailLines = buildList {
      metadata.tpmsSensorId?.let { add("Sensor ID: $it") }
      metadata.tpmsModel?.let { add("Protocol: $it") }
      metadata.vendorName?.let { vendor ->
        val source = metadata.vendorSource?.let { " ($it)" }.orEmpty()
        add("Vendor: $vendor$source")
      }
      metadata.classificationLabel?.let { add("Classification: $it") }
      metadata.tpmsPressureKpa?.let { add(Formatters.formatPressure(it)) }
      metadata.tpmsTemperatureC?.let { add(Formatters.formatTemperature(it)) }
      metadata.tpmsBatteryOk?.let { add("Battery: ${if (it) "OK" else "Low"}") }
      metadata.tpmsFrequencyMhz?.let { add(String.format("Frequency: %.2f MHz", it)) }
      metadata.tpmsSnr?.let { add(String.format("SNR: %.1f dB", it)) }
      metadata.rssi?.let { add(Formatters.formatRssi(it)) }
      metadata.source?.let { add("Source: $it") }
    }

    val searchText = buildString {
      append(preferredTitle)
      append('\n')
      listOfNotNull(
        metadata.vendorName,
        metadata.vendorSource,
        metadata.classificationLabel,
        metadata.tpmsSensorId,
        metadata.tpmsModel,
        metadata.rawJson
      ).forEach {
        append(it)
        append('\n')
      }
    }

    return SensorPresentation(
      title = preferredTitle,
      listSummary = listSummaryParts.joinToString(" • "),
      detailLines = detailLines,
      searchText = searchText
    )
  }
}
