package ninja.spider.scan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ScanDiagnosticsSnapshot(
  val startTimeMs: Long? = null,
  val sourceLabel: String? = null,
  val hardwareLabel: String? = null,
  val networkHost: String? = null,
  val networkPort: Int? = null,
  val frequencyHz: Int? = null,
  val gain: Int? = null,
  val sdrCallbackCount: Int = 0,
  val rawCallbackCount: Int = 0,
  val deviceKeys: Set<String> = emptySet(),
  val lastReadingAt: Long? = null,
  val lastError: String? = null
) {
  val uniqueDeviceCount: Int
    get() = deviceKeys.size
}

object ScanDiagnosticsStore {
  private val _snapshot = MutableStateFlow(ScanDiagnosticsSnapshot())
  val snapshot: StateFlow<ScanDiagnosticsSnapshot> = _snapshot.asStateFlow()

  fun reset(snapshot: ScanDiagnosticsSnapshot = ScanDiagnosticsSnapshot()) {
    _snapshot.value = snapshot
  }

  fun update(transform: (ScanDiagnosticsSnapshot) -> ScanDiagnosticsSnapshot) {
    _snapshot.update(transform)
  }
}
