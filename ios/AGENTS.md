# AGENTS.md（iOS）

面向在 `ios/` 目录工作的开发者 / AI 代理。用户视角的说明见 [README.md](./README.md)；
两个平台共用的接口口径、评分算法、收藏格式和发布流程见[根目录 AGENTS.md](../AGENTS.md)。

## 技术栈

- Xcode 16 或更新版本（工程使用 Xcode 16 的文件夹同步分组，新增文件不需要手改 `project.pbxproj`）
- iOS 18.0+ / Swift 6（严格并发）
- 无第三方依赖、无 CocoaPods / SPM：只用 Foundation + SwiftUI + Observation
- 架构：SwiftUI + `@Observable` + async/await，没有手写 UIKit 生命周期

工程里没有任何 entitlement / CloudKit 容器。收藏不支持 iCloud 同步：免费的个人 Team
（Personal Team）无法签名 iCloud 能力（Xcode 会报 `Personal development teams ... do not
support the iCloud capability`），所以收藏只存在本机，跨设备迁移用首页右上角「⋯」菜单里的
导入 / 导出 JSON 文件。

## 工程结构

```txt
ios/
├── README.md
├── AGENTS.md
├── WoWsInsight.xcodeproj
│   └── xcshareddata/xcschemes/WoWsInsight.xcscheme
└── WoWsInsight/
    ├── App/
    │   ├── WoWsInsightApp.swift        // SwiftUI App 生命周期
    │   ├── AppConfig.swift            // 战舰名称语言（跟随系统语言）
    │   └── L10n.swift                 // 所有界面文案 key 的统一入口
    ├── Models/
    │   ├── GameServer.swift           // 五个服务器与 API 服务器码映射
    │   ├── APIModels.swift            // 界面使用的数据模型
    │   ├── BattleMode.swift           // 战斗类型与 API 代码
    │   ├── BattleStats.swift          // 统计字段、派生数据与最高记录
    │   ├── PersonalRating.swift       // 评分值与评分档位
    │   ├── PlayerReport.swift         // 玩家报告、战舰表现、排序与最高记录
    │   ├── ChartStats.swift           // 图表数据：每日记录、舰种 / 等级分组
    │   ├── FavoritePlayer.swift       // 本地收藏的玩家
    │   ├── FavoritesArchive.swift     // 收藏导入 / 导出的 JSON 文件格式
    │   └── ShipNation.swift           // 战舰国家与本地化标签
    ├── Networking/
    │   ├── YuyukoClient.swift         // async/await API 客户端
    │   └── YuyukoModels.swift         // Yuyuko 响应 DTO 与到界面模型的映射
    ├── ViewModels/
    │   ├── SearchModel.swift          // 搜索、防抖、取消
    │   ├── PlayerModel.swift          // 并发加载玩家总览 + 战舰列表
    │   └── FavoritesStore.swift       // 收藏读写、排序、导入导出
    ├── Views/
    │   ├── RootView.swift             // 根视图、评分说明
    │   ├── SearchView.swift           // 搜索页 + 首页收藏
    │   ├── PlayerView.swift           // 玩家页与战舰列表
    │   ├── PlayerCards.swift          // 玩家页卡片组件
    │   ├── PlayerCharts.swift         // 玩家页图表（趋势、评分分布）
    │   ├── ShipDetailView.swift       // 单舰详情
    │   ├── AboutView.swift            // 关于页（图标 / 版本 / 外链）
    │   ├── Components.swift           // 评分徽章、统计格子等
    │   └── Theme.swift                // 颜色、卡片样式
    ├── Utilities/
    │   ├── Formatting.swift           // 数字 / 百分比 / 日期格式化
    │   └── TaskCancellation.swift     // 取消错误的统一判断
    ├── Localizable.xcstrings          // en / ja / ko / zh-Hans / zh-Hant 界面文案
    ├── InfoPlist.xcstrings            // 各语言 App 名称
    ├── PrivacyInfo.xcprivacy          // UserDefaults 隐私清单
    └── Assets.xcassets                // AppIcon、AppIconPreview、AccentColor
```

页面有四个主界面：搜索 / 收藏首页、玩家详情、单舰详情、关于（从首页「⋯」收藏菜单进入）。

## 实现要点

- **Swift 6 / 严格并发**：`YuyukoClient` 是 `Sendable` 值类型，ViewModel 使用
  `@MainActor @Observable`。
