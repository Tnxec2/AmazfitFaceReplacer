package com.kontranik.amazfitfacereplacer

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kontranik.amazfitfacereplacer.FileChooser.FileSelectedListener
import java.io.File


class MainActivity : AppCompatActivity() {

    private var permissionGranted = false

    var path: String? = null;
    var rootStorageDirectory: File? = null;

    var databaseAdapter: DatabaseAdapter? = null


    var lastFileList: ListView? = null
    var lastFileAdapter: ArrayAdapter<FileState>? = null
    var lastFiles: MutableList<FileState> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        databaseAdapter = DatabaseAdapter(applicationContext);

        databaseAdapter!!.open()
        databaseAdapter!!.allClear()
        databaseAdapter!!.close()

        lastFileList = findViewById(R.id.listview_main);

        lastFileList!!.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, which, id ->
                val fileChosen = lastFileList!!.getItemAtPosition(which) as FileState
                if (!fileChosen.isDir) {
                    if (fileChosen.path != null) {
                        val f = File(fileChosen.path)
                        if ( f.exists() && f.canRead()) openFile(f)
                    }
                }
            }

        if (  Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
            rootStorageDirectory =  Environment.getExternalStorageDirectory()
        }

        val settings = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
        path = settings.getString(
            PREF_NAME_LAST_PATH, Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            ).absolutePath
        )

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { processFile() }

        if ( rootStorageDirectory == null) {
            Toast.makeText(this, "Storage not found", Toast.LENGTH_LONG).show()
            fab.visibility = View.INVISIBLE
        }

        if ( ! permissionGranted ) {
            checkPermissions()
        } else {
            processFile()
        }
    }

    override fun onResume() {
        super.onResume()

        databaseAdapter!!.open()
        lastFiles = databaseAdapter!!.lastfiles.toMutableList()
        databaseAdapter!!.close()

        lastFileAdapter = FileListAdapter(this, R.layout.list_item, lastFiles)

        lastFileList!!.adapter =  lastFileAdapter
    }

    fun openFile(file: File) {
        val filename: String = file.getAbsolutePath()
        val baseName = file.name
        Toast.makeText(applicationContext, "File Name" + filename, Toast.LENGTH_SHORT)
            .show()
        // then actually do something in another module
        Log.d("Main", filename)

        AlertDialog.Builder(this@MainActivity)
            .setTitle("Replace Bin")
            .setMessage("Do you really want to replace this file $baseName?")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Yes",
                DialogInterface.OnClickListener { dialog, whichButton ->
                    val target = File(REPLACE_FILE_PATH)
                    if (target.exists() && target.canWrite()) {
                        file.copyTo(File(REPLACE_FILE_PATH), overwrite = true)
                        val settings = getSharedPreferences(PREF_FILE, MODE_PRIVATE);
                        val prefEditor = settings.edit()
                        prefEditor.putString(PREF_NAME_LAST_PATH, file.parent)
                        prefEditor.apply()

                        databaseAdapter!!.open()
                        val fileState = databaseAdapter!!.getByPath(file.absolutePath)
                        if ( fileState != null )
                            databaseAdapter!!.update(fileState)
                        else
                            databaseAdapter!!.insert(FileState(false, file.name, null, file.absolutePath))
                        databaseAdapter!!.close()

                        Toast.makeText(
                            applicationContext,
                            "Launch Zepp...",
                            Toast.LENGTH_SHORT
                        ).show()

                        val launchIntent =
                            packageManager.getLaunchIntentForPackage("com.huami.watch.hmwatchmanager")
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(
                                this@MainActivity, "Zepp is not installed",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Can not write to target",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
            .setNegativeButton("No", null).show()

    }

    private fun processFile() {
        if ( path == null) return
        val fileChooser = FileChooser(this@MainActivity, File(path!!), ".bin")
        fileChooser.setFileListener(object : FileSelectedListener {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun fileSelected(file: File?) {
                if (file != null) {
                    openFile(file)
                }
            }
        })
        // Set up and filter my extension I am looking for
        //fileChooser.setExtension("pdf");
        fileChooser.showDialog()
    }


    fun isExternalStorageReadable(): Boolean {
        val state: String = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state ||
                Environment.MEDIA_MOUNTED_READ_ONLY == state
    }

    private fun checkPermissions(): Boolean {
        if (!isExternalStorageReadable() ) {
            Toast.makeText(this, "external storage not available", Toast.LENGTH_LONG).show()
            return false
        }
        val permissionCheck = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                WRITE_STORAGE_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WRITE_STORAGE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "read permissions granted", Toast.LENGTH_LONG).show()
                    processFile()
                } else {
                    Toast.makeText(
                        this,
                        "need permissions to read external storage",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    companion object {
        const val WRITE_STORAGE_PERMISSION_REQUEST_CODE=0x3

        const val REPLACE_FILE_PATH = "/storage/emulated/0/Android/data/com.huami.watch.hmwatchmanager/files/watch_skin_local/78/6738b4b57e566f14a91365309c92448e/6738b4b57e566f14a91365309c92448e.bin"

        const val PREF_FILE = "Paths"
        const val PREF_NAME_LAST_PATH = "PREF_LAST_PATH"
    }
}