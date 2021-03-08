package com.kontranik.amazfitfacereplacer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView


class FileListAdapter(context: Context?, resource: Int,
                      private val files: MutableList<FileState>
) :
    ArrayAdapter<FileState>(context!!, resource, files) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val layout: Int = resource

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = inflater.inflate(layout, parent, false)
            viewHolder = ViewHolder(convertView)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        val file = files[position]
        if ( file != null) {
            if ( file.isDir) viewHolder.imageView.setImageResource(R.drawable.ic_baseline_folder_24)
            else viewHolder.imageView.setImageResource(R.drawable.ic_baseline_sticky_note_2_24)
            viewHolder.nameView.text = file.name
        }
        return convertView!!
    }

    private inner class ViewHolder internal constructor(view: View) {
        val imageView: ImageView = view.findViewById<View>(R.id.icon) as ImageView
        val nameView: TextView = view.findViewById<View>(R.id.name) as TextView

    }

}