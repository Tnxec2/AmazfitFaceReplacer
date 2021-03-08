package com.kontranik.amazfitfacereplacer

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DATABASE_NAME, null, SCHEMA) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE " + TABLE + " ( " + COLUMN_ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_NAME + " TEXT, "
                    + COLUMN_TIMESTAMP + " INTEGER, "
                    + COLUMN_PATH + " TEXT);"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE)
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "filestatestore.db" // название бд
        private const val SCHEMA = 2
        const val TABLE = "lastfiles"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_TIMESTAMP = "updatetimestamp"
        const val COLUMN_PATH = "path"
    }
}