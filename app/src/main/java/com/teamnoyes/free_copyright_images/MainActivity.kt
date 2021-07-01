package com.teamnoyes.free_copyright_images

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.snackbar.Snackbar
import com.teamnoyes.free_copyright_images.data.Repository
import com.teamnoyes.free_copyright_images.data.models.PhotoResponse
import com.teamnoyes.free_copyright_images.databinding.ActivityMainBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private val PERMISSIONS = arrayOf(
            WRITE_EXTERNAL_STORAGE
        )
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val scope = MainScope()

    private lateinit var adapter: PhotoAdapter
    private var tempUrl: String? = null

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        for (permission in it.entries){
            if (!permission.value) {
                Toast.makeText(this, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
        }

        downloadPhoto(tempUrl)
        tempUrl = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        bindViews()
        fetchRandomPhotos()
    }

    private fun initViews() = with(binding) {
        adapter = PhotoAdapter{ photo ->
            showDownloadPhotoConfirmationDialog(photo)
        }
        recyclerView.layoutManager =
            LinearLayoutManager(this@MainActivity, RecyclerView.VERTICAL, false)
        recyclerView.adapter = adapter
    }

    private fun bindViews() = with(binding) {
        searchEditText.setOnEditorActionListener { editText, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                currentFocus?.let { view ->
                    val inputMethodManager =
                        getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(view.windowToken, 0)

                    view.clearFocus()
                }

                fetchRandomPhotos(editText.text.toString())
            }

            true
        }

        refreshLayout.setOnRefreshListener {
            fetchRandomPhotos(searchEditText.text.toString())
        }
    }

    private fun fetchRandomPhotos(query: String? = null) = scope.launch {
        try {
            Repository.getRandomPhotos(query)?.let { photos ->
                binding.errorDescriptionTextView.visibility = View.GONE
                binding.shimmerLayout.visibility = View.VISIBLE
                adapter.submitList(photos)
                binding.recyclerView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            binding.recyclerView.visibility = View.INVISIBLE
            binding.errorDescriptionTextView.visibility = View.VISIBLE
        } finally {
            binding.shimmerLayout.visibility = View.GONE
            binding.refreshLayout.isRefreshing = false
        }

    }

    private fun showDownloadPhotoConfirmationDialog(photo: PhotoResponse) {
        AlertDialog.Builder(this)
            .setMessage("사진을 저장하시겠습니까?")
            .setPositiveButton("저장"){ dialog: DialogInterface, _ ->
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                        tempUrl = photo.urls?.full
                        requestPermissions.launch(PERMISSIONS)
                    } else {
                        downloadPhoto(photo.urls?.full)
                    }
                } else {
                    downloadPhoto(photo.urls?.full)
                }
                dialog.dismiss()
            }
            .setNegativeButton("취소"){dialog: DialogInterface, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun downloadPhoto(photoUrl: String?) {
        photoUrl ?: return


        Glide.with(this)
            .asBitmap()
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(
                object : CustomTarget<Bitmap>(SIZE_ORIGINAL, SIZE_ORIGINAL) {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        saveBitmapToMediaStore(resource)

                        val wallpaperManager = WallpaperManager.getInstance(this@MainActivity)
                        val snackBar = Snackbar.make(binding.root, "다운로드 완료", Snackbar.LENGTH_SHORT)

                        if (wallpaperManager.isWallpaperSupported &&
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                                wallpaperManager.isSetWallpaperAllowed) {
                            snackBar.setAction("배경 화면으로 저장") {
                                try {
                                    wallpaperManager.setBitmap(resource)
                                } catch (e: Exception) {
                                    Snackbar.make(binding.root, "배경 화면 저장 실패", Snackbar.LENGTH_SHORT)
                                }
                            }
                            snackBar.duration = Snackbar.LENGTH_INDEFINITE
                        }

                        snackBar.show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)

                        Snackbar.make(binding.root, "다운로드 실패", Snackbar.LENGTH_SHORT).show()

                    }

                    override fun onLoadStarted(placeholder: Drawable?) {
                        super.onLoadStarted(placeholder)

                        Snackbar.make(binding.root, "다운로드 중...", Snackbar.LENGTH_INDEFINITE).show()
                    }
                }
            )
    }

    private fun saveBitmapToMediaStore(bitmap: Bitmap) {
        val fileName = "${System.currentTimeMillis()}.jpg"
        val resolver = applicationContext.contentResolver
        val imageCollectionUri =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                )
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
        val imageDetails = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollectionUri, imageDetails)

        imageUri ?: return

        resolver.openOutputStream(imageUri).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            imageDetails.clear()
            imageDetails.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(imageUri, imageDetails, null, null)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}