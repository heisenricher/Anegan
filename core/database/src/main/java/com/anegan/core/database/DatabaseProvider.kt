/*
 * Copyright (c) 2026 Mahilan (heisenricher). All rights reserved.
 * 
 * This source code is licensed under the custom Anegan Attribution License.
 * Any person or entity using, modifying, or building upon this code must
 * prominently attribute the original creator Mahilan (heisenricher).
 * Personal and educational use only.
 */

package com.anegan.core.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var instance: AneganDatabase? = null

    fun getDatabase(context: Context): AneganDatabase {
        return instance ?: synchronized(this) {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AneganDatabase::class.java,
                "anegan_database"
            ).fallbackToDestructiveMigration().build()
            instance = db
            db
        }
    }
}
