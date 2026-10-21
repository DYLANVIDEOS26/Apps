package com.example.data.parser

import com.example.data.model.Channel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern

object M3uParser {

    private val TVG_ID_PATTERN = Pattern.compile("tvg-id=\"([^\"]*)\"")
    private val TVG_LOGO_PATTERN = Pattern.compile("tvg-logo=\"([^\"]*)\"")
    private val GROUP_TITLE_PATTERN = Pattern.compile("group-title=\"([^\"]*)\"")
    private val NUMBER_PREFIX_PATTERN = Pattern.compile("^\\s*\\(?([0-9]{1,4})\\)?[-.:]?\\s*(.*)$")

    fun parse(inputStream: InputStream): List<Channel> {
        val channels = mutableListOf<Channel>()
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        var currentLine: String?
        var currentTvgId = ""
        var currentTvgLogo = ""
        var currentGroup = "General"
        var currentRawName = ""
        var currentNumber = ""
        var currentCleanName = ""
        var channelIndex = 1

        while (reader.readLine().also { currentLine = it } != null) {
            val line = currentLine?.trim() ?: continue
            if (line.isEmpty()) continue

            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                // Parse attributes
                val tvgIdMatcher = TVG_ID_PATTERN.matcher(line)
                currentTvgId = if (tvgIdMatcher.find()) tvgIdMatcher.group(1).orEmpty() else ""

                val logoMatcher = TVG_LOGO_PATTERN.matcher(line)
                currentTvgLogo = if (logoMatcher.find()) logoMatcher.group(1).orEmpty() else ""

                val groupMatcher = GROUP_TITLE_PATTERN.matcher(line)
                currentGroup = if (groupMatcher.find()) {
                    val g = groupMatcher.group(1).orEmpty().trim()
                    if (g.isNotEmpty()) g else "General"
                } else {
                    "General"
                }

                // Parse display name after comma
                val commaIndex = line.lastIndexOf(',')
                currentRawName = if (commaIndex != -1 && commaIndex < line.length - 1) {
                    line.substring(commaIndex + 1).trim()
                } else {
                    "Canal $channelIndex"
                }

                // Extract number prefix if present, e.g. (001) Willax Peru -> number: 001, clean: Willax Peru
                val numMatcher = NUMBER_PREFIX_PATTERN.matcher(currentRawName)
                if (numMatcher.matches()) {
                    currentNumber = numMatcher.group(1) ?: String.format("%03d", channelIndex)
                    currentCleanName = numMatcher.group(2)?.trim()?.ifEmpty { currentRawName } ?: currentRawName
                } else {
                    currentNumber = String.format("%03d", channelIndex)
                    currentCleanName = currentRawName
                }
            } else if (!line.startsWith("#")) {
                // Stream URL
                val streamUrl = line.trim()
                if (streamUrl.startsWith("http://") || streamUrl.startsWith("https://")) {
                    val id = if (currentTvgId.isNotEmpty()) {
                        "${currentTvgId}_${channels.size}"
                    } else {
                        "ch_${channels.size}_${streamUrl.hashCode()}"
                    }

                    channels.add(
                        Channel(
                            id = id,
                            name = currentRawName.ifEmpty { "Canal $channelIndex" },
                            cleanName = currentCleanName.ifEmpty { currentRawName },
                            number = currentNumber.ifEmpty { String.format("%03d", channelIndex) },
                            group = currentGroup,
                            logoUrl = currentTvgLogo,
                            streamUrl = streamUrl,
                            tvgId = currentTvgId
                        )
                    )
                    channelIndex++
                    // Reset line data
                    currentTvgId = ""
                    currentTvgLogo = ""
                    currentGroup = "General"
                    currentRawName = ""
                    currentNumber = ""
                    currentCleanName = ""
                }
            }
        }

        reader.close()
        return channels
    }
}
