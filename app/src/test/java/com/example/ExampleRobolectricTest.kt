package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.EpgTimeZone
import com.example.data.parser.M3uParser
import com.example.data.parser.XmlTvParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LatinPlayer IPTV", appName)
    }

    @Test
    fun `m3u parser parses channels accurately`() {
        val sampleM3u = """
            #EXTM3U url-tvg="https://epg.programadorx.cl/mdiaz/gratis.xml.gz"
            #EXTINF:-1 tvg-id="16mvstr.pe" tvg-logo="https://i.imgur.com/HK0UOso.png" group-title="Perú", (001) Willax Peru
            http://190.93.224.42/WILLAX/index.m3u8
            #EXTINF:-1 tvg-id="2mvstr.pe" tvg-logo="https://i.imgur.com/4iCOFSF.png" group-title="Perú", (002) Latina TV Peru
            http://190.93.224.42/LATINA/index.m3u8
        """.trimIndent()

        val channels = M3uParser.parse(ByteArrayInputStream(sampleM3u.toByteArray()))
        assertEquals(2, channels.size)
        assertEquals("Willax Peru", channels[0].cleanName)
        assertEquals("001", channels[0].number)
        assertEquals("Perú", channels[0].group)
        assertEquals("https://i.imgur.com/HK0UOso.png", channels[0].logoUrl)
        assertEquals("http://190.93.224.42/WILLAX/index.m3u8", channels[0].streamUrl)
    }

    @Test
    fun `xmltv parser parses epg programmes accurately`() {
        val sampleXmlTv = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tv>
              <programme start="20261020210000 -0300" stop="20261020223000 -0300" channel="194dtv.pe">
                <title>Noticias Central</title>
                <desc>Resumen informativo de la jornada con cobertura nacional e internacional.</desc>
              </programme>
              <programme start="20261020223000 -0300" stop="20261020234500 -0300" channel="194dtv.pe">
                <title>Deportes en Vivo</title>
                <desc>Análisis deportivo y jugadas destacadas.</desc>
              </programme>
            </tv>
        """.trimIndent()

        val epgMap = XmlTvParser.parse(ByteArrayInputStream(sampleXmlTv.toByteArray()))
        val programs = epgMap["194dtv.pe"]
        assertNotNull(programs)
        assertEquals(2, programs?.size)
        assertEquals("Noticias Central", programs?.get(0)?.title)

        // Verify GMT-5 conversion: 21:00 (-0300) should be 19:00 in GMT-5
        val tzGmt5 = TimeZone.getTimeZone("GMT-5")
        val formattedStartGmt5 = programs?.get(0)?.getFormattedStart(tzGmt5)
        assertEquals("19:00", formattedStartGmt5)

        // Verify GMT-3: 21:00 (-0300) should be 21:00 in GMT-3
        val tzGmt3 = TimeZone.getTimeZone("GMT-3")
        val formattedStartGmt3 = programs?.get(0)?.getFormattedStart(tzGmt3)
        assertEquals("21:00", formattedStartGmt3)
    }
}
