package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

object ApkHelper {

    /**
     * Extracts and copies the currently running app's APK to cache, then launches
     * Android's system share sheet to share the .apk file directly via WhatsApp, Bluetooth,
     * Quick Share / Nearby Share, Telegram, Google Drive, Email, etc.
     */
    fun shareApkFile(context: Context) {
        try {
            val srcDir = context.applicationInfo.sourceDir
            val sourceApk = File(srcDir)
            if (!sourceApk.exists() || !sourceApk.canRead()) {
                Toast.makeText(context, "Não foi possível localizar o arquivo APK do aplicativo", Toast.LENGTH_LONG).show()
                return
            }

            val apksDir = File(context.cacheDir, "apks")
            if (!apksDir.exists()) {
                apksDir.mkdirs()
            }

            val targetApk = File(apksDir, "LeoMusicas.apk")
            sourceApk.copyTo(targetApk, overwrite = true)

            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetApk
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, apkUri)
                putExtra(Intent.EXTRA_SUBJECT, "Leo Músicas - Instalador / Atualização do App")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Instale ou atualize o aplicativo Leo Músicas no seu celular!\nAbra o arquivo .apk para instalar."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartilhar Aplicativo (APK)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Erro ao preparar arquivo APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Returns human readable size of the app APK file.
     */
    fun getApkSizeMb(context: Context): String {
        return try {
            val srcDir = context.applicationInfo.sourceDir
            val sourceApk = File(srcDir)
            if (sourceApk.exists()) {
                val bytes = sourceApk.length()
                val mb = bytes / (1024.0 * 1024.0)
                String.format(Locale.getDefault(), "%.1f MB", mb)
            } else {
                "~15 MB"
            }
        } catch (e: Exception) {
            "~15 MB"
        }
    }

    /**
     * Opens a URL in the external browser.
     */
    fun openUrl(context: Context, url: String) {
        try {
            val cleanUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) {
                "https://$url"
            } else {
                url
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Copies text to the clipboard and notifies user.
     */
    fun copyToClipboard(context: Context, text: String, message: String = "Link copiado com sucesso! 📋") {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Link do App", text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Falha ao copiar", Toast.LENGTH_SHORT).show()
        }
    }
}
