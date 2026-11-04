# AGENTS.md

面向在本仓库工作的开发者 / AI 代理的项目说明。用户视角的功能介绍见 [README.md](./README.md)。

## 技术栈

- Xcode 16 或更新版本（工程使用 Xcode 16 的文件夹同步分组，新增文件不需要手改 `project.pbxproj`）
- iOS 18.0+ / Swift 6（严格并发）
- 无第三方依赖、无 CocoaPods / SPM：只用 Foundation + SwiftUI + Observation
- 架构：SwiftUI + `@Observable` + async/await，没有手写 UIKit 生命周期

## 数据来源

App 不直接调用 Wargaming API，所有数据来自社区的 Yuyuko API（`https://v3-api.wows.shinoaki.com`，文档 `https://v3-api.wows.shinoaki.com/v3/index.html`），只使用 `/public/*` 系列接口，无需 Authorization。

因此**不要**再引入 `Config.plist`、`application_id`、环境变量 Key 之类的配置，也不要把 `Yuyuko-Client-Type` 之外的认证信息写进代码。

- 请求统一带 `Yuyuko-Client-Type: APP;WoWsInsight;1.0`（文档标注为必填，服务端用它区分客户端）。
- 响应是 `{ code, message, data }` 信封，`code == 200` 才算成功。
- 这是第三方服务，没有 SLA。改动接口层时优先保证「字段缺失也能正常显示」，`YuyukoModels.swift` 里的 DTO 全部是 optional 就是这个原因。

运行只需在 Target → Signing & Capabilities 里选 Team 并按需改 Bundle Identifier。

收藏不支持 iCloud 同步：免费的个人 Team（Personal Team）无法签名 iCloud 能力（Xcode 会报 `Personal development teams ... do not support the iCloud capability`），所以工程里没有任何 entitlement / CloudKit 容器，收藏只存在本机，跨设备迁移用首页右上角「⋯」菜单里的导入 / 导出 JSON 文件。

## 工程结构

```txt
.github/
└── workflows/release.yml   // 手动触发：打包未签名 IPA 并发布 Release

WoWsInsight/
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

页面只有三个主界面：搜索/收藏首页、玩家详情、单舰详情。

## 数据来源与口径

| 数据                       | Yuyuko 接口                                     | 说明                            |
| -------------------------- | ----------------------------------------------- | ------------------------------- |
| 玩家搜索                   | `POST public/wows/account/search/{server}/user` | 返回 `accountId` + `userName`   |
| 玩家总览 + 分类型战绩      | `GET public/wows/account/user/info2`            | 按战斗类型返回 `battleTypeInfo` |
| 每舰战绩 + 舰船信息 + 评分 | `GET public/wows/account/ship/info/list`        | 一次拿全，无需再下百科          |

三个接口都不需要鉴权。`{server}` 直接用 `GameServer.rawValue`，取值是 `ru` / `eu` / `na` / `asia` / `cn`，字段名以 v3 文档为准，`YuyukoModels.swift` 另外处理两处嵌套差异：`account/user/info2` 的 `battleTypeInfo[code].shipInfo` 和 `ship/info/list` 的 `typeInfo[code].battleInfo` 是同一套 `BattleInfoData`，只是名字不同。

### 战斗类型

`account/user/info2` 的 `battleTypeInfo` 是以 API 代码为 key 的字典：`PVP` / `PVP_SOLO` / `PVP_DIV2` / `PVP_DIV3` / `RANK(_SOLO/_DIV2/_DIV3)` / `RANK_OLD*` /`CLAN` / `BRAWL` / `OPER_MM` / `PVE(_SOLO/_DIV2/_DIV3)` / `RATING(_SOLO/_DIV)`。`BattleMode` 的 raw value 就是这些代码，声明顺序即选择器顺序（随机战最前），未知代码直接忽略。每艘战舰的 `typeInfo` 用同一套代码，所以玩家页切换类型时，总览、详细数据、最高记录和战舰列表会一起切到该类型。默认显示随机战（`PVP`）。

只有该账号在某个类型里打过仗（`battleCount > 0`）才会出现在选择器里。

### 个人评分

评分由服务端给出：

- 玩家评分按类型取 `battleTypeInfo[code].prInfo.value`，页面切换到哪个类型就显示哪个评分。顶层 `prInfo` 是「全部模式」口径，没有使用。
- 每艘战舰的评分取该舰 `typeInfo[code].prInfo.value`，同样跟随当前类型。
- `prInfo.code == 0` 表示「暂无数据」，映射成 `PlayerRating.unknown`（显示为 `—`）。
- 服务端用的是 wows-numbers 公式套各服务器自己的期望值，所以同一账号的分数会和直接查 wows-numbers 有出入；分档与颜色仍由本地 `RatingTier` 决定。

```txt
rDmg   = 实际平均伤害 / 期望平均伤害
rFrags = 实际平均击杀 / 期望平均击杀
rWins  = 实际胜率     / 期望胜率

nDmg   = max(0, (rDmg   - 0.4) / 0.6)
nFrags = max(0, (rFrags - 0.1) / 0.9)
nWins  = max(0, (rWins  - 0.7) / 0.3)

