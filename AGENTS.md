# AGENTS.md

面向在本仓库工作的开发者 / AI 代理的仓库级说明。用户视角的功能介绍见 [README.md](./README.md)。

本仓库是一个多平台仓库：`ios/` 与 `android/` 是两个功能对齐的原生客户端，共用同一套
Yuyuko 公共接口、统计口径和收藏文件格式。平台自己的实现约定放在对应目录的 `AGENTS.md`，
本文只保留两个平台共享的内容。

## 先读哪份文档

| 要改的地方 | 必读 |
| --- | --- |
| iOS（Swift / SwiftUI / Xcode / 真机调试） | [ios/AGENTS.md](./ios/AGENTS.md) |
| Android（Kotlin / Compose / Gradle / 本地化资源） | [android/AGENTS.md](./android/AGENTS.md) |
| 接口口径、评分算法、收藏格式、发布流程 | 本文 |
| 跨平台多步流程（多语言、应用图标、收藏文件、发布、接口接入） | [.agents/skills/](./.agents/skills/) |

## 仓库结构

```txt
.
├── ios/                        # 原生 iOS 客户端
│   ├── README.md               # 用户视角：运行、构建、已知限制
│   ├── AGENTS.md               # 开发者视角：结构、实现要点、调试
│   ├── WoWsInsight.xcodeproj
│   └── WoWsInsight/
├── android/                    # 原生 Android 客户端
│   ├── README.md
│   ├── AGENTS.md
│   ├── app/
│   ├── docs/
│   └── icon.svg                # 应用图标源文件
├── .agents/
│   └── skills/                 # 可复用流程：多语言、应用图标、收藏格式、发布、接口口径
│       ├── wows-localization/SKILL.md
│       ├── wows-app-icon/SKILL.md
│       ├── wows-favorites-archive/SKILL.md
│       ├── wows-release/SKILL.md
│       └── wows-yuyuko-api/SKILL.md
├── .github/
│   └── workflows/
│       └── release.yml         # 手动触发：打包已签名 APK + 未签名 IPA 并发布
├── README.md                   # 仓库总览与两个平台共用的用户说明
└── AGENTS.md                   # 本文件
```

## 数据来源

两个客户端都不直接调用 Wargaming API，所有数据来自社区的 Yuyuko API
（`https://v3-api.wows.shinoaki.com`，文档 `https://v3-api.wows.shinoaki.com/v3/index.html`），
只使用 `/public/*` 系列接口，无需 Authorization。

**不要**引入 `Config.plist`、`application_id`、环境变量 Key 之类的配置，也不要把
`Yuyuko-Client-Type` 之外的认证信息写进代码。

- 请求统一带 `Yuyuko-Client-Type: APP;WoWsInsight;1.0`（文档标注为必填，服务端用它区分客户端）。
- 响应是 `{ code, message, data }` 信封，`code == 200` 才算成功。
- 这是第三方服务，没有 SLA。接口层必须保证「字段缺失也能正常显示」，两端的 DTO 都按可空处理。
- HTTP 404 / 405 与信封里同样的 code 统一映射为「找不到」，取消请求不能弹出错误。

| 数据                       | Yuyuko 接口                                     | 说明                            |
| -------------------------- | ----------------------------------------------- | ------------------------------- |
| 玩家搜索                   | `POST public/wows/account/search/{server}/user` | 返回 `accountId` + `userName`   |
| 玩家总览 + 分类型战绩      | `GET public/wows/account/user/info2`            | 按战斗类型返回 `battleTypeInfo`，另外带 `shipTypeInfo` / `levelInfo` |
| 每舰战绩 + 舰船信息 + 评分 | `GET public/wows/account/ship/info/list`        | 一次拿全，无需再下百科          |
| 每日近期记录               | `GET public/wows/account/day/list/info`         | `recentInfo` 按天给出当天战绩，用于趋势图 |

`{server}` 取值是 `ru` / `eu` / `na` / `asia` / `cn`，字段名以 v3 文档为准。两处嵌套差异：
`account/user/info2` 的 `battleTypeInfo[code].shipInfo` 和 `ship/info/list` 的
`typeInfo[code].battleInfo` 是同一套 `BattleInfoData`，只是名字不同。

