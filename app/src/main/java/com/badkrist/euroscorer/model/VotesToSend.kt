package com.badkrist.euroscorer.model

import com.google.gson.annotations.SerializedName

data class VotesToSend(
    @SerializedName("votes") var votes: ArrayList<String> = ArrayList()
)