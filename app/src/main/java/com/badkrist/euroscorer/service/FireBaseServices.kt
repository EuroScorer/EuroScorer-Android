package com.badkrist.euroscorer.service

import com.badkrist.euroscorer.model.Country
import com.badkrist.euroscorer.model.Song
import com.badkrist.euroscorer.model.Votes
import com.badkrist.euroscorer.model.VotesToSend
import kotlinx.coroutines.Deferred
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface FireBaseServices {
    @GET("songs")
    suspend fun retrieveSongs(): Response<List<Song>>

    @GET("vote")
    suspend fun retrieveVote(@Header("Authorization") token: String?): Response<Votes>

    @POST("vote")
    suspend fun sendVote(@Header("Authorization") token: String?, @Body votes: RequestBody)
}