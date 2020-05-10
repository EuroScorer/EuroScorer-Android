package com.badkrist.euroscorer.model

import com.squareup.moshi.Json

data class Song(
    @field:Json(name = "image_original") val image_original: String? = "",
    @field:Json(name = "image") val image: String? = "",
    @field:Json(name = "link") var link: String? = "",
    @field:Json(name = "number") var number: Int? = 0,
    @field:Json(name = "title") var title: String? = "",
    @field:Json(name = "country") var country: Country? = null,
    var vote: Int = 0
)