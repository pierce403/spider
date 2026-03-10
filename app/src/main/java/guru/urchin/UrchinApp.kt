package guru.urchin

import android.app.Application
import guru.urchin.data.AppDatabase
import guru.urchin.data.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import guru.urchin.scan.ObservationRecorder
import guru.urchin.sdr.SdrController

class UrchinApp : Application() {
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  val database: AppDatabase by lazy { AppDatabase.build(this) }
  val repository: DeviceRepository by lazy {
    DeviceRepository(database, database.deviceDao(), database.sightingDao())
  }
  val observationRecorder: ObservationRecorder by lazy {
    ObservationRecorder(repository, applicationScope)
  }
  val sdrController: SdrController by lazy {
    SdrController(this, applicationScope, observationRecorder)
  }
}
