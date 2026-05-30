/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.app

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(context, throwable)
            } catch (e: Exception) {
                // Ignore fallback failures
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashLog(context: Context, throwable: Throwable) {
        val rootPath = Environment.getExternalStorageDirectory().absolutePath + "/Anegan"
        val crashFolder = File(rootPath, "crash_logs")
        if (!crashFolder.exists()) {
            crashFolder.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val crashFile = File(crashFolder, "crash_$timestamp.txt")

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val deviceInfo = """
App Version: ${try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (e: Exception) { "Unknown" }}
Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
Device Model: ${Build.MANUFACTURER} ${Build.MODEL}
Build ID: ${Build.DISPLAY}
Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
--------------------------------------------------------

""".trimIndent()

        FileWriter(crashFile).use { writer ->
            writer.write(deviceInfo)
            writer.write(stackTrace)
        }
    }
}
