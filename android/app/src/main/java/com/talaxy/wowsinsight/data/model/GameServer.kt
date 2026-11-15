package com.talaxy.wowsinsight.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The World of Warships realms exposed by the Yuyuko API.
 *
 * [apiValue] doubles as the API path segment and the value persisted in a
 * favorites archive. The declaration order is the order of the server picker.
 */
@Serializable
enum class GameServer(
    val apiValue: String,
    val usesExactSearch: Boolean = false,
) {
    @SerialName("ru")
    RU("ru"),

    @SerialName("eu")
    EU("eu"),

    @SerialName("na")
    NA("na"),

    @SerialName("asia")
    ASIA("asia"),

    @SerialName("cn")
    CN("cn", usesExactSearch = true),
    ;

    companion object {
        fun fromApi(value: String?): GameServer? =
            entries.firstOrNull { it.apiValue == value?.lowercase() }
    }
}
