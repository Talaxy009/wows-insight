---
name: wows-yuyuko-api
description: Integrate or debug the Yuyuko public API used by both WoWs Insight clients. Use when adding endpoints or fields, editing Yuyuko DTOs and mappers, handling nullable payloads, error envelopes, battle type codes, player/ship ratings, 64-bit IDs, or daily recent records.
---

# Yuyuko 公共接口口径

两个客户端都不直接调用 Wargaming API，只使用社区 Yuyuko 的 `/public/*` 接口。口径细节见根
[AGENTS.md](../../../AGENTS.md#数据来源)。

## 请求约定

- Base URL：`https://v3-api.wows.shinoaki.com`，文档
  `https://v3-api.wows.shinoaki.com/v3/index.html`。
- 只使用 `/public/*`，无需 Authorization；所有请求带
  `Yuyuko-Client-Type: APP;WoWsInsight;1.0`（文档标注为必填）。
- 响应信封 `{ code, message, data }`，只有 `code == 200` 算成功。
- HTTP 404 / 405 与信封里的 404 / 405 统一映射为「找不到」；取消请求不能显示成错误。
- 国服精确搜索收到 HTTP 400 / 信封 `code 540` 时当作空结果。
- 首次查询玩家要回源 Wargaming，约 6–8 秒；超时设为 30 秒，命中缓存后 0.3–0.6 秒。

## 接口

| 数据 | 接口 |
| --- | --- |
| 玩家搜索 | `POST public/wows/account/search/{server}/user` |
| 玩家总览 + 分类型战绩 | `GET public/wows/account/user/info2` |
| 每舰战绩 + 舰船信息 + 评分 | `GET public/wows/account/ship/info/list` |
| 每日近期记录 | `GET public/wows/account/day/list/info` |

`{server}` 为 `ru` / `eu` / `na` / `asia` / `cn`。国服只支持精确昵称（`one: true`），其他
服务器是 3 字符起的模糊搜索。

## 结构要点

- 战斗类型是 `battleTypeInfo` 字典，key 为 `PVP` / `PVP_SOLO` / `PVP_DIV2` / `PVP_DIV3` /
  `RANK(_SOLO/_DIV2/_DIV3)` / `RANK_OLD*` / `CLAN` / `BRAWL` / `OPER_MM` /
  `PVE(_SOLO/_DIV2/_DIV3)` / `RATING(_SOLO/_DIV)`。两端 `BattleMode` 使用同一套 raw value，
  声明顺序即选择器顺序，未知代码忽略，默认 `PVP`。只有 `battleCount > 0` 的类型才显示。
- `account/user/info2` 的 `battleTypeInfo[code].shipInfo` 与 `ship/info/list` 的
  `typeInfo[code].battleInfo` 是同一套 `BattleInfoData`，只是字段名不同。
- 评分按当前类型取 `prInfo.value`（玩家取 `battleTypeInfo[code]`，战舰取 `typeInfo[code]`）；
  顶层 `prInfo` 是全部模式口径，不使用。`prInfo.code == 0` 表示暂无数据，显示 `—`。
- 每日记录在 `recentInfo`，按天给出当天战绩；服务端只为观察过的账号保留，少于两天不画趋势图。

## 类型与容错

- 账号 ID、战舰 ID 是 64 位：Android `Long`、iOS `Int64`（亚服账号 ID 已超过 `Int.MAX_VALUE`）。
- 生涯总计（潜在伤害最常见）可能超过 `Int.MAX_VALUE`：伤害 / 经验 / 潜在伤害 / 侦查伤害 /
  记录值用 64 位，单场次数用 32 位。
- 每个 DTO 字段都可空；缺字段归一化成 0 / `unknown`，任何缺失都不能让页面崩溃。
- 接口没有平局数：`draws = battles - wins - losses`；没有主炮射击数：命中率用
  `hitRatioInfo.ratioMain`。
- 每日记录是尽力而为：失败只表示没有趋势图，不能影响玩家页。
- 不做离线缓存；收藏是唯一的本地用户数据。

## 代码位置

- Android：`data/remote/YuyukoApi.kt`、`YuyukoDto.kt`、`YuyukoMappers.kt`；
  `YuyukoApi` 是 interface，测试用 fake 实现。
- iOS：`Networking/YuyukoClient.swift`、`Networking/YuyukoModels.swift`。
- 改动字段时两端映射和测试一起更新，并检查根 `AGENTS.md` 的接口表是否仍然准确。
