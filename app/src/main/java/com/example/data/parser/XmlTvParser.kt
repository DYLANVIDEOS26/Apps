package com.example.data.parser

import android.util.Log
import com.example.data.model.EpgProgram
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

object XmlTvParser {

    private const val TAG = "XmlTvParser"
    private val DATE_FORMAT_TZ = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val DATE_FORMAT_SIMPLE = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
    private val TIME_FORMAT_DISPLAY = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun parse(inputStream: InputStream): Map<String, List<EpgProgram>> {
        val programsByChannel = mutableMapOf<String, MutableList<EpgProgram>>()
        val now = System.currentTimeMillis()
        val sixHoursAgo = now - (6 * 3600 * 1000)

        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = false
            val parser = factory.newPullParser()
            parser.setInput(inputStream, "UTF-8")

            var eventType = parser.eventType
            var currentChannelId: String? = null
            var currentStartEpoch: Long = 0
            var currentStopEpoch: Long = 0
            var currentFormattedStart = ""
            var currentFormattedStop = ""
            var currentTitle = ""
            var currentDesc = ""
            var currentTag = ""

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        currentTag = parser.name
                        if (currentTag.equals("programme", ignoreCase = true)) {
                            currentChannelId = parser.getAttributeValue(null, "channel")
                            val rawStart = parser.getAttributeValue(null, "start")
                            val rawStop = parser.getAttributeValue(null, "stop")

                            currentStartEpoch = parseDateToEpoch(rawStart)
                            currentStopEpoch = parseDateToEpoch(rawStop)
                            currentFormattedStart = formatEpochTime(currentStartEpoch)
                            currentFormattedStop = formatEpochTime(currentStopEpoch)
                            currentTitle = ""
                            currentDesc = ""
                        }
                    }

                    XmlPullParser.TEXT -> {
                        val text = parser.text?.trim().orEmpty()
                        if (text.isNotEmpty()) {
                            when (currentTag.lowercase()) {
                                "title" -> {
                                    if (currentTitle.isEmpty()) {
                                        currentTitle = text
                                    }
                                }
                                "desc" -> {
                                    if (currentDesc.isEmpty()) {
                                        currentDesc = text
                                    }
                                }
                            }
                        }
                    }

                    XmlPullParser.END_TAG -> {
                        val endTag = parser.name
                        if (endTag.equals("programme", ignoreCase = true)) {
                            val channelId = currentChannelId
                            // Only retain current and future/recent programs
                            if (channelId != null && currentStopEpoch >= sixHoursAgo && currentTitle.isNotBlank()) {
                                val prog = EpgProgram(
                                    channelId = channelId,
                                    title = currentTitle,
                                    description = currentDesc,
                                    startEpoch = currentStartEpoch,
                                    stopEpoch = currentStopEpoch
                                )
                                programsByChannel.getOrPut(channelId) { mutableListOf() }.add(prog)
                            }
                            currentChannelId = null
                            currentTag = ""
                        } else if (endTag.equals(currentTag, ignoreCase = true)) {
                            currentTag = ""
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing XMLTV EPG", e)
        }

        // Sort each channel's programs by start time
        programsByChannel.values.forEach { list ->
            list.sortBy { it.startEpoch }
        }

        return programsByChannel
    }

    private fun parseDateToEpoch(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            val trimmed = dateStr.trim()
            if (trimmed.length > 14 && (trimmed.contains("+") || trimmed.contains("-"))) {
                DATE_FORMAT_TZ.parse(trimmed)?.time ?: 0L
            } else {
                val clean = if (trimmed.length >= 14) trimmed.substring(0, 14) else trimmed
                DATE_FORMAT_SIMPLE.parse(clean)?.time ?: 0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun formatEpochTime(epoch: Long): String {
        if (epoch <= 0L) return "--:--"
        return try {
            TIME_FORMAT_DISPLAY.format(epoch)
        } catch (e: Exception) {
            "--:--"
        }
    }
}
