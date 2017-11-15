package com.spylab.spycam.ui

import android.content.Context
import android.support.annotation.NonNull
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.spylab.spycam.R
import kotlinx.android.synthetic.main.item_photo.view.*
import java.io.File

/**
 * @author a.hatrus.
 */

class PhotosAdapter(private var context: Context, private val imageWidth: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var photos: MutableList<File> = mutableListOf()
    override fun getItemCount(): Int {
        return photos.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        var photo = photos[position]
        Glide.with(context)
                .load(photo)
                .apply(RequestOptions().centerCrop())
                .into(holder?.itemView?.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): RecyclerView.ViewHolder {
        var v = ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false))
        v.itemView.layoutParams.apply {
            width = imageWidth
            height = imageWidth
        }
        return v
    }

    fun setData(@NonNull photos: MutableList<File>) {
        this.photos.clear()
        this.photos.addAll(photos)
        notifyDataSetChanged()
    }

    fun addData(@NonNull photos: MutableList<File>) {
        this.photos.addAll(photos)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private lateinit var image: ImageView
    }
}