package com.talaxy.wowsinsight.data.model

import java.util.Locale

/**
 * Language used to pick a ship name from the encyclopedia payload, which
 * carries a Chinese and an English name for every ship.
 */
enum class ShipNameLanguage {
    ENGLISH,
    CHINESE,
    ;

    companion object {
        /**
         * Both zh-Hans and zh-Hant use the Chinese names; ja / ko and every
         * other language fall back to English like the iOS app.
         */
        fun current(): ShipNameLanguage =
            if (Locale.getDefault().language == "zh") CHINESE else ENGLISH
    }
}
