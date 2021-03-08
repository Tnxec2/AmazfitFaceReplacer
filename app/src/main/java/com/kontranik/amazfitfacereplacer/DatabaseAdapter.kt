package com.kontranik.amazfitfacereplacer

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

class DatabaseAdapter(context: Context) {
    private val dbHelper: DatabaseHelper
    private var database: SQLiteDatabase? = null
    fun open(): DatabaseAdapter {
        database = dbHelper.writableDatabase
        return this
    }

    fun close() {
        dbHelper.close()
    }

    private val allEntries: Cursor
        private get() {
            val columns = arrayOf(
                DatabaseHelper.COLUMN_ID,
                DatabaseHelper.COLUMN_NAME,
                DatabaseHelper.COLUMN_TIMESTAMP,
                DatabaseHelper.COLUMN_PATH
            )
            return database!!.query(
                DatabaseHelper.TABLE,
                columns,
                null,
                null,
                null,
                null,
                DatabaseHelper.COLUMN_TIMESTAMP + " DESC"
            )
        }

    val lastfiles: List<FileState>
        get() {
            val lastfiles: ArrayList<FileState> = ArrayList()
            val cursor = allEntries
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
                val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME))
                val path = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PATH))
                lastfiles.add(FileState(false, name, id, path))
            }
            cursor.close()
            return lastfiles
        }
    val count: Long
        get() = DatabaseUtils.queryNumEntries(database, DatabaseHelper.TABLE)

    fun getByPath(path: String): FileState? {
        var result: FileState? = null
        val query = String.format(
            "SELECT * FROM %s WHERE %s=?",
            DatabaseHelper.TABLE,
            DatabaseHelper.COLUMN_PATH
        )
        val cursor = database!!.rawQuery(query, arrayOf(path))
        if (cursor.moveToFirst()) {
            val id = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID))
            val name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_NAME))
            val path2 = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PATH))
            result = FileState(false, name, id, path2)
        }
        cursor.close()
        return result
    }
    fun insert(fileState: FileState): Long {
        val cv = ContentValues()
        cv.put(DatabaseHelper.COLUMN_NAME, fileState.name)
        cv.put(DatabaseHelper.COLUMN_PATH, fileState.path)
        cv.put(DatabaseHelper.COLUMN_TIMESTAMP, Date().time)
        return database!!.insert(DatabaseHelper.TABLE, null, cv)
    }

    fun delete(fileStateId: Long): Long {
        val whereClause = "_id = ?"
        val whereArgs = arrayOf(fileStateId.toString())
        return database!!.delete(DatabaseHelper.TABLE, whereClause, whereArgs).toLong()
    }

    fun update(fileState: FileState): Long {
        val whereClause = DatabaseHelper.COLUMN_ID + "=" + java.lang.String.valueOf(fileState.id)
        val cv = ContentValues()
        cv.put(DatabaseHelper.COLUMN_NAME, fileState.name)
        cv.put(DatabaseHelper.COLUMN_PATH, fileState.path)
        cv.put(DatabaseHelper.COLUMN_TIMESTAMP, Date().time)
        return database!!.update(DatabaseHelper.TABLE, cv, whereClause, null).toLong()
    }

    fun allClear() {
        lastfiles.forEach { lf ->
            if ( lf.path == null) delete(lf.id!!)
            else {
                val f = File(lf.path)
                if (!f.exists()) {
                    delete(lf.id!!)
                }
            }
        }
    }

    init {
        dbHelper = DatabaseHelper(context.applicationContext)
    }
}