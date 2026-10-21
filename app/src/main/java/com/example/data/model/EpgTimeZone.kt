package com.example.data.model

import java.util.TimeZone

enum class EpgTimeZone(
    val code: String,
    val title: String,
    val shortLabel: String,
    val countries: String,
    val gmtOffsetString: String
) {
    GMT_MINUS_5(
        code = "GMT-5",
        title = "GMT-5 (Perú, Colombia, Ecuador)",
        shortLabel = "GMT-5",
        countries = "Perú, Colombia, Ecuador, Panamá",
        gmtOffsetString = "GMT-5"
    ),
    GMT_MINUS_4(
        code = "GMT-4",
        title = "GMT-4 (Bolivia, Paraguay, Venezuela)",
        shortLabel = "GMT-4",
        countries = "Bolivia, Paraguay, Venezuela",
        gmtOffsetString = "GMT-4"
    ),
    GMT_MINUS_3(
        code = "GMT-3",
        title = "GMT-3 (Argentina, Chile, Uruguay)",
        shortLabel = "GMT-3",
        countries = "Argentina, Chile, Uruguay, Brasil",
        gmtOffsetString = "GMT-3"
    ),
    GMT_MINUS_6(
        code = "GMT-6",
        title = "GMT-6 (México, Centroamérica)",
        shortLabel = "GMT-6",
        countries = "México, Costa Rica, Guatemala, Honduras",
        gmtOffsetString = "GMT-6"
    ),
    AUTO_BY_COUNTRY(
        code = "AUTO",
        title = "Auto (Según país de cada canal)",
        shortLabel = "Auto País",
        countries = "Detecta país del canal (Perú: GMT-5, etc.)",
        gmtOffsetString = "AUTO"
    ),
    DEVICE_LOCAL(
        code = "DEVICE",
        title = "Hora de mi dispositivo",
        shortLabel = "Dispositivo",
        countries = "Zona horaria local del teléfono o TV",
        gmtOffsetString = "LOCAL"
    );

    fun resolveTimeZone(channelGroup: String): TimeZone {
        return when (this) {
            AUTO_BY_COUNTRY -> {
                val groupLower = channelGroup.lowercase()
                when {
                    groupLower.contains("perú") || groupLower.contains("peru") -> TimeZone.getTimeZone("GMT-5")
                    groupLower.contains("colombia") -> TimeZone.getTimeZone("GMT-5")
                    groupLower.contains("ecuador") -> TimeZone.getTimeZone("GMT-5")
                    groupLower.contains("bolivia") -> TimeZone.getTimeZone("GMT-4")
                    groupLower.contains("paraguay") -> TimeZone.getTimeZone("GMT-4")
                    groupLower.contains("venezuela") -> TimeZone.getTimeZone("GMT-4")
                    groupLower.contains("chile") -> TimeZone.getTimeZone("GMT-3")
                    groupLower.contains("argentina") -> TimeZone.getTimeZone("GMT-3")
                    groupLower.contains("uruguay") -> TimeZone.getTimeZone("GMT-3")
                    groupLower.contains("méxico") || groupLower.contains("mexico") -> TimeZone.getTimeZone("GMT-6")
                    else -> TimeZone.getTimeZone("GMT-5") // Default for LatinPlayer
                }
            }
            GMT_MINUS_5 -> TimeZone.getTimeZone("GMT-5")
            GMT_MINUS_4 -> TimeZone.getTimeZone("GMT-4")
            GMT_MINUS_3 -> TimeZone.getTimeZone("GMT-3")
            GMT_MINUS_6 -> TimeZone.getTimeZone("GMT-6")
            DEVICE_LOCAL -> TimeZone.getDefault()
        }
    }
}
