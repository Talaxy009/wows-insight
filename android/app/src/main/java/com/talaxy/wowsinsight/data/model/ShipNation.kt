package com.talaxy.wowsinsight.data.model

/** A warship nation. The API has used several spellings over time. */
enum class ShipNation {
    USA,
    JAPAN,
    GERMANY,
    USSR,
    UK,
    FRANCE,
    ITALY,
    PAN_ASIA,
    EUROPE,
    PAN_AMERICA,
    NETHERLANDS,
    SPAIN,
    COMMONWEALTH,
    LATIN_AMERICA,
    EVENTS,
    UNKNOWN,
    ;

    companion object {
        fun fromApi(name: String?): ShipNation = when (name?.lowercase()) {
            "usa" -> USA
            "japan" -> JAPAN
            "germany" -> GERMANY
            "russia", "ussr" -> USSR
            "united_kingdom", "uk" -> UK
            "france" -> FRANCE
            "italy" -> ITALY
            "pan_asia" -> PAN_ASIA
            "europe" -> EUROPE
            "pan_america" -> PAN_AMERICA
            "netherlands" -> NETHERLANDS
            "spain" -> SPAIN
            "commonwealth" -> COMMONWEALTH
            "latin_america" -> LATIN_AMERICA
            "events" -> EVENTS
            else -> UNKNOWN
        }
    }
}
