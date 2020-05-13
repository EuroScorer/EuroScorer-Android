package com.badkrist.euroscorer

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.badkrist.euroscorer.model.Song
import com.badkrist.euroscorer.service.FireBaseServices
import com.badkrist.euroscorer.utils.Utils
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity()  {
    var TAG = "MainActivity"
    private lateinit var idToken: String
    private lateinit var userCountryCode: String
    var songList: List<Song> = ArrayList();
    private var totalCount: Int = 0
    private lateinit var service: FireBaseServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendButton.setOnClickListener { sendVote() }
        service = Retrofit.Builder()
            .baseUrl("https://us-central1-eurovision2020-ea486.cloudfunctions.net/api/v1/")
            .addConverterFactory(MoshiConverterFactory.create().asLenient())
            .build()
            .create(FireBaseServices::class.java)

        retrieveData()
    }
    fun adaptSongsLayout() {
        runOnUiThread {
            val adapter = SongAdapter(this, songList as ArrayList)
            songsLayout.adapter = adapter
        }
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
        runOnUiThread {
            totalCounter.text = getString(R.string.total_counter_label, count.toString(), plurial)
            sendButton.isEnabled = totalCount < 20

        }
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

    private fun retrieveData() {
        val mUser = FirebaseAuth.getInstance().currentUser
        userCountryCode = Utils.getCountryCodeFromPhoneNumber(mUser!!.phoneNumber!!)
        Log.i(TAG, "userCountryCode: " + userCountryCode)
        mUser!!.getIdToken(true)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    idToken = task.result!!.token!!
                    CoroutineScope(Dispatchers.IO).launch {
                        val songs = service.retrieveSongs()
                        try {
                            songList = songs.body()!!
                            try {
                                val votes = service.retrieveVote(idToken)
                                Log.i(TAG, votes.code().toString())
                                if (votes.code() == 200) {
                                    for (song: Song in songList) {
                                        song.vote = votes.body()!!.votes!!.count { it.equals(song.country!!.countryCode) }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.i(TAG, e.message)
                            }
                        } catch (e: Exception) {
                            Log.i(TAG, e.message)
                        }
                        adaptSongsLayout()
                        updateTotalCounter()
                    }

                } else {
                    Log.e(TAG, task.exception.toString())
                }
            }
    }

    fun sendVote() {
        if(idToken != null) {
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
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var body = RequestBody.create(MediaType.parse("application/json"), bodyJson.toString())
                    var sendVote = service.sendVote(idToken, body)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, R.string.vote_saved, Toast.LENGTH_LONG)
                    }
                } catch (e: Exception) {
                    Log.i(TAG, e.message)
                }
            }
        }
    }
}