- **取消与防抖**：搜索框每次变化通过 `.task(id:)` 取消上一次请求，等待 350ms 后请求；
  网络返回后检查 `Task.isCancelled` 和请求 key，避免旧结果覆盖新结果。请求 key 用去掉首尾空白的
  昵称，只改空白不会触发新请求。
- **错误口径**：`URLSession` 的 `URLError.cancelled` 在 `YuyukoClient` 里统一转成
  `CancellationError`，页面切换不会弹出错误；HTTP 404 / 405 统一映射为 `notFound`，
  和信封里 `code` 的语义保持一致。
- **并发加载**：玩家总览、战舰列表和每日记录用 `async let` 并发请求；玩家首次查询约 6–8 秒
  （服务端要回源 Wargaming），命中缓存后 0.3–0.6 秒，所以超时设为 30 秒。
- **一次成型**：三个请求都返回后才构建 `PlayerReport`，不再有「总览先出、百科后到」的中间态。
  每日记录例外：`recentDays` 不抛错，失败就当作「没有记录」，页面照常显示。
- **玩家页头像**：昵称左侧显示 `userInfo.dogTag`。它是服务端给的 190×190 PNG 直链，
  没有该字段或取不到图时显示占位暗块，所以模型里存成 `AccountInfo.dogTagURL`。
- **玩家页公会**：`AccountInfo.clanName` 由 `YuyukoClanInfo.displayName` 与 Android
  一致地格式化成 `[TAG] Name`；没有公会时为 `nil`，头部卡片不显示该行。公会行复用
  `PlayerCards.swift` 的 `InfoRow`，用 `lineLimit(2)` 容纳长名称。
- **战斗类型切换**：`PlayerReport` 按 `BattleMode` 保存总览、评分和最高记录，战舰保存每艘舰的
  `[BattleMode: ModeStats]`。玩家页用菜单式 `BattleModePicker` 切换，未选中时回退到
  `defaultMode`（有随机战用随机战，否则用第一个有数据的类型）。
- **图表**：玩家页有两张卡，都跟随当前战斗类型。
  ①「近期趋势」用 Swift Charts 折线 + 面积画 `account/day/list/info` 的 `recentInfo`
  （每天一条当天战绩），在评分 / 胜率之间切换；面积的基线必须显式写成 y 轴下界
  （`AreaMark(yStart:yEnd:)`），默认基线是图表 frame 底部，会盖住日期轴并溢出卡片；少于两天
  不画（一个点不是趋势）。x 轴的窗口固定为「最后一条记录往前七天」，刻度落在第 1 / 3 / 5 / 7
  天，所以最后一天没有战斗时空隙出现在右侧、刻度也不会跳位；为此 `TrendCard` 同时接 `days`
  （当前类型有战斗的天，用来画点）和 `window`（整份记录，只用来定轴）。**窗口外的记录必须丢掉**：
  接口按天保留快照，可能给出早于七天前的记录（例如 11/11、11/15、11/19 三条，最新一条往前七天
  的窗口从 11/13 起），而 Swift Charts 不会把 x 域外的 mark 裁进绘图区——那个点会连同折线、面积
  一起画到绘图区左侧、压住 y 轴标签并溢出卡片。`TrendCard.plottedDays` 只保留窗口内的天，趋势卡
  是否出现也按过滤后的条数（`> 1`）判断；Android 的 `TrendChart` 用同样的过滤。
  胜率会把刻度略微放出 0–100 之外，免得 0% / 100% 的点被裁掉。②「评分分布」取 `info2` 的 `shipTypeInfo` /
  `levelInfo`，在舰种 / 等级之间切换；这张卡是自绘的条形行而不是 Swift Charts 的横向条形图
  ——iOS 26 上分类轴的标签会画进绘图区、和条形重叠。两张卡的选中项存在 `PlayerView` 里，
  因为 `LazyVStack` 回收卡片时会丢掉卡片自己的 `@State`。没有数据的卡片整张隐藏，所以服务端
  没记录过的账号（`code: 412 无recent记录`）只会少一张卡，不会看到空图。两张卡的切换控件由
  `Components.swift` 的 `CardHeader` 排版（图表卡、`StatCard`、战舰列表标题共用它）：
  默认和标题同一行、按内容取宽，**不要给它加固定 `maxWidth`**（以前写死 180pt，日文的
  「レーティング」会被省略号截断）；放不下时 `ViewThatFits` 会退回「标题 + 满宽控件」的两行
  形态，所以标题也不能加 `lineLimit` 以外的压缩手段。趋势图的横坐标也**不用** `AxisValueLabel`：
  日期轴会把标签居中在「每个刻度所在的 band」里，而网格线和数据点画在 band 的边上，两者差半个
  band，看起来就像日期没对准虚线。现在虚线是 `RuleMark`（与数据点同一条 x），日期文字在
  `chartOverlay` 里用 `proxy.position(forX:)` 定位、以该点为圆心居中，所以「虚线—点—日期」
  三者同列；`chartPlotStyle` 的底部 padding 就是给这些文字留的高度，改字号时要同步
  `xAxisLabelHeight`。日期**不要**夹回绘图区（以前会 `clamp` 到容器宽度）：最外侧的日期会被
  推向中间、离开它那条虚线，而两端各半天的 inset 已经让文字有地方溢进 y 轴留白和卡片内边距。
