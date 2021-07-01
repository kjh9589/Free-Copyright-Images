package com.teamnoyes.free_copyright_images

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.teamnoyes.free_copyright_images.data.models.PhotoResponse
import com.teamnoyes.free_copyright_images.databinding.ItemPhotoBinding

class PhotoAdapter(private val onItemClicked: (PhotoResponse) -> Unit) : ListAdapter<PhotoResponse, PhotoAdapter.ViewHolder>(diffUtil) {
    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<PhotoResponse>() {
            override fun areItemsTheSame(oldItem: PhotoResponse, newItem: PhotoResponse): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(
                oldItem: PhotoResponse,
                newItem: PhotoResponse
            ): Boolean {
                return oldItem == newItem
            }
        }
    }

    inner class ViewHolder(private val binding: ItemPhotoBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(photoResponse: PhotoResponse) = with(binding) {
            val dimensionRatio = photoResponse.height / photoResponse.width.toFloat()
            // 스크린 가로 사이즈 - (뷰 좌우 패딩)
            val targetWidth = root.resources.displayMetrics.widthPixels - (root.paddingStart + root.paddingEnd)
            val targetHeight = (targetWidth * dimensionRatio).toInt()

            contentsContainer.layoutParams = contentsContainer.layoutParams.apply {
                height = targetHeight
            }

            Glide.with(root)
                .load(photoResponse.urls?.regular)
                .thumbnail(
                    Glide.with(root)
                        .load(photoResponse.urls?.thumb)
                        .transition(DrawableTransitionOptions.withCrossFade())
                )
                .override(targetWidth, targetHeight)
                .into(photoImageView)

            Glide.with(root)
                .load(photoResponse.user?.profileImageUrls?.small)
                .placeholder(R.drawable.shape_profile_placeholder)
                .circleCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(profileImageView)

            if (photoResponse.user?.name.isNullOrBlank()) {
                authorTextView.isGone = true
            } else {
                authorTextView.text = photoResponse.user?.name
            }

            if (photoResponse.description.isNullOrBlank()) {
                descriptionTextView.isGone = true
            } else {
                descriptionTextView.text = photoResponse.description
            }

            root.setOnClickListener {
                onItemClicked(photoResponse)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemPhotoBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}