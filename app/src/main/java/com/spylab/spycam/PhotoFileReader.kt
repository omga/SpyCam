package com.spylab.spycam

import android.content.Context
import java.io.File

/**
 * @author a.hatrus.
 */
class PhotoFileReader(var context: Context) {

    private val folder = context.getExternalFilesDir(null)

    fun listFiles(): MutableList<File> = folder.listFiles().toMutableList()
    fun deleteAllFiles(): Unit = folder.listFiles().forEach { it -> it.delete() }

}