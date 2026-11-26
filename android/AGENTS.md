# AGENTS.md（Android）

面向在 `android/` 目录工作的开发者 / AI 代理。用户视角的说明见 [README.md](./README.md)；
两个平台共用的接口口径、评分算法、收藏格式和发布流程见[根目录 AGENTS.md](../AGENTS.md)。
可复用的多步流程见 [`.agents/skills/`](../.agents/skills/)（多语言、应用图标、收藏格式、发布、接口口径）。

## 技术栈与版本

- Kotlin 2.2.21 / Jetpack Compose（BOM 2025.06.01）/ Material 3
- Gradle 8.13（wrapper）+ AGP 8.13.2，依赖版本统一在 `gradle/libs.versions.toml` 管理
- JDK 21、compileSdk 35、targetSdk 35、minSdk 26
- Android Studio 2025.2 或更新版本（本仓库在 2026.2 上验证）；如果 IDE 没生成
  `local.properties`，手工写入 `sdk.dir=<Android SDK 路径>`
- 导航 Navigation Compose，路由用 `@Serializable` 类型安全声明
- 网络 OkHttp + kotlinx.serialization，图片 Coil，持久化 Preferences DataStore
- 依赖注入是手写的 `AppContainer`，没有 Hilt / Koin
- 图表用 Compose Canvas 自绘，**不要引入图表库**

## 工程结构

```txt
android/
├── README.md
├── AGENTS.md
├── app/src/main/java/com/talaxy/wowsinsight/
│   ├── AppContainer.kt / MainActivity.kt / WoWsInsightApplication.kt
│   ├── util/              // runCatchingCancellable 等与层无关的小工具
│   ├── data/
│   │   ├── model/         // 领域模型、64 位 ID、评分档位、收藏模型
│   │   ├── remote/        // OkHttp 客户端、DTO 与映射
│   │   ├── repository/    // 并发加载玩家报告
│   │   └── favorites/     // DataStore 与 SAF 导入导出
│   └── ui/
│       ├── search/        // 首页与防抖搜索
│       ├── player/        // 玩家页、图表、评分说明
│       ├── ship/          // 单舰详情
│       ├── about/         // 关于页
│       ├── components/    // 卡片、统计格子、Canvas 图表
│       ├── navigation/    // 类型安全路由与 NavHost
│       ├── theme/         // Material 3 颜色与字体
│       └── util/          // 格式化、UiText、无障碍标签
├── app/src/debug/         // 仅 Debug 的图表预览 Activity
└── app/src/test/          // JVM 单元测试
```

应用图标源文件在仓库级的 `docs/icon.svg`（两个平台共用），不放在 `android/` 下。

## 架构与编码约定

- **分层**：`data/model`（纯 Kotlin 领域模型）、`data/remote`（DTO、映射、HTTP）、
  `data/repository`（组合接口）、`ui/*`（页面 + ViewModel）。UI 不直接依赖 OkHttp / DTO。
- **接口化**：`YuyukoApi`、`PlayerRepository`、`FavoritesRepository` 都是 interface，测试里用
  fake 实现；`Default*` 是生产实现。`FavoritesFileService` 是封装 SAF 的具体实现，不参与
  ViewModel 的接口替换。
- **状态**：ViewModel 暴露 `StateFlow<UiState>`，用 `combine` / `stateIn` 聚合；
  Compose 侧用 `collectAsStateWithLifecycle()`。不要往 Composable 里塞业务状态。
- **搜索**：`SearchViewModel` 用 `combine(query, server, retryToken)` + `distinctUntilChanged()`
  + `flatMapLatest`，实现 350ms 防抖和「旧请求自动取消」；输入不足 3 个字符直接回到 Idle。
- **取消**：`YuyukoApi` 用 `suspendCancellableCoroutine` 包 OkHttp，协程取消会 cancel 底层
  `Call`；协程里要捕获失败时用 `util/Coroutines.kt` 的 `runCatchingCancellable`（不会吞掉
  `CancellationException`），不要手写 try/catch，更不要直接用 `runCatching`。
- **错误**：`ApiException` 区分 HTTP / 信封 code / 网络 / 解码；UI 把异常转成 `UiText`
  后显示，`UiText` 用字符串资源 id，不要在 ViewModel 里拼死字符串。
