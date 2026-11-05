
package com.example.roamly.data.sync

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import com.example.roamly.R
import com.example.roamly.data.auth.SessionManager
import com.example.roamly.data.model.Holiday
import com.example.roamly.data.network.ApiConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object HolidayCoverImage {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    fun jpegBytes(
        context: Context,
        source: String
    ): ByteArray {
        val resolver = context.contentResolver
        val uri = Uri.parse(source)

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        val firstStream = resolver.openInputStream(uri)
            ?: error("Cannot access the selected cover image.")

        firstStream.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        require(
            bounds.outWidth > 0 &&
                    bounds.outHeight > 0
        ) {
            "The selected cover image is not supported."
        }

        var sample = 1

        while (
            bounds.outWidth / sample > 2048 ||
            bounds.outHeight / sample > 2048
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
        }

        val bitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("Cannot read the selected cover image.")

        val output = ByteArrayOutputStream()

        try {
            check(
                bitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    85,
                    output
                )
            ) {
                "Could not prepare the cover image."
            }
        } finally {
            bitmap.recycle()
        }

        return output.toByteArray().also {
            require(
                it.isNotEmpty() &&
                        it.size <= 5 * 1024 * 1024
            ) {
                "Cover image exceeds the 5 MB upload limit."
            }
        }
    }

    fun saveUploaded(
        context: Context,
        userId: String,
        cloudUrl: String,
        bytes: ByteArray
    ) {
        val file = cacheFile(
            context,
            userId,
            cloudUrl
        )

        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    fun load(
        view: ImageView,
        holiday: Holiday
    ) {
        val source = holiday.coverImageUri

        view.tag = source
        view.setImageResource(
            R.drawable.ic_holiday_placeholder
        )

        if (source.isNullOrBlank()) return

        if (!source.startsWith("https://", true)) {
            try {
                view.setImageURI(Uri.parse(source))

                if (view.drawable == null) {
                    view.setImageResource(
                        R.drawable.ic_holiday_placeholder
                    )
                }
            } catch (error: Exception) {
                Log.w(
                    "HolidayCoverImage",
                    "Could not read local cover",
                    error
                )

                view.setImageResource(
                    R.drawable.ic_holiday_placeholder
                )
            }

            return
        }

        val context = view.context.applicationContext

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    val session =
                        SessionManager(context).getSession()
                            ?: return@withContext null

                    val file = cacheFile(
                        context,
                        session.userId,
                        source
                    )

                    if (file.isFile) {
                        BitmapFactory.decodeFile(
                            file.absolutePath
                        )?.let {
                            return@withContext it
                        }
                    }

                    val id = HolidayIdMappingStore(context)
                        .getServerId(holiday.holidayId)
                        ?: return@withContext null

                    val request = Request.Builder()
                        .url(
                            "${ApiConfig.BASE_URL}api/holidays/$id/cover"
                        )
                        .header(
                            "Authorization",
                            "Bearer ${session.accessToken}"
                        )
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext null
                        }

                        val bytes = response.body?.bytes()
                            ?: return@withContext null

                        if (bytes.size > 5 * 1024 * 1024) {
                            return@withContext null
                        }

                        val decoded = BitmapFactory.decodeByteArray(
                            bytes,
                            0,
                            bytes.size
                        ) ?: return@withContext null

                        saveUploaded(
                            context,
                            session.userId,
                            source,
                            bytes
                        )

                        decoded
                    }
                }

                if (bitmap != null && view.tag == source) {
                    view.setImageBitmap(bitmap)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w(
                    "HolidayCoverImage",
                    "Could not load cloud cover",
                    error
                )
            }
        }
    }

    private fun cacheFile(
        context: Context,
        userId: String,
        url: String
    ): File {
        val digest = MessageDigest.getInstance("SHA-256")

        val key = digest.digest(
            "$userId:$url".toByteArray()
        ).joinToString("") {
            "%02x".format(it)
        }

        return File(
            context.filesDir,
            "holiday_covers/$key.jpg"
        )
    }
}