### 战斗类型

`account/user/info2` 的 `battleTypeInfo` 是以 API 代码为 key 的字典：`PVP` / `PVP_SOLO` /
`PVP_DIV2` / `PVP_DIV3` / `RANK(_SOLO/_DIV2/_DIV3)` / `RANK_OLD*` / `CLAN` / `BRAWL` /
`OPER_MM` / `PVE(_SOLO/_DIV2/_DIV3)` / `RATING(_SOLO/_DIV)`。两端的 `BattleMode`（iOS 的
`BattleMode.swift`、Android 的 `BattleMode.kt`）用同一套 raw value，声明顺序即选择器顺序
（随机战最前），未知代码直接忽略。每艘战舰的 `typeInfo` 用同一套代码，所以玩家页切换类型时，
总览、详细数据、最高记录和战舰列表会一起切到该类型。默认显示随机战（`PVP`）。只有该账号在
某个类型里打过仗（`battleCount > 0`）才会出现在选择器里。

### 个人评分

评分由服务端给出：

- 玩家评分按类型取 `battleTypeInfo[code].prInfo.value`，页面切换到哪个类型就显示哪个评分。
  顶层 `prInfo` 是「全部模式」口径，不使用。
- 每艘战舰的评分取该舰 `typeInfo[code].prInfo.value`，同样跟随当前类型。
- `prInfo.code == 0` 表示「暂无数据」，显示为 `—`。
- 服务端用的是 wows-numbers 公式套各服务器自己的期望值，所以同一账号的分数会和直接查
  wows-numbers 有出入；分档与颜色由两端本地的 `RatingTier` 决定，分数段必须保持一致：
  0–749 / 750–1099 / 1100–1349 / 1350–1549 / 1550–1749 / 1750–2099 / 2100–2449 / 2450+。

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
- 国服只支持精确昵称搜索（`one: true`），输入不完整时上游返回 HTTP 400（`code: 540`），
  客户端把它当作「无结果」；其他服务器仍然是 3 字符起的模糊搜索。
- 服务端只为观察过的账号保留每日记录，少于两天时趋势图不画；每日记录失败不能影响玩家页。
- 不做离线缓存：评分、舰船名称、图标全部来自服务端，收藏是唯一的本地用户数据。

## 跨平台约定

### 收藏文件

两端使用同一份版本化 JSON 信封，扩展名 `.json`，`version` 目前是 `1`：

```json
{
  "version": 1,
  "exportedAt": "2026-11-12T04:27:22Z",
  "players": [
    { "accountId": 3002439652, "nickname": "Yamato", "server": "asia" }
  ]
}
```

- `exportedAt` 截断到整秒：Foundation 的 `.iso8601` 解码器不接受小数秒。
- 导入语义是「追加合并」：已存在（同 `accountId` + 同 `server`）的条目跳过，新的追加到末尾，
  保留本机现有顺序，**导入永远不能删除本地收藏**。
- 只认这个格式本身，不保留旧格式 / 裸数组的兼容解析；读不懂就提示导入失败。
- iOS 存在 `UserDefaults`，Android 存在 Preferences DataStore 的 `favorites.players` 键。
- 修改格式时必须同时改两端、Android 的 `FavoritesArchiveTest` / iOS 的导入导出逻辑，以及
  [`.agents/skills/wows-favorites-archive/SKILL.md`](./.agents/skills/wows-favorites-archive/SKILL.md)。

### 多语言

两端都提供简体中文 / 繁体中文 / 英语 / 日语 / 韩语，界面语言跟随系统，App 内没有切换开关。
iOS 的 `Localizable.xcstrings` 是所有用户可见文案的唯一来源，Android 资源由脚本生成；新增或
修改文案时先改 iOS 的 String Catalog，再运行 Android 的生成脚本，不能手改生成的
`values*/strings.xml`。跨平台的完整步骤与新增语言清单见
[`.agents/skills/wows-localization/SKILL.md`](./.agents/skills/wows-localization/SKILL.md)。

战舰名称规则两端一致：

