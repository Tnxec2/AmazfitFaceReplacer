package com.kontranik.amazfitfacereplacer

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView.OnItemClickListener
import android.widget.ListView
import android.widget.TextView
import java.io.File
import java.util.*


class FileChooser(private val activity: Activity, startPath: File, private val extension: String?) {
    private val list: ListView
    private val dialog: Dialog
    private var currentPath: File? = null

    // file selection event handling
    interface FileSelectedListener {
        fun fileSelected(file: File?)
    }

    fun setFileListener(fileListener: FileSelectedListener?): FileChooser {
        this.fileListener = fileListener
        return this
    }

    private var fileListener: FileSelectedListener? = null
    fun showDialog() {
        dialog.show()
    }

    /**
     * Sort, filter and display the files for the given path.
     */
    private fun refresh(path: File) {
        currentPath = path
        if (path.exists()) {

            val dirs = path.listFiles{ f -> f.isDirectory && f.canRead() }
            if ( dirs != null) {
                val files = path.listFiles { file ->
                    if ( ! file.isDirectory ) {
                        if (!file.canRead()) {
                            false
                        } else if (extension == null) {
                            true
                        } else {
                            file.name.endsWith(extension, ignoreCase = true)
                        }
                    } else {
                        false
                    }
                }

                // convert to an array
                val fileList: MutableList<FileState> = mutableListOf()
                if (path.parentFile != null) {
                    fileList.add(FileState(isDir = true, PARENT_DIR, id = null, path = null))
                }
                Arrays.sort(dirs)
                Arrays.sort(files)
                for (dir in dirs) {
                    fileList.add(FileState(isDir = true, dir.name, id = null, path = null))
                }
                for (file in files) {
                    fileList.add(FileState(isDir = false, file.name, id = null, path = null))
                }

                // refresh the user interface
                dialog.setTitle(currentPath!!.path)
                list.adapter = FileListAdapter(
                        activity,
                        R.layout.list_item, fileList
                )
            }
        }
    }

    /**
     * Convert a relative filename into an actual File object.
     */
    private fun getChosenFile(fileChosen: String): File {
        return if (fileChosen == PARENT_DIR) {
            currentPath!!.parentFile
        } else {
            File(currentPath, fileChosen)
        }
    }

    companion object {
        private const val PARENT_DIR = ".."
    }

    init {
        dialog = Dialog(activity)
        list = ListView(activity)
        list.onItemClickListener =
            OnItemClickListener { parent, view, which, id ->
                val fileChosen = list.getItemAtPosition(which) as FileState
                val chosenFile = getChosenFile(fileChosen.name)
                if (chosenFile.isDirectory) {
                    refresh(chosenFile)
                } else {
                    if (fileListener != null) {
                        fileListener!!.fileSelected(chosenFile)
                    }
                    dialog.dismiss()
                }
            }
        dialog.setContentView(list)
        dialog.window!!.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        )

        refresh(startPath)
    }
}