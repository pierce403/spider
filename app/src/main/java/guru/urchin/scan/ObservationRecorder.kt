package guru.urchin.scan

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import guru.urchin.alerts.AlertObservation
import guru.urchin.alerts.DeviceAlertMatcher
import guru.urchin.alerts.DeviceAlertNotifier
import guru.urchin.data.AlertRuleEntity
import guru.urchin.data.AlertRuleRepository
import guru.urchin.data.DeviceObservation
import guru.urchin.data.DeviceRepository
import guru.urchin.util.DebugLog
import org.json.JSONArray
import org.json.JSONObject

class ObservationRecorder(
  private val repository: DeviceRepository,
  private val scope: CoroutineScope,
  private val alertRuleRepository: AlertRuleRepository? = null,
  private val alertNotifier: DeviceAlertNotifier? = null
) {
  private var alertRules: List<AlertRuleEntity> = emptyList()
  private val firedAlertKeys = mutableSetOf<String>()

  init {
    alertRuleRepository?.observeEnabledRules()
      ?.onEach { alertRules = it }
      ?.launchIn(scope)
  }

  fun resetAlertDedup() {
    firedAlertKeys.clear()
  }

  fun record(input: ObservationInput) {
    val key = DeviceKey.from(input)
    ScanDiagnosticsStore.update {
      it.copy(deviceKeys = it.deviceKeys + key)
    }
    val metadata = buildMetadataJson(input)
    DebugLog.log(
      "Observation ${input.source} protocol=${input.protocolType ?: "unknown"} rssi=${input.rssi}"
    )
    val observation = DeviceObservation(
      deviceKey = key,
      name = input.name,
      address = input.tpmsSensorId ?: input.pocsagCapCode ?: input.adsbIcao ?: input.p25UnitId ?: input.loraDevAddr ?: input.meshNodeId ?: input.wmbusSerialNumber ?: input.zwaveHomeId ?: input.sidewalkSmsn ?: input.address,
      rssi = input.rssi,
      timestamp = input.timestamp,
      metadataJson = metadata,
      protocolType = input.protocolType
    )

    evaluateAlerts(input, key)

    scope.launch {
      repository.recordObservation(observation)
    }
  }

  private fun evaluateAlerts(input: ObservationInput, deviceKey: String) {
    if (alertNotifier == null || alertRules.isEmpty()) return

    val sensorId = input.tpmsSensorId ?: input.adsbIcao ?: input.pocsagCapCode ?: input.p25UnitId ?: input.loraDevAddr ?: input.meshNodeId ?: input.wmbusSerialNumber ?: input.zwaveHomeId ?: input.sidewalkSmsn
    val alertObs = AlertObservation(
      deviceKey = deviceKey,
      displayName = input.name ?: input.tpmsModel ?: input.adsbCallsign,
      sensorId = sensorId,
      protocolType = input.protocolType,
      source = input.source
    )

    val matches = DeviceAlertMatcher.findMatches(alertRules, alertObs)
    for (match in matches) {
      val alertKey = "${match.rule.id}:$deviceKey"
      if (firedAlertKeys.add(alertKey)) {
        alertNotifier.notifyMatch(match, alertObs)
      }
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

    json.putIfNotNull("protocolType", input.protocolType)

    // TPMS fields
    json.putIfNotNull("tpmsModel", input.tpmsModel)
    json.putIfNotNull("tpmsSensorId", input.tpmsSensorId)
    json.putIfNotNull("tpmsPressureKpa", input.tpmsPressureKpa)
    json.putIfNotNull("tpmsTemperatureC", input.tpmsTemperatureC)
    json.putIfNotNull("tpmsBatteryOk", input.tpmsBatteryOk)
    json.putIfNotNull("tpmsFrequencyMhz", input.tpmsFrequencyMhz)
    json.putIfNotNull("tpmsSnr", input.tpmsSnr)

    // POCSAG fields
    json.putIfNotNull("pocsagCapCode", input.pocsagCapCode)
    json.putIfNotNull("pocsagFunctionCode", input.pocsagFunctionCode)
    json.putIfNotNull("pocsagMessage", input.pocsagMessage)

    // ADS-B fields
    json.putIfNotNull("adsbIcao", input.adsbIcao)
    json.putIfNotNull("adsbCallsign", input.adsbCallsign)
    json.putIfNotNull("adsbAltitude", input.adsbAltitude)
    json.putIfNotNull("adsbSpeed", input.adsbSpeed)
    json.putIfNotNull("adsbHeading", input.adsbHeading)
    json.putIfNotNull("adsbLat", input.adsbLat)
    json.putIfNotNull("adsbLon", input.adsbLon)
    json.putIfNotNull("adsbSquawk", input.adsbSquawk)

    // P25 fields
    json.putIfNotNull("p25UnitId", input.p25UnitId)
    json.putIfNotNull("p25Nac", input.p25Nac)
    json.putIfNotNull("p25Wacn", input.p25Wacn)
    json.putIfNotNull("p25SystemId", input.p25SystemId)
    json.putIfNotNull("p25TalkGroupId", input.p25TalkGroupId)

    // LoRaWAN fields
    json.putIfNotNull("loraDevAddr", input.loraDevAddr)
    json.putIfNotNull("loraSpreadingFactor", input.loraSpreadingFactor)
    json.putIfNotNull("loraCodingRate", input.loraCodingRate)
    json.putIfNotNull("loraPayloadSize", input.loraPayloadSize)
    json.putIfNotNull("loraCrcOk", input.loraCrcOk)

    // Meshtastic fields
    json.putIfNotNull("meshNodeId", input.meshNodeId)
    json.putIfNotNull("meshDestId", input.meshDestId)
    json.putIfNotNull("meshPacketId", input.meshPacketId)
    json.putIfNotNull("meshHopLimit", input.meshHopLimit)
    json.putIfNotNull("meshHopStart", input.meshHopStart)
    json.putIfNotNull("meshChannelHash", input.meshChannelHash)

    // Wireless M-Bus fields
    json.putIfNotNull("wmbusManufacturer", input.wmbusManufacturer)
    json.putIfNotNull("wmbusSerialNumber", input.wmbusSerialNumber)
    json.putIfNotNull("wmbusMeterVersion", input.wmbusMeterVersion)
    json.putIfNotNull("wmbusMeterType", input.wmbusMeterType)

    // Z-Wave fields
    json.putIfNotNull("zwaveHomeId", input.zwaveHomeId)
    json.putIfNotNull("zwaveNodeId", input.zwaveNodeId)
    json.putIfNotNull("zwaveFrameType", input.zwaveFrameType)

    // Amazon Sidewalk fields
    json.putIfNotNull("sidewalkSmsn", input.sidewalkSmsn)
    json.putIfNotNull("sidewalkFrameType", input.sidewalkFrameType)

    json.putIfNotNull("rawJson", input.rawJson)

    return json.toString(2)
  }

  private fun JSONObject.putIfNotNull(key: String, value: Any?) {
    if (value != null) {
      put(key, value)
    }
  }
}