- 系统为简体中文 → `nameCn`
- 系统为繁体中文 → 界面繁体，舰名仍用 `nameCn`（接口只提供简体舰名）
- 系统为英文 → `nameEnglish`
- 系统为日语 / 韩语 → 界面日 / 韩语，舰名回退为 `nameEnglish`
- 其他系统语言 → 跟随界面回退结果
- 国服（`cn`）优先使用 360 译名 `nameCn360`（例如「鲸」而不是「大和」），其他服务器用 `nameCn`

### 数值类型

- 账号 ID、战舰 ID 是 64 位，Android 端必须用 `Long`（现有亚服账号 ID 已超过 `Int.MAX_VALUE`），
  iOS 端用 `Int64` 语义的 `Int`。
- 生涯总计可能超过 `Int.MAX_VALUE`（潜在伤害最常见），伤害 / 经验 / 潜在伤害 / 侦查伤害等
  总量与记录值要用 64 位，单场次数保持 32 位。

## 发布

[`.github/workflows/release.yml`](./.github/workflows/release.yml) 是**手动触发**的
（Actions → Release → Run workflow），一次构建两个平台，并发布到同一个 GitHub Release：

- `version` 输入是发布版本号（可带 `v` 前缀），例如 `1.0.1` → 标签 `v1.0.1`。留空时使用
  iOS 工程的 `MARKETING_VERSION`，Android 的 `versionName` 也使用同一个值。
- `versionCode` / `CURRENT_PROJECT_VERSION` 统一使用 workflow 的 run number。
- 标签已存在会直接报错，需要换一个版本号。
- 产物是 `WoWsInsight-<version>.ipa` 和 `WoWsInsight-<version>.apk`；iOS IPA **不签名**，
  需要使用者自己重签，Android APK 由 CI 用仓库里配置的 keystore **签名后发布**。
  两个文件同时上传成 Actions artifact。Release 说明由「如何安装」+ `--generate-notes` 的
  变更日志组成。
- Android 签名需要四个 repository secrets：`ANDROID_KEYSTORE_BASE64`、
  `ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`；缺失时 Android
  job 会直接失败，不会产出未签名包。本地把同样的信息填进 `android/keystore.properties`
  （模板 `keystore.properties.example`，已被 gitignore）后直接 `assembleRelease` 即可签名；
  CI 通过环境变量注入，优先级高于 properties。
- 发布需要仓库在 Settings → Actions → General → Workflow permissions 里允许
  **Read and write permissions**，否则 `gh release create` 会因为没有写权限失败。

iOS 的镜像/签名注意事项见 [ios/AGENTS.md](./ios/AGENTS.md)，Android 的版本覆盖和产物路径
见 [android/AGENTS.md](./android/AGENTS.md)；发布 / 签名 / 版本号的完整检查清单见
[`.agents/skills/wows-release/SKILL.md`](./.agents/skills/wows-release/SKILL.md)。

## 文档维护

文档按平台拆分，避免两个平台的内容互相污染：

- 根 `README.md`：只放仓库总览、平台索引、两个平台共用的功能 / 评分 / 限制 / 声明。
  不写某一平台的构建命令或界面细节。
- `ios/README.md`、`android/README.md`：用户视角的平台文档（运行、构建、发布、平台限制）。
- 根 `AGENTS.md`：共享接口口径、收藏格式、发布流程、文档规则。
- `ios/AGENTS.md`、`android/AGENTS.md`：该平台的实现细节、代码约定、调试方法。
- `.agents/skills/<name>/SKILL.md`：跨平台的多步流程（目前有 `wows-localization`、
  `wows-app-icon`、`wows-favorites-archive`、`wows-release`、`wows-yuyuko-api`），配套脚本放在
  同名目录的 `scripts/` 下。写流程而不是重复平台细节，平台细节仍然放在对应的 `AGENTS.md`。

改动用户可见行为时，同时更新对应平台的 `README.md`；改动实现约定时更新对应的
`AGENTS.md`；改动接口、收藏格式或多语言流程时同时检查相关 skill。新增平台时创建
`<platform>/README.md` 和 `<platform>/AGENTS.md`，在根 README 的平台表和本文件的文档索引里
登记，并在发布工作流里增加对应 job 与产物。

## 后续可扩展方向

- 收藏分组、按名称 / 评分自动排序
- 排位赛季与成就
- 两个平台之间的界面 / 功能差异持续收敛
