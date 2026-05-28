/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.smbshare

import kotlinx.coroutines.*
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * Pure Kotlin SMB1/CIFS client for local-network file sharing.
 *
 * Implements a minimal subset of the SMB1 protocol:
 *   NEGOTIATE, SESSION_SETUP_ANDX, TREE_CONNECT_ANDX,
 *   TRANS2 (FIND_FIRST2/FIND_NEXT2), NT_CREATE_ANDX,
 *   READ_ANDX, WRITE_ANDX, CLOSE,
 *   TREE_DISCONNECT, LOGOFF_ANDX
 */
class SmbClient {

    // ── Connection state ──────────────────────────────────────────────────
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    private var uid: Int = 0          // Session user ID
    private var tid: Int = 0          // Tree connection ID
    private var mid: Int = 1          // Multiplex ID (incremented per request)
    private var maxBufferSize: Int = 16644
    private var sessionKey: Int = 0

    var isConnected: Boolean = false
        private set

    // ── SMB1 Constants ────────────────────────────────────────────────────
    companion object {
        private const val SMB_PORT = 445
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 15_000

        // SMB header signature
        private val SMB_MAGIC = byteArrayOf(0xFF.toByte(), 'S'.code.toByte(), 'M'.code.toByte(), 'B'.code.toByte())

        // Commands
        private const val SMB_COM_NEGOTIATE: Byte = 0x72
        private const val SMB_COM_SESSION_SETUP_ANDX: Byte = 0x73
        private const val SMB_COM_TREE_CONNECT_ANDX: Byte = 0x75
        private const val SMB_COM_TREE_DISCONNECT: Byte = 0x71
        private const val SMB_COM_LOGOFF_ANDX: Byte = 0x74
        private const val SMB_COM_TRANSACTION2: Byte = 0x32
        private const val SMB_COM_NT_CREATE_ANDX: Byte = 0xA2.toByte()
        private const val SMB_COM_READ_ANDX: Byte = 0x2E
        private const val SMB_COM_WRITE_ANDX: Byte = 0x2F
        private const val SMB_COM_CLOSE: Byte = 0x04

        // TRANS2 sub-commands
        private const val TRANS2_FIND_FIRST2: Short = 0x0001
        private const val TRANS2_FIND_NEXT2: Short = 0x0002

        // Flags
        private const val FLAGS_CANONICAL_PATHNAMES = 0x10
        private const val FLAGS_CASELESS_PATHNAMES = 0x08
        private const val FLAGS2_UNICODE = 0x8000
        private const val FLAGS2_NT_STATUS = 0x4000
        private const val FLAGS2_LONG_NAMES = 0x0001
        private const val FLAGS2_EXT_SEC = 0x0800

        // File attributes
        private const val FILE_ATTR_DIRECTORY = 0x10
        private const val FILE_ATTR_NORMAL = 0x80

        // NT_CREATE disposition / access
        private const val FILE_OPEN = 0x01
        private const val FILE_CREATE = 0x02
        private const val FILE_OVERWRITE_IF = 0x05
        private const val GENERIC_READ = 0x80000000.toInt()
        private const val GENERIC_WRITE = 0x40000000
        private const val FILE_SHARE_READ = 0x01
        private const val FILE_SHARE_WRITE = 0x02
    }

