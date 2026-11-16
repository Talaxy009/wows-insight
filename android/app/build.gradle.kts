import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// 签名配置的读取顺序：环境变量（CI）> android/keystore.properties（本地日常打包）。
// 两者都没有时仍可构建未签名包用于调试，但发布流程会拒绝未签名产物。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun signingSetting(envName: String, propertyName: String): String? =
    System.getenv(envName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val releaseKeystorePath: String? = signingSetting("ANDROID_KEYSTORE_PATH", "storeFile")
val releaseKeystorePassword: String? = signingSetting("ANDROID_KEYSTORE_PASSWORD", "storePassword")
val releaseKeyAlias: String? = signingSetting("ANDROID_KEY_ALIAS", "keyAlias")
val releaseKeyPassword: String? = signingSetting("ANDROID_KEY_PASSWORD", "keyPassword")
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

// 版本号的唯一来源是仓库根目录的 version.xcconfig（iOS 工程也引用同一个文件）。
// CI 仍可用 -PversionName / -PversionCode 覆盖；本地构建不传时使用共享文件里的值。
// 通过 providers.fileContents 读取，改版本号后 configuration cache 会自动失效。
val sharedVersionFile = rootProject.layout.projectDirectory.file("../version.xcconfig")
val sharedVersionText: String = providers.fileContents(sharedVersionFile).asText.get()

fun sharedVersionValue(key: String): String =
    sharedVersionText.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("//") }
        .mapNotNull { line ->
            val separator = line.indexOf('=')
            if (separator <= 0) {
                null
            } else {
                line.substring(0, separator).trim() to line.substring(separator + 1).trim()
            }
        }
        .firstOrNull { (name, _) -> name == key }
        ?.second
        ?.takeIf { it.isNotEmpty() }
        ?: error("${sharedVersionFile.asFile} 缺少 ${key}")

val sharedVersionName: String =
    (findProperty("versionName") as String?) ?: sharedVersionValue("VERSION_NAME")
val sharedVersionCode: Int =
    (findProperty("versionCode") as String?)?.toIntOrNull()
        ?: sharedVersionValue("VERSION_CODE").toIntOrNull()
        ?: error("version.xcconfig 的 VERSION_CODE 必须是整数")

android {
    namespace = "com.talaxy.wowsinsight"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.talaxy.wowsinsight"
        minSdk = 26
        targetSdk = 35
        // 默认取 version.xcconfig；CI 用 -PversionName / -PversionCode 覆盖。
        versionCode = sharedVersionCode
        versionName = sharedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
        )
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
