package guru.urchin.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import guru.urchin.data.DeviceObservation
import guru.urchin.data.DeviceRepository
import guru.urchin.util.DebugLog
import org.json.JSONArray
import org.json.JSONObject

class ObservationRecorder(
  private val repository: DeviceRepository,
  private val scope: CoroutineScope
) {
  fun record(input: ObservationInput) {
    val key = DeviceKey.from(input)
    ScanDiagnosticsStore.update {
      it.copy(deviceKeys = it.deviceKeys + key)
    }
    val metadata = buildMetadataJson(input)
    DebugLog.log(
      "Observation ${input.source} sensor=${input.tpmsSensorId ?: "unknown"} protocol=${input.tpmsModel ?: "n/a"} " +
        "rssi=${input.rssi}"
    )
    val observation = DeviceObservation(
      deviceKey = key,
      name = input.name,
      address = input.tpmsSensorId ?: input.address,
      rssi = input.rssi,
      timestamp = input.timestamp,
      metadataJson = metadata
    )

    scope.launch {
      repository.recordObservation(observation)
    }
  }

  private fun buildMetadataJson(input: ObservationInput): String {
    val json = JSONObject()
    json.put("source", input.source)
    json.putIfNotNull("transport", input.transport)
    json.put("name", input.name)
    json.put("address", input.address)
    json.putIfNotNull("nameSource", input.nameSource)
    json.putIfNotNull("vendorName", input.vendorName)
    json.putIfNotNull("vendorSource", input.vendorSource)
    json.putIfNotNull("vendorConfidence", input.vendorConfidence)
    json.putIfNotNull("classificationCategory", input.classificationCategory)
    json.putIfNotNull("classificationLabel", input.classificationLabel)
    json.putIfNotNull("classificationConfidence", input.classificationConfidence)
    json.put("rssi", input.rssi)
    json.put("timestamp", input.timestamp)

    val classificationEvidence = JSONArray()
    input.classificationEvidence.forEach { classificationEvidence.put(it) }
    json.put("classificationEvidence", classificationEvidence)

    json.putIfNotNull("tpmsModel", input.tpmsModel)
    json.putIfNotNull("tpmsSensorId", input.tpmsSensorId)
    json.putIfNotNull("tpmsPressureKpa", input.tpmsPressureKpa)
    json.putIfNotNull("tpmsTemperatureC", input.tpmsTemperatureC)
    json.putIfNotNull("tpmsBatteryOk", input.tpmsBatteryOk)
    json.putIfNotNull("tpmsFrequencyMhz", input.tpmsFrequencyMhz)
    json.putIfNotNull("tpmsSnr", input.tpmsSnr)
    json.putIfNotNull("rawJson", input.rawJson)

    return json.toString(2)
  }

  private fun JSONObject.putIfNotNull(key: String, value: Any?) {
    if (value != null) {
      put(key, value)
    }
  }
}