- **没有本地缓存**：评分、舰船名称、图标全部来自服务端，App 不打包期望值数据，也不往 Caches
  里写百科 JSON。
- **收藏**：`FavoritesStore` 是 `@MainActor @Observable`，通过 `@Environment` 注入所有页面；
  收藏列表以 JSON 存入 `UserDefaults`，并在 `PrivacyInfo.xcprivacy` 中声明
  `NSPrivacyAccessedAPICategoryUserDefaults`（CA92.1）。顺序由用户手动拖动决定。
- **收藏列表没有编辑态**：首页右上角「⋯」菜单只有导入 / 导出，没有「编辑」开关，列表也不进
  `EditMode`。删除靠左滑（`swipeActions` 里的 `star.slash`，没有用 `onDelete`），排序靠长按行
  直接拖动（`ForEach.onMove`）。收藏行和搜索结果一样是 `NavigationLink(value:)` +
  `playerCell()`，所以 `NavigationStack` 不需要 `path` 绑定。
- **收藏导入 / 导出**：`FavoritesArchive` 是分享用的 JSON（`version` / `exportedAt` /
  `players`），通过 `ShareLink` 交给系统分享面板（存到「文件」、AirDrop、邮件都行）写入，
  读取用 `.fileImporter`（只接受 `.json`）。只认导出格式本身，读不懂就提示导入失败——不保留
  任何旧格式 / 裸数组的兼容解析。
- **导入语义**：`FavoritesStore.importArchive(_:)` 是「追加合并」而不是「覆盖」：已存在
  （同 `accountId` + 同 `server`）的条目跳过，新的追加到末尾并保留现有顺序，返回
  `ImportSummary(added:skipped:)` 供弹窗提示。这样导入永远不会删掉本机已有的收藏。
- **首页工具栏**：右上角是「⋯」收藏菜单和服务器菜单。服务器菜单的当前服务器名必须用
  `HStack { Image(systemName: "globe"); Text(...) }` 显式排版——工具栏里的 `Label` 只渲染图标，
  用 `Label(displayName, systemImage: "globe")` 会让人看不到当前选的是哪个服。Android 侧
  `ServerMenu` 用 `TextButton` 显示同样的「地球图标 + 服务器名」。iOS 26 会把相邻的 trailing
  工具栏项合并成一个玻璃胶囊，两个菜单之间要放 `ToolbarSpacer(.fixed)`（包在
  `if #available(iOS 26.0, *)` 里，工程最低版本是 iOS 18）才能显示为两个独立按钮。
- **关于页**：首页「⋯」收藏菜单的最后一项 → `AboutView`。图标用 `Assets` 里的
  `AppIconPreview`（`AppIcon.png` 的副本；`Image("AppIcon")` 不能可靠地按名字加载应用图标），
  版本号读 `CFBundleShortVersionString` / `CFBundleVersion`（来自 `version.xcconfig` 映射的
  `MARKETING_VERSION` / `CURRENT_PROJECT_VERSION`），文案全部走 `L10n.About`。GitHub / Yuyuko
  用 `Link` 交给系统打开，VoiceOver 会把整行读成链接。链接行里的文字用 `Color.primary` /
  `Color.secondary` 这类具体颜色，**不要**用层级 `.primary` / `.secondary`：`Link` 里的层级样式
  会相对链接 tint（深色的 AccentColor）解析，深色模式下文字会变成几乎看不见的深蓝；Android
  的关于页同样用 label / secondary 颜色。
