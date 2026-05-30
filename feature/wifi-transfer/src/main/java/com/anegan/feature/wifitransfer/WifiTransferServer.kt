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
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.Locale

object WifiTransferServer {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private var job: Job? = null
    private const val PORT = 8080
    
    // Callback to notify the UI of active progress/updates
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
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    fun getWebUrl(): String {
        val ip = getIpAddress() ?: "localhost"
        return "http://$ip:$PORT"
    }

    private suspend fun handleClient(socket: Socket, context: Context) = withContext(Dispatchers.IO) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val output = BufferedOutputStream(socket.getOutputStream())
            
            // Read HTTP request header
            val requestLine = reader.readLine() ?: return@withContext
            val tokens = requestLine.split(" ")
            if (tokens.size < 3) return@withContext
            val method = tokens[0]
            val uri = URLDecoder.decode(tokens[1], "UTF-8")
            
            // Read remaining headers
            var contentLength = 0L
            var contentType = ""
            var line: String?
            while (true) {
                line = reader.readLine()
                if (line.isNullOrBlank()) break
                val lower = line.lowercase(Locale.ROOT)
                if (lower.startsWith("content-length:")) {
                    contentLength = lower.substringAfter("content-length:").trim().toLongOrNull() ?: 0L
                } else if (lower.startsWith("content-type:")) {
                    contentType = line.substringAfter("content-type:").trim()
                }
            }
            
            when {
                method == "GET" && uri == "/" -> {
                    // Serve modern web dashboard HTML
                    sendHtmlResponse(output, getDashboardHtml())
                }
                method == "GET" && uri == "/api/files" -> {
                    // Serve files in JSON format
                    sendJsonResponse(output, getFilesJson(context))
                }
                method == "GET" && uri.startsWith("/download") -> {
                    // Serve file download
                    val path = uri.substringAfter("?path=", "")
                    val baseDir = getBaseDir()
                    if (path.isNotBlank()) {
                        val file = File(path)
                        if (isPathSafe(baseDir, file) && file.exists() && file.isFile) {
                            sendFileResponse(output, file)
                        } else {
                            send404Response(output)
                        }
                    } else {
                        send404Response(output)
                    }
                }
                method == "POST" && uri == "/upload" -> {
                    // Handle multi-part file upload
                    val inputStream = socket.getInputStream()
                    val success = handleUpload(inputStream, contentType, contentLength, context)
                    if (success) {
                        sendJsonResponse(output, "{\"status\":\"success\"}")
                    } else {
                        sendJsonResponse(output, "{\"status\":\"error\"}")
                    }
                }
                else -> {
                    send404Response(output)
                }
            }
            
            output.flush()
            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun sendHtmlResponse(out: BufferedOutputStream, html: String) {
        val bytes = html.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(out)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: text/html; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        out.write(bytes)
    }
    
    private fun sendJsonResponse(out: BufferedOutputStream, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(out)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        out.write(bytes)
    }
    
    private fun sendFileResponse(out: BufferedOutputStream, file: File) {
        val size = file.length()
        val writer = PrintWriter(out)
        writer.print("HTTP/1.1 200 OK\r\n")
        writer.print("Content-Type: application/octet-stream\r\n")
        writer.print("Content-Disposition: attachment; filename=\"${file.name}\"\r\n")
        writer.print("Content-Length: $size\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        
        onTransferProgress?.invoke(file.name, 0.0f, false)
        
        val buffer = ByteArray(1024 * 64)
        var totalBytesWritten = 0L
        FileInputStream(file).use { fis ->
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                out.write(buffer, 0, bytesRead)
                totalBytesWritten += bytesRead
                val progress = if (size > 0) totalBytesWritten.toFloat() / size else 0f
                onTransferProgress?.invoke(file.name, progress, false)
            }
        }
        
        onTransferProgress?.invoke(file.name, 1.0f, false)
    }
    
