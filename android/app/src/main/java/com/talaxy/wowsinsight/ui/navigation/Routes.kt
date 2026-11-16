package com.talaxy.wowsinsight.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

@Serializable
data class PlayerRoute(
    val accountId: Long,
    val nickname: String,
    val server: String,
)

@Serializable
data class ShipRoute(
    val shipId: Long,
    val mode: String,
)

@Serializable
data object AboutRoute
