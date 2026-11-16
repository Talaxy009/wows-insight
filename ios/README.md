# WoWs Insight for iOS

原生 SwiftUI 客户端，功能与仓库里的 [Android 版本](../android/README.md) 对齐，共用同一套 [Yuyuko](https://v3-api.wows.shinoaki.com/v3/index.html) 公共接口和统计口径。

- 系统要求：iOS 18.0+
- 开发环境：Xcode 16 或更新版本、Swift 6（严格并发）
- 无第三方依赖：没有 CocoaPods / SPM，只使用 Foundation + SwiftUI + Observation

## 功能

- 五个服务器：俄服 / 欧服 / 美服 / 亚服 / 国服
- 按昵称搜索玩家，国服使用接口的精确匹配模式
- 玩家页
  - 按战斗类型（随机战 / 排位战 / 联合作战 / 公会战 …）切换，默认随机战
  - 总览：胜率、场数、平均伤害、平均击杀、平均经验、生存率
  - 详细数据：胜负平、总伤害、总经验、命中率、KD、潜在伤害、侦查伤害、侦测、击落飞机
  - 最高记录，以及对应的战舰
  - 当前战斗类型的个人评分
  - 近期每日评分 / 胜率趋势，以及按舰种 / 按等级的评分分布
- 战舰列表：按场数 / 胜率 / 平均伤害 / 评分 / 最近战斗排序；点进单舰查看同样的统计
- 收藏
  - 玩家页右上角星标收藏 / 取消收藏，搜索结果左滑也能直接收藏
  - 首页左滑取消收藏，长按行拖动调整顺序
  - 首页右上角「⋯」菜单把收藏导出成 JSON，或在另一台设备上导入
- 关于页：应用图标、名称、版本号，GitHub 仓库与 Yuyuko 数据服务的跳转，以及非官方声明
  （首页「⋯」菜单最后一项）
- 界面语言：简体中文 / 繁體中文 / English / 日本語 / 한국어，跟随系统语言

## 运行

1. 用 Xcode 打开 [`WoWsInsight.xcodeproj`](./WoWsInsight.xcodeproj)。
2. 在 Target → Signing & Capabilities 里选择自己的 Team，按需修改 Bundle Identifier。
3. 选择模拟器或真机运行。

命令行构建：

```sh
cd ios

# 类型检查
SDK=$(xcrun --sdk iphonesimulator --show-sdk-path)
swiftc -typecheck -swift-version 6 -sdk "$SDK" -target arm64-apple-ios18.0-simulator \
  $(find WoWsInsight -name '*.swift')

# 构建
xcodebuild -project WoWsInsight.xcodeproj -scheme WoWsInsight \
  -destination 'generic/platform=iOS Simulator' build
```

工程使用 Xcode 16 的文件夹同步分组，新增 Swift 文件不需要手改 `project.pbxproj`。

## 发布

发布由仓库根目录的 [`.github/workflows/release.yml`](../.github/workflows/release.yml) 手动触发（Actions → Release → Run workflow），一次同时构建 iOS 和 Android。iOS 侧产物是 `WoWsInsight-<版本>.ipa`：

- IPA **不签名**（`CODE_SIGNING_ALLOWED=NO`），需要自己用 AltStore / Sideloadly / Xcode 重签。
- 重签名时，描述文件的 App ID 必须等于 `PRODUCT_BUNDLE_IDENTIFIER`。否则 App 能启动、界面正常，
  但 `.fileImporter` 拿不到可读的 URL（表现为「选了文件没反应」）。
- CI 用带 iOS 27 SDK 的 `xcode-27` 镜像构建，不要退回旧镜像：旧 SDK 构建的包在 iOS 26+
  上会走兼容外观，和本地用 Xcode 27 构建的界面不一致。

## 工程结构

```txt
ios/
├── README.md
├── AGENTS.md
├── WoWsInsight.xcodeproj
│   └── xcshareddata/xcschemes/WoWsInsight.xcscheme
└── WoWsInsight/
    ├── App/               // SwiftUI 生命周期、语言配置、文案 key
    ├── Models/            // 服务器、战斗类型、统计数据、评分、收藏格式
    ├── Networking/        // async/await API 客户端与 DTO 映射
    ├── ViewModels/        // 搜索、玩家页加载、收藏读写
    ├── Views/             // 搜索 / 玩家 / 单舰 / 关于四个界面与组件
    ├── Utilities/         // 格式化与取消错误判断
    ├── Localizable.xcstrings   // en / ja / ko / zh-Hans / zh-Hant 界面文案
    ├── InfoPlist.xcstrings     // 各语言 App 名称
    ├── PrivacyInfo.xcprivacy   // UserDefaults 隐私清单
    └── Assets.xcassets         // AppIcon、AppIconPreview、AccentColor
```

页面有四个主界面：搜索 / 收藏首页、玩家详情、单舰详情、关于（从首页「⋯」收藏菜单进入）。

## 多语言

界面语言由 iOS 根据系统语言自动选择，App 内没有切换开关。所有用户可见文案都在 `Localizable.xcstrings` 里，代码通过 `L10n.swift` 的语义化 key 引用。

- 简体中文 → 界面中文，战舰名称用 `nameCn`
- 繁体中文 → 界面繁体中文，战舰名称仍用 `nameCn`（接口只提供简体舰名）
- 英语 → 界面英文，战舰名称用 `nameEnglish`
- 日语 / 韩语 → 界面日语 / 韩语，战舰名称回退为 `nameEnglish`
- 其他语言 → 由 iOS 回退，战舰名称与回退后的界面保持一致
- 国服（`cn`）优先使用 360 译名 `nameCn360`（例如「鲸」而不是「大和」）

新增语言或新增文案的具体步骤见 [`AGENTS.md`](./AGENTS.md)。

## 已知限制

- 收藏只保存在本机，没有 iCloud 同步。免费的个人 Team 无法签名 iCloud 能力，跨设备迁移
  只能靠「⋯」菜单导出 / 导入 JSON。
- 卸载 App 会丢失收藏，建议定期导出备份。
- 只支持按昵称搜索，不能用账号 ID；国服只支持完整昵称。
- 隐藏战绩的账号只能显示昵称和注册 / 最后战斗时间。
- 未查询过的玩家首次打开需要 6–8 秒（服务端要回源 Wargaming），之后会快很多。
- 接口只提供简体的战舰译名，所以繁体界面里的舰名仍是简体；日 / 韩界面的舰名显示英文。

个人评分的分档、颜色和计算公式见[根目录 README](../README.md#个人评分怎么看)。
