/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.feature.conversion.worker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.anegan.core.database.DatabaseProvider
import com.anegan.core.database.ConversionHistoryEntity
import java.io.File

abstract class BaseConversionWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    protected val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    protected val notificationChannelId = "file_conversion_channel"
    protected val notificationId = id.hashCode()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "File Conversion",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of background file conversions"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    protected fun createNotification(title: String, progress: Int, isIndeterminate: Boolean = false): Notification {
        val contentText = if (isIndeterminate) "Processing..." else "$progress%"
        
        // Use android.R.drawable.stat_sys_download or standard app icon if available.
        // We'll use android.R.drawable.stat_sys_download for compatibility.
        return NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, isIndeterminate)
            .setOnlyAlertOnce(true)
            .build()
    }

    protected fun createForegroundInfo(title: String, progress: Int, isIndeterminate: Boolean = false): ForegroundInfo {
        val notification = createNotification(title, progress, isIndeterminate)
        val serviceType = getForegroundServiceTypeOverride()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && serviceType != 0) {
            ForegroundInfo(notificationId, notification, serviceType)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    open fun getForegroundServiceTypeOverride(): Int {
        return 0
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return createForegroundInfo("File Conversion", 0, true)
    }

    protected suspend fun updateProgress(title: String, progress: Int) {
        setProgress(workDataOf("progress" to progress))
        try {
            setForeground(createForegroundInfo(title, progress))
        } catch (e: Exception) {
            // If background execution is restricted from foreground service, update normally via notify
            notificationManager.notify(notificationId, createNotification(title, progress))
        }
    }

    protected fun showSuccessNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    protected fun showFailureNotification(title: String, text: String) {
        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(notificationId, notification)
    }

    protected suspend fun saveToHistory(
        originalFileName: String,
        outputFileName: String,
        originalFormat: String,
        outputFormat: String,
        status: String,
        originalSize: Long,
        outputSize: Long,
        outputPath: String
    ) {
        val historyDao = DatabaseProvider.getDatabase(applicationContext).historyDao()
        historyDao.insertConversion(
            ConversionHistoryEntity(
                originalFileName = originalFileName,
                outputFileName = outputFileName,
                originalFormat = originalFormat,
                outputFormat = outputFormat,
                status = status,
                timestamp = System.currentTimeMillis(),
                originalSize = originalSize,
                outputSize = outputSize,
                outputPath = outputPath
            )
        )
    }
}
