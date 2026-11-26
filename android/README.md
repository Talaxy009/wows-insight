# WoWs Insight for Android

原生 Android 客户端（Kotlin + Jetpack Compose + Material 3），功能、接口与统计口径和 [iOS 版本](../ios/README.md) 一致，功能列表、评分说明和共同限制见[根目录 README](../README.md)。界面遵循 Material 3 与 Android 的交互习惯，不照搬 iOS 布局。

- 系统要求：Android 8.0（API 26）及以上
- 开发环境：Android Studio 2025.2 或更新版本、JDK 21、Android SDK 35

## 平台特性

- 防抖昵称搜索（350 ms、至少 3 个字符）。
- 收藏用拖动手柄排序，并提供 TalkBack 上移 / 下移操作。
- 浅色 / 深色主题、Android 12+ 的 Material You 动态取色、edge-to-edge、下拉刷新。
- 支持 Android 13+ 的按应用设置语言。
- 刷新已加载的页面失败时保留旧数据；没有数据的图表卡片整张隐藏。

## 运行与构建

用 Android Studio 打开 `android/` 目录，等待 Gradle 同步完成后直接运行 `app`。如果 IDE 没有生成 `local.properties`，在其中写入 `sdk.dir=<Android SDK 路径>`。

```sh
./gradlew :app:assembleDebug        # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # 单元测试
./gradlew :app:lintDebug            # lint（有 error 会失败）
./gradlew :app:assembleRelease      # 配置签名后是已签名包，否则是未签名包
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows 使用 `gradlew.bat`。Gradle 8.13 需要 JDK 21，不要用 Android Studio 2026.2 自带的 JBR 25；签名配置和 Debug 图表预览见 [AGENTS.md](./AGENTS.md)。

## 安装发布包

[Releases](https://github.com/Talaxy009/wows-insight/releases) 里的 `WoWsInsight-<版本>.apk` 已签名，可以直接安装。
