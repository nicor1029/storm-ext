package com.stormunblessed

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class OkRuCustom : ExtractorApi() {
    override val name = "OK.ru"
    override val mainUrl = "https://ok.ru"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val embed = url.replace("/video/", "/videoembed/")
        val html = app.get(embed, referer = referer ?: mainUrl).text

        val options = Regex("""data-options="([^"]+)"""").find(html)?.groupValues?.get(1)
            ?: return
        val decoded = options
            .replace("&quot;", "\"")
            .replace("\\\\u0026", "&")
            .replace("\\\\/", "/")

        Regex(""""name":"(\w+)","url":"(https?://[^"]+)"""").findAll(decoded).forEach { m ->
            val quality = when (m.groupValues[1].lowercase()) {
                "mobile", "lowest", "low" -> 360
                "sd" -> 480
                "hd" -> 720
                "full" -> 1080
                "quad" -> 1440
                else -> Qualities.Unknown.value
            }
            callback(
                ExtractorLink(
                    source = name,
                    name = "$name ${m.groupValues[1]}",
                    url = m.groupValues[2],
                    referer = embed,
                    quality = quality,
                    isM3u8 = false,
                    headers = mapOf("Referer" to embed, "User-Agent" to USER_AGENT)
                )
            )
        }
    }
}
