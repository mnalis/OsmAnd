package net.osmand.shared.filters

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.TrackItem

abstract class BaseTrackFilter(
	@Serializable
	@SerialName("filterType") val trackFilterType: TrackFilterType,
	var filterChangedListener: FilterChangedListener?) {

	abstract fun isEnabled(): Boolean

	abstract fun isTrackAccepted(trackItem: TrackItem): Boolean

	open fun initWithValue(value: BaseTrackFilter) {
		filterChangedListener?.onFilterChanged()
	}

	open fun isValid(): Boolean {
		return true
	}

	open fun initFilter() {}

	override fun equals(other: Any?): Boolean {
		return other is BaseTrackFilter && other.trackFilterType == trackFilterType
	}

	override fun hashCode(): Int {
		var result = trackFilterType.hashCode()
		result = 31 * result + (filterChangedListener?.hashCode() ?: 0)
		return result
	}

}