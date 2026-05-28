/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.anegan.app.MainActivity
import com.anegan.app.R

class AneganWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.anegan_widget_layout)

            // Bind click events to tool shortcuts
            views.setOnClickPendingIntent(R.id.btn_widget_images, createPendingIntent(context, "com.anegan.action.SHORTCUT_IMAGES"))
            views.setOnClickPendingIntent(R.id.btn_widget_pdf, createPendingIntent(context, "com.anegan.action.SHORTCUT_PDF"))
            views.setOnClickPendingIntent(R.id.btn_widget_notes, createPendingIntent(context, "com.anegan.action.SHORTCUT_NOTES"))
            views.setOnClickPendingIntent(R.id.btn_widget_compass, createPendingIntent(context, "com.anegan.action.SHORTCUT_COMPASS"))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun createPendingIntent(context: Context, actionString: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = actionString
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(context, actionString.hashCode(), intent, flags)
    }
}
