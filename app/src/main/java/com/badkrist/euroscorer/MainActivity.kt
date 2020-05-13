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

    var TAG = "MainActivity"
    private lateinit var userCountryCode: String
    private var songList = listOf<Song>()
    private var totalCount: Int = 0
    private lateinit var service: FireBaseServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        service = Retrofit.Builder()
            .baseUrl("https://us-central1-eurovision2020-ea486.cloudfunctions.net/api/v1/")
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(FireBaseServices::class.java)

        sendButton.setOnClickListener {
            launch {
                sendVote()
                Toast.makeText(this@MainActivity, R.string.vote_saved, Toast.LENGTH_LONG)
            }
        }

        val mUser = FirebaseAuth.getInstance().currentUser
        userCountryCode = Utils.getCountryCodeFromPhoneNumber(mUser!!.phoneNumber!!)

        refreshSongs()
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
        var count = 20
        for(song: Song in songList){
            count -= song.vote
        }
        var plurial = ""
        if(count > 1) {
            plurial = "s"
        }
        totalCount = count
        totalCounter.text = getString(R.string.total_counter_label, count.toString(), plurial)
        sendButton.isEnabled = totalCount < 20
    }

    fun getTotalCount(): Int {
        return totalCount
    }
    fun getUserCountryCode(): String {
        return userCountryCode
    }

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
        var bodyJson = JSONObject()
        var votes = JSONArray()
        for (song: Song in songList) {
            if(song.vote > 0) {
                for (x in 1..song.vote) {
                    votes.put(song.country!!.countryCode!!)
                }
            }
        }
        bodyJson.put("votes", votes)
        var body = RequestBody.create(MediaType.parse("application/json"), bodyJson.toString())
        var sendVote = service.sendVote(token, body)
        return@withContext true
    }
}
