package com.badkrist.euroscorer.model

import com.squareup.moshi.Json

data class Votes(
    @field:Json(name = "votes") val votes: List<String>? = ArrayList(),
    @field:Json(name = "country") val country: String? = ""
)