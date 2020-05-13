package com.badkrist.euroscorer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.badkrist.euroscorer.model.Song
import com.squareup.picasso.Picasso

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

        playButton.setOnClickListener {
            var stringArray: List<String> = song.link!!.split('/')
            (context as MainActivity).startVideoPlayer(stringArray.get(stringArray.size - 1))
        }
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
        rowView.setOnClickListener {
            if(position == selectedPosition) {
                selectedPosition = -1
            } else {
                selectedPosition = position
            }
            notifyDataSetChanged()
        }
        if(selectedPosition == position)
            rowView.findViewById<ConstraintLayout>(R.id.song_details_layout).visibility = View.VISIBLE
        else
            rowView.findViewById<ConstraintLayout>(R.id.song_details_layout).visibility = View.GONE
        return rowView
    }
}