package guru.urchin.ui

import android.content.ClipData
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import guru.urchin.UrchinApp
import guru.urchin.databinding.ActivityDiagnosticsBinding
import guru.urchin.scan.ScanDiagnosticsStore
import guru.urchin.sdr.SdrRuntimeInspector
import guru.urchin.util.DebugLog
import guru.urchin.util.WindowInsetsHelper

class DiagnosticsActivity : AppCompatActivity() {
  private lateinit var binding: ActivityDiagnosticsBinding
  private val app by lazy { application as UrchinApp }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityDiagnosticsBinding.inflate(layoutInflater)
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    WindowInsetsHelper.applyToolbarInsets(binding.toolbar)
    WindowInsetsHelper.applyBottomInsets(binding.diagnosticsScroll)
    WindowInsetsHelper.requestApplyInsets(binding.root)

    binding.copyDebugReportButton.setOnClickListener {
      val clip = ClipData.newPlainText("Urchin diagnostics", binding.diagnosticsText.text)
      getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(clip)
      Toast.makeText(this, guru.urchin.R.string.copied_to_clipboard_simple, Toast.LENGTH_SHORT).show()
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        combine(
          app.sdrController.sdrState,
          ScanDiagnosticsStore.snapshot,
          app.repository.observeDevices(),
          DebugLog.entries
        ) { sdrState, diagnostics, devices, logs ->
          DiagnosticsReportBuilder.build(
            sdrState = sdrState,
            diagnostics = diagnostics,
            deviceCount = devices.size,
            logEntries = logs,
            usbInventoryLines = SdrRuntimeInspector.usbInventoryLines(this@DiagnosticsActivity),
            nativeToolLines = SdrRuntimeInspector.nativeToolLines(this@DiagnosticsActivity)
          )
        }.collect { report ->
          binding.diagnosticsText.text = report
        }
      }
    }
  }

  override fun onSupportNavigateUp(): Boolean {
    finish()
    return true
  }
}
