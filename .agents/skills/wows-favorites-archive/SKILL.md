---
name: wows-favorites-archive
description: Change the cross-platform favorites JSON archive or local favorites storage in WoWs Insight. Use when editing FavoritesArchive, FavoritePlayer, version/exportedAt, import/export semantics, DataStore favorites.players, iOS UserDefaults FavoritesStore, or FavoritesArchiveTest.
---

# 收藏文件格式

两端共用同一份版本化 JSON 信封，iOS 导出的文件必须能直接导入 Android，反之亦然。
格式定义见根 [AGENTS.md](../../../AGENTS.md#收藏文件)。

```json
{
  "version": 1,
  "exportedAt": "2026-11-12T04:27:22Z",
  "players": [
    { "accountId": 3002439652, "nickname": "Yamato", "server": "asia" }
  ]
}
```

## 存储位置

- iOS：`UserDefaults`，由 `ios/WoWsInsight/ViewModels/FavoritesStore.swift` 读写；
  `PrivacyInfo.xcprivacy` 已声明 `NSPrivacyAccessedAPICategoryUserDefaults`（CA92.1）。
- Android：Preferences DataStore，键 `favorites.players`，值是 `List<FavoritePlayer>` 的 JSON
  数组（不是信封）；由 `data/favorites/FavoritesRepository.kt` 读写，`Mutex` 串行化，
  解析失败回退空列表。

## 硬性约束

- `accountId` 是 64 位：Android `Long`、iOS `Int64`（现有亚服账号 ID 已超过 `Int.MAX_VALUE`）。
- 必须同时保存 `server`：不同服务器的 `accountId` 会重复，收藏的唯一键是
  `(accountId, server)`。
- `exportedAt` 截断到整秒：Foundation 的 `.iso8601` 解码器不接受小数秒。Android 用
  `Instant.truncatedTo(ChronoUnit.SECONDS)`。
- 导入语义是「追加合并」：已存在的条目跳过，新条目追加到末尾，保留本机现有顺序，
  **导入永远不能删除本地收藏**，并返回 `ImportSummary(added, skipped)` 供界面提示。
- 只接受 `version == 1`。读不懂就提示导入失败，不保留旧格式 / 裸数组的兼容解析。
- Android 通过 SAF（`FavoritesFileService`）读写，不需要存储权限；读取上限 2 MiB，
  导出用 `wt` 覆盖写。iOS 用 `ShareLink` / `.fileImporter`，导出文件名
  `WoWs Insight Favorites.json`，只接受 `.json`。

## 改动时要同步的文件

- iOS：`Models/FavoritesArchive.swift`、`Models/FavoritePlayer.swift`、
  `ViewModels/FavoritesStore.swift`、`PrivacyInfo.xcprivacy`（动了存储 API 时）。
- Android：`data/model/Favorites.kt`、`data/favorites/FavoritesRepository.kt`、
  `data/favorites/FavoritesFileService.kt`、`ui/favorites/FavoritesViewModel.kt`。
- 测试：`android/app/src/test/java/com/talaxy/wowsinsight/FavoritesArchiveTest.kt`，至少覆盖
  iOS 信封解码、编码回读、整秒时间戳；运行 `./gradlew :app:testDebugUnitTest`。
- 文档：根 `AGENTS.md#收藏文件`、`android/AGENTS.md#收藏`、两端 `README.md` 的用户说明。

## 版本升级

提高 `version` 时，必须让两端的 reader 对旧版本和新版本都有明确行为（拒绝并提示，或显式
迁移），不能静默误读；旧版 App 遇到新版本文件必须提示导入失败而不是崩溃。
