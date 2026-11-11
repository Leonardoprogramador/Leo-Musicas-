package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns

object MetadataHelper {
    data class SongMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long
    )

    fun getSongMetadata(context: Context, uri: Uri): SongMetadata {
        val retriever = MediaMetadataRetriever()
        var title = ""
        var artist = ""
        var album = ""
        var durationMs = 0L

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: ""
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: ""
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: ""
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }

        // Fallbacks
        if (title.isBlank()) {
            title = getFileName(context, uri).substringBeforeLast(".")
        }
        if (artist.isBlank()) {
            artist = "Artista Desconhecido"
        }
        if (album.isBlank()) {
            album = "Álbum Desconhecido"
        }

        return SongMetadata(title, artist, album, durationMs)
    }

    fun getArtwork(context: Context, uriString: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(uriString)
            retriever.setDataSource(context, uri)
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } catch (e: Exception) {
                // ignore
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "Música Desconhecida"
    }
}