    private fun send404Response(out: BufferedOutputStream) {
        val html = "<h1>404 Not Found</h1>"
        val bytes = html.toByteArray(Charsets.UTF_8)
        val writer = PrintWriter(out)
        writer.print("HTTP/1.1 404 Not Found\r\n")
        writer.print("Content-Type: text/html; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n")
        writer.print("\r\n")
        writer.flush()
        out.write(bytes)
    }

    private fun handleUpload(
        input: InputStream,
        contentType: String,
        contentLength: Long,
        context: Context
    ): Boolean {
        val boundary = contentType.substringAfter("boundary=").trim()
        if (boundary.isBlank()) return false
        
        try {
            val receiveFolder = File(getBaseDir(), "Received")
            if (!receiveFolder.exists()) receiveFolder.mkdirs()
            
            val boundaryBytes = "--$boundary".toByteArray(Charsets.UTF_8)
            val contentStream = PushbackInputStream(input, 1024 * 128)
            
            fun readHeaderLine(stream: InputStream): String {
                val sb = StringBuilder()
                var c: Int
                while (stream.read().also { c = it } != -1) {
                    if (c == '\r'.code) {
                        val next = stream.read()
                        if (next == '\n'.code) {
                            break
                        } else {
                            sb.append('\r')
                            if (next != -1) sb.append(next.toChar())
                        }
                    } else {
                        sb.append(c.toChar())
                    }
                }
                return sb.toString()
            }
            
            val firstLine = readHeaderLine(contentStream)
            if (!firstLine.contains(boundary)) return false
            
            var filename = "uploaded_file_${System.currentTimeMillis()}"
            while (true) {
                val headerLine = readHeaderLine(contentStream)
                if (headerLine.isBlank()) break
                
                if (headerLine.lowercase(Locale.ROOT).startsWith("content-disposition:")) {
                    val fn = headerLine.substringAfter("filename=\"", "").substringBefore("\"")
                    if (fn.isNotBlank()) {
                        filename = sanitizeFileName(fn)
                    }
                }
            }
            
            val targetFile = File(receiveFolder, filename)
            if (!isPathSafe(receiveFolder, targetFile)) return false

            val fos = FileOutputStream(targetFile)
            val outputStream = BufferedOutputStream(fos)
            
            val separator = "\r\n--$boundary".toByteArray(Charsets.UTF_8)
            val separatorLen = separator.size
            
            val circularBuffer = ByteArray(separatorLen)
            var bufferIndex = 0
            var bufferFull = false
            
            onTransferProgress?.invoke(filename, 0.0f, true)
            
            var bytesWritten = 0L
            var currentByte: Int
            while (contentStream.read().also { currentByte = it } != -1) {
                if (bufferFull) {
                    val oldestByte = circularBuffer[bufferIndex]
                    outputStream.write(oldestByte.toInt())
                    bytesWritten++
                    
                    val progress = if (contentLength > 0) bytesWritten.toFloat() / contentLength else 0f
                    onTransferProgress?.invoke(filename, progress, true)
                }
                
                circularBuffer[bufferIndex] = currentByte.toByte()
                bufferIndex = (bufferIndex + 1) % separatorLen
                if (bufferIndex == 0) {
                    bufferFull = true
                }
                
                if (bufferFull) {
                    var matches = true
                    for (i in 0 until separatorLen) {
                        val idx = (bufferIndex + i) % separatorLen
                        if (circularBuffer[idx] != separator[i]) {
                            matches = false
                            break
                        }
                    }
                    if (matches) {
                        break
                    }
                }
            }
            
            outputStream.flush()
            outputStream.close()
            fos.close()
            
            onTransferProgress?.invoke(filename, 1.0f, true)
            
            try {
                android.media.MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    null,
                    null
                )
            } catch (e: Exception) {
                // Ignore
            }
            
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun getFilesJson(context: Context): String {
        val root = getBaseDir()
        val folders = listOf(
            root,
            File(root, "Received"),
            File(root, "Documents"),
            File(root, "Video"),
            File(root, "Audio"),
            File(root, "Images")
        )
        
        val files = mutableListOf<File>()
        for (folder in folders) {
            if (folder.exists() && folder.isDirectory) {
                folder.listFiles()?.forEach { 
                    if (it.isFile && !it.name.startsWith(".")) {
                        files.add(it)
                    }
                }
            }
        }
        
        val sb = StringBuilder()
        sb.append("[")
        files.distinctBy { it.absolutePath }.forEachIndexed { index, file ->
            if (index > 0) sb.append(",")
            val sizeMb = file.length() / (1024f * 1024f)
            val cleanName = file.name.replace("\"", "\\\"")
            val cleanPath = file.absolutePath.replace("\\", "/").replace("\"", "\\\"")
            sb.append("{")
            sb.append("\"name\":\"$cleanName\",")
            sb.append("\"path\":\"$cleanPath\",")
            sb.append("\"sizeMb\":${String.format(Locale.ROOT, "%.2f", sizeMb)},")
            sb.append("\"extension\":\"${file.extension}\",")
            sb.append("\"lastModified\":${file.lastModified()}")
            sb.append("}")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun getBaseDir(): File {
        return File(Environment.getExternalStorageDirectory(), "Anegan").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun sanitizeFileName(fileName: String): String {
        val cleaned = fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .replace(Regex("[\r\n\u0000]"), "")
            .replace("..", "_")
            .trim()

        return cleaned.ifBlank { "uploaded_file_${System.currentTimeMillis()}" }
    }

    private fun isPathSafe(baseDir: File, targetFile: File): Boolean {
        return try {
            val baseCanonical = baseDir.canonicalPath
            val targetCanonical = targetFile.canonicalPath
            targetCanonical == baseCanonical || targetCanonical.startsWith(baseCanonical + File.separator)
        } catch (e: Exception) {
            false
        }
    }
    
    private fun getDashboardHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Anegan Local Transfer Portal</title>
            <link href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&family=Outfit:wght@400;500;600;700;800&display=swap" rel="stylesheet">
            <style>
                :root {
                    --primary-grad: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    --accent: #764ba2;
                    --bg: #f8fafc;
                    --card-bg: #ffffff;
                    --text-main: #0f172a;
                    --text-muted: #64748b;
                    --border: #e2e8f0;
                    --success: #10b981;
                }
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                }
                body {
                    font-family: 'Inter', sans-serif;
                    background-color: var(--bg);
                    color: var(--text-main);
                    min-height: 100vh;
                    display: flex;
                    flex-direction: column;
                    padding: 40px 20px;
                }
                .container {
                    max-width: 1100px;
                    margin: 0 auto;
                    width: 100%;
                }
                header {
                    background: var(--primary-grad);
                    color: white;
                    padding: 40px;
                    border-radius: 24px;
                    text-align: center;
                    margin-bottom: 40px;
                    box-shadow: 0 10px 30px rgba(102, 126, 234, 0.2);
                    position: relative;
                    overflow: hidden;
                }
                header::before {
                    content: '';
                    position: absolute;
                    top: -50%;
                    left: -50%;
                    width: 200%;
                    height: 200%;
                    background: radial-gradient(circle, rgba(255,255,255,0.1) 0%, transparent 80%);
                    pointer-events: none;
                }
                header h1 {
                    font-family: 'Outfit', sans-serif;
                    font-size: 2.8rem;
                    font-weight: 800;
                    margin-bottom: 12px;
                    letter-spacing: -0.5px;
                }
                header p {
                    font-size: 1.1rem;
                    opacity: 0.9;
                    font-weight: 400;
                }
                .layout {
                    display: grid;
                    grid-template-columns: 1.2fr 1.8fr;
                    gap: 30px;
                }
                @media (max-width: 900px) {
                    .layout {
                        grid-template-columns: 1fr;
                    }
                }
                .card {
                    background: var(--card-bg);
                    padding: 30px;
                    border-radius: 24px;
                    border: 1px solid var(--border);
                    box-shadow: 0 4px 20px rgba(0,0,0,0.02);
                    display: flex;
                    flex-direction: column;
                }
                .card h2 {
                    font-family: 'Outfit', sans-serif;
                    color: var(--text-main);
                    margin-bottom: 24px;
                    font-size: 1.5rem;
                    font-weight: 700;
                    display: flex;
                    align-items: center;
                    gap: 12px;
                }
                .card h2 svg {
                    width: 24px;
                    height: 24px;
                    fill: none;
                    stroke: currentColor;
                    stroke-width: 2;
                }
                #drop-zone {
                    border: 2px dashed #cbd5e1;
                    border-radius: 20px;
                    padding: 50px 20px;
                    text-align: center;
                    cursor: pointer;
                    background-color: #f8fafc;
                    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                }
                #drop-zone:hover, #drop-zone.dragover {
                    border-color: #667eea;
                    background-color: #f1f5f9;
                    transform: scale(0.99);
                }
                #drop-zone svg {
                    width: 64px;
                    height: 64px;
                    fill: none;
                    stroke: #667eea;
                    stroke-width: 1.5;
                    margin-bottom: 20px;
                    transition: transform 0.3s ease;
                }
                #drop-zone:hover svg {
                    transform: translateY(-5px);
                }
                .upload-title {
                    font-weight: 600;
                    font-size: 1.1rem;
                    color: var(--text-main);
                    margin-bottom: 6px;
                }
                .upload-sub {
                    font-size: 0.9rem;
                    color: var(--text-muted);
                }
                .progress-bar {
                    width: 100%;
                    background-color: #f1f5f9;
                    height: 10px;
                    border-radius: 10px;
                    margin-top: 24px;
                    overflow: hidden;
                    display: none;
                }
                .progress-fill {
                    background: var(--primary-grad);
                    height: 100%;
                    width: 0%;
                    transition: width 0.1s ease;
                }
                #upload-status {
                    margin-top: 12px;
                    font-weight: 600;
                    text-align: center;
                    color: var(--accent);
                    font-size: 0.95rem;
                }
                .search-container {
                    margin-bottom: 16px;
                    position: relative;
                    width: 100%;
                }
                .search-input {
                    width: 100%;
                    padding: 12px 16px 12px 42px;
                    border-radius: 12px;
                    border: 1px solid var(--border);
                    font-family: inherit;
                    font-size: 0.95rem;
                    outline: none;
                    transition: border-color 0.2s;
                }
                .search-input:focus {
                    border-color: #667eea;
                }
                .search-icon {
                    position: absolute;
                    left: 14px;
                    top: 50%;
                    transform: translateY(-50%);
                    width: 18px;
                    height: 18px;
                    stroke: var(--text-muted);
                    stroke-width: 2;
                    fill: none;
                }
                #file-list {
                    list-style: none;
                    max-height: 500px;
                    overflow-y: auto;
                    padding-right: 4px;
                }
                #file-list::-webkit-scrollbar {
                    width: 6px;
                }
                #file-list::-webkit-scrollbar-thumb {
                    background-color: #cbd5e1;
                    border-radius: 3px;
                }
                .file-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 14px 16px;
                    border: 1px solid var(--border);
                    border-radius: 16px;
                    margin-bottom: 10px;
                    transition: all 0.2s ease;
                }
                .file-item:hover {
                    border-color: #cbd5e1;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.01);
                    background-color: #f8fafc;
                }
                .file-details {
                    display: flex;
                    align-items: center;
                    gap: 12px;
                    min-width: 0;
                }
                .file-icon {
                    width: 44px;
                    height: 44px;
                    border-radius: 12px;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    font-weight: 800;
                    font-size: 0.75rem;
                    flex-shrink: 0;
                }
                .ext-pdf { background-color: #ffebee; color: #c62828; }
                .ext-doc, .ext-docx, .ext-txt { background-color: #e3f2fd; color: #1565c0; }
                .ext-xls, .ext-xlsx { background-color: #e8f5e9; color: #2e7d32; }
                .ext-mp4, .ext-mkv, .ext-avi { background-color: #fff3e0; color: #ef6c00; }
                .ext-mp3, .ext-wav, .ext-flac { background-color: #f3e5f5; color: #6a1b9a; }
                .ext-zip, .ext-rar, .ext-7z { background-color: #efebe9; color: #4e342e; }
                .ext-jpg, .ext-png, .ext-webp { background-color: #f1f8e9; color: #33691e; }
                .ext-apk { background-color: #e8f5e9; color: #1b5e20; }
                .ext-other { background-color: #eceff1; color: #37474f; }

                .file-info {
                    min-width: 0;
                }
                .file-name {
                    font-weight: 600;
                    font-size: 0.95rem;
                    color: var(--text-main);
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    display: block;
                    margin-bottom: 2px;
                }
                .file-meta {
                    font-size: 0.8rem;
                    color: var(--text-muted);
                }
                .download-btn {
                    padding: 8px 16px;
                    background: var(--primary-grad);
                    color: white;
                    text-decoration: none;
                    border-radius: 10px;
                    font-weight: 600;
                    font-size: 0.85rem;
                    transition: opacity 0.2s ease;
                    flex-shrink: 0;
                    box-shadow: 0 4px 10px rgba(102, 126, 234, 0.15);
                }
                .download-btn:hover {
                    opacity: 0.9;
                }
                footer {
                    margin-top: auto;
                    text-align: center;
                    padding-top: 40px;
                    color: var(--text-muted);
                    font-size: 0.9rem;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <header>
                    <h1>🛡️ Anegan</h1>
                    <p>High-speed, 100% offline local Wi-Fi file sharing workspace</p>
                </header>
                <div class="layout">
                    <div class="card">
                        <h2>
                            <svg viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z"/></svg>
                            Upload to Device
                        </h2>
                        <div id="drop-zone" onclick="document.getElementById('file-input').click()">
                            <svg viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z"/>
                            </svg>
                            <p class="upload-title">Drag & Drop files here</p>
                            <p class="upload-sub">or click to browse your computer</p>
                            <input type="file" id="file-input" style="display: none;" multiple>
                        </div>
                        <div class="progress-bar" id="progress-container">
                            <div class="progress-fill" id="progress-fill"></div>
                        </div>
                        <p id="upload-status"></p>
                    </div>
                    <div class="card">
                        <h2>
                            <svg viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M2.25 12.75V12A9 9 0 0112 3v.75m-9.75 9h9.75M3 12.75a9 9 0 009-9M3 12.75h9.75m0 0A9 9 0 0021 12v-.75m-9.75 9h9.75M12 21a9 9 0 009-9M12 21h9.75m-9.75 0V12m0 9v-.75m0 0h9.75"/></svg>
                            Files in Anegan
                        </h2>
                        <div class="search-container">
                            <svg class="search-icon" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/></svg>
                            <input type="text" id="search-input" class="search-input" placeholder="Search files...">
                        </div>
                        <ul id="file-list">
                            <li style="text-align: center; padding: 40px 0; color: var(--text-muted);">Scanning folders...</li>
                        </ul>
                    </div>
                </div>
                <footer>
                    <p>© 2026 Mahilan (heisenricher) — Anegan Workspace</p>
                </footer>
            </div>
            <script>
                const dropZone = document.getElementById('drop-zone');
                const fileInput = document.getElementById('file-input');
                const fileList = document.getElementById('file-list');
                const progressContainer = document.getElementById('progress-container');
                const progressFill = document.getElementById('progress-fill');
                const uploadStatus = document.getElementById('upload-status');
                let allFilesList = [];

                async function loadFiles() {
                    try {
                        const res = await fetch('/api/files');
                        allFilesList = await res.json();
                        renderFiles(allFilesList);
                    } catch (e) {
                        fileList.innerHTML = '<li style="text-align: center; padding: 40px 0; color: red;">Failed to load files.</li>';
                    }
                }

                function renderFiles(files) {
                    fileList.innerHTML = '';
                    if (files.length === 0) {
                        fileList.innerHTML = '<li style="text-align: center; padding: 40px 0; color: var(--text-muted);">No matching files found.</li>';
                        return;
                    }
                    files.forEach(file => {
                        const ext = file.extension.toLowerCase();
                        let extClass = 'ext-other';
                        if (['pdf'].includes(ext)) extClass = 'ext-pdf';
                        else if (['doc', 'docx', 'txt', 'rtf'].includes(ext)) extClass = 'ext-doc';
                        else if (['xls', 'xlsx', 'csv'].includes(ext)) extClass = 'ext-xls';
                        else if (['mp4', 'mkv', 'avi', 'mov', 'webm'].includes(ext)) extClass = 'ext-mp4';
                        else if (['mp3', 'wav', 'flac', 'aac', 'ogg', 'opus'].includes(ext)) extClass = 'ext-mp3';
                        else if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) extClass = 'ext-zip';
                        else if (['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'].includes(ext)) extClass = 'ext-jpg';
                        else if (['apk'].includes(ext)) extClass = 'ext-apk';

                        const li = document.createElement('li');
                        li.className = 'file-item';
                        li.innerHTML = `
                            <div class="file-details">
                                <div class="file-icon ${"$"}{extClass}">${"$"}{escapeHtml(ext.toUpperCase() || 'FILE')}</div>
                                <div class="file-info">
                                    <span class="file-name" title="${"$"}{escapeHtml(file.name)}">${"$"}{escapeHtml(file.name)}</span>
                                    <span class="file-meta">${"$"}{file.sizeMb} MB • Modified: ${"$"}{new Date(file.lastModified).toLocaleDateString()}</span>
                                </div>
                            </div>
                            <a href="/download?path=${"$"}{encodeURIComponent(file.path)}" class="download-btn">Download</a>
                        `;
                        fileList.appendChild(li);
                    });
                }

                const searchInput = document.getElementById('search-input');
                searchInput.addEventListener('input', (e) => {
                    const query = e.target.value.toLowerCase();
                    const filtered = allFilesList.filter(file => 
                        file.name.toLowerCase().includes(query) || 
                        file.extension.toLowerCase().includes(query)
                    );
                    renderFiles(filtered);
                });

                function escapeHtml(str) {
                    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
                }

                async function uploadFiles(files) {
                    if (files.length === 0) return;
                    
                    progressContainer.style.display = 'block';
                    for (let i = 0; i < files.length; i++) {
                        const file = files[i];
                        uploadStatus.innerText = `Uploading ${"$"}{file.name}...`;
                        
                        const formData = new FormData();
                        formData.append('file', file);
                        
                        try {
                            const xhr = new XMLHttpRequest();
                            xhr.open('POST', '/upload', true);
                            
                            xhr.upload.onprogress = function(e) {
                                if (e.lengthComputable) {
                                    const percent = (e.loaded / e.total) * 100;
                                    progressFill.style.width = percent + '%';
                                }
                            };
                            
                            await new Promise((resolve, reject) => {
                                xhr.onload = () => {
                                    if (xhr.status === 200) resolve();
                                    else reject();
                                };
                                xhr.onerror = () => reject();
                                xhr.send(formData);
                            });
                            
                        } catch (e) {
                            alert(`Failed to upload ${"$"}{file.name}`);
                        }
                    }
                    
                    uploadStatus.innerText = 'Upload completed!';
                    progressFill.style.width = '0%';
                    progressContainer.style.display = 'none';
                    loadFiles();
                }

                fileInput.addEventListener('change', () => uploadFiles(fileInput.files));

                ['dragenter', 'dragover'].forEach(eventName => {
                    dropZone.addEventListener(eventName, e => {
                        e.preventDefault();
                        dropZone.classList.add('dragover');
                    }, false);
                });

                ['dragleave', 'drop'].forEach(eventName => {
                    dropZone.addEventListener(eventName, e => {
                        e.preventDefault();
                        dropZone.classList.remove('dragover');
                    }, false);
                });

                dropZone.addEventListener('drop', e => {
                    const dt = e.dataTransfer;
                    const files = dt.files;
                    uploadFiles(files);
                }, false);

                loadFiles();
            </script>
        </body>
        </html>
        """.trimIndent()
    }
}
