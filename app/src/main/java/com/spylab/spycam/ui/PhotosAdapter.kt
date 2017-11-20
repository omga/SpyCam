package com.spylab.spycam.ui

import android.app.Dialog
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

class PhotosAdapter(private var context: Context, private val imageWidth: Int,
                    private val selectedCallback: OnItemsSelectedCallback) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var photos: MutableList<File> = mutableListOf()
    private var selectedItems: MutableList<Int> = mutableListOf()

    override fun getItemCount(): Int {
        return photos.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {
        var photo = photos[position]
        Glide.with(context)
                .load(photo)
                .apply(RequestOptions().centerCrop().sizeMultiplier(0.2f))
                .into(holder?.itemView?.image)

        if (selectedItems.contains(position)) {
            holder?.itemView?.imageSelected?.visibility = View.VISIBLE
        } else {
            holder?.itemView?.imageSelected?.visibility = View.GONE
        }
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

    fun deleteSelectedPhotos() {
        selectedItems.sort()
        var lastIndex = selectedItems.size - 1
        for (i in photos.size downTo 0) {
            if (i == selectedItems[lastIndex]) {
                photos.removeAt(i).delete()
                if (--lastIndex == -1)
                    break
            }
        }
        selectedItems.clear()
        notifyDataSetChanged()
        selectedCallback.onSelectionChanged(false)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view),
            View.OnClickListener, View.OnLongClickListener {
        init {
            itemView.setOnLongClickListener(this)
            itemView.setOnClickListener(this)
        }

        override fun onClick(v: View?) {
            if (!selectedItems.isEmpty()) {
                v?.performLongClick()
                return
            }
            val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            val imageFullScreen = ImageView(context)
            imageFullScreen.setOnClickListener { dialog.cancel() }
            Glide.with(context)
                    .load(photos[adapterPosition])
                    .apply(RequestOptions().centerCrop())
                    .into(imageFullScreen)
            dialog.setContentView(imageFullScreen)
            dialog.window.attributes.windowAnimations = android.R.style.Animation_Dialog
            dialog.show()
        }

        override fun onLongClick(v: View?): Boolean {
            val index = adapterPosition
            if (selectedItems.contains(index)) {
                selectedItems.remove(index)
            } else {
                selectedItems.add(index)
            }
            notifyItemChanged(index)
            selectedCallback.onSelectionChanged(!selectedItems.isEmpty())
            return true
        }
    }

    interface OnItemsSelectedCallback {
        fun onSelectionChanged(hasSelectedItems: Boolean)
    }
}