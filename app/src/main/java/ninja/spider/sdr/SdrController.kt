package ninja.spider.sdr

import android.content.BroadcastReceiver
import android.content.Context
import ninja.spider.scan.ObservationRecorder
import ninja.spider.scan.ScanDiagnosticsStore
import ninja.spider.scan.ScanDiagnosticsSnapshot
import ninja.spider.util.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SdrController(
  private val context: Context,
  private val scope: CoroutineScope,
  private val observationRecorder: ObservationRecorder
) {
  private val _sdrState = MutableStateFlow<SdrState>(SdrState.Idle)
  val sdrState: StateFlow<SdrState> = _sdrState.asStateFlow()

  private val networkBridge = Rtl433NetworkBridge()
  private val rtl433Process by lazy { Rtl433Process(context) }
  private var usbReceiver: BroadcastReceiver? = null

  fun startSdr() {
    if (!SdrPreferences.isEnabled(context)) {
      _sdrState.value = SdrState.Idle
      return
    }
    if (_sdrState.value is SdrState.Scanning) return

    ScanDiagnosticsStore.reset(
      ScanDiagnosticsSnapshot(
        startTimeMs = System.currentTimeMillis(),
        sourceLabel = SdrPreferences.source(context).value,
        networkHost = SdrPreferences.networkHost(context),
        networkPort = SdrPreferences.networkPort(context),
        frequencyHz = SdrPreferences.frequencyHz(context),
        gain = SdrPreferences.gain(context)
      )
    )

    when (SdrPreferences.source(context)) {
      SdrPreferences.SdrSource.NETWORK -> startNetworkBridge()
      SdrPreferences.SdrSource.USB -> startUsbSdr()
    }
  }

  fun stopSdr() {
    networkBridge.disconnect()
    rtl433Process.stop()
    _sdrState.value = SdrState.Idle
    DebugLog.log("SDR scanning stopped")
  }

  fun registerUsbDetection() {
    if (usbReceiver != null) return
    usbReceiver = SdrUsbDetector.registerReceiver(
      context = context,
      onAttached = {
        if (SdrPreferences.isEnabled(context) &&
          SdrPreferences.source(context) == SdrPreferences.SdrSource.USB &&
          _sdrState.value !is SdrState.Scanning
        ) {
          startUsbSdr()
        }
      },
      onDetached = {
        if (_sdrState.value is SdrState.Scanning &&
          SdrPreferences.source(context) == SdrPreferences.SdrSource.USB
        ) {
          rtl433Process.stop()
          _sdrState.value = SdrState.UsbNotConnected
          ScanDiagnosticsStore.update { it.copy(lastError = "USB SDR disconnected.") }
        }
      },
      onPermissionResult = { granted ->
        if (granted) {
          startUsbSdr()
        } else {
          _sdrState.value = SdrState.UsbPermissionDenied
          ScanDiagnosticsStore.update { it.copy(lastError = "USB permission denied.") }
        }
      }
    )
  }

  fun unregisterUsbDetection() {
    usbReceiver?.let {
      SdrUsbDetector.unregisterReceiver(context, it)
      usbReceiver = null
    }
  }

  private fun startNetworkBridge() {
    val host = SdrPreferences.networkHost(context)
    val port = SdrPreferences.networkPort(context)
    DebugLog.log("SDR starting network bridge to $host:$port")
    _sdrState.value = SdrState.Scanning
    ScanDiagnosticsStore.update {
      it.copy(sourceLabel = SdrPreferences.SdrSource.NETWORK.value)
    }

    networkBridge.connect(
      scope = scope,
      host = host,
      port = port,
      onReading = ::handleTpmsReading,
      onError = { message ->
        _sdrState.value = SdrState.Error(message)
        ScanDiagnosticsStore.update { snapshot -> snapshot.copy(lastError = message) }
      }
    )
  }

  private fun startUsbSdr() {
    val device = SdrUsbDetector.findSdrDevice(context)
    if (device == null) {
      _sdrState.value = SdrState.UsbNotConnected
      DebugLog.log("No SDR USB device found")
      ScanDiagnosticsStore.update { it.copy(lastError = "No RTL-SDR or HackRF detected over USB.") }
      return
    }

    if (!SdrUsbDetector.hasPermission(context, device.usbDevice)) {
      SdrUsbDetector.requestPermission(context, device.usbDevice)
      return
    }

    DebugLog.log("SDR starting on-device rtl_433: ${SdrUsbDetector.deviceDescription(device.usbDevice)}")
    _sdrState.value = SdrState.Scanning
    ScanDiagnosticsStore.update {
      it.copy(
        sourceLabel = SdrPreferences.SdrSource.USB.value,
        hardwareLabel = device.profile.label,
        lastError = null
      )
    }

    rtl433Process.start(
      scope = scope,
      hardwareProfile = device.profile,
      frequencyHz = SdrPreferences.frequencyHz(context),
      gain = SdrPreferences.gain(context),
      onReading = ::handleTpmsReading,
      onError = { message ->
        _sdrState.value = SdrState.Error(message)
        ScanDiagnosticsStore.update { snapshot -> snapshot.copy(lastError = message) }
      }
    )
  }

  internal fun handleTpmsReading(reading: TpmsReading) {
    val input = TpmsObservationBuilder.build(reading)
    ScanDiagnosticsStore.update {
      it.copy(
        sdrCallbackCount = it.sdrCallbackCount + 1,
        rawCallbackCount = it.rawCallbackCount + 1,
        lastReadingAt = System.currentTimeMillis(),
        lastError = null
      )
    }
    DebugLog.log(
      "SDR observation model=${reading.model} sensor=${reading.sensorId} " +
        "pressure=${reading.pressureKpa} temp=${reading.temperatureC}"
    )
    observationRecorder.record(input)
  }
}
