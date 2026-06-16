package com.streamflixreborn.streamflix.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.concurrent.TimeUnit

class HlsDownloader(
    private val context: Context,
    private val url: String,
    private val headers: Map<String, String>?,
    private val title: String,
    private val onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    data class Result(val uri: Uri, val displayName: String)

    private val sanitized: String by lazy {
        title.replace(Regex("""[\\/:*?"<>|]"""), "_").replace(Regex("\\s+"), "_")
    }

    fun download(): Result {
        if (isDirectMp4()) {
            return downloadDirect()
        }
        return downloadHls()
    }

    private fun isDirectMp4(): Boolean {
        val u = url.lowercase()
        return u.endsWith(".mp4") || u.contains(".mp4?")
    }

    private fun downloadDirect(): Result {
        val fileName = "$sanitized.mp4"
        val tempFile = File(context.cacheDir, fileName)
        downloadToFile(url, tempFile)
        val mimeType = "video/mp4"
        val uri = saveToMediaStore(tempFile, fileName, mimeType)
        tempFile.delete()
        return Result(uri, fileName)
    }

    private fun downloadHls(): Result {
        val masterContent = fetchString(url)
        val variantUrl = if (masterContent.contains("#EXT-X-STREAM-INF")) {
            pickBestVariant(masterContent, url)
        } else {
            url
        }
        val variantContent = if (variantUrl != url) fetchString(variantUrl) else masterContent
        val segments = parseSegments(variantContent, variantUrl)
        if (segments.isEmpty()) throw RuntimeException("No segments found in playlist")
        if (variantContent.contains("#EXT-X-KEY")) throw RuntimeException("AES-encrypted streams not yet supported")

        val total = segments.size
        val tempFile = File(context.cacheDir, "${sanitized}_temp.ts")
        val output = FileOutputStream(tempFile)

        segments.forEachIndexed { index, segmentUrl ->
            val data = fetchBytes(segmentUrl)
            output.write(data)
            onProgress(index + 1, total)
        }

        output.close()
        val fileName = "$sanitized.ts"
        val uri = saveToMediaStore(tempFile, fileName, "video/mp2t")
        tempFile.delete()
        return Result(uri, fileName)
    }

    private fun pickBestVariant(playlist: String, baseUrl: String): String {
        val lines = playlist.lines()
        var bestBandwidth = -1
        var bestUrl: String? = null
        var currentBandwidth = -1

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXT-X-STREAM-INF:")) {
                val bwMatch = Regex("""BANDWIDTH=(\d+)""").find(trimmed)
                currentBandwidth = bwMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
            } else if (!trimmed.startsWith("#") && trimmed.isNotBlank()) {
                if (currentBandwidth > bestBandwidth) {
                    bestBandwidth = currentBandwidth
                    bestUrl = resolveUrl(trimmed, baseUrl)
                }
                currentBandwidth = -1
            }
        }
        return bestUrl ?: baseUrl
    }

    private fun parseSegments(playlist: String, baseUrl: String): List<String> {
        return playlist.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { resolveUrl(it, baseUrl) }
    }

    private fun resolveUrl(segment: String, baseUrl: String): String {
        return if (segment.startsWith("http://") || segment.startsWith("https://")) {
            segment
        } else {
            URL(URL(baseUrl), segment).toString()
        }
    }

    private fun fetchString(url: String): String {
        val request = buildRequest(url)
        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw RuntimeException("Empty response from $url")
    }

    private fun fetchBytes(url: String): ByteArray {
        val request = buildRequest(url)
        val response = client.newCall(request).execute()
        return response.body?.bytes() ?: throw RuntimeException("Empty response from $url")
    }

    private fun downloadToFile(url: String, file: File) {
        val request = buildRequest(url)
        val response = client.newCall(request).execute()
        response.body?.byteStream()?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw RuntimeException("Empty response from $url")
    }

    private fun buildRequest(url: String): Request {
        val builder = Request.Builder().url(url)
        headers?.forEach { (key, value) ->
            builder.header(key, value)
        }
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        return builder.build()
    }

    private fun saveToMediaStore(file: File, fileName: String, mimeType: String): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, mimeType)
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Streamflix")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values
            ) ?: throw RuntimeException("Failed to create MediaStore entry")

            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: throw RuntimeException("Failed to open output stream")

            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            uri
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val dest = File(dir, "Streamflix/$fileName")
            dest.parentFile?.mkdirs()
            file.copyTo(dest, overwrite = true)
            Uri.fromFile(dest)
        }
    }
}
