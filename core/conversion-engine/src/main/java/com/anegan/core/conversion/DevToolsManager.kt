/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.conversion

import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class DevToolsManager {

    suspend fun generateTextHash(text: String, algorithm: String): String = withContext(Dispatchers.Default) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val hashBytes = digest.digest(text.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun generateFileHash(file: File, algorithm: String): String = withContext(Dispatchers.IO) {
        try {
            val digest = MessageDigest.getInstance(algorithm)
            val fis = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead = fis.read(buffer)
            while (bytesRead != -1) {
                digest.update(buffer, 0, bytesRead)
                bytesRead = fis.read(buffer)
            }
            fis.close()
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun encodeTextBase64(text: String): String = withContext(Dispatchers.Default) {
        try {
            Base64.encodeToString(text.toByteArray(Charsets.UTF_8), Base64.DEFAULT)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun decodeTextBase64(base64Str: String): String = withContext(Dispatchers.Default) {
        try {
            String(Base64.decode(base64Str, Base64.DEFAULT), Charsets.UTF_8)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    suspend fun generateQrCode(
        text: String,
        size: Int = 512,
        fgColor: Int = android.graphics.Color.BLACK,
        bgColor: Int = android.graphics.Color.WHITE
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) fgColor else bgColor)
                }
            }
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun scanQrCode(context: android.content.Context, uri: android.net.Uri): Result<String> = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            val image = com.google.mlkit.vision.common.InputImage.fromFilePath(context, uri)
            val scanner = com.google.mlkit.vision.barcode.BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val rawValue = barcodes[0].rawValue ?: ""
                        if (rawValue.isNotEmpty()) {
                            continuation.resume(Result.success(rawValue))
                        } else {
                            continuation.resume(Result.failure(Exception("QR Code is empty or invalid")))
                        }
                    } else {
                        continuation.resume(Result.failure(Exception("No QR Code detected in image")))
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(Result.failure(e))
                }
        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }
}
