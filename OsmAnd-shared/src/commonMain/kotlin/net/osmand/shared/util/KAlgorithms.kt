package net.osmand.shared.util

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.io.KFile
import kotlin.math.max
import kotlin.math.min

object KAlgorithms {
	private const val CHAR_TO_SPLIT = ','

	fun isEmpty(c: Collection<*>?): Boolean {
		return c == null || c.isEmpty()
	}

	fun isEmpty(map: Map<*, *>?): Boolean {
		return map == null || map.isEmpty()
	}

	fun <T> isEmpty(array: Array<T>?): Boolean {
		return array.isNullOrEmpty()
	}

	fun emptyIfNull(s: String?): String {
		return s ?: ""
	}

	fun trimIfNotNull(s: String?): String? {
		return s?.trim()
	}

	fun isEmpty(s: CharSequence?): Boolean {
		return s.isNullOrEmpty()
	}

	fun isBlank(s: String?): Boolean {
		return s == null || s.trim().isEmpty()
	}

	fun hash(vararg values: Any?): Int {
		return values.contentHashCode()
	}

	fun stringsEqual(s1: String?, s2: String?): Boolean {
		return s1 == s2
	}

	fun parseLongSilently(input: String?, def: Long): Long {
		return if (!isEmpty(input)) {
			try {
				input?.toLong() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseIntSilently(input: String?, def: Int): Int {
		return if (!isEmpty(input)) {
			try {
				input?.toInt() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseDoubleSilently(input: String?, def: Double): Double {
		return if (!isEmpty(input)) {
			try {
				input?.toDouble() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseFloatSilently(input: String?, def: Float): Float {
		return if (!isEmpty(input)) {
			try {
				input?.toFloat() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun colorToString(color: Int): String {
		return if ((0xFF000000.toInt() and color) == 0xFF000000.toInt()) {
			"#" + format(6, (color and 0x00FFFFFF).toString(16))
		} else {
			"#" + format(8, color.toString(16))
		}
	}

	private fun format(i: Int, hexString: String): String {
		var formattedString = hexString
		while (formattedString.length < i) {
			formattedString = "0$formattedString"
		}
		return formattedString
	}

	/**
	 * Parse the color string, and return the corresponding color-int.
	 * If the string cannot be parsed, throws an IllegalArgumentException
	 * exception. Supported formats are:
	 * #RRGGBB
	 * #AARRGGBB
	 */
	fun parseColor(colorString: String): Int {
		if (colorString.startsWith("#")) {
			var colorStr = colorString
			if (colorStr.length == 4) {
				colorStr =
					"#" + colorStr[1] + colorStr[1] + colorStr[2] + colorStr[2] + colorStr[3] + colorStr[3]
			}
			val color = colorStr.substring(1).toLong(16)
			return when (colorStr.length) {
				7 -> (color or 0x00000000ff000000).toInt() // Set the alpha value
				9 -> color.toInt()
				else -> throw IllegalArgumentException("Unknown color $colorString")
			}
		} else {
			throw IllegalArgumentException("Unknown color $colorString")
		}
	}

	fun decodeStringSet(s: String): Set<String> {
		return decodeStringSet(s, CHAR_TO_SPLIT.toString())
	}

	fun decodeStringSet(s: String, split: String): Set<String> {
		if (s.isEmpty()) {
			return emptySet()
		}
		return s.split(split).toSet()
	}

	fun <T> encodeCollection(collection: Collection<T>): String {
		return encodeCollection(collection, CHAR_TO_SPLIT.toString())
	}

	fun <T> encodeCollection(collection: Collection<T>, split: String): String {
		if (collection.isNotEmpty()) {
			val sb = StringBuilder()
			for (item in collection) {
				sb.append(item).append(split)
			}
			return sb.toString()
		}
		return ""
	}

	fun extendRectToContainPoint(mapRect: KQuadRect, longitude: Double, latitude: Double) {
		mapRect.left = if (mapRect.left == 0.0) longitude else min(mapRect.left, longitude)
		mapRect.right = max(mapRect.right, longitude)
		mapRect.bottom = if (mapRect.bottom == 0.0) latitude else min(mapRect.bottom, latitude)
		mapRect.top = max(mapRect.top, latitude)
	}

	fun extendRectToContainRect(mapRect: KQuadRect, gpxRect: KQuadRect) {
		mapRect.left = if (mapRect.left == 0.0) gpxRect.left else min(mapRect.left, gpxRect.left)
		mapRect.right = max(mapRect.right, gpxRect.right)
		mapRect.top = max(mapRect.top, gpxRect.top)
		mapRect.bottom = if (mapRect.bottom == 0.0) gpxRect.bottom else min(mapRect.bottom, gpxRect.bottom)
	}

	fun removeAllFiles(file: KFile?): Boolean {
		if (file == null) {
			return false
		}
		return if (file.isDirectory()) {
			val files: Array<KFile> = file.listFiles()
			if (!isEmpty<KFile>(files)) {
				for (f in files) {
					removeAllFiles(f)
				}
			}
			file.delete()
		} else {
			file.delete()
		}
	}

	fun getFileNameWithoutExtension(f: KFile): String? {
		return getFileNameWithoutExtension(f.name())
	}

	fun getFileNameWithoutExtension(name: String?): String? {
		if (name != null) {
			val index = name.lastIndexOf('.')
			if (index != -1) {
				return name.substring(0, index)
			}
		}
		return name
	}

	fun getFileNameWithoutExtensionAndRoadSuffix(fileName: String?): String? {
		val name:String? = getFileNameWithoutExtension(fileName)
		return if (name!!.endsWith(".road")) name.substring(0, name.lastIndexOf(".road")) else name
	}

	fun capitalizeFirstLetter(s: String?): String? {
		return if (!s.isNullOrEmpty()) {
			s[0].uppercaseChar().toString() + if (s.length > 1) s.substring(1) else ""
		} else {
			s
		}
	}

	fun objectEquals(a: Any?, b: Any?): Boolean {
		return if (a == null) {
			b == null
		} else {
			a == b
		}
	}
}
