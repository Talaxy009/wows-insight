# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.talaxy.wowsinsight.**$$serializer { *; }
-keepclassmembers class com.talaxy.wowsinsight.** {
    *** Companion;
}
-keepclasseswithmembers class com.talaxy.wowsinsight.** {
    kotlinx.serialization.KSerializer serializer(...);
}
