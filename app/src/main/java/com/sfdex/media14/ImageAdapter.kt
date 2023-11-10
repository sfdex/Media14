package com.sfdex.media14

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(private val list: List<Media>) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView

        init {
            image = itemView.findViewById(R.id.image)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, null, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = list[position]
        try {
            holder.image.setImageURI(image.uri)
        } catch (e: Exception) {
            Log.d("ImageAdapter", "onBindViewHolder error: ${e.message}")
//            e.printStackTrace()
        }
    }
}