package guru.urchin.sdr

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import java.io.File

data class NativeToolStatus(
  val label: String,
  val candidates: List<File>
) {
  init {
    require(candidates.isNotEmpty()) { "Native tool candidates must not be empty." }
  }

  val resolvedFile: File
    get() = candidates.firstOrNull(File::exists) ?: candidates.first()

  val exists: Boolean
    get() = resolvedFile.exists()

  fun diagnosticsLine(): String {
    val expected = candidates.joinToString(" or ") { it.absolutePath }
    return if (exists) {
      "$label: present (${resolvedFile.absolutePath})"
    } else {
      "$label: missing (expected $expected)"
    }
  }

  fun missingMessage(): String {
    val expected = candidates.joinToString(" or ") { it.absolutePath }
    return "$label is not bundled in this APK. Expected $expected."
  }
}

object SdrRuntimeInspector {
  fun rtl433Status(context: Context): NativeToolStatus =
    nativeToolStatus(context, "rtl_433", "librtl_433.so", "rtl_433")

  fun dump1090Status(context: Context): NativeToolStatus =
    nativeToolStatus(context, "dump1090", "libdump1090.so", "dump1090")

  fun p25ScannerStatus(context: Context): NativeToolStatus =
    nativeToolStatus(context, "p25_scanner", "libp25_scanner.so", "p25_scanner")

  fun nativeToolLines(context: Context): List<String> {
    val nativeDir = context.applicationInfo.nativeLibraryDir
    return buildList {
      add("Native lib dir: $nativeDir")
      add(rtl433Status(context).diagnosticsLine())
      add(dump1090Status(context).diagnosticsLine())
      add(p25ScannerStatus(context).diagnosticsLine())
    }
  }

  fun usbInventoryLines(context: Context): List<String> {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as? UsbManager
      ?: return listOf("USB manager unavailable.")
    val devices = SdrUsbDetector.findAllUsbDevices(context)
    if (devices.isEmpty()) {
      return listOf("No USB devices currently attached.")
    }

    return devices.map { device ->
      val label = SdrUsbDetector.supportLabel(device)
      val prefix = if (label != null) "$label:" else "Unsupported USB device:"
      val permissionLabel = if (usbManager.hasPermission(device)) "granted" else "not granted"
      "$prefix ${usbDeviceSummary(device)} (permission=$permissionLabel)"
    }
  }

  fun firstUnsupportedUsbSummary(context: Context): String? {
    val unsupported = SdrUsbDetector.findAllUsbDevices(context)
      .firstOrNull { !SdrUsbDetector.isSupported(it) }
      ?: return null
    return usbDeviceSummary(unsupported)
  }

  fun missingRequiredToolLabels(context: Context, enabledProtocols: Set<String>): String? {
    val missing = requiredToolStatuses(context, enabledProtocols)
      .filterNot(NativeToolStatus::exists)
      .map(NativeToolStatus::label)
    if (missing.isEmpty()) return null
    return missing.joinToString(", ")
  }

  private fun requiredToolStatuses(context: Context, enabledProtocols: Set<String>): List<NativeToolStatus> {
    return buildList {
      if ("tpms" in enabledProtocols || "pocsag" in enabledProtocols) {
        add(rtl433Status(context))
      }
      if ("adsb" in enabledProtocols) {
        add(dump1090Status(context))
      }
      if ("p25" in enabledProtocols) {
        add(p25ScannerStatus(context))
      }
    }
  }

  private fun nativeToolStatus(
    context: Context,
    label: String,
    preferredName: String,
    fallbackName: String
  ): NativeToolStatus {
    val nativeDir = File(context.applicationInfo.nativeLibraryDir)
    return NativeToolStatus(
      label = label,
      candidates = listOf(
        File(nativeDir, preferredName),
        File(nativeDir, fallbackName)
      )
    )
  }

  private fun usbDeviceSummary(device: UsbDevice): String {
    val product = device.productName ?: device.manufacturerName ?: "Unknown device"
    val path = device.deviceName?.takeIf(String::isNotBlank)
    return buildString {
      append(product)
      append(" (vendor=0x${"%04X".format(device.vendorId)}, product=0x${"%04X".format(device.productId)}")
      path?.let {
        append(", path=")
        append(it)
      }
      append(")")
    }
  }
}
