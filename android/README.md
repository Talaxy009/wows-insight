# WoWs Insight for Android

原生 Android 客户端，使用 Kotlin + Jetpack Compose + Material 3 实现。功能与仓库里的 [iOS 版本](../ios/README.md) 对齐，共用同一套 [Yuyuko](https://v3-api.wows.shinoaki.com/v3/index.html) 公共接口，收藏的 JSON 导出格式也完全互通。界面遵循 Material 3 与 Android 的交互习惯，不照搬 iOS 布局。

- 系统要求：Android 8.0（API 26）及以上
- 开发环境：Android Studio 2025.2 或更新版本、JDK 21、Android SDK 35

## 功能

- 五个服务器：俄服 / 欧服 / 美服 / 亚服 / 国服。
- 防抖昵称搜索（350 ms、至少 3 个字符），国服使用接口的精确匹配模式。
- 玩家页：战斗类型切换、总览、详细数据、最高记录、个人评分与评分档位说明。
- 近期每日评分 / 胜率趋势，以及按舰种 / 按等级的评分分布；没有数据的卡片整张隐藏。
- 战舰列表按当前战斗类型过滤，可按场数 / 胜率 / 平均伤害 / 评分 / 最近战斗排序，
  点进单舰查看详情。
- 本地收藏：添加 / 删除、拖动排序、TalkBack 上移 / 下移操作，以及与 iOS 互通的
  版本化 JSON 导入 / 导出。
- 浅色 / 深色主题、Android 12+ 的 Material You 动态取色、edge-to-edge、下拉刷新、
  按应用设置语言。
- 界面语言：英语 / 简体中文 / 繁体中文 / 日语 / 韩语。舰名规则与 iOS 一致：
  中文界面用中文舰名，其他界面用英文舰名，国服优先使用 360 译名。

## 运行与构建

用 Android Studio 打开 `android/` 目录，等待 Gradle 同步完成后直接运行 `app`。如果 IDE 没有生成 `local.properties`，在其中写入 `sdk.dir=<Android SDK 路径>`。

命令行构建：

```sh
# Debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleDebug

# 单元测试
./gradlew :app:testDebugUnitTest

# Android lint（有 error 会失败）
./gradlew :app:lintDebug

# Release APK：配置签名后是已签名包，否则是未签名包
./gradlew :app:assembleRelease
```

Windows 使用 `gradlew.bat`。Gradle 8.13 需要 JDK 21，不要用 Android Studio 2026.2 自带的 JBR 25；JDK 路径、签名配置和 Debug 图表预览等开发细节见 [AGENTS.md](./AGENTS.md)。

本地 Debug 包可以直接安装到已连接的设备：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 发布

发布由仓库根目录的 [`.github/workflows/release.yml`](../.github/workflows/release.yml) 手动触发，和 iOS 一起发布到同一个 GitHub Release。Android 产物是已签名的 `WoWsInsight-<版本>.apk`，可以直接安装，版本号与 iOS 保持一致；签名、secrets 和产物细节见 [AGENTS.md](./AGENTS.md#版本号与发布)。

## 已知限制

- Yuyuko 是社区服务，没有 SLA。请求失败会显示重试操作；刷新已加载的页面失败时保留旧数据。
- 未查询过的玩家首次打开需要 6–8 秒（服务端要回源 Wargaming），之后会快很多。
- 服务端只对观察过的账号保留每日记录；少于两天数据时趋势卡片隐藏。
- 个人评分由服务端按各服务器期望值计算，可能和直接查 wows-numbers 有出入。
- 收藏只在本机，没有云同步；换设备请用导出 / 导入。
- 最低支持 Android 8.0（API 26），动态取色只在 Android 12+ 生效。

评分分档、颜色和计算公式见[根目录 README](../README.md#个人评分怎么看)。