- **字体**：字号统一走 `Theme.swift` 里的语义化 `Font` token（`statLabel` / `statValue` /
  `detailValue` / `rowSubtitle` / `rowChevron` / `ratingValue` / `ratingTier`），全部基于
  text style 以跟随 Dynamic Type。可读文字最小用 `footnote`（默认 13pt）；`caption` /
  `caption2` 在默认尺寸下只有 12pt / 11pt，除图标外不再使用。层级靠字重和颜色区分，不靠缩小
  字号，`minimumScaleFactor` 不要低于 0.8。
- **日期格式**：`StatFormat.date` 用 `.abbreviated`（对应 Android 的 `FormatStyle.MEDIUM`），
  中文是「2016年10月21日」、英文是「Oct 21, 2016」。**不要退回 `.numeric`**：它会把中文日期
  变成「2016/10/21」，丢掉年 / 月 / 日，和 Android 也对不上。这个日期比数字形式宽，战舰列表
  行内副标题（舰种 · 日期 · 胜率）一行放不下，所以那里是 `lineLimit(2)` 换行而不是截断。
- **战舰列表行**：右侧评分的下一行用「火焰图标 + 均伤数字」而不是「均伤 62,196」文本，和
  Android 的 `ShipRow` 一致（图标 `flame.fill`，`.caption` 12pt）。`L10n.Player.shipAverageDamage`
  仍然保留，只用于该行的 `accessibilityLabel`——VoiceOver 念「均伤 62,196」，不要因为界面上
  看不到这句话就把 key 删掉。
- **多语言**：`L10n.swift` 只声明语义化 key，文案（含英文）全部只存在 `Localizable.xcstrings`
  里，同一个标签不需要在两个地方各写一遍；`String(localized:)` 自动跟随系统语言，没有 App 内
  切换开关。key 在 catalog 里缺失时会直接显示 key 本身，等于把漏翻译暴露出来。App 名称由
  `InfoPlist.xcstrings` 本地化；战舰名称语言由 `AppConfig.shipNameLanguage` 跟随。

## 多语言与新增语言

