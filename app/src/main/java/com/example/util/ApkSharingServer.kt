package com.example.util

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket

/**
 * Lightweight local HTTP server that runs when the user is on the QR code screen
 * and connected to Wi-Fi or hotspot. Allows any device scanning the QR code to download
 * the exact APK directly from this phone without requiring external servers or internet.
 */
object ApkSharingServer {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    var serverPort: Int = 8080
        private set

    /**
     * Finds the device's local IPv4 address on Wi-Fi or tethering interface.
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = intf.inetAddresses ?: continue
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host != "127.0.0.1" && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Starts listening on available port and serves the installed APK.
     */
    fun start(context: Context, onStarted: (String?) -> Unit = {}) {
        stop()
        val appContext = context.applicationContext
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val portsToTry = listOf(8080, 8081, 8888, 9090, 0)
                var ss: ServerSocket? = null
                for (p in portsToTry) {
                    try {
                        ss = ServerSocket(p)
                        serverPort = ss.localPort
                        break
                    } catch (_: Exception) {}
                }
                if (ss == null) {
                    withContext(Dispatchers.Main) { onStarted(null) }
                    return@launch
                }
                serverSocket = ss

                val ip = getLocalIpAddress()
                val url = if (ip != null) "http://$ip:$serverPort/LeoMusicas.apk" else null
                withContext(Dispatchers.Main) { onStarted(url) }

                while (isActive && !ss.isClosed) {
                    try {
                        val clientSocket = ss.accept()
                        launch(Dispatchers.IO) {
                            handleClient(clientSocket, appContext)
                        }
                    } catch (e: Exception) {
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onStarted(null) }
            }
        }
    }

    private fun handleClient(client: Socket, context: Context) {
        try {
            client.soTimeout = 15000
            val input = client.getInputStream()
            val reader = input.bufferedReader()
            val requestLine = reader.readLine() ?: return

            // Consume rest of headers
            var line: String? = reader.readLine()
            while (!line.isNullOrEmpty()) {
                line = reader.readLine()
            }

            val apkPath = context.applicationInfo.sourceDir
            val apkFile = File(apkPath)
            val output = client.getOutputStream()

            if (apkFile.exists() && apkFile.canRead()) {
                val length = apkFile.length()
                val headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/vnd.android.package-archive\r\n" +
                        "Content-Disposition: attachment; filename=\"LeoMusicas.apk\"\r\n" +
                        "Content-Length: $length\r\n" +
                        "Accept-Ranges: bytes\r\n" +
                        "Connection: close\r\n\r\n"
                output.write(headers.toByteArray(Charsets.UTF_8))

                apkFile.inputStream().use { fileInput ->
                    fileInput.copyTo(output, bufferSize = 64 * 1024)
                }
                output.flush()
            } else {
                val notFound = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n"
                output.write(notFound.toByteArray(Charsets.UTF_8))
                output.flush()
            }
        } catch (_: Exception) {
            // Client closed connection or cancelled download
        } finally {
            try { client.close() } catch (_: Exception) {}
        }
    }

    /**
     * Stops the local HTTP server.
     */
    fun stop() {
        job?.cancel()
        job = null
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
    }
}
