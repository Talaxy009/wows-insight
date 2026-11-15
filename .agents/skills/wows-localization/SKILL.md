---
name: wows-localization
description: Add or change WoWs Insight UI copy and supported languages across iOS and Android. Use when editing Localizable.xcstrings, L10n.swift, L10n keys, Android values*/strings.xml, generate_strings.py, EXTRA strings, locales_config.xml, or ship-name localization.
---

# WoWs Insight 多语言

两端的界面文案来自同一份 iOS String Catalog；Android 资源是生成产物。改错一侧或手改生成的
XML 会让两端文案漂移。

## 事实来源

- iOS `ios/WoWsInsight/Localizable.xcstrings`：唯一文案源，语言为 en / ja / ko / zh-Hans / zh-Hant。
- iOS `ios/WoWsInsight/InfoPlist.xcstrings`：各语言 App 名称（`CFBundleDisplayName`）。
- iOS 代码通过 `ios/WoWsInsight/App/L10n.swift` 的语义化 key 引用；key 在 catalog 里缺失时
  界面直接显示 key，没有英文兜底。
- Android `android/app/src/main/res/values*/strings.xml`：由本技能的
  `scripts/generate_strings.py` 生成，**不要手改**。

## 新增或修改文案

1. 在 `L10n.swift` 加语义化 key（例如 `player.stats.winRate`），并在
   `Localizable.xcstrings` 补齐全部五种语言；`%1$@` / `%2$@` / `%ld` 等占位符不能删除或调序。
2. 在仓库根目录生成 Android 资源：

   ```sh
   python .agents/skills/wows-localization/scripts/generate_strings.py
   ```

3. Android 独有的无障碍 / 平台文案（TalkBack 标签等）加到脚本顶部的 `EXTRA`
   字典，五种语言齐全；key 与 catalog 生成结果重名时脚本会直接报错。
4. 校验改动范围：`git diff --stat` 只应出现预期的 `Localizable.xcstrings` / `L10n.swift` /
   `strings.xml`（生成文件的头部注释会带脚本新路径）；需要时再跑 `./gradlew :app:lintDebug`。

## 转换规则

- 资源名：点转下划线、camelCase 前插下划线、全部小写
  （`player.stats.winRate` → `player_stats_win_rate`）。
- 占位符：`%1$@` → `%1$s`，`%ld` → `%d`，裸 `%@` → `%s`。
- 撇号转义成 `\'`（Android 资源不接受裸撇号）。
- `app.title` 生成 `app_title`。

## 新增一种语言

1. iOS：在 `Localizable.xcstrings` 添加语言并翻译全部 key；在 `InfoPlist.xcstrings` 添加同一
   语言的 `CFBundleDisplayName`；在 `ios/WoWsInsight.xcodeproj/project.pbxproj` 的
   `knownRegions` 加语言码；如 Xcode 的 Project → Info → Localizations 里没有，手动添加。
2. Android：在脚本的 `LOCALES` 加 `values-xx` → iOS locale 映射。
3. 确认 `android/app/src/main/res/xml/locales_config.xml` 加了 `<locale android:name="…" />`，
   并保持 `AndroidManifest.xml` 的 `android:localeConfig` 指向该文件（通常无需改动）。
4. 跑脚本，确认每个语言目录都生成了 `strings.xml`。
5. 舰名：接口只提供中文和英文舰名。新语言如果需要自己的舰名，扩展 iOS
   `AppConfig.shipNameLanguage` / `YuyukoShipMeta.name(language:server:)` 和 Android
   `data/model/ShipNameLanguage.kt`。

## 舰名规则（两端必须一致）

- 简体中文 / 繁体中文 → `nameCn`（繁体界面也用简体舰名，接口没有繁体舰名）。
- 英语 → `nameEnglish`；日语 / 韩语 → 界面日 / 韩语，舰名回退 `nameEnglish`。
- 其他系统语言 → 跟随界面回退结果。
- 国服（`cn`）优先 `nameCn360`（例如「鲸」而不是「大和」），其他服务器用 `nameCn`。

## 不要做

- 不要手改 `values*/strings.xml`，也不要只改 Android 一侧。
- 不要新增 App 内语言切换开关；界面语言跟随系统（Android 13+ 的每应用语言由
  `localeConfig` 提供）。
- 不要把 Android 独有文案塞进 iOS catalog，也不要在 Kotlin 里拼死文案（走 `UiText` 资源 id）。
