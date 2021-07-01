package com.teamnoyes.free_copyright_images.data

import com.teamnoyes.free_copyright_images.BuildConfig
import com.teamnoyes.free_copyright_images.data.models.PhotoResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface UnsplashApiService {
    @GET(
        "/photos/random?" +
                "client_id=${BuildConfig.UNSPLASH_API_KEY}" +
                "&count=30"
    )
    suspend fun getRandomPhotos(
        @Query("query") query: String?
    ):Response<List<PhotoResponse>>
}