rating = 700 × nDmg + 300 × nFrags + 150 × nWins   // 0 ~ 9999
```

### 已知妥协

- 接口没有平局场数，`drawCount` 由 `battle - wins - losses` 推出。
- 接口没有主炮射击数 / 命中数，命中率直接用 `hitRatioInfo.ratioMain` 的百分比。
- 国服只支持精确昵称搜索（`one: true`），输入不完整时上游返回 HTTP 400（`code: 540`），搜索层把它当作「无结果」；其他服务器仍然是 3 字符起的模糊搜索。

## 实现要点

- **Swift 6 / 严格并发**：`YuyukoClient` 是 `Sendable` 值类型，ViewModel 使用 `@MainActor @Observable`。
- **取消与防抖**：搜索框每次变化通过 `.task(id:)` 取消上一次请求，等待 350ms 后请求；网络返回后检查 `Task.isCancelled` 和请求 key，避免旧结果覆盖新结果。请求 key 用去掉首尾空白的昵称，只改空白不会触发新请求。
- **错误口径**：`URLSession` 的 `URLError.cancelled` 在 `YuyukoClient` 里统一转成 `CancellationError`，页面切换不会弹出错误；HTTP 404 / 405 统一映射为 `notFound`，和信封里 `code` 的语义保持一致。
- **并发加载**：玩家总览和战舰列表用 `async let` 并发请求；玩家首次查询约 6–8 秒（服务端要回源 Wargaming），命中缓存后 0.3–0.6 秒，所以超时设为 30 秒。
- **一次成型**：两个请求都返回后才构建 `PlayerReport`，不再有「总览先出、百科后到」的中间态。
- **玩家页头像**：昵称左侧显示 `userInfo.dogTag`。它是服务端给的 190×190 PNG 直链，没有该字段或取不到图时显示占位暗块，所以模型里存成 `AccountInfo.dogTagURL`。
- **战斗类型切换**：`PlayerReport` 按 `BattleMode` 保存总览、评分和最高记录，战舰保存每艘舰的 `[BattleMode: ModeStats]`。玩家页用菜单式 `BattleModePicker` 切换，未选中时回退到`defaultMode`（有随机战用随机战，否则用第一个有数据的类型）。
- **没有本地缓存**：评分、舰船名称、图标全部来自服务端，App 不再打包期望值数据，也不再往 Caches 里写百科 JSON。
- **收藏**：`FavoritesStore` 是 `@MainActor @Observable`，通过 `@Environment` 注入所有页面；收藏列表以 JSON 存入 `UserDefaults`，并在 `PrivacyInfo.xcprivacy` 中声明 `NSPrivacyAccessedAPICategoryUserDefaults`（CA92.1）。顺序由用户手动拖动决定。
- **收藏列表的编辑态**：首页右上角只有一个「⋯」菜单（编辑 / 导入 / 导出，编辑只在收藏页可用），它通过 `@State editMode` + `.environment(\.editMode, $editMode)` 切换编辑态，而不是 `EditButton`。`ForEach.onMove` 提供系统的拖拽把手；删除用行内自绘的红色减号按钮，**没有**用 `onDelete`：系统的删除控件会紧贴卡片左边、没有内边距，而且会额外插入一段左移量。
- **收藏行的图标**：行内容自己推导航（`NavigationPath` + Button），没用 `NavigationLink`，因为 `NavigationLink` 的箭头无法隐藏，编辑时会和拖拽把手同时出现。现在箭头只在非编辑态显示，编辑态替换成减号按钮。
- **编辑态要让出尾部空间**：系统拖拽把手贴着 cell 尾部布局，不受 `listRowInsets` 影响，卡片会一直延伸到它下面。所以 `playerCell(isEditing:)` 在编辑态把卡片右内边距加 30pt、内容尾部 padding 加到 62pt，让把手落在卡片右侧的留白里。这两个数字要一起改。
- **收藏导入 / 导出**：`FavoritesArchive` 是分享用的 JSON（`version` / `exportedAt` / `players`），通过 `ShareLink` 交给系统分享面板（存到「文件」、AirDrop、邮件都行）写入，读取用 `.fileImporter`（只接受 `.json`）。只认导出格式本身，读不懂就提示导入失败——不保留任何旧格式 / 裸数组的兼容解析。
- **导入语义**：`FavoritesStore.importArchive(_:)` 是「追加合并」而不是「覆盖」：已存在（同 `accountId` + 同 `server`）的条目跳过，新的追加到末尾并保留现有顺序，返回 `ImportSummary(added:skipped:)` 供弹窗提示。这样导入永远不会删掉本机已有的收藏。
- **字体**：字号统一走 `Theme.swift` 里的语义化 `Font` token（`statLabel` / `statValue` / `detailValue` / `rowSubtitle` / `rowChevron` / `ratingValue` / `ratingTier`），全部基于 text style 以跟随 Dynamic Type。可读文字最小用 `footnote`（默认 13pt）；`caption` / `caption2` 在默认尺寸下只有 12pt / 11pt，除图标外不再使用。层级靠字重和颜色区分，不靠缩小字号，`minimumScaleFactor` 不要低于 0.8。
- **多语言**：`L10n.swift` 只声明语义化 key，文案（含英文）全部只存在 `Localizable.xcstrings` 里，同一个标签不需要在两个地方各写一遍；`String(localized:)` 自动跟随系统语言，没有 App 内切换开关。key 在 catalog 里缺失时会直接显示 key 本身，等于把漏翻译暴露出来。App 名称由 `InfoPlist.xcstrings` 本地化；战舰名称语言由 `AppConfig.shipNameLanguage` 跟随。

## 多语言与新增语言

界面语言由 iOS 根据系统语言自动选择，代码中没有任何语言切换开关：

- 系统为简体中文 → 界面中文，战舰名称为 `nameCn`
- 系统为繁体中文 → 界面繁体中文，战舰名称同样用 `nameCn`（接口只提供简体舰名）
- 系统为英文 → 界面英文，战舰名称为 `nameEnglish`
- 系统为日语 / 韩语 → 界面日语 / 韩语，战舰名称回退为 `nameEnglish`（接口没有日 / 韩舰名）
- 其他系统语言 → 由 iOS 回退到英文或中文，战舰名称与回退后的界面保持一致
- 国服（`cn`）优先用 360 的译名 `nameCn360`（例如「鲸」而不是「大和」），其他服务器用 `nameCn`

### 新增一种语言

1. 在 `Localizable.xcstrings` 中添加语言（例如 `de`），翻译全部 key。每个 key 都有注释说明使用场景；格式字符串中的 `%1$@`、`%2$@` 等占位符不要删除或调序。
2. 在 `InfoPlist.xcstrings` 中添加同一语言的 `CFBundleDisplayName`（桌面显示名称）。
3. 在 `WoWsInsight.xcodeproj/project.pbxproj` 的 `knownRegions` 里加上该语言码。
4. 如果新语言需要自己的舰名（接口目前只提供中 / 英 / 俄三种），在 `ShipNameLanguage`和 `YuyukoShipMeta.name(language:server:)` 中扩展。
5. 用 Xcode 打开工程时如果新语言没有出现在 Project → Info → Localizations 中，把它加进去。

因为所有文案都走 `L10n.swift` + String Catalog，人工翻译或让 AI 翻译都只需要处理这两个 `.xcstrings` 文件，不需要改 View 代码。新增用户可见文案时：先在 `L10n.swift` 加 key，
**同时**在 `Localizable.xcstrings` 补上 en 和其余全部语言（目前是 en / ja / ko / zh-Hans / zh-Hant）——代码里没有英文兜底，漏掉 key 的界面会直接显示原始 key。

## 构建与验证

工程没有单元测试 target，验证方式是编译；需要看界面时再手动走一遍主要流程。
CI 只有一个 Release 工作流，不跑测试，只负责编译打包。

> 功能变更后默认只做类型检查和 `xcodebuild` 构建。**如需启动模拟器（`simctl boot` /
> `install` / `launch`、截图等）验证，先征求用户同意**，不要自行开启。

```sh
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

