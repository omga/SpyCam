package com.spylab.spycam.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import com.spylab.spycam.remote.CameraService
import com.spylab.spycam.util.PhotoFileReader
import com.spylab.spycam.R
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity(), PhotosAdapter.OnItemsSelectedCallback {

    val SPAN_COUNT = 3
    val REQUEST_CAMERA_PERMISSION = 333
    var hasSelectedItems = false

    private val photoFileReader by lazy {
        PhotoFileReader(this)
    }
    private val adapter by lazy {
        PhotosAdapter(this, resources.displayMetrics.widthPixels / SPAN_COUNT, this)
    }

    private lateinit var photos: MutableList<File>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<Button>(R.id.startService).setOnClickListener {
            startCameraService(this@MainActivity)
        }
        setupRecycler()

    }

    override fun onStart() {
        super.onStart()
        photos = photoFileReader.listFiles()
        adapter.setData(photos)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (hasSelectedItems) {
            menu?.findItem(R.id.menu_item_delete)?.isVisible = true
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_activity_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId) {
        R.id.menu_item_delete -> {
            adapter.deleteSelectedPhotos()
            true
        }
        R.id.menu_item_settings -> {
            openSettings()
            true
        }
        R.id.menu_item_delete_all -> {
            photoFileReader.deleteAllFiles()
            adapter.setData(mutableListOf())
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java)
        )
    }

    private fun setupRecycler() {
        recyclerView.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        recyclerView.adapter = adapter
    }

    private fun startCameraService(context: Context) {
        val intent = Intent(context, CameraService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startService(intent)
    }

    override fun onSelectionChanged(hasSelectedItems: Boolean) {
        this.hasSelectedItems = hasSelectedItems
        invalidateOptionsMenu()
    }
}
