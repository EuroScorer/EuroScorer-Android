package com.badkrist.euroscorer

import android.content.ComponentCallbacks2
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.badkrist.euroscorer.model.Song
import com.badkrist.euroscorer.model.Votes
import com.badkrist.euroscorer.model.VotesToSend
import com.badkrist.euroscorer.service.FireBaseServices
import com.badkrist.euroscorer.utils.Utils
import com.google.android.youtube.player.YouTubeStandalonePlayer
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.math.min


class MainActivity : AppCompatActivity()  {
    var TAG = "MainActivity"
    private lateinit var idToken: String
    private lateinit var userCountryCode: String
    var songList: List<Song> = ArrayList();

    private lateinit var songsLayout: ListView
    private lateinit var totalCounter: TextView
    private var totalCount: Int = 0
    private lateinit var sendButton: Button
    private lateinit var service: FireBaseServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        songsLayout = findViewById(R.id.listview_songs)
        totalCounter = findViewById(R.id.total_counter)
        sendButton = findViewById(R.id.send_vote_button)
        sendButton.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                sendVote()
            }
        })
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

class SongAdapter(private val context: Context,
                  private val dataSource: ArrayList<Song>) : BaseAdapter() {
    private var selectedPosition: Int = -1
    private val inflater: LayoutInflater
            = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int {
        return dataSource.size
    }

    override fun getItem(position: Int): Any {
        return dataSource[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val rowView = inflater.inflate(R.layout.song_item, parent, false)
        val songNameView = rowView.findViewById<TextView>(R.id.song_item_songname)
        val countryNameView = rowView.findViewById<TextView>(R.id.song_item_countryname)
        val voteSummaryView = rowView.findViewById<TextView>(R.id.song_item_vote_summary)
        val playButton = rowView.findViewById<Button>(R.id.song_details_play_button)
        val minusButton = rowView.findViewById<Button>(R.id.song_details_minus)
        val plusButton = rowView.findViewById<Button>(R.id.song_details_plus)
        val voteCounter = rowView.findViewById<TextView>(R.id.song_details_vote)
        val song = getItem(position) as Song

        songNameView.text = song.title
        countryNameView.text = song.country!!.name
        voteSummaryView.text = song.vote.toString() + if(song.vote < 2)  "vote" else "votes"
        voteCounter.text = song.vote.toString()

        playButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                var stringArray: List<String> = song.link!!.split('/')
                (context as MainActivity).startVideoPlayer(stringArray.get(stringArray.size - 1))
            }

        })
        if((context as MainActivity).getUserCountryCode().equals(song.country!!.countryCode)) {
            rowView.findViewById<LinearLayout>(R.id.song_details_vote_layout).visibility = View.INVISIBLE
            voteSummaryView.visibility = View.INVISIBLE
        } else {
            minusButton.isEnabled = song.vote > 0
            plusButton.isEnabled = song.vote < 20 && (context as MainActivity).getTotalCount() > 0
            minusButton.setOnClickListener {
                song.vote--
                voteSummaryView.text =
                    song.vote.toString() + if (song.vote < 2) "vote" else "votes"
                voteCounter.text = song.vote.toString()
                (context as MainActivity).updateTotalCounter()
                minusButton.isEnabled = song.vote > 0
                plusButton.isEnabled = true
            }
            plusButton.setOnClickListener {
                song.vote++
                voteSummaryView.text =
                    song.vote.toString() + if (song.vote < 2) "vote" else "votes"
                voteCounter.text = song.vote.toString()
                (context as MainActivity).updateTotalCounter()
                plusButton.isEnabled = (context as MainActivity).getTotalCount() > 0
                minusButton.isEnabled = true
            }
        }

        Picasso.get()
            .load(song.country!!.flag)
            .resize(50, 50)
            .centerCrop()
            .into(rowView.findViewById<ImageView>(R.id.song_item_flag))

        rowView.tag = position
        rowView.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
               if(position == selectedPosition) {
                   selectedPosition = -1
               } else {
                   selectedPosition = position
               }
                notifyDataSetChanged()
            }
        })
        if(selectedPosition == position)
            rowView.findViewById<ConstraintLayout>(R.id.song_details_layout).visibility = View.VISIBLE
        else
            rowView.findViewById<ConstraintLayout>(R.id.song_details_layout).visibility = View.GONE
        return rowView
    }
}