- **导航**：`ui/navigation/Routes.kt` 使用 `@Serializable` 类型安全路由（`SearchRoute` /
  `PlayerRoute` / `ShipRoute` / `AboutRoute`），新增页面时同时加 route 和 `WoWsInsightApp` 的
  composable。
- **Material 3**：界面按 Material 3 和 Android 指南实现（下拉刷新、edge-to-edge、
  TalkBack 操作等），不要为了「和 iOS 一样」复制 iOS 布局。颜色 / 字体走 `ui/theme`，
  不要散落硬编码色值。例外是卡片头部的次要控件（图表卡的评分 / 胜率、舰种 / 等级切换）：
  放进 `SectionCard(trailing = ...)` 和标题同一行，**不要** `fillMaxWidth()` 独占一行——
  两个短选项用满宽分段按钮会压掉图表一整个行高、也抢走标题和数据的视觉层级。
  这类小切换器用 `ui/components/CommonComponents.kt` 的 `SegmentedChoice`（图表卡是唯一使用者，
  放共享位置是因为它跟业务无关）：`SingleChoiceSegmentedButtonRow` 不会按标签自适应宽度，
  而是取父级给它的宽度再按每段 `weight(1f)` 平分，直接塞进标题行会把英文「Ship type」压成
  「Ship ty…」。所以它用 `TextMeasurer` 量出最宽标签再加两侧 padding（12dp，与
  `ButtonDefaults.TextButtonContentPadding` 一致）、乘以段数，把行宽显式传进去，并用
  `BoxWithConstraints` 兜住上限（超大字号时收缩而不是溢出卡片），测量结果按标签 `remember`。
  段内传 `icon = {}` 去掉前置对勾：选中态由填充容器表达，对勾每段要吃约 30dp。
- **下拉菜单对齐**：`DropdownMenu` 默认把菜单左边缘贴住锚点左边缘。需要让菜单靠右（满宽控件如
  战斗类型选择器、右对齐按钮如舰船排序）时，用 `ui/components/CommonComponents.kt` 的
  `EndAlignedDropdownMenu`：它把菜单放进 `Box(Modifier.align(Alignment.BottomEnd))`，这个零尺寸
  锚点位于父 `Box`（按钮）右下角，M3 的定位器发现左对齐会超出窗口后会自动退到“菜单右边缘贴锚点
  右边缘”，不需要用 `offset` 猜菜单宽度。新增这类菜单直接用这个组件，不要再手写那一层 Box。
- **二级页顶栏**：玩家页、单舰页、关于页统一用 `BackTopAppBar(title, onBack, actions)`，不要再各写一遍
  `TopAppBar` + 返回箭头。
- **玩家行**：搜索结果行（`PlayerRowCard`）和收藏行共用 `PlayerIdentity`（昵称 + 「服务器 · ID」）和
  `PlayerRowTrailing`（收藏星 + 箭头）；收藏的身份比较统一用 `FavoritePlayer.idOf(server, accountId)`。
- **玩家页头部**：`AccountInfo.clanName` 由 `YuyukoClanInfo.displayName` 统一格式化成
  `[TAG] Name`。没有公会时为 `null`，头部不显示公会行；有公会时用
  `InfoRow(valueMaxLines = 2)`，长名称换行而不是把「公会」标签挤掉。
- **玩家页列表**：战舰行是 `LazyColumn` 的独立 item（几百艘舰只组合屏幕内的行），圆角卡片效果由
  `stackedItemShape` 按首 / 尾行切角拼成；所以这个 `LazyColumn` 不用 `spacedBy`，各区块的间距是
  `sectionItem` 给的底部 padding。排序放在 `remember` 里，不要在 item 构建里排序。
- **可复用的行级组件**：玩家行的次要行统一用 `serverAccountLabel(server, accountId)`（「亚服 · ID …」），
  收藏星统一用 `FavoriteStarButton`（已选时 `Filled.Star` + tertiary 色），列表分组标题统一用
  `ListHeader(title, count)`，菜单里标记当前项的行统一用 `SelectedMenuItem`。新增同类界面直接拼这些
  组件，不要各写一遍 `stringResource` + 三元色，否则改文案或配色时必然漏改一处。
