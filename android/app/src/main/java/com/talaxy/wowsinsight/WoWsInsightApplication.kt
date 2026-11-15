package com.talaxy.wowsinsight

import android.app.Application

class WoWsInsightApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
