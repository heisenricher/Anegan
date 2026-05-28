/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.wifitransfer

import android.content.Context
import android.os.Environment
import kotlinx.coroutines.*
import java.io.*
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FtpServer {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var job: Job? = null
    private const val PORT = 2121

    // Callbacks to communicate with UI
    var onServerStatusChanged: ((Boolean) -> Unit)? = null
    var onTransferProgress: ((String, Float, Boolean) -> Unit)? = null // name, progress, isUpload

    fun startServer(context: Context, scope: CoroutineScope) {
        if (isRunning) return
        isRunning = true
        onServerStatusChanged?.invoke(true)

        job = scope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(PORT)
                while (isActive && isRunning) {
                    val socket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(socket, context)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                stopServer()
            }
        }
    }

    fun stopServer() {
        if (!isRunning) return
        isRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        serverSocket = null
        job?.cancel()
        job = null
        onServerStatusChanged?.invoke(false)
    }

    fun isServerRunning(): Boolean = isRunning

    fun getIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getFtpUrl(): String {
        val ip = getIpAddress() ?: "localhost"
        return "ftp://$ip:$PORT"
    }

    private suspend fun handleClient(controlSocket: Socket, context: Context) = withContext(Dispatchers.IO) {
        var reader: BufferedReader? = null
        var writer: BufferedWriter? = null
        var passiveServerSocket: ServerSocket? = null
        var passiveDataSocket: Socket? = null
        
        // Expose directory Downloads/Anegan/
        val baseDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Anegan").apply {
            if (!exists()) mkdirs()
        }
        var currentRelPath = "" // Relative to baseDir
        
        try {
            reader = BufferedReader(InputStreamReader(controlSocket.getInputStream(), "UTF-8"))
            writer = BufferedWriter(OutputStreamWriter(controlSocket.getOutputStream(), "UTF-8"))

            fun sendResponse(code: String, text: String) {
                try {
                    writer?.write("$code $text\r\n")
                    writer?.flush()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            sendResponse("220", "Anegan Offline Secure FTP Server Ready.")

            while (isRunning && controlSocket.isConnected) {
                val line = reader.readLine() ?: break
                val tokens = line.trim().split(" ", limit = 2)
                val cmd = tokens[0].uppercase(Locale.ROOT)
                val arg = if (tokens.size > 1) tokens[1] else ""

                when (cmd) {
                    "USER" -> {
                        sendResponse("331", "Anonymous login allowed, send pass.")
                    }
                    "PASS" -> {
                        sendResponse("230", "User logged in, proceed.")
                    }
                    "SYST" -> {
                        sendResponse("215", "UNIX Type: L8")
                    }
                    "FEAT" -> {
                        writer?.write("211-Features:\r\n UTF8\r\n211 End\r\n")
                        writer?.flush()
                    }
                    "OPTS" -> {
                        if (arg.uppercase(Locale.ROOT).startsWith("UTF8")) {
                            sendResponse("200", "UTF8 option enabled.")
                        } else {
                            sendResponse("200", "Command OK.")
                        }
                    }
                    "PWD" -> {
                        val displayPath = if (currentRelPath.isEmpty()) "/" else "/$currentRelPath"
                        sendResponse("257", "\"$displayPath\" is current directory.")
                    }
                    "TYPE" -> {
                        sendResponse("200", "Type set to $arg.")
                    }
                    "PASV" -> {
                        try {
                            passiveServerSocket?.close()
                            passiveServerSocket = ServerSocket(0) // Listen on dynamic random port
                            val localPort = passiveServerSocket!!.localPort
                            val ip = getIpAddress() ?: "127.0.0.1"
                            val ipParts = ip.split(".")
                            if (ipParts.size == 4) {
                                val p1 = localPort / 256
                                val p2 = localPort % 256
                                val ipTuple = "${ipParts[0]},${ipParts[1]},${ipParts[2]},${ipParts[3]},$p1,$p2"
                                sendResponse("227", "Entering Passive Mode ($ipTuple)")
                            } else {
                                sendResponse("500", "Cannot establish passive port.")
                            }
                        } catch (e: Exception) {
                            sendResponse("500", "PASV establishment failed.")
                        }
                    }
                    "CWD" -> {
                        val newRel = resolveRelativePath(currentRelPath, arg)
                        val targetDir = File(baseDir, newRel)
                        if (isPathSafe(baseDir, targetDir) && targetDir.exists() && targetDir.isDirectory) {
                            currentRelPath = newRel
                            sendResponse("250", "Directory changed successfully.")
                        } else {
                            sendResponse("550", "Failed to change directory.")
                        }
                    }
                    "CDUP" -> {
                        val newRel = resolveRelativePath(currentRelPath, "..")
                        val targetDir = File(baseDir, newRel)
                        if (isPathSafe(baseDir, targetDir) && targetDir.exists() && targetDir.isDirectory) {
                            currentRelPath = newRel
                            sendResponse("250", "Directory changed successfully.")
                        } else {
                            sendResponse("550", "Failed to change directory.")
                        }
                    }
                    "LIST" -> {
                        val activeDir = File(baseDir, currentRelPath)
                        if (!isPathSafe(baseDir, activeDir) || !activeDir.exists() || !activeDir.isDirectory) {
                            sendResponse("550", "Cannot list directory.")
                            continue
                        }

                        sendResponse("150", "Here comes the directory listing.")
                        
                        try {
                            passiveDataSocket = passiveServerSocket?.accept()
                            if (passiveDataSocket != null) {
                                val dataOut = BufferedWriter(OutputStreamWriter(passiveDataSocket!!.getOutputStream(), "UTF-8"))
                                val files = activeDir.listFiles() ?: emptyArray()
                                val sdf = SimpleDateFormat("MMM dd HH:mm", Locale.ROOT)
                                
                                for (f in files) {
                                    if (f.name.startsWith(".")) continue
                                    val dateStr = sdf.format(Date(f.lastModified()))
                                    if (f.isDirectory) {
                                        dataOut.write("drwxr-xr-x    1 owner    group            0 $dateStr ${f.name}\r\n")
                                    } else {
                                        dataOut.write("-rw-r--r--    1 owner    group     ${f.length()} $dateStr ${f.name}\r\n")
                                    }
                                }
                                dataOut.flush()
                                passiveDataSocket!!.close()
                                passiveDataSocket = null
                                sendResponse("226", "Directory send OK.")
                            } else {
                                sendResponse("425", "Can't open data connection.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sendResponse("425", "Listing data connection failed.")
                        } finally {
                            passiveServerSocket?.close()
                            passiveServerSocket = null
                        }
                    }
                    "RETR" -> {
                        val targetFile = File(File(baseDir, currentRelPath), arg)
                        if (!isPathSafe(baseDir, targetFile) || !targetFile.exists() || !targetFile.isFile) {
                            sendResponse("550", "File not found.")
                            continue
                        }

                        sendResponse("150", "Opening BINARY mode data connection for ${targetFile.name} (${targetFile.length()} bytes).")

                        try {
                            passiveDataSocket = passiveServerSocket?.accept()
                            if (passiveDataSocket != null) {
                                val dataOut = BufferedOutputStream(passiveDataSocket!!.getOutputStream())
                                val fis = FileInputStream(targetFile)
                                val buffer = ByteArray(1024 * 64)
                                val fileSize = targetFile.length()
                                var totalWritten = 0L
                                var len: Int

                                onTransferProgress?.invoke(targetFile.name, 0.0f, false)

                                while (fis.read(buffer).also { len = it } != -1) {
                                    dataOut.write(buffer, 0, len)
                                    totalWritten += len
                                    val prog = if (fileSize > 0) totalWritten.toFloat() / fileSize else 0f
                                    onTransferProgress?.invoke(targetFile.name, prog, false)
                                }
                                dataOut.flush()
                                fis.close()
                                passiveDataSocket!!.close()
                                passiveDataSocket = null
                                
                                onTransferProgress?.invoke(targetFile.name, 1.0f, false)
                                sendResponse("226", "Transfer complete.")
                            } else {
                                sendResponse("425", "Can't open data connection.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sendResponse("425", "Download data connection failed.")
                        } finally {
                            passiveServerSocket?.close()
                            passiveServerSocket = null
                        }
                    }
                    "STOR" -> {
                        val targetFile = File(File(baseDir, currentRelPath), arg)
                        if (!isPathSafe(baseDir, targetFile)) {
                            sendResponse("550", "Permission denied.")
                            continue
                        }

                        sendResponse("150", "Ok to send data.")

                        try {
                            passiveDataSocket = passiveServerSocket?.accept()
                            if (passiveDataSocket != null) {
                                val dataIn = BufferedInputStream(passiveDataSocket!!.getInputStream())
                                val fos = FileOutputStream(targetFile)
                                val buffer = ByteArray(1024 * 64)
                                var len: Int
                                var totalRead = 0L

                                onTransferProgress?.invoke(targetFile.name, 0.0f, true)

                                while (dataIn.read(buffer).also { len = it } != -1) {
                                    fos.write(buffer, 0, len)
                                    totalRead += len
                                    // Progress estimated since exact upload content size isn't always given prior
                                    onTransferProgress?.invoke(targetFile.name, 0.5f, true)
                                }
                                fos.flush()
                                fos.close()
                                dataIn.close()
                                passiveDataSocket!!.close()
                                passiveDataSocket = null

                                try {
                                    android.media.MediaScannerConnection.scanFile(
                                        context,
                                        arrayOf(targetFile.absolutePath),
                                        null,
                                        null
                                    )
                                } catch (ex: Exception) {
                                    // Ignore scanner issues
                                }

                                onTransferProgress?.invoke(targetFile.name, 1.0f, true)
                                sendResponse("226", "Transfer complete.")
                            } else {
                                sendResponse("425", "Can't open data connection.")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            sendResponse("425", "Upload data connection failed.")
                        } finally {
                            passiveServerSocket?.close()
                            passiveServerSocket = null
                        }
                    }
                    "DELE" -> {
                        val targetFile = File(File(baseDir, currentRelPath), arg)
                        if (isPathSafe(baseDir, targetFile) && targetFile.exists() && targetFile.isFile) {
                            if (targetFile.delete()) {
                                sendResponse("250", "File deleted successfully.")
                            } else {
                                sendResponse("550", "Could not delete file.")
                            }
                        } else {
                            sendResponse("550", "File not found.")
                        }
                    }
                    "MKD" -> {
                        val targetDir = File(File(baseDir, currentRelPath), arg)
                        if (isPathSafe(baseDir, targetDir)) {
                            if (!targetDir.exists() && targetDir.mkdirs()) {
                                sendResponse("257", "\"$arg\" created.")
                            } else {
                                sendResponse("550", "Could not create directory.")
                            }
                        } else {
                            sendResponse("550", "Permission denied.")
                        }
                    }
                    "RMD" -> {
                        val targetDir = File(File(baseDir, currentRelPath), arg)
                        if (isPathSafe(baseDir, targetDir) && targetDir.exists() && targetDir.isDirectory) {
                            if (targetDir.deleteRecursively()) {
                                sendResponse("250", "Directory removed successfully.")
                            } else {
                                sendResponse("550", "Could not remove directory.")
                            }
                        } else {
                            sendResponse("550", "Directory not found.")
                        }
                    }
                    "NOOP" -> {
                        sendResponse("200", "OK")
                    }
                    "QUIT" -> {
                        sendResponse("221", "Goodbye.")
                        break
                    }
                    else -> {
                        sendResponse("502", "Command not implemented.")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                passiveDataSocket?.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                passiveServerSocket?.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                reader?.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                writer?.close()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                controlSocket.close()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun resolveRelativePath(currentRel: String, arg: String): String {
        val root = currentRel.split("/").filter { it.isNotEmpty() }.toMutableList()
        val segments = arg.split("/", "\\").filter { it.isNotEmpty() }
        for (seg in segments) {
            if (seg == "..") {
                if (root.isNotEmpty()) root.removeAt(root.size - 1)
            } else if (seg != ".") {
                root.add(seg)
            }
        }
        return root.joinToString("/")
    }

    private fun isPathSafe(baseDir: File, targetFile: File): Boolean {
        return try {
            val baseCanonical = baseDir.canonicalPath
            val targetCanonical = targetFile.canonicalPath
            targetCanonical.startsWith(baseCanonical)
        } catch (e: Exception) {
            false
        }
    }
}