- **关于页**：首页「⋯」菜单最后一项进入 `AboutScreen`。图标用 `res/drawable-nodpi/app_icon.png`
  （`AppIcon.png` 的方形副本；adaptive icon 不能直接当普通图片画），版本号取
  `BuildConfig.VERSION_NAME` / `BuildConfig.VERSION_CODE`（与 `version.xcconfig` 同源），
  外链用 `LocalUriHandler` 打开并用 `runCatching` 兜住没有浏览器的设备，文案全部走
  `R.string.about_*`。
- **日期显示**：`StatFormat.date` 用 `FormatStyle.MEDIUM`（「2026年11月14日」/「Nov 14, 2026」），
  与 iOS 的 `.abbreviated` 对齐，别换成 `SHORT`（中文会退化成「2016/10/21」、英文变「10/21/16」）。
  这个日期比纯数字宽，战舰列表行内副标题（舰种 · 日期 · 胜率，按「最近战斗」排序时）一行放不下，
  所以那里是 `maxLines = 2` 换行而不是省略号——写成 `maxLines = 1` 会把胜率截掉。
- **趋势图窗口**：`PlayerReport.trendDays` 只保留轴窗口（最后一条记录往前七天）内的天，趋势卡是否
  出现（`hasTrend`）按过滤后的条数 `> 1` 判断，和 iOS 的 `plottedDays` 一致。
- **趋势图横坐标**：接口的每日记录最多一周，所以 `TrendChart` 的 x 轴固定是「最后一条记录往前
  七天」，不是「有战斗的那几天」：日期刻度落在 1 / 3 / 5 / 7 天，空隙在右侧出现在最后一天没
  战斗时也不会让刻度跳位。为此它同时接 `days`（当前类型有战斗的天，用来画点）和 `window`
  （整份记录，只用来定轴）；日期文字以虚线为中线，两端各留半天的 horizontal inset，让最外侧
  的日期不会被 `coerceIn` 推到左边。
- **Compose 性能**：列表用 `LazyColumn` + 稳定 key；图表数据计算放在 `ui/components/ChartMath.kt`
  并保持纯函数，便于单元测试。

## 数据口径

