package com.badkrist.euroscorer

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.badkrist.euroscorer.model.Song
import com.badkrist.euroscorer.service.FireBaseServices
import com.badkrist.euroscorer.utils.Utils
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity(), CoroutineScope {

    private val supervisorJob = SupervisorJob()
    override val coroutineContext = Dispatchers.Main + supervisorJob

    private lateinit var userCountryCode: String
    private var songList = listOf<Song>()
    private var totalCount: Int = 0
    private val service: FireBaseServices = Retrofit.Builder()
        .baseUrl("https://us-central1-eurovision2020-ea486.cloudfunctions.net/api/v1/")
        .addConverterFactory(MoshiConverterFactory.create().asLenient())
        .build()
        .create(FireBaseServices::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendButton.setOnClickListener { sendVoteTapped() }

        val mUser = FirebaseAuth.getInstance().currentUser
        userCountryCode = Utils.getCountryCodeFromPhoneNumber(mUser!!.phoneNumber!!)

        refreshSongs()
    }

    private fun sendVoteTapped() = launch {
        sendVote()
        Toast.makeText(this@MainActivity, R.string.vote_saved, Toast.LENGTH_LONG).show()
    }

    private fun refreshSongs() = launch {
        songList = fetchSongs()
        adaptSongsLayout()
        updateTotalCounter()
    }

    fun adaptSongsLayout() {
        val adapter = SongAdapter(this, songList as ArrayList)
        songsLayout.adapter = adapter
    }

    fun updateTotalCounter() {
        val count = 20
        val totalVotesGiven = songList.map { it.vote }.reduce { sum, element -> sum + element }
        val plural = if(totalVotesGiven > 1) "s" else ""
        totalCount = count - totalVotesGiven
        totalCounter.text = getString(R.string.total_counter_label, totalCount.toString(), plural)
        sendButton.isEnabled = totalCount < 20
    }

    fun getTotalCount(): Int = totalCount

    fun getUserCountryCode(): String = userCountryCode

    fun startVideoPlayer(link: String?) {
        val intent = YouTubeStandalonePlayer.createVideoIntent(this, getString(R.string.YOUTUBE_API_KEY), link);
        startActivity(intent);
    }

    private suspend fun fetchSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = service.retrieveSongs().body() ?: emptyList()
        val token = fetchToken()
        val votes = service.retrieveVote(token).body()?.votes ?: emptyList()
        songs.forEach { s -> s.vote = votes.count { it == s.country?.countryCode } }
        songs
    }

    private var cachedToken: String? = null
    private suspend fun fetchToken() : String? {
        return if (cachedToken != null) cachedToken else suspendCoroutine { continuation ->
            FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.addOnCompleteListener {
                cachedToken = it.result?.token
                continuation.resume(cachedToken)
            }
        }
    }

    private suspend fun sendVote(): Boolean = withContext(Dispatchers.IO) {
        val token = fetchToken()
        if (token == null) {
            return@withContext false
        }
        val votes = songList.fold(emptyList<String>()) { acc, song ->
            acc + 0.until(song.vote).mapNotNull { song.country?.countryCode }
        }
        val bodyJson = JSONObject()
        bodyJson.put("votes", JSONArray(votes))
        val body = RequestBody.create(MediaType.parse("application/json"), bodyJson.toString())
        service.sendVote(token, body)
        return@withContext true
    }
}
