package com.sfdex.media14

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var adapter: ImageAdapter
    private val list = ArrayList<Media>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        adapter = ImageAdapter(list)
        val layoutManager = GridLayoutManager(this, 3)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.request).setOnClickListener {
            requestPermission()
        }

        if (checkMediaAccessType() == MediaAccessType.DENIED) {
            requestPermission()
        } else {
            showImages()
        }

        /*if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_MEDIA_IMAGES), 1001)
        }*/
    }

    private fun showImages() {
        launch {
            list.clear()
            val images = getImages(contentResolver)
            list.addAll(images)
            adapter.notifyDataSetChanged()
        }
    }

    enum class MediaAccessType {
        PARTIAL,
        FULL,
        FULL_UP_12,
        DENIED
    }

    private fun checkMediaAccessType(): MediaAccessType {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && (
                    checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                            checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED)
        ) {
            // Full access on Android 13 (API level 33) or higher
            MediaAccessType.FULL
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && checkSelfPermission(
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Partial access on Android 14 (API level 34) or higher
            MediaAccessType.PARTIAL
        } else if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            MediaAccessType.FULL_UP_12
        } else {
            // Access denied
            MediaAccessType.DENIED
        }
    }

    // Register ActivityResult handler
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            run {
                // Handle permission requests results
                Log.d(TAG, "permission results: $results")
                showImages()
            }
        }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            )
        } else {
            requestPermissions.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
        }
    }

    // Run the querying logic in a coroutine outside of the main thread to keep the app responsive.
    // Keep in mind that this code snippet is querying only images of the shared storage.
    private suspend fun getImages(contentResolver: ContentResolver): List<Media> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(
                Images.Media._ID,
                Images.Media.DISPLAY_NAME,
                Images.Media.SIZE,
                Images.Media.MIME_TYPE
            )

            val collectionUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Query all the device storage volumes instead of the primary only
                Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                Images.Media.EXTERNAL_CONTENT_URI
            }

            val images = mutableListOf<Media>()

            contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                "${Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(Images.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(Images.Media.SIZE)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val uri = ContentUris.withAppendedId(collectionUri, cursor.getLong(idColumn))
                    val name = cursor.getString(displayNameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val mimeType = cursor.getString(mimeTypeColumn)

                    val image = Media(uri, name, size, mimeType)
                    images.add(image)
                }
            }

            return@withContext images
        }
}