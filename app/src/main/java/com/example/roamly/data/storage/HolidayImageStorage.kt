package com.example.roamly.data.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File

object HolidayImageStorage {

    fun saveImage(
        context: Context,
        sourceUri: Uri
    ): Uri? {

        return try {

            val directory =
                File(
                    context.filesDir,
                    "holiday_covers"
                )

            if (!directory.exists()) {
                directory.mkdirs()
            }

            val mimeType =
                context.contentResolver
                    .getType(sourceUri)

            val extension =
                MimeTypeMap
                    .getSingleton()
                    .getExtensionFromMimeType(
                        mimeType
                    )
                    ?: "jpg"

            val destination =
                File(
                    directory,
                    "holiday_${System.currentTimeMillis()}.$extension"
                )

            context.contentResolver
                .openInputStream(sourceUri)
                ?.use { input ->

                    destination
                        .outputStream()
                        .use { output ->

                            input.copyTo(output)
                        }
                }
                ?: return null

            Uri.fromFile(destination)

        } catch (exception: Exception) {

            Log.e(
                "RoamlyHoliday",
                "Could not save holiday image",
                exception
            )

            null
        }
    }
}