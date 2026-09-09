package com.lezzwatch.app.data.parser

/**
 * ISO-3166-1 alpha-2 country code -> display name lookup.
 *
 * Many public IPTV playlists (including the bundled one) encode a country hint as a two-letter
 * code inside `tvg-id`, e.g. `tvg-id="BBCNews.uk@SD"`. This table turns that code into a
 * human-readable name for filtering/display. It intentionally covers the full ISO-3166-1 list
 * (not just the codes seen in the bundled playlist) so a future/replacement playlist with
 * different country coverage still resolves correctly without code changes.
 */
object CountryCodes {

    private val codeToName: Map<String, String> = mapOf(
        "ad" to "Andorra", "ae" to "United Arab Emirates", "af" to "Afghanistan",
        "ag" to "Antigua and Barbuda", "ai" to "Anguilla", "al" to "Albania", "am" to "Armenia",
        "ao" to "Angola", "ar" to "Argentina", "as" to "American Samoa", "at" to "Austria",
        "au" to "Australia", "aw" to "Aruba", "ax" to "Åland Islands", "az" to "Azerbaijan",
        "ba" to "Bosnia and Herzegovina", "bb" to "Barbados", "bd" to "Bangladesh",
        "be" to "Belgium", "bf" to "Burkina Faso", "bg" to "Bulgaria", "bh" to "Bahrain",
        "bi" to "Burundi", "bj" to "Benin", "bm" to "Bermuda", "bn" to "Brunei",
        "bo" to "Bolivia", "br" to "Brazil", "bs" to "Bahamas", "bt" to "Bhutan",
        "bw" to "Botswana", "by" to "Belarus", "bz" to "Belize", "ca" to "Canada",
        "cd" to "DR Congo", "cf" to "Central African Republic", "cg" to "Congo",
        "ch" to "Switzerland", "ci" to "Côte d'Ivoire", "ck" to "Cook Islands", "cl" to "Chile",
        "cm" to "Cameroon", "cn" to "China", "co" to "Colombia", "cr" to "Costa Rica",
        "cu" to "Cuba", "cv" to "Cabo Verde", "cw" to "Curaçao", "cy" to "Cyprus",
        "cz" to "Czechia", "de" to "Germany", "dj" to "Djibouti", "dk" to "Denmark",
        "dm" to "Dominica", "do" to "Dominican Republic", "dz" to "Algeria", "ec" to "Ecuador",
        "ee" to "Estonia", "eg" to "Egypt", "er" to "Eritrea", "es" to "Spain",
        "et" to "Ethiopia", "fi" to "Finland", "fj" to "Fiji", "fk" to "Falkland Islands",
        "fm" to "Micronesia", "fo" to "Faroe Islands", "fr" to "France", "ga" to "Gabon",
        "gb" to "United Kingdom", "gd" to "Grenada", "ge" to "Georgia", "gf" to "French Guiana",
        "gg" to "Guernsey", "gh" to "Ghana", "gi" to "Gibraltar", "gl" to "Greenland",
        "gm" to "Gambia", "gn" to "Guinea", "gp" to "Guadeloupe", "gq" to "Equatorial Guinea",
        "gr" to "Greece", "gt" to "Guatemala", "gu" to "Guam", "gw" to "Guinea-Bissau",
        "gy" to "Guyana", "hk" to "Hong Kong", "hn" to "Honduras", "hr" to "Croatia",
        "ht" to "Haiti", "hu" to "Hungary", "id" to "Indonesia", "ie" to "Ireland",
        "il" to "Israel", "im" to "Isle of Man", "in" to "India", "iq" to "Iraq",
        "ir" to "Iran", "is" to "Iceland", "it" to "Italy", "je" to "Jersey",
        "jm" to "Jamaica", "jo" to "Jordan", "jp" to "Japan", "ke" to "Kenya",
        "kg" to "Kyrgyzstan", "kh" to "Cambodia", "ki" to "Kiribati", "km" to "Comoros",
        "kn" to "Saint Kitts and Nevis", "kp" to "North Korea", "kr" to "South Korea",
        "kw" to "Kuwait", "ky" to "Cayman Islands", "kz" to "Kazakhstan", "la" to "Laos",
        "lb" to "Lebanon", "lc" to "Saint Lucia", "li" to "Liechtenstein", "lk" to "Sri Lanka",
        "lr" to "Liberia", "ls" to "Lesotho", "lt" to "Lithuania", "lu" to "Luxembourg",
        "lv" to "Latvia", "ly" to "Libya", "ma" to "Morocco", "mc" to "Monaco",
        "md" to "Moldova", "me" to "Montenegro", "mg" to "Madagascar", "mh" to "Marshall Islands",
        "mk" to "North Macedonia", "ml" to "Mali", "mm" to "Myanmar", "mn" to "Mongolia",
        "mo" to "Macao", "mp" to "Northern Mariana Islands", "mq" to "Martinique",
        "mr" to "Mauritania", "ms" to "Montserrat", "mt" to "Malta", "mu" to "Mauritius",
        "mv" to "Maldives", "mw" to "Malawi", "mx" to "Mexico", "my" to "Malaysia",
        "mz" to "Mozambique", "na" to "Namibia", "nc" to "New Caledonia", "ne" to "Niger",
        "ng" to "Nigeria", "ni" to "Nicaragua", "nl" to "Netherlands", "no" to "Norway",
        "np" to "Nepal", "nr" to "Nauru", "nu" to "Niue", "nz" to "New Zealand",
        "om" to "Oman", "pa" to "Panama", "pe" to "Peru", "pf" to "French Polynesia",
        "pg" to "Papua New Guinea", "ph" to "Philippines", "pk" to "Pakistan",
        "pl" to "Poland", "pm" to "Saint Pierre and Miquelon", "pr" to "Puerto Rico",
        "ps" to "Palestine", "pt" to "Portugal", "pw" to "Palau", "py" to "Paraguay",
        "qa" to "Qatar", "re" to "Réunion", "ro" to "Romania", "rs" to "Serbia",
        "ru" to "Russia", "rw" to "Rwanda", "sa" to "Saudi Arabia", "sb" to "Solomon Islands",
        "sc" to "Seychelles", "sd" to "Sudan", "se" to "Sweden", "sg" to "Singapore",
        "si" to "Slovenia", "sk" to "Slovakia", "sl" to "Sierra Leone", "sm" to "San Marino",
        "sn" to "Senegal", "so" to "Somalia", "sr" to "Suriname", "ss" to "South Sudan",
        "st" to "São Tomé and Príncipe", "sv" to "El Salvador", "sx" to "Sint Maarten",
        "sy" to "Syria", "sz" to "Eswatini", "tc" to "Turks and Caicos Islands", "td" to "Chad",
        "tg" to "Togo", "th" to "Thailand", "tj" to "Tajikistan", "tl" to "Timor-Leste",
        "tm" to "Turkmenistan", "tn" to "Tunisia", "to" to "Tonga", "tr" to "Türkiye",
        "tt" to "Trinidad and Tobago", "tv" to "Tuvalu", "tw" to "Taiwan", "tz" to "Tanzania",
        "ua" to "Ukraine", "ug" to "Uganda", "uk" to "United Kingdom", "us" to "United States",
        "uy" to "Uruguay", "uz" to "Uzbekistan", "va" to "Vatican City",
        "vc" to "Saint Vincent and the Grenadines", "ve" to "Venezuela",
        "vg" to "British Virgin Islands", "vi" to "U.S. Virgin Islands", "vn" to "Vietnam",
        "vu" to "Vanuatu", "ws" to "Samoa", "ye" to "Yemen", "za" to "South Africa",
        "zm" to "Zambia", "zw" to "Zimbabwe",
    )

    /** Resolves a 2-letter code (case-insensitive) to a display name, or null if unrecognized. */
    fun nameFor(code: String?): String? {
        if (code.isNullOrBlank()) return null
        return codeToName[code.trim().lowercase()]
    }
}
