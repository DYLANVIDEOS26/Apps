package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String = "",
    val startEpoch: Long,
    val stopEpoch: Long
) {
    fun isCurrent(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime in startEpoch until stopEpoch
    }

    fun isUpcoming(currentTime: Long = System.currentTimeMillis()): Boolean {
        return startEpoch > currentTime
    }

    fun getProgress(currentTime: Long = System.currentTimeMillis()): Float {
        if (currentTime < startEpoch) return 0f
        if (currentTime >= stopEpoch) return 1f
        val total = (stopEpoch - startEpoch).toFloat()
        return if (total > 0) ((currentTime - startEpoch) / total).coerceIn(0f, 1f) else 0f
    }

    fun getFormattedStart(timeZone: TimeZone): String {
        return formatEpochTime(startEpoch, timeZone)
    }

    fun getFormattedStop(timeZone: TimeZone): String {
        return formatEpochTime(stopEpoch, timeZone)
    }

    fun getFormattedRange(timeZone: TimeZone): String {
        return "${getFormattedStart(timeZone)} - ${getFormattedStop(timeZone)}"
    }

    companion object {
        fun formatEpochTime(epoch: Long, timeZone: TimeZone): String {
            if (epoch <= 0L) return "--:--"
            return try {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                    this.timeZone = timeZone
                }
                sdf.format(Date(epoch))
            } catch (e: Exception) {
                "--:--"
            }
        }
    }
}

data class ChannelEpgInfo(
    val channelId: String,
    val currentProgram: EpgProgram? = null,
    val nextProgram: EpgProgram? = null,
    val allPrograms: List<EpgProgram> = emptyList(),
    val timeZone: TimeZone = TimeZone.getTimeZone("GMT-5")
) {
    companion object {
        // Generates a complete 24-hour continuous guide for channels with missing EPG data
        fun generate24HourFallback(channel: Channel, timeZone: TimeZone): ChannelEpgInfo {
            val programs = mutableListOf<EpgProgram>()
            val calendar = Calendar.getInstance(timeZone).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val blocks = listOf(
                Pair(Pair(0, 3), "Transmisión Nocturna: ${channel.cleanName}"),
                Pair(Pair(3, 6), "Madrugada en Vivo • Música y Repeticiones"),
                Pair(Pair(6, 9), "Primer Noticiero Matinal • Noticias del Día"),
                Pair(Pair(9, 12), "Revista Matutina • Entretenimiento y Actualidad"),
                Pair(Pair(12, 14), "Emisión Central de Noticias • En Vivo"),
                Pair(Pair(14, 17), "Bloque de Tarde • Telenovelas y Series"),
                Pair(Pair(17, 19), "Magazine de la Tarde • Espectáculos y Deportes"),
                Pair(Pair(19, 21), "Noticiero Estelar • Información Nacional e Internacional"),
                Pair(Pair(21, 23), "Prime Time Estelar: ${channel.cleanName}"),
                Pair(Pair(23, 24), "Cine y Resumen Nocturno")
            )

            val baseStartDay = calendar.timeInMillis
            // Build for yesterday, today, and tomorrow
            for (dayOffset in -1..1) {
                val dayBase = baseStartDay + (dayOffset * 24 * 3600 * 1000L)
                for (block in blocks) {
                    val startH = block.first.first
                    val endH = block.first.second
                    val startEpoch = dayBase + (startH * 3600 * 1000L)
                    val stopEpoch = dayBase + (endH * 3600 * 1000L)

                    programs.add(
                        EpgProgram(
                            channelId = channel.tvgId.ifEmpty { channel.id },
                            title = block.second,
                            description = "Señal en directo de ${channel.cleanName} (${channel.group}). Programación continua.",
                            startEpoch = startEpoch,
                            stopEpoch = stopEpoch
                        )
                    )
                }
            }

            val now = System.currentTimeMillis()
            val current = programs.firstOrNull { it.isCurrent(now) }
            val next = programs.firstOrNull { it.isUpcoming(now) }

            return ChannelEpgInfo(
                channelId = channel.tvgId.ifEmpty { channel.id },
                currentProgram = current,
                nextProgram = next,
                allPrograms = programs.filter { it.stopEpoch >= now - 6 * 3600 * 1000L },
                timeZone = timeZone
            )
        }
    }
}
