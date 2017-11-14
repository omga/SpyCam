package com.spylab.spycam

import android.content.Context
import android.media.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageSaver internal constructor(
        /**
         * The JPEG image
         */
        private val mImage: Image,
        /**
         * The file we save the image into.
         */
        private val mFile: File) : Runnable {

    internal constructor(image: Image, context: Context) : this(image, File(context.getExternalFilesDir(null),
            System.currentTimeMillis().toString() + ".jpg"))

    override fun run() {
        val buffer = mImage.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(mFile)
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            mImage.close()
            if (null != output) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

}