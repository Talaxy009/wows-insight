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
    │   ├── Components.swift           // 评分徽章、统计格子等
    │   └── Theme.swift                // 颜色、卡片样式
    ├── Utilities/
    │   ├── Formatting.swift           // 数字 / 百分比 / 日期格式化
    │   └── TaskCancellation.swift     // 取消错误的统一判断
    ├── Localizable.xcstrings          // en / ja / ko / zh-Hans / zh-Hant 界面文案
    ├── InfoPlist.xcstrings            // 各语言 App 名称
    ├── PrivacyInfo.xcprivacy          // UserDefaults 隐私清单
    └── Assets.xcassets                // AppIcon、AccentColor
```

页面只有三个主界面：搜索 / 收藏首页、玩家详情、单舰详情。

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
- **战斗类型切换**：`PlayerReport` 按 `BattleMode` 保存总览、评分和最高记录，战舰保存每艘舰的
  `[BattleMode: ModeStats]`。玩家页用菜单式 `BattleModePicker` 切换，未选中时回退到
  `defaultMode`（有随机战用随机战，否则用第一个有数据的类型）。
- **图表**：玩家页有两张卡，都跟随当前战斗类型。
  ①「近期趋势」用 Swift Charts 折线 + 面积画 `account/day/list/info` 的 `recentInfo`
  （每天一条当天战绩），在评分 / 胜率之间切换；面积的基线必须显式写成 y 轴下界
  （`AreaMark(yStart:yEnd:)`），默认基线是图表 frame 底部，会盖住日期轴并溢出卡片；少于两天
  不画（一个点不是趋势），x 轴按整天取刻度避免同一天出现两个日期标签，胜率会把刻度略微放出
  0–100 之外，免得 0% / 100% 的点被裁掉。②「评分分布」取 `info2` 的 `shipTypeInfo` /
  `levelInfo`，在舰种 / 等级之间切换；这张卡是自绘的条形行而不是 Swift Charts 的横向条形图
  ——iOS 26 上分类轴的标签会画进绘图区、和条形重叠。两张卡的选中项存在 `PlayerView` 里，
  因为 `LazyVStack` 回收卡片时会丢掉卡片自己的 `@State`。没有数据的卡片整张隐藏，所以服务端
  没记录过的账号（`code: 412 无recent记录`）只会少一张卡，不会看到空图。
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
- **字体**：字号统一走 `Theme.swift` 里的语义化 `Font` token（`statLabel` / `statValue` /
  `detailValue` / `rowSubtitle` / `rowChevron` / `ratingValue` / `ratingTier`），全部基于
  text style 以跟随 Dynamic Type。可读文字最小用 `footnote`（默认 13pt）；`caption` /
  `caption2` 在默认尺寸下只有 12pt / 11pt，除图标外不再使用。层级靠字重和颜色区分，不靠缩小
  字号，`minimumScaleFactor` 不要低于 0.8。
- **多语言**：`L10n.swift` 只声明语义化 key，文案（含英文）全部只存在 `Localizable.xcstrings`
  里，同一个标签不需要在两个地方各写一遍；`String(localized:)` 自动跟随系统语言，没有 App 内
  切换开关。key 在 catalog 里缺失时会直接显示 key 本身，等于把漏翻译暴露出来。App 名称由
  `InfoPlist.xcstrings` 本地化；战舰名称语言由 `AppConfig.shipNameLanguage` 跟随。

## 多语言与新增语言

界面语言由 iOS 根据系统语言自动选择，代码中没有任何语言切换开关（舰名规则见根
[AGENTS.md](../AGENTS.md#多语言)）：

- 系统为简体中文 → 界面中文，战舰名称为 `nameCn`
- 系统为繁体中文 → 界面繁体中文，战舰名称同样用 `nameCn`
- 系统为英文 → 界面英文，战舰名称为 `nameEnglish`
- 系统为日语 / 韩语 → 界面日语 / 韩语，战舰名称回退为 `nameEnglish`
- 其他系统语言 → 由 iOS 回退到英文或中文，战舰名称与回退后的界面保持一致
- 国服（`cn`）优先用 360 的译名 `nameCn360`

### 新增一种语言

1. 在 `Localizable.xcstrings` 中添加语言（例如 `de`），翻译全部 key。每个 key 都有注释说明
   使用场景；格式字符串中的 `%1$@`、`%2$@` 等占位符不要删除或调序。
2. 在 `InfoPlist.xcstrings` 中添加同一语言的 `CFBundleDisplayName`（桌面显示名称）。
3. 在 `WoWsInsight.xcodeproj/project.pbxproj` 的 `knownRegions` 里加上该语言码。
4. 如果新语言需要自己的舰名（接口目前只提供中 / 英 / 俄三种），在 `ShipNameLanguage` 和
   `YuyukoShipMeta.name(language:server:)` 中扩展。
5. 用 Xcode 打开工程时如果新语言没有出现在 Project → Info → Localizations 中，把它加进去。
6. 在仓库根目录运行 `python .agents/skills/wows-localization/scripts/generate_strings.py`，让
   Android 资源跟上（见
   [`wows-localization`](../.agents/skills/wows-localization/SKILL.md)）。

因为所有文案都走 `L10n.swift` + String Catalog，人工翻译或让 AI 翻译都只需要处理这两个
`.xcstrings` 文件，不需要改 View 代码。新增用户可见文案时：先在 `L10n.swift` 加 key，
**同时**在 `Localizable.xcstrings` 补上 en 和其余全部语言（目前是 en / ja / ko / zh-Hans /
zh-Hant）——代码里没有英文兜底，漏掉 key 的界面会直接显示原始 key。

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

iOS 和 Android 由同一个[发布工作流](../.github/workflows/release.yml)手动触发，iOS 侧细节：

- 跑在 `xcode-27` 镜像上，用 Xcode 27 / iOS 27 SDK 构建。**不要退回旧镜像**：用 iOS 18 SDK
  构建的包在 iOS 26+ 上会走兼容外观，界面和本地用 Xcode 27 构建的不一致（`xcode-27`
  目前是 preview 标签，GA 后换成 `macos-27`）。
- `version` 输入就是发布版本号（可带 `v` 前缀），例如 `1.0.1` → 标签 `v1.0.1`；
  同时用 `MARKETING_VERSION=1.0.1` 覆盖工程里的版本号，`CURRENT_PROJECT_VERSION` 用 run number。
- `version` 留空时用工程的 `MARKETING_VERSION`；标签已存在会直接报错，需要换一个版本号。
- 产物是 `WoWsInsight-<version>.ipa`（`Payload/WoWsInsight.app`），同时上传成 Actions artifact；
  IPA **不签名**（`CODE_SIGNING_ALLOWED=NO`），使用者需要自己用 AltStore / Sideloadly / Xcode 重签。

> **重签名必须让描述文件的 App ID 等于 `PRODUCT_BUNDLE_IDENTIFIER`。** 重签工具会用描述文件里的
> `Entitlements` 覆盖整个 App（CI 产物本身一个 entitlement 都没有），而 `application-identifier`
> 必须是 `<TeamID>.<BundleID>`。用别的 App ID 的 profile 签出来的 App 能启动、界面正常，但
> 文档选择器发出的沙盒扩展落不到 App 上——`.fileImporter` 会拿不到可读的 URL（表现为「选了文件
> 没反应」），iCloud Drive 的文件也会卡在同步。检查方法：`codesign -d --entitlements :- Payload/WoWsInsight.app`
> 里 `application-identifier` 的后缀要等于 `CFBundleIdentifier`。

> 仓库需要在 Settings → Actions → General → Workflow permissions 里允许
> **Read and write permissions**，否则 `gh release create` 会因 `GITHUB_TOKEN` 没有写权限而失败。

## 后续可扩展方向

- 收藏分组、按名称 / 评分自动排序
- 排位赛季与成就
