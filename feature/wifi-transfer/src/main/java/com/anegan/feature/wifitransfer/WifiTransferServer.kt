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
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return File(publicDir, "Anegan").apply {
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
            <title>Anegan Wi-Fi Transfer</title>
            <style>
                :root {
                    --primary: #1A237E;
                    --primary-light: #3F51B5;
                    --bg: #F5F6FA;
                    --card-bg: #FFFFFF;
                    --text: #2C3E50;
                    --gray: #7F8C8D;
                }
                * {
                    box-sizing: border-box;
                    margin: 0;
                    padding: 0;
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                }
                body {
                    background-color: var(--bg);
                    color: var(--text);
                    padding: 40px 20px;
                }
                .container {
                    max-width: 1000px;
                    margin: 0 auto;
                }
                header {
                    text-align: center;
                    margin-bottom: 40px;
                }
                header h1 {
                    color: var(--primary);
                    font-size: 2.5rem;
                    margin-bottom: 10px;
                }
                header p {
                    color: var(--gray);
                    font-size: 1.1rem;
                }
                .layout {
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 30px;
                }
                @media (max-width: 768px) {
                    .layout {
                        grid-template-columns: 1fr;
                    }
                }
                .card {
                    background: var(--card-bg);
                    padding: 30px;
                    border-radius: 20px;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.05);
                }
                .card h2 {
                    color: var(--primary);
                    margin-bottom: 20px;
                    font-size: 1.5rem;
                    display: flex;
                    align-items: center;
                    gap: 10px;
                }
                #drop-zone {
                    border: 3px dashed var(--primary-light);
                    border-radius: 20px;
                    padding: 40px 20px;
                    text-align: center;
                    cursor: pointer;
                    background-color: rgba(63, 81, 181, 0.02);
                    transition: all 0.3s ease;
                }
                #drop-zone:hover, #drop-zone.dragover {
                    background-color: rgba(63, 81, 181, 0.08);
                    border-color: var(--primary);
                }
                #drop-zone svg {
                    width: 64px;
                    height: 64px;
                    fill: var(--primary-light);
                    margin-bottom: 15px;
                }
                #file-list {
                    list-style: none;
                    max-height: 400px;
                    overflow-y: auto;
                    margin-top: 10px;
                }
                .file-item {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 15px;
                    border-bottom: 1px solid #ECEFF1;
                    transition: background 0.2s ease;
                }
                .file-item:hover {
                    background-color: #F8F9FA;
                }
                .file-info {
                    display: flex;
                    flex-direction: column;
                }
                .file-name {
                    font-weight: 600;
                    font-size: 0.95rem;
                    color: var(--primary);
                    margin-bottom: 4px;
                }
                .file-meta {
                    font-size: 0.8rem;
                    color: var(--gray);
                }
                .download-btn {
                    padding: 8px 16px;
                    background-color: var(--primary);
                    color: white;
                    text-decoration: none;
                    border-radius: 10px;
                    font-weight: 600;
                    font-size: 0.85rem;
                    transition: background 0.2s ease;
                }
                .download-btn:hover {
                    background-color: var(--primary-light);
                }
                .progress-bar {
                    width: 100%;
                    background-color: #ECEFF1;
                    height: 8px;
                    border-radius: 4px;
                    margin-top: 15px;
                    overflow: hidden;
                    display: none;
                }
                .progress-fill {
                    background-color: var(--primary-light);
                    height: 100%;
                    width: 0%;
                    transition: width 0.1s ease;
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
                        <h2>📤 Upload to Device</h2>
                        <div id="drop-zone" onclick="document.getElementById('file-input').click()">
                            <svg viewBox="0 0 24 24">
                                <path d="M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z"/>
                            </svg>
                            <p style="font-weight: 600; color: var(--primary); margin-bottom: 5px;">Drag & Drop files here</p>
                            <p style="font-size: 0.85rem; color: var(--gray);">or click to browse your computer</p>
                            <input type="file" id="file-input" style="display: none;" multiple>
                        </div>
                        <div class="progress-bar" id="progress-container">
                            <div class="progress-fill" id="progress-fill"></div>
                        </div>
                        <p id="upload-status" style="margin-top: 10px; font-weight: 600; text-align: center; color: var(--primary-light);"></p>
                    </div>
                    <div class="card">
                        <h2>📥 Files inside Anegan</h2>
                        <ul id="file-list">
                            <li style="text-align: center; padding: 40px 0; color: var(--gray);">Scanning folders...</li>
                        </ul>
                    </div>
                </div>
            </div>
            <script>
                const dropZone = document.getElementById('drop-zone');
                const fileInput = document.getElementById('file-input');
                const fileList = document.getElementById('file-list');
                const progressContainer = document.getElementById('progress-container');
                const progressFill = document.getElementById('progress-fill');
                const uploadStatus = document.getElementById('upload-status');

                async function loadFiles() {
                    try {
                        const res = await fetch('/api/files');
                        const files = await res.json();
                        fileList.innerHTML = '';
                        if (files.length === 0) {
                            fileList.innerHTML = '<li style="text-align: center; padding: 40px 0; color: var(--gray);">No files found in Anegan yet.</li>';
                            return;
                        }
                        files.forEach(file => {
                            const li = document.createElement('li');
                            li.className = 'file-item';
                            li.innerHTML = `
                                <div class="file-info">
                                    <span class="file-name">${"$"}{escapeHtml(file.name)}</span>
                                    <span class="file-meta">${"$"}{file.sizeMb} MB • ${"$"}{escapeHtml(file.extension.toUpperCase())}</span>
                                </div>
                                <a href="/download?path=${"$"}{encodeURIComponent(file.path)}" class="download-btn">Download</a>
                            `;
                            fileList.appendChild(li);
                        });
                    } catch (e) {
                        fileList.innerHTML = '<li style="text-align: center; padding: 40px 0; color: red;">Failed to load files.</li>';
                    }
                }

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
