package guru.urchin.ui

import guru.urchin.scan.ScanDiagnosticsSnapshot
import guru.urchin.sdr.SdrState
import guru.urchin.util.Formatters

object DiagnosticsReportBuilder {
  fun build(
    sdrState: SdrState,
    diagnostics: ScanDiagnosticsSnapshot,
    deviceCount: Int,
    logEntries: List<String>
  ): String {
    return buildString {
      appendLine("Urchin diagnostics")
      appendLine("Generated: ${Formatters.formatTimestamp(System.currentTimeMillis())}")
      appendLine("State: ${describeState(sdrState)}")
      appendLine("Source: ${diagnostics.sourceLabel ?: "unknown"}")
      appendLine("Hardware: ${diagnostics.hardwareLabel ?: "none detected"}")
      appendLine("Host: ${diagnostics.networkHost ?: "-"}")
      appendLine("Port: ${diagnostics.networkPort ?: 0}")
      appendLine("Frequency: ${diagnostics.frequencyHz ?: 0} Hz")
      appendLine("Gain: ${diagnostics.gain?.toString() ?: "auto"}")
      appendLine("Observed sensors: $deviceCount")
      appendLine("Unique keys: ${diagnostics.uniqueDeviceCount}")
      appendLine("rtl_433 callbacks: ${diagnostics.sdrCallbackCount}")
      appendLine("Raw observations: ${diagnostics.rawCallbackCount}")
      appendLine("Last reading: ${diagnostics.lastReadingAt?.let(Formatters::formatTimestamp) ?: "none"}")
      appendLine("Last error: ${diagnostics.lastError ?: "none"}")
      appendLine()
      appendLine("Recent log")
      logEntries.takeLast(40).forEach(::appendLine)
    }.trimEnd()
  }

  private fun describeState(state: SdrState): String {
    return when (state) {
      SdrState.Idle -> "idle"
      SdrState.Scanning -> "scanning"
      SdrState.UsbNotConnected -> "usb device not connected"
      SdrState.UsbPermissionDenied -> "usb permission denied"
      is SdrState.Error -> "error: ${state.message}"
    }
  }
}
