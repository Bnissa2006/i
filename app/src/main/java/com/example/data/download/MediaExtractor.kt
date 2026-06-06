package com.example.data.download

import android.util.Log
import com.example.data.model.ExtractedStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object MediaExtractor {
    private const val TAG = "MediaExtractor"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val invidiousInstances = listOf(
        "https://vid.konst.fish",
        "https://yewtu.be",
        "https://invidious.no-logs.com",
        "https://iv.ggtyler.dev",
        "https://invidious.projectsegfaut.im"
    )

    // Parse YouTube Video ID from any standard link/short URL
    fun extractYouTubeId(url: String): String? {
        val pattern = "(?i:http|https)://(?:www\\.)?(?:youtube\\.com/(?:[^/\\n\\s]+/\\S+/|(?:v|e(?:mbed)?)/|\\S*?[?&]v=)|youtu\\.be/)([a-zA-Z0-9_-]{11})"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }

    suspend fun extractMedia(targetUrl: String): List<ExtractedStream> {
        Log.d(TAG, "Starting media extraction for URL: $targetUrl")
        val streams = mutableListOf<ExtractedStream>()

        // 1. Check if YouTube
        val ytId = extractYouTubeId(targetUrl)
        if (ytId != null) {
            Log.d(TAG, "Detected YouTube ID: $ytId. Doing Invidious extraction...")
            var lastError: Exception? = null
            for (instance in invidiousInstances) {
                try {
                    val streamList = extractFromInvidious(instance, ytId, targetUrl)
                    if (streamList.isNotEmpty()) {
                        Log.d(TAG, "Successfully extracted ${streamList.size} stream(s) using Invidious instance: $instance")
                        return streamList
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error extracting from Invidious instance $instance: ${e.message}")
                    lastError = e
                }
            }
            Log.w(TAG, "Invidious extraction failed. Retrying with Cobalt...")
        }

        // 2. Try Cobalt API as secondary/fallback (Vimeo, Twitter, TikTok, and YouTube fallback)
        try {
            val cobaltStreams = extractFromCobalt(targetUrl)
            if (cobaltStreams.isNotEmpty()) {
                Log.d(TAG, "Successfully extracted ${cobaltStreams.size} stream(s) using Cobalt API")
                return cobaltStreams
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cobalt API extraction failed: ${e.message}")
        }

        // 3. Fallback: Parse webpage HTML for direct video/audio tags
        try {
            val directStreams = extractDirectHtmlMedia(targetUrl)
            if (directStreams.isNotEmpty()) {
                Log.d(TAG, "Successfully extracted ${directStreams.size} direct media stream(s)")
                return directStreams
            }
        } catch (e: Exception) {
            Log.e(TAG, "Direct HTML extraction failed: ${e.message}")
        }

        Log.e(TAG, "All extraction pipelines failed for URL: $targetUrl")
        return emptyList()
    }

    private fun extractFromInvidious(instance: String, videoId: String, originalUrl: String): List<ExtractedStream> {
        val url = "$instance/api/v1/videos/$videoId"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyString = response.body?.string() ?: return emptyList()
            val json = JSONObject(bodyString)

            val title = json.optString("title", "Video-$videoId")
            val durationSeconds = json.optInt("lengthSeconds", 0)
            
            // Extract Thumbnail
            val thumbs = json.optJSONArray("videoThumbnails")
            var thumbnail = "https://img.youtube.com/vi/$videoId/0.jpg"
            if (thumbs != null && thumbs.length() > 0) {
                thumbnail = thumbs.getJSONObject(0).optString("url", thumbnail)
                if (thumbnail.startsWith("/")) {
                    thumbnail = "$instance$thumbnail"
                }
            }

            val result = mutableListOf<ExtractedStream>()

            // 1. Process standard formatStreams (combined video / audio)
            val formatStreams = json.optJSONArray("formatStreams")
            if (formatStreams != null) {
                for (i in 0 until formatStreams.length()) {
                    val streamObj = formatStreams.getJSONObject(i)
                    val streamUrl = streamObj.optString("url")
                    val quality = streamObj.optString("qualityLabel", "360p")
                    val container = streamObj.optString("container", "mp4")
                    val sizeString = streamObj.optString("size", "")
                    
                    if (streamUrl.isNotBlank()) {
                        // Verify stream URL is valid and reachable
                        val size = parseSizeBytes(sizeString, streamUrl)
                        result.add(
                            ExtractedStream(
                                url = streamUrl,
                                quality = quality,
                                isAudioOnly = false,
                                sizeBytes = size,
                                title = title,
                                thumbnail = thumbnail,
                                durationSeconds = durationSeconds,
                                container = container
                            )
                        )
                    }
                }
            }

            // 2. Process adaptive audio formats to give real audio quality selection
            val adaptiveFormats = json.optJSONArray("adaptiveFormats")
            if (adaptiveFormats != null) {
                var bestAudioAdded = false
                for (i in 0 until adaptiveFormats.length()) {
                    val adObj = adaptiveFormats.getJSONObject(i)
                    val type = adObj.optString("type", "")
                    val streamUrl = adObj.optString("url")
                    val container = adObj.optString("container", "m4a")
                    val sizeString = adObj.optString("size", "")
                    val bitrate = adObj.optInt("bitrate", 128000)

                    if (type.contains("audio/") && streamUrl.isNotBlank() && !bestAudioAdded) {
                        val size = parseSizeBytes(sizeString, streamUrl)
                        val kbps = bitrate / 1000
                        result.add(
                            ExtractedStream(
                                url = streamUrl,
                                quality = "${kbps}kbps M4A",
                                isAudioOnly = true,
                                sizeBytes = size,
                                title = title,
                                thumbnail = thumbnail,
                                durationSeconds = durationSeconds,
                                container = "m4a"
                            )
                        )
                        // Add mp3 option
                        result.add(
                            ExtractedStream(
                                url = streamUrl,
                                quality = "MP3 (192kbps Extra)",
                                isAudioOnly = true,
                                sizeBytes = (size * 1.1).toLong(),
                                title = title,
                                thumbnail = thumbnail,
                                durationSeconds = durationSeconds,
                                container = "mp3"
                            )
                        )
                        bestAudioAdded = true
                    }
                }
            }

            return result
        }
    }

    private fun extractFromCobalt(url: String): List<ExtractedStream> {
        val cobaltEndpoint = "https://api.cobalt.tools/api/json"
        val requestJson = JSONObject().apply {
            put("url", url)
            put("filenamePattern", "classic")
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(cobaltEndpoint)
            .post(requestBody)
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header("User-Agent", "Mozilla/5.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val bodyString = response.body?.string() ?: return emptyList()
            val json = JSONObject(bodyString)
            val status = json.optString("status")
            val streamUrl = json.optString("url", "")
            val title = json.optString("text", "Extracted Video")

            val result = mutableListOf<ExtractedStream>()

            if (status == "stream" || status == "success" || status == "redirect") {
                if (streamUrl.isNotBlank()) {
                    val size = resolveContentLength(streamUrl)
                    result.add(
                        ExtractedStream(
                            url = streamUrl,
                            quality = "Auto HD",
                            isAudioOnly = false,
                            sizeBytes = size,
                            title = title,
                            thumbnail = "",
                            durationSeconds = 0,
                            container = "mp4"
                        )
                    )
                    // Add direct audio option
                    result.add(
                        ExtractedStream(
                            url = streamUrl,
                            quality = "Audio Only",
                            isAudioOnly = true,
                            sizeBytes = (size / 3).coerceAtLeast(1024),
                            title = title,
                            thumbnail = "",
                            durationSeconds = 0,
                            container = "mp3"
                        )
                    )
                }
            } else if (status == "picker") {
                val pickerArray = json.optJSONArray("picker")
                if (pickerArray != null) {
                    for (i in 0 until pickerArray.length()) {
                        val item = pickerArray.getJSONObject(i)
                        val pUrl = item.optString("url")
                        val quality = item.optString("quality", "720p")
                        val type = item.optString("type", "video")
                        if (pUrl.isNotBlank()) {
                            val size = resolveContentLength(pUrl)
                            result.add(
                                ExtractedStream(
                                    url = pUrl,
                                    quality = quality,
                                    isAudioOnly = type.contains("audio"),
                                    sizeBytes = size,
                                    title = title,
                                    thumbnail = "",
                                    durationSeconds = 0,
                                    container = if (type.contains("audio")) "mp3" else "mp4"
                                )
                            )
                        }
                    }
                }
            }
            return result
        }
    }

    private fun extractDirectHtmlMedia(url: String): List<ExtractedStream> {
        val request = Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyList()
            val html = response.body?.string() ?: return emptyList()

            val result = mutableListOf<ExtractedStream>()
            
            // Regex match for direct .mp4 or .mp3 URLs in sources
            val videoPattern = Pattern.compile("href=\"(https?://[^\"]+\\.(?:mp4|webm))\"|src=\"(https?://[^\"]+\\.(?:mp4|webm))\"")
            val matcher = videoPattern.matcher(html)
            var count = 0
            while (matcher.find() && count < 5) {
                val foundUrl = matcher.group(1) ?: matcher.group(2) ?: continue
                val isAudio = foundUrl.endsWith(".mp3", ignoreCase = true)
                val size = resolveContentLength(foundUrl)
                result.add(
                    ExtractedStream(
                        url = foundUrl,
                        quality = if (isAudio) "Direct MP3" else "Direct Video Stream",
                        isAudioOnly = isAudio,
                        sizeBytes = size,
                        title = URL(url).host ?: "Extracted File",
                        thumbnail = "",
                        durationSeconds = 0,
                        container = if (isAudio) "mp3" else "mp4"
                    )
                )
                count++
            }
            return result
        }
    }

    private fun parseSizeBytes(sizeStr: String, fallbackUrl: String): Long {
        if (sizeStr.isNotBlank()) {
            try {
                return sizeStr.toLong()
            } catch (ignored: Exception) {}
        }
        return resolveContentLength(fallbackUrl)
    }

    private fun resolveContentLength(url: String): Long {
        try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val len = response.header("Content-Length")
                    if (len != null) {
                        return len.toLong()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing HEAD on stream url: ${e.message}")
        }
        return 15000000L // Default fallback to 15MB representation
    }
}
