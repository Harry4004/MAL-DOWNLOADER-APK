package com.harry.maldownloader

import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.harry.maldownloader.data.AnimeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
import java.util.concurrent.TimeUnit

// Centralized file helpers
object FileUtils {
    fun sanitizeFileName(name: String): String {
        val tmp = Normalizer.normalize(name, Normalizer.Form.NFKC)
        return tmp.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "untitled" }
    }

    fun ensureDir(root: File, child: String): File = File(root, sanitizeFileName(child)).apply { mkdirs() }

    fun writeText(file: File, text: String) = file.writeText(text)
}
