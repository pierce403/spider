package ninja.spider.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ninja.spider.SpiderApp
import ninja.spider.sdr.SdrPreferences
import ninja.spider.sdr.SdrState
import ninja.spider.util.Formatters
import ninja.spider.util.SensorPresentationBuilder
import ninja.spider.util.TpmsMetadataParser

class MainViewModel(app: Application) : AndroidViewModel(app) {
  private val spiderApp = app as SpiderApp
  private val repository = spiderApp.repository
  private val sdrController = spiderApp.sdrController

  private val filterQuery = MutableStateFlow("")
  private val sortMode = MutableStateFlow(SortMode.RECENT)
  private val liveOnly = MutableStateFlow(false)
  private val starredOnly = MutableStateFlow(false)
  private val batteryLowOnly = MutableStateFlow(false)

  private val liveTicker = flow {
    while (true) {
      emit(System.currentTimeMillis())
      delay(LiveDeviceWindow.TICK_MS)
    }
  }
    .onStart { emit(System.currentTimeMillis()) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), System.currentTimeMillis())

  private val observedDevices = repository.observeDevices()
    .conflate()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val sdrState: StateFlow<SdrState> = sdrController.sdrState

  private val devicesFlow = observedDevices.map { devices ->
    devices.map { device ->
      val metadata = TpmsMetadataParser.parse(device.lastMetadataJson)
      val presentation = SensorPresentationBuilder.build(device)
      val metaParts = buildList {
        if (presentation.listSummary.isNotBlank()) {
          add(presentation.listSummary)
        }
        add("Seen ${Formatters.formatTimestamp(device.lastSeen)}")
        add(Formatters.formatSightingsCount(device.sightingsCount))
      }
      DeviceListItem(
        deviceKey = device.deviceKey,
        displayName = device.displayName,
        displayTitle = presentation.title,
        metaLine = metaParts.joinToString(" • "),
        searchText = presentation.searchText,
        sortTimestamp = device.lastSeen,
        lastSeen = device.lastSeen,
        lastRssi = device.lastRssi,
        sightingsCount = device.sightingsCount,
        starred = device.starred,
        sensorId = metadata.tpmsSensorId,
        vendorName = metadata.vendorName,
        batteryLow = metadata.tpmsBatteryOk == false
      )
    }
  }

  val devices: StateFlow<List<DeviceListItem>> = devicesFlow
    .combine(filterQuery) { list, query ->
      if (query.isBlank()) {
        list
      } else {
        list.filter { item ->
          item.searchText.contains(query, ignoreCase = true) ||
            item.sensorId?.contains(query, ignoreCase = true) == true ||
            item.vendorName?.contains(query, ignoreCase = true) == true
        }
      }
    }
    .combine(starredOnly) { list, starred ->
      if (starred) list.filter { it.starred } else list
    }
    .combine(batteryLowOnly) { list, batteryLow ->
      if (batteryLow) list.filter { it.batteryLow } else list
    }
    .combine(liveOnly) { list, live ->
      list to live
    }
    .combine(liveTicker) { (list, live), now ->
      if (live) list.filter { LiveDeviceWindow.isLive(it.lastSeen, now) } else list
    }
    .combine(sortMode) { list, sort ->
      when (sort) {
        SortMode.RECENT -> list.sortedByDescending { it.sortTimestamp }
        SortMode.STRONGEST -> list.sortedByDescending { it.lastRssi }
        SortMode.NAME -> list.sortedBy { it.displayTitle.lowercase() }
      }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val liveDeviceCount: StateFlow<Int> = observedDevices
    .combine(liveTicker) { devices, now ->
      devices.count { LiveDeviceWindow.isLive(it.lastSeen, now) }
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  fun updateQuery(query: String) {
    filterQuery.value = query
  }

  fun updateSortMode(mode: SortMode) {
    sortMode.value = mode
  }

  fun setLiveOnly(live: Boolean) {
    liveOnly.value = live
  }

  fun setStarredOnly(starred: Boolean) {
    starredOnly.value = starred
  }

  fun setBatteryLowOnly(enabled: Boolean) {
    batteryLowOnly.value = enabled
  }

  fun setStarred(deviceKey: String, starred: Boolean) {
    viewModelScope.launch {
      repository.setStarred(deviceKey, starred)
    }
  }

  fun startScan() {
    SdrPreferences.setEnabled(getApplication(), true)
    sdrController.startSdr()
  }

  fun stopScan() {
    SdrPreferences.setEnabled(getApplication(), false)
    sdrController.stopSdr()
  }

  fun refreshScan() {
    if (SdrPreferences.isEnabled(getApplication())) {
      sdrController.startSdr()
    }
  }

  override fun onCleared() {
    sdrController.stopSdr()
    super.onCleared()
  }
}
