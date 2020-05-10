package com.badkrist.euroscorer.model

import com.squareup.moshi.Json

data class Country(
    @field:Json(name = "code") val countryCode: String? = "",
    @field:Json(name = "flag") val flag: String? = "",
    @field:Json(name = "name") val name: String? = ""
)