- `accountId` / `shipId` / 各种 ID 一律 `Long`。现有亚服账号 ID 已超过 `Int.MAX_VALUE`。
- 伤害、经验、潜在伤害、侦查伤害、最高记录等总量用 `Long`；单场次数可以保持 `Int`。
- DTO 字段全部可空，缺失时归一化成 0 / `unknown`；任何缺失都不能让页面崩溃。
- 信封 `code == 200` 才算成功；HTTP 404 / 405 与信封 404 / 405 统一为 `ApiException.NotFound`。
- 国服搜索（`server.usesExactSearch`）收到 `code 540` 时当作空结果，不弹错误。
- 平局由 `battles - wins - losses` 推出；命中率直接用 `ratioMain`。
- 玩家评分取 `battleTypeInfo[code].prInfo.value`，战舰评分取 `typeInfo[code].prInfo.value`，
  跟随当前战斗类型；顶层 `prInfo` 是「全部模式」口径，不使用。`prInfo.code == 0` 表示
  暂无评分，显示为 `—`，分档见根 [AGENTS.md](../AGENTS.md#个人评分)。
- `recentDays` 是尽力而为：任何失败都返回空列表，玩家页照常显示，只是没有趋势卡。
- Yuyuko 首次查询某个玩家要回源 Wargaming，约 6–8 秒；OkHttp 超时 30 秒，命中缓存后
  0.3–0.6 秒。
- 不做离线缓存；收藏是唯一持久化的用户数据。

## 收藏

- Preferences DataStore 键 `favorites.players` 存 `List<FavoritePlayer>` 的 JSON 数组，读改写用
  `Mutex` 串行化；导入 / 导出走 SAF（`FavoritesFileService`）。
- 无障碍的「上移 / 下移」走 `moveBy`，和拖动共用同一套顺序逻辑。
- 格式与导入语义见根 [AGENTS.md](../AGENTS.md#收藏文件)，改动清单见
  [`wows-favorites-archive`](../.agents/skills/wows-favorites-archive/SKILL.md)。

## 多语言与字符串资源

`values*/strings.xml` 全部由 `.agents/skills/wows-localization/scripts/generate_strings.py`
从 iOS 的 `Localizable.xcstrings` 生成，**不要手改**；Android 独有文案放脚本的 `EXTRA` 字典。
舰名规则在 `data/model/ShipNameLanguage.kt`，与 iOS 一致。完整流程见
[`wows-localization`](../.agents/skills/wows-localization/SKILL.md)。

## 构建与验证

```sh
cd android

# Debug APK
./gradlew :app:assembleDebug

# 单元测试（当前 42 个）
./gradlew :app:testDebugUnitTest

# lint（有 error 会失败）
./gradlew :app:lintDebug

# Release APK（keystore.properties 或环境变量就绪时签名，否则为未签名包）
./gradlew :app:assembleRelease
```

Windows 用 `gradlew.bat`；Gradle 8.13 需要 JDK 21（不要用 Android Studio 2026.2 自带的 JBR 25 跑 Gradle 8.13）：

```powershell
$env:JAVA_HOME = "D:\Program Files\Java\jdk-21.0.12.1"
.\gradlew.bat :app:assembleDebug
```

产物路径：Debug 是 `app/build/outputs/apk/debug/app-debug.apk`；Release 配置签名后是
`app/build/outputs/apk/release/app-release.apk`，缺签名配置时是 `app-release-unsigned.apk`
（仅供本地调试，CI 会拒绝）。

> 改过 `applicationId` / `namespace` 后，Android Studio 需要执行一次
> **Sync Project with Gradle Files**（必要时 Invalidate Caches），否则 IDE 会继续用缓存的旧
> applicationId 启动，报 `Activity class {旧包名/新类名} does not exist`。

改动数据模型 / 映射 / 收藏逻辑时至少跑 `:app:testDebugUnitTest`，并尽量跑一次
`:app:lintDebug`。改 UI 后如需真机验证：

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.talaxy.wowsinsight/.MainActivity
```

趋势图通常没有真实数据，用 Debug 的预览 Activity 检查布局：

```sh
adb shell am start -n com.talaxy.wowsinsight/.ui.preview.ChartPreviewActivity
```

## 版本号与发布

- `versionName` / `versionCode` 默认读根目录 [`version.xcconfig`](../version.xcconfig)，可用
  `-PversionName=1.2.0 -PversionCode=42` 覆盖（CI 就是这样注入的）。
- 签名配置在 `app/build.gradle.kts`：环境变量（CI）优先，其次 `android/keystore.properties`
  （模板 `keystore.properties.example`，`storeFile` 相对 `android/` 解析）；都没有时产出未签名包。
- 同一包名的后续更新必须用同一份 keystore；keystore 和密码不能提交仓库。

secrets、CI 步骤和发布检查清单见 [`wows-release`](../.agents/skills/wows-release/SKILL.md)。

## 已知约定 / 限制

- minSdk 26；动态取色只在 Android 12+ 生效，低版本走固定配色，不要用新 API 时需要版本判断。
- 没有数据库：收藏是小 JSON，DataStore 足够；不要为一个页面引入 Room。
- 没有 Hilt：新增依赖在 `AppContainer` 里显式创建，再通过 ViewModel factory 注入。
- 不复制 iOS 的视觉细节；两端共享的是数据、评分和收藏文件，不是像素。
- 应用图标源文件是仓库级的 `docs/icon.svg`。改完在仓库根目录运行
  `python .agents/skills/wows-app-icon/scripts/build_adaptive_icon.py` 重新生成
  `drawable/ic_launcher_foreground.xml`，不要手改生成的 vector；关于页用的
  `drawable-nodpi/app_icon.png` 与 iOS 的 `AppIconPreview` 是同一张方形图标的副本，换图标时一起
  覆盖。流程见 [`.agents/skills/wows-app-icon/SKILL.md`](../.agents/skills/wows-app-icon/SKILL.md)。
- 文档按平台维护：用户可见变化更新 `android/README.md`，实现约定变化更新本文件；
  可复用的多步流程更新 `.agents/skills/` 下对应的 `SKILL.md`。
