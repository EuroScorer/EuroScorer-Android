package com.badkrist.euroscorer.utils

object Utils {
    var prefixes: Map<String, String> = mapOf(
        "355" to "AL",
        "374" to "AM",
        "43" to "AT",
        "61" to "AU",
        "994" to "AZ",
        "32" to "BE",
        "359" to "BG",
        "375" to "BY",
        "41" to "CH",
        "357" to "CY",
        "420" to "CZ",
        "49" to "DE",
        "45" to "DK",
        "372" to "EE",
        "34" to "ES",
        "358" to "FI",
        "33" to "FR",
        "44" to "GB",
        "995" to "GE",
        "30" to "GR",
        "385" to "HR",
        "36" to "HU",
        "353" to "IE",
        "972" to "IL",
        "354" to "IS",
        "39" to "IT",
        "370" to "LT",
        "371" to "LV",
        "373" to "MD",
        "389" to "MK",
        "356" to "MT",
        "31" to "NL",
        "47" to "NO",
        "48" to "PL",
        "351" to "PT",
        "40" to "RO",
        "381" to "RS",
        "7" to "RU",
        "46" to "SE",
        "386" to "SI",
        "378" to "SM",
        "380" to "UA"
    )


    @JvmStatic
    public fun getCountryCodeFromPhoneNumber(phoneNumber: String): String  {
        var twoDigitCode = phoneNumber.substring(1, 3)
        var threeDigitCode = phoneNumber.substring(1, 4)
        if (prefixes[twoDigitCode] != null) {
            return prefixes[twoDigitCode]!!
        }
        return prefixes[threeDigitCode]!!
    }
}