## 发布

`.github/workflows/release.yml` 是**手动触发**的（Actions → Release → Run workflow），
归档、打包成未签名 IPA，并在 GitHub Releases 里建标签和 Release。

- 跑在 `xcode-27` 镜像上，用 Xcode 27 / iOS 27 SDK 构建。**不要退回旧镜像**：用 iOS 18 SDK
  构建的包在 iOS 26+ 上会走兼容外观，界面和本地用 Xcode 27 构建的不一致（`xcode-27`
  目前是 preview 标签，GA 后换成 `macos-27`）。
- `version` 输入就是发布版本号（可带 `v` 前缀），例如 `1.0.1` → 标签 `v1.0.1`；
  同时用 `MARKETING_VERSION=1.0.1` 覆盖工程里的版本号，`CURRENT_PROJECT_VERSION` 用 run number。
- `version` 留空时用工程的 `MARKETING_VERSION`；标签已存在会直接报错，需要换一个版本号。
- Release 说明由「如何安装」+ GitHub 自动生成的变更日志（`--generate-notes`）组成，
  所以 checkout 用了 `fetch-depth: 0`。
- 产物是 `WoWsInsight-<version>.ipa`（`Payload/WoWsInsight.app`），同时上传成 Actions artifact；
  IPA **不签名**（`CODE_SIGNING_ALLOWED=NO`），使用者需要自己用 AltStore / Sideloadly / Xcode 重签。

> 仓库需要在 Settings → Actions → General → Workflow permissions 里允许
> **Read and write permissions**，否则 `gh release create` 会因 `GITHUB_TOKEN` 没有写权限而失败。

## 后续可扩展方向

- 收藏分组、按名称 / 评分自动排序
- 图表（Swift Charts）展示评分和胜率变化
- 排位赛季与成就
- 接口已有的按舰种 / 按等级评分（`shipTypeInfo`、`levelInfo`）可以做成新卡片
