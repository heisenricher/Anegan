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
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

object ShortcutHelper {
    fun setupShortcuts(context: Context) {
        val imagesShortcut = ShortcutInfoCompat.Builder(context, "shortcut_images")
            .setShortLabel("Image Tools")
            .setLongLabel("Convert & Compress Images")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = "com.anegan.action.SHORTCUT_IMAGES"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            .build()

        val pdfShortcut = ShortcutInfoCompat.Builder(context, "shortcut_pdf")
            .setShortLabel("PDF Tools")
            .setLongLabel("Split, Compress & Edit PDFs")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = "com.anegan.action.SHORTCUT_PDF"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            .build()

        val historyShortcut = ShortcutInfoCompat.Builder(context, "shortcut_history")
            .setShortLabel("History")
            .setLongLabel("View Conversion History")
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_launcher))
            .setIntent(Intent(context, MainActivity::class.java).apply {
                action = "com.anegan.action.SHORTCUT_HISTORY"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            .build()

        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, listOf(imagesShortcut, pdfShortcut, historyShortcut))
        } catch (e: Exception) {
            android.util.Log.e("AneganShortcuts", "Failed to register launcher shortcuts", e)
        }
    }
}