    // ── Data Classes ──────────────────────────────────────────────────────
    data class SmbFile(
        val name: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    // ── Public API ────────────────────────────────────────────────────────

    /**
     * Connect to an SMB share.
     * @param host  IP address or hostname of the SMB server
     * @param share Share name (e.g. "SharedFolder")
     * @param user  Username (empty for guest/anonymous)
     * @param pass  Password (empty for guest)
     */
    suspend fun connect(
        host: String,
        share: String,
        user: String = "",
        pass: String = ""
    ) = withContext(Dispatchers.IO) {
        disconnect()

        try {
            val sock = Socket()
            sock.connect(InetSocketAddress(host, SMB_PORT), CONNECT_TIMEOUT_MS)
            sock.soTimeout = READ_TIMEOUT_MS
            socket = sock
            outputStream = BufferedOutputStream(sock.getOutputStream())
            inputStream = BufferedInputStream(sock.getInputStream())

            // Protocol handshake
            negotiate()
            sessionSetup(user, pass)
            treeConnect(host, share)

            isConnected = true
        } catch (e: Exception) {
            disconnect()
            throw e
        }
    }

    /**
     * Disconnect from the SMB share.
     */
    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            if (isConnected) {
                try { treeDisconnect() } catch (_: Exception) {}
                try { logoff() } catch (_: Exception) {}
            }
        } finally {
            isConnected = false
            try { socket?.close() } catch (_: Exception) {}
            socket = null
            outputStream = null
            inputStream = null
            uid = 0
            tid = 0
        }
    }

    /**
     * List files in a directory on the remote share.
     * @param path  Remote directory path (e.g. "" for root, "Documents\\Sub")
     */
    suspend fun listFiles(path: String = ""): List<SmbFile> = withContext(Dispatchers.IO) {
        ensureConnected()
        val searchPath = if (path.isEmpty()) "\\*" else "\\${path.replace('/', '\\')}\\*"
        findFirst2(searchPath)
    }

    /**
     * Download a file from the remote share.
     * @param remotePath  Path on the share (e.g. "Documents\\readme.txt")
     * @param localFile   Local File to write to
     * @param onProgress  Progress callback (0.0 to 1.0)
     */
    suspend fun downloadFile(
        remotePath: String,
        localFile: File,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        ensureConnected()
        val smbPath = "\\${remotePath.replace('/', '\\')}"

        val fid = ntCreateFile(smbPath, GENERIC_READ, FILE_SHARE_READ, FILE_OPEN)
        try {
            FileOutputStream(localFile).use { fos ->
                val bufOut = BufferedOutputStream(fos)
                var offset = 0L
                val chunkSize = (maxBufferSize - 256).coerceAtMost(60000)
                var fileSize = -1L

                while (true) {
                    val data = readFile(fid, offset, chunkSize)
                    if (data.isEmpty()) break
                    bufOut.write(data)
                    offset += data.size

                    if (fileSize <= 0L && offset > 0) {
                        // Estimate from first read; progress will be approximate
                        fileSize = if (data.size < chunkSize) offset else offset * 4
                    }
                    if (fileSize > 0L) {
                        onProgress?.invoke((offset.toFloat() / fileSize).coerceAtMost(0.99f))
                    }
                }
                bufOut.flush()
                onProgress?.invoke(1.0f)
            }
        } finally {
            try { closeFile(fid) } catch (_: Exception) {}
        }
    }

    /**
     * Upload a file to the remote share.
     * @param localFile   Local file to upload
     * @param remotePath  Destination path on the share
     * @param onProgress  Progress callback (0.0 to 1.0)
     */
    suspend fun uploadFile(
        localFile: File,
        remotePath: String,
        onProgress: ((Float) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        ensureConnected()
        val smbPath = "\\${remotePath.replace('/', '\\')}"

        val fid = ntCreateFile(
            smbPath,
            GENERIC_READ or GENERIC_WRITE,
            FILE_SHARE_READ or FILE_SHARE_WRITE,
            FILE_OVERWRITE_IF
        )
        try {
            val totalSize = localFile.length()
            var offset = 0L
            val chunkSize = (maxBufferSize - 256).coerceAtMost(60000)

            FileInputStream(localFile).use { fis ->
                val buffer = ByteArray(chunkSize)
                while (true) {
                    val bytesRead = fis.read(buffer)
                    if (bytesRead <= 0) break
                    val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                    writeFile(fid, offset, chunk)
                    offset += bytesRead
                    if (totalSize > 0) {
                        onProgress?.invoke((offset.toFloat() / totalSize).coerceAtMost(0.99f))
                    }
                }
            }
            onProgress?.invoke(1.0f)
        } finally {
            try { closeFile(fid) } catch (_: Exception) {}
        }
    }

    // ── SMB Protocol Implementation ───────────────────────────────────────

    private fun ensureConnected() {
        if (!isConnected) throw IOException("Not connected to SMB share")
    }

    private fun nextMid(): Int {
        val m = mid
        mid = (mid + 1) and 0xFFFF
        return m
    }

    // ── Packet Builder ────────────────────────────────────────────────────

    /**
     * Build an SMB1 request packet.
     * Layout: [4-byte NetBIOS header][32-byte SMB header][params][data]
     */
    private fun buildPacket(
        command: Byte,
        wordCount: Int,
        paramWords: ByteArray,
        dataBytes: ByteArray,
        flags: Int = FLAGS_CANONICAL_PATHNAMES or FLAGS_CASELESS_PATHNAMES,
        flags2: Int = FLAGS2_UNICODE or FLAGS2_NT_STATUS or FLAGS2_LONG_NAMES
    ): ByteArray {
        val smbHeaderSize = 32
        val paramSize = 1 + paramWords.size   // WordCount(1) + words
        val dataSize = 2 + dataBytes.size      // ByteCount(2) + data
        val smbPayloadSize = smbHeaderSize + paramSize + dataSize

        val buf = ByteBuffer.allocate(4 + smbPayloadSize).order(ByteOrder.LITTLE_ENDIAN)

        // NetBIOS session header (big-endian length)
        buf.put(0x00.toByte())
        buf.put(((smbPayloadSize shr 16) and 0xFF).toByte())
        buf.put(((smbPayloadSize shr 8) and 0xFF).toByte())
        buf.put((smbPayloadSize and 0xFF).toByte())

        // SMB header
        buf.put(SMB_MAGIC)
        buf.put(command)
        buf.putInt(0) // Status
        buf.put(flags.toByte())
        buf.putShort(flags2.toShort())
        buf.putShort(0) // PID high
        buf.putLong(0)  // Signature (8 bytes)
        buf.putShort(0) // Reserved
        buf.putShort(tid.toShort())
        buf.putShort(android.os.Process.myPid().toShort()) // PID low
        buf.putShort(uid.toShort())
        buf.putShort(nextMid().toShort())

        // Parameter words
        buf.put(wordCount.toByte())
        buf.put(paramWords)

        // Data bytes
        buf.putShort(dataBytes.size.toShort())
        buf.put(dataBytes)

        return buf.array()
    }

    // ── Packet Reader ─────────────────────────────────────────────────────

    private data class SmbResponse(
        val command: Byte,
        val status: Int,
        val flags: Int,
        val flags2: Int,
        val returnedTid: Int,
        val returnedUid: Int,
        val returnedMid: Int,
        val wordCount: Int,
        val paramWords: ByteArray,
        val dataBytes: ByteArray
    )

    private fun readResponse(): SmbResponse {
        val stream = inputStream ?: throw IOException("Not connected")

        // Read NetBIOS header (4 bytes)
        val nbHeader = readExact(stream, 4)
        val length = ((nbHeader[1].toInt() and 0xFF) shl 16) or
                ((nbHeader[2].toInt() and 0xFF) shl 8) or
                (nbHeader[3].toInt() and 0xFF)

        if (length < 32) throw IOException("SMB response too short: $length bytes")

        val payload = readExact(stream, length)
        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)

        // Verify magic
        val magic = ByteArray(4)
        buf.get(magic)
        if (!magic.contentEquals(SMB_MAGIC)) {
            throw IOException("Invalid SMB magic in response")
        }

        val command = buf.get()
        val status = buf.getInt()
        val flags = buf.get().toInt() and 0xFF
        val flags2 = buf.getShort().toInt() and 0xFFFF
        buf.getShort() // PID high
        buf.getLong()   // Signature
        buf.getShort()  // Reserved
        val retTid = buf.getShort().toInt() and 0xFFFF
        buf.getShort()  // PID low
        val retUid = buf.getShort().toInt() and 0xFFFF
        val retMid = buf.getShort().toInt() and 0xFFFF

        // Read parameter words
        val wordCount = buf.get().toInt() and 0xFF
        val paramWords = ByteArray(wordCount * 2)
        if (paramWords.isNotEmpty()) buf.get(paramWords)

        // Read data bytes
        val byteCount = if (buf.remaining() >= 2) buf.getShort().toInt() and 0xFFFF else 0
        val dataBytes = ByteArray(byteCount.coerceAtMost(buf.remaining()))
        if (dataBytes.isNotEmpty()) buf.get(dataBytes)

        if (status != 0 && status != 0x00000006 /* MORE_ENTRIES */) {
            val statusHex = String.format("0x%08X", status)
            throw IOException("SMB error: status=$statusHex, command=0x${String.format("%02X", command)}")
        }

        return SmbResponse(command, status, flags, flags2, retTid, retUid, retMid, wordCount, paramWords, dataBytes)
    }

    private fun readExact(stream: InputStream, count: Int): ByteArray {
        val data = ByteArray(count)
        var offset = 0
        while (offset < count) {
            val n = stream.read(data, offset, count - offset)
            if (n < 0) throw IOException("Connection closed while reading SMB response")
            offset += n
        }
        return data
    }

    private fun sendPacket(packet: ByteArray) {
        val out = outputStream ?: throw IOException("Not connected")
        out.write(packet)
        out.flush()
    }

    // ── NEGOTIATE ─────────────────────────────────────────────────────────

    private fun negotiate() {
        // Offer NT LM 0.12 dialect
        val dialectStr = "NT LM 0.12"
        val dialectBytes = ByteArray(2 + dialectStr.length)
        dialectBytes[0] = 0x02 // Dialect buffer format
        System.arraycopy(dialectStr.toByteArray(Charsets.US_ASCII), 0, dialectBytes, 1, dialectStr.length)
        dialectBytes[dialectBytes.size - 1] = 0x00 // Null terminator

        val packet = buildPacket(
            command = SMB_COM_NEGOTIATE,
            wordCount = 0,
            paramWords = ByteArray(0),
            dataBytes = dialectBytes,
            flags2 = FLAGS2_NT_STATUS or FLAGS2_LONG_NAMES
        )
        sendPacket(packet)

        val resp = readResponse()
        if (resp.wordCount >= 17) {
            val params = ByteBuffer.wrap(resp.paramWords).order(ByteOrder.LITTLE_ENDIAN)
            params.getShort() // Dialect index
            params.get()      // Security mode
            params.getShort() // Max MPX count
            params.getShort() // Max number VCs
            maxBufferSize = params.getInt()
            params.getInt()   // Max raw size
            sessionKey = params.getInt()
        }
    }

    // ── SESSION SETUP ─────────────────────────────────────────────────────

    private fun sessionSetup(user: String, pass: String) {
        val passwordBytes = if (pass.isEmpty()) byteArrayOf(0x00) else (pass + "\u0000").toByteArray(Charsets.UTF_8)
        val userBytes = (user.uppercase(Locale.ROOT) + "\u0000").toByteArray(Charsets.UTF_8)
        val domainBytes = "?\u0000".toByteArray(Charsets.UTF_8)
        val osBytes = "Anegan\u0000".toByteArray(Charsets.UTF_8)
        val lanmanBytes = "Anegan SMB\u0000".toByteArray(Charsets.UTF_8)

        val params = ByteBuffer.allocate(13 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand: no further
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset
        params.putShort(maxBufferSize.toShort()) // MaxBufferSize
        params.putShort(1)        // MaxMpxCount
        params.putShort(0)        // VcNumber
        params.putInt(sessionKey) // SessionKey
        params.putShort(passwordBytes.size.toShort()) // OEMPasswordLen
        params.putShort(0)        // UnicodePasswordLen
        params.putInt(0)          // Reserved2
        params.putInt(0)          // Capabilities

        val data = ByteArrayOutputStream()
        data.write(passwordBytes)
        // Padding for alignment
        if (passwordBytes.size % 2 != 0) data.write(0)
        data.write(userBytes)
        data.write(domainBytes)
        data.write(osBytes)
        data.write(lanmanBytes)

        val packet = buildPacket(
            command = SMB_COM_SESSION_SETUP_ANDX,
            wordCount = 13,
            paramWords = params.array(),
            dataBytes = data.toByteArray()
        )
        sendPacket(packet)

        val resp = readResponse()
        uid = resp.returnedUid
    }

    // ── TREE CONNECT ──────────────────────────────────────────────────────

    private fun treeConnect(host: String, share: String) {
        val params = ByteBuffer.allocate(4 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand: no further
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset
        params.putShort(0)        // Flags
        params.putShort(1)        // PasswordLength
        params.put(0x00)          // Padding

        val data = ByteArrayOutputStream()
        data.write(0x00) // Password (null for already authenticated)
        // Path in UNC format: \\host\share
        val path = "\\\\$host\\$share"
        data.write(path.toByteArray(Charsets.UTF_8))
        data.write(0x00)
        // Service: any type
        data.write("?????\u0000".toByteArray(Charsets.US_ASCII))

        val packet = buildPacket(
            command = SMB_COM_TREE_CONNECT_ANDX,
            wordCount = 4,
            paramWords = params.array(),
            dataBytes = data.toByteArray()
        )
        sendPacket(packet)

        val resp = readResponse()
        tid = resp.returnedTid
    }

    // ── TRANS2 FIND_FIRST2 (Directory Listing) ────────────────────────────

    private fun findFirst2(searchPattern: String): List<SmbFile> {
        val results = mutableListOf<SmbFile>()

        val patternBytes = searchPattern.toByteArray(Charsets.UTF_8) + byteArrayOf(0x00)

        // TRANS2 parameter block for FIND_FIRST2
        val trans2Params = ByteBuffer.allocate(12 + patternBytes.size).order(ByteOrder.LITTLE_ENDIAN)
        trans2Params.putShort(0x0016)          // Search attributes (hidden+system+dir)
        trans2Params.putShort(512)              // Search count
        trans2Params.putShort(0x0006)          // Flags: CLOSE_AT_END | RETURN_RESUME_KEYS
        trans2Params.putShort(0x0104.toShort()) // Info level: SMB_FIND_FILE_BOTH_DIRECTORY_INFO
        trans2Params.putInt(0)                  // Storage type
        trans2Params.put(patternBytes)

        val trans2ParamArray = ByteArray(trans2Params.position())
        System.arraycopy(trans2Params.array(), 0, trans2ParamArray, 0, trans2ParamArray.size)

        // Build TRANS2 wrapper parameters
        val setupWords = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN)
        setupWords.putShort(TRANS2_FIND_FIRST2)

        val paramOffset = 32 + 1 + 30 + 2 // SMB header + WordCount + params(15 words) + ByteCount
        val dataOffset = paramOffset + trans2ParamArray.size

        val wrapperParams = ByteBuffer.allocate(15 * 2).order(ByteOrder.LITTLE_ENDIAN)
        wrapperParams.putShort(trans2ParamArray.size.toShort())  // TotalParameterCount
        wrapperParams.putShort(0)                                 // TotalDataCount
        wrapperParams.putShort(10)                                // MaxParameterCount
        wrapperParams.putShort(maxBufferSize.toShort())           // MaxDataCount
        wrapperParams.put(0)                                      // MaxSetupCount
        wrapperParams.put(0)                                      // Reserved
        wrapperParams.putShort(0)                                 // Flags
        wrapperParams.putInt(0)                                   // Timeout
        wrapperParams.putShort(0)                                 // Reserved2
        wrapperParams.putShort(trans2ParamArray.size.toShort())  // ParameterCount
        wrapperParams.putShort(paramOffset.toShort())            // ParameterOffset
        wrapperParams.putShort(0)                                 // DataCount
        wrapperParams.putShort(dataOffset.toShort())             // DataOffset
        wrapperParams.put(1)                                      // SetupCount
        wrapperParams.put(0)                                      // Reserved3
        wrapperParams.putShort(TRANS2_FIND_FIRST2)               // Setup[0]

        val packet = buildPacket(
            command = SMB_COM_TRANSACTION2,
            wordCount = 15,
            paramWords = wrapperParams.array(),
            dataBytes = trans2ParamArray
        )
        sendPacket(packet)

        val resp = readResponse()
        parseFindResults(resp.dataBytes, results)

        return results.filter { it.name != "." && it.name != ".." }
    }

    private fun parseFindResults(data: ByteArray, results: MutableList<SmbFile>) {
        if (data.isEmpty()) return
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

        try {
            while (buf.remaining() > 70) {
                val entryStart = buf.position()
                val nextOffset = buf.getInt()      // NextEntryOffset
                buf.getInt()                        // FileIndex / ResumeKey

                // CreationTime, LastAccessTime, LastWriteTime, ChangeTime (each 8 bytes)
                buf.getLong() // creation
                buf.getLong() // last access
                val lastWrite = buf.getLong()
                buf.getLong() // change

                val endOfFile = buf.getLong()       // File size
                buf.getLong()                        // AllocationSize
                val extFileAttr = buf.getInt()      // ExtFileAttributes
                val fileNameLen = buf.getInt()      // FileNameLength
                buf.getInt()                         // EaSize
                val shortNameLen = buf.get().toInt() and 0xFF
                buf.get()                            // Reserved

                // Short name (24 bytes)
                val shortNameBytes = ByteArray(24)
                buf.get(shortNameBytes)

                // File name (Unicode)
                val nameBytes = ByteArray(fileNameLen.coerceAtMost(buf.remaining()))
                buf.get(nameBytes)
                val fileName = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000')

                if (fileName.isNotBlank()) {
                    results.add(
                        SmbFile(
                            name = fileName,
                            isDirectory = (extFileAttr and FILE_ATTR_DIRECTORY) != 0,
                            size = endOfFile,
                            lastModified = windowsTimeToUnix(lastWrite)
                        )
                    )
                }

                if (nextOffset == 0) break
                buf.position(entryStart + nextOffset)
            }
        } catch (_: Exception) {
            // Partial parse is acceptable
        }
    }

    // ── NT_CREATE_ANDX (Open File) ────────────────────────────────────────

    private fun ntCreateFile(
        path: String,
        desiredAccess: Int,
        shareAccess: Int,
        disposition: Int
    ): Int {
        val nameBytes = path.toByteArray(Charsets.UTF_8) + byteArrayOf(0x00)

        val params = ByteBuffer.allocate(24 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset
        params.put(0x00)          // Reserved2
        params.putShort(nameBytes.size.toShort()) // NameLength
        params.putInt(0)          // Flags (NT_CREATE)
        params.putInt(0)          // RootDirectoryFID
        params.putInt(desiredAccess) // DesiredAccess
        params.putLong(0)         // AllocationSize
        params.putInt(FILE_ATTR_NORMAL) // ExtFileAttributes
        params.putInt(shareAccess) // ShareAccess
        params.putInt(disposition) // CreateDisposition
        params.putInt(0)          // CreateOptions
        params.putInt(0x02)       // ImpersonationLevel
        params.put(0x00)          // SecurityFlags

        val packet = buildPacket(
            command = SMB_COM_NT_CREATE_ANDX,
            wordCount = 24,
            paramWords = params.array(),
            dataBytes = nameBytes
        )
        sendPacket(packet)

        val resp = readResponse()
        if (resp.paramWords.size < 6) throw IOException("NT_CREATE response too short")
        val respBuf = ByteBuffer.wrap(resp.paramWords).order(ByteOrder.LITTLE_ENDIAN)
        respBuf.get()      // AndXCommand
        respBuf.get()      // Reserved
        respBuf.getShort() // AndXOffset
        respBuf.get()      // OpLockLevel
        val fid = respBuf.getShort().toInt() and 0xFFFF
        return fid
    }

    // ── READ_ANDX ─────────────────────────────────────────────────────────

    private fun readFile(fid: Int, offset: Long, maxCount: Int): ByteArray {
        val params = ByteBuffer.allocate(12 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset
        params.putShort(fid.toShort()) // FID
        params.putInt(offset.toInt())  // Offset (low 32 bits)
        params.putShort(maxCount.toShort()) // MaxCountOfBytesToReturn
        params.putShort(maxCount.toShort()) // MinCountOfBytesToReturn
        params.putInt((offset shr 32).toInt()) // Timeout / OffsetHigh
        params.putShort(0)        // Remaining
        params.putInt(0)          // Offset high (for 64-bit; repeated for some dialects)

        val packet = buildPacket(
            command = SMB_COM_READ_ANDX,
            wordCount = 12,
            paramWords = params.array(),
            dataBytes = ByteArray(0)
        )
        sendPacket(packet)

        val resp = readResponse()
        return resp.dataBytes
    }

    // ── WRITE_ANDX ────────────────────────────────────────────────────────

    private fun writeFile(fid: Int, offset: Long, data: ByteArray) {
        val params = ByteBuffer.allocate(14 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset
        params.putShort(fid.toShort()) // FID
        params.putInt(offset.toInt())  // Offset (low 32 bits)
        params.putInt(0)          // Timeout
        params.putShort(0)        // WriteMode
        params.putShort(0)        // Remaining
        params.putShort(0)        // DataLengthHigh
        params.putShort(data.size.toShort()) // DataLength
        // Data offset: 32 (SMB header) + 1 (WordCount) + 28 (14 words) + 2 (ByteCount) = 63
        params.putShort(63)       // DataOffset
        params.putInt((offset shr 32).toInt()) // OffsetHigh

        val packet = buildPacket(
            command = SMB_COM_WRITE_ANDX,
            wordCount = 14,
            paramWords = params.array(),
            dataBytes = data
        )
        sendPacket(packet)

        readResponse() // Verify success
    }

    // ── CLOSE ─────────────────────────────────────────────────────────────

    private fun closeFile(fid: Int) {
        val params = ByteBuffer.allocate(3 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.putShort(fid.toShort()) // FID
        params.putInt(-1)              // LastTimeModified = -1 (don't change)

        val packet = buildPacket(
            command = SMB_COM_CLOSE,
            wordCount = 3,
            paramWords = params.array(),
            dataBytes = ByteArray(0)
        )
        sendPacket(packet)

        readResponse()
    }

    // ── TREE DISCONNECT / LOGOFF ──────────────────────────────────────────

    private fun treeDisconnect() {
        val packet = buildPacket(
            command = SMB_COM_TREE_DISCONNECT,
            wordCount = 0,
            paramWords = ByteArray(0),
            dataBytes = ByteArray(0)
        )
        sendPacket(packet)
        readResponse()
    }

    private fun logoff() {
        val params = ByteBuffer.allocate(2 * 2).order(ByteOrder.LITTLE_ENDIAN)
        params.put(0xFF.toByte()) // AndXCommand
        params.put(0x00)          // Reserved
        params.putShort(0)        // AndXOffset

        val packet = buildPacket(
            command = SMB_COM_LOGOFF_ANDX,
            wordCount = 2,
            paramWords = params.array(),
            dataBytes = ByteArray(0)
        )
        sendPacket(packet)
        readResponse()
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    /**
     * Convert Windows FILETIME (100-ns intervals since 1601-01-01) to Unix millis.
     */
    private fun windowsTimeToUnix(windowsTime: Long): Long {
        if (windowsTime <= 0) return 0
        return (windowsTime / 10000) - 11644473600000L
    }
}