文案全部在 `Localizable.xcstrings`，代码通过 `L10n.swift` 的语义化 key 引用，没有英文兜底（漏 key
会直接显示 key）。舰名规则见根 [AGENTS.md](../AGENTS.md#多语言)；新增文案 / 语言的步骤见
[`wows-localization`](../.agents/skills/wows-localization/SKILL.md)。

## 构建与验证

工程没有单元测试 target，验证方式是编译；需要看界面时再手动走一遍主要流程。
仓库的 CI 只有发布工作流，不跑测试，只负责编译打包。

> 功能变更后默认只做类型检查和 `xcodebuild` 构建。**如需启动模拟器（`simctl boot` /
> `install` / `launch`、截图等）验证，先征求用户同意**，不要自行开启；需要看真实界面时，
> 走下面的 Pinky 流程装到用户的真机上。

```sh
cd ios

# 指定 Xcode（xcode-select 指向 CommandLineTools 时必需）
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
SDK=$(xcrun --sdk iphonesimulator --show-sdk-path)

# 类型检查
swiftc -typecheck -swift-version 6 -sdk "$SDK" -target arm64-apple-ios18.0-simulator \
  $(find WoWsInsight -name '*.swift')

# 构建
xcodebuild -project WoWsInsight.xcodeproj -scheme WoWsInsight \
  -destination 'generic/platform=iOS Simulator' build
```

> 本机的 DSH 文件沙盒会阻止 Xcode 的 Swift 宏插件子进程；命令行构建时加了
> `OTHER_SWIFT_FLAGS='-Xfrontend -disable-sandbox'`。正常用 Xcode IDE 构建不需要这个参数。

### 真机调试（Pinky）

用 Pinky 的 `phone_*` 工具把 Debug 构建装到用户 iPhone 上看真实界面。一个「改 → 装 → 看」
循环约 1–2 分钟，所以尽量一次改完再装。

```sh
# 1. UDID：xcrun devicectl list devices，取 physical 设备的 Identifier
# 2. 打包（自动签名，-allowProvisioningUpdates 缺了会报没有描述文件）
cd ios
xcodebuild -project WoWsInsight.xcodeproj -scheme WoWsInsight -configuration Debug \
  -destination 'platform=iOS,id=<UDID>' -derivedDataPath /tmp/wows-device \
  -allowProvisioningUpdates build OTHER_SWIFT_FLAGS='-Xfrontend -disable-sandbox'
# 3. 确认签名与版本
codesign -d --entitlements :- /tmp/wows-device/Build/Products/Debug-iphoneos/WoWsInsight.app
/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' \
  /tmp/wows-device/Build/Products/Debug-iphoneos/WoWsInsight.app/Info.plist
```

- 装：把 `WoWsInsight.app` 整个目录复制进 Pinky 的电脑端文件夹（`phone_files {}` 会打印路径，
  macOS 上是 `~/Library/Application Support/Pinky/files/<UDID>`，iPhone 本身没有 Pinky 目录），
  再 `phone_app { action: "install", paths: ["WoWsInsight.app"] }`。覆盖安装保留 UserDefaults
  里的收藏；版本不能比已装的低，同版本可以反复装。
- 手机锁屏会返回 `device_locked`，要 `phone_handoff` 让用户解锁；`phone_app stop` 不需要审批，
  用来验证「冷启动后的持久化」（退出再 `phone_launch`，收藏顺序/数据应保持）。
- **这个 App 的界面几乎全是 SwiftUI 自绘文本，元素树是空的**，一律用
  `phone_observe { ocr: true }` 读文字，按 OCR 坐标点。
- **排版问题只能靠截图看**：重叠、溢出、裁切在 OCR 里看不出来。用
  `phone_observe { save: "x.png" }` 存图，再用 `sips -c <高> <宽> --cropOffset <y> 0 x.png --out c.png`
  裁出局部放大检查（截图是 3x，OCR 坐标就是截图像素）。
- 返回上一页后列表位置会变，操作前重新 `observe` 再取坐标；`phone_swipe` 只能二选一：
  `direction` 或 `from`+`to`，同时给会报 `invalid_argument`。
- 长按拖动排序用 `phone_drag`（`hold_ms` 至少 800 才能把行拿起来），`via` 给中间点、
  `pause_ms` 松手前停一下；`phone_swipe` 只会滚动。
- `phone_batch` 里只放 tap / type / swipe / launch 这类界面动作（`phone_app` 放进去会报参数错误），
  且遇到第一个非 settled 的步骤就停，剩下的步骤要另起一批。
- `phone_type` 返回 `uncertain`（「没有焦点」）时先 `observe`，文字可能已经输进去了，别盲目重打。
- 会改数据的操作要能撤回：左滑只滑一半、看到「取消收藏」就点空白关掉；拖动排序后拖回原位，
  收尾时用 OCR 核对顺序；导入 / 导出只确认面板能弹出，然后点面板外关闭，不要真的写文件。
- 日志：iPhone 上 `phone_logs` 没有历史，第一次调用才开始监听，所以按「调用一次 → 复现 →
  再调用（`since: "5m"`）」的顺序取；不要传 `kind`（传 `"logs"` 会被 MCP 以
  `invalid_argument` 拒绝），输出过大时按关键字 grep（本 App 的请求主机是
  `v3-api.wows.shinoaki.com`）。
- 收尾：`phone_release`，并删掉 `/tmp/wows-device` 和 Pinky 文件夹里的截图。

## 发布

- CI 跑在 `xcode-27` 镜像（iOS 27 SDK），**不要退回旧镜像**：旧 SDK 构建的包在 iOS 26+ 上会走
  兼容外观（`xcode-27` 目前是 preview 标签，GA 后换成 `macos-27`）。
- 版本号不要写进 `project.pbxproj`，也不要在 Xcode 的 General 页改 Version：target 的 base
  configuration 是根目录的 [`version.xcconfig`](../version.xcconfig)，改了会盖掉共享文件。
- IPA 不签名。**重签名必须让描述文件的 App ID 等于 `PRODUCT_BUNDLE_IDENTIFIER`**：重签工具会用
  描述文件里的 entitlements 覆盖整个 App，用别的 App ID 签出来的包能启动，但 `.fileImporter`
  拿不到可读 URL（「选了文件没反应」）。检查：`codesign -d --entitlements :- Payload/WoWsInsight.app`
  里 `application-identifier` 的后缀要等于 `CFBundleIdentifier`。

完整流程见 [`wows-release`](../.agents/skills/wows-release/SKILL.md)。
