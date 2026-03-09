package ninja.spider.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ninja.spider.data.SightingEntity
import ninja.spider.databinding.ItemSightingBinding
import ninja.spider.util.Formatters
import ninja.spider.util.TpmsMetadataParser

class SightingAdapter : ListAdapter<SightingEntity, SightingAdapter.SightingViewHolder>(DiffCallback) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SightingViewHolder {
    val binding = ItemSightingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return SightingViewHolder(binding)
  }

  override fun onBindViewHolder(holder: SightingViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  class SightingViewHolder(
    private val binding: ItemSightingBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bind(item: SightingEntity) {
      val metadata = TpmsMetadataParser.parse(item.metadataJson)
      binding.sightingTimestamp.text = Formatters.formatTimestamp(item.timestamp)
      binding.sightingRssi.text = Formatters.formatRssi(item.rssi)
      binding.sightingMeta.text = buildList {
        metadata.tpmsPressureKpa?.let { add(Formatters.formatPressure(it)) }
        metadata.tpmsTemperatureC?.let { add(Formatters.formatTemperature(it)) }
        metadata.tpmsBatteryOk?.let { add(if (it) "Battery OK" else "Battery low") }
        metadata.tpmsSnr?.let { add(String.format("SNR %.1f dB", it)) }
      }.joinToString(" • ")
    }
  }

  companion object {
    private val DiffCallback = object : DiffUtil.ItemCallback<SightingEntity>() {
      override fun areItemsTheSame(oldItem: SightingEntity, newItem: SightingEntity): Boolean {
        return oldItem.id == newItem.id
      }

      override fun areContentsTheSame(oldItem: SightingEntity, newItem: SightingEntity): Boolean {
        return oldItem == newItem
      }
    }
  }
}
