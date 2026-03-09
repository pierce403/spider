package ninja.spider.ui

import android.content.Context

object MainDisplayPreferences {
  private const val PREFS_NAME = "spider_display"
  private const val KEY_COMPACT_DEVICE_CARDS = "compact_device_cards"

  fun isCompactDeviceCards(context: Context): Boolean {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .getBoolean(KEY_COMPACT_DEVICE_CARDS, false)
  }

  fun setCompactDeviceCards(context: Context, compact: Boolean) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
      .edit()
      .putBoolean(KEY_COMPACT_DEVICE_CARDS, compact)
      .apply()
  }
}
