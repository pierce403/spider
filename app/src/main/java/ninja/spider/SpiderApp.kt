package ninja.spider

import android.app.Application
import ninja.spider.data.AppDatabase
import ninja.spider.data.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ninja.spider.scan.ObservationRecorder
import ninja.spider.sdr.SdrController

class SpiderApp : Application() {
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
