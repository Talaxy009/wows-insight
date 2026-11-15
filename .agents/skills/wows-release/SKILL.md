---
name: wows-release
description: Trigger or modify the WoWs Insight release that publishes a signed Android APK and an unsigned iOS IPA. Use when publishing a version, bumping versionName/versionCode/MARKETING_VERSION, editing release.yml, configuring signing secrets or keystores, or verifying release artifacts.
---

# WoWs Insight 发布

[`release.yml`](../../../.github/workflows/release.yml) 手动触发（Actions → Release → Run
workflow），一次构建 iOS 和 Android，发到同一个 GitHub Release。

## 版本规则

- 输入 `version` 可带 `v` 前缀：`1.0.1` → 标签 `v1.0.1`；格式必须是 `1.2` 或 `1.2.3`。
- `version` 留空时取 iOS 工程的 `MARKETING_VERSION`。
- 两个平台共用同一个 `versionName`；`versionCode` / `CURRENT_PROJECT_VERSION` 都用
  workflow run number。
- 标签已存在时 workflow 直接失败，需要换一个版本号。
- 产物：`WoWsInsight-<version>.ipa`（未签名）和 `WoWsInsight-<version>.apk`（已签名），
  同时作为 Actions artifact（`ios-ipa` / `android-apk`）保留 14 天。
- Release 说明 = 固定「安装」段 + `--generate-notes` 的变更日志。

## Android 签名

- 需要四个 repository secrets：`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、
  `ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`。缺任意一个，Android job 直接失败，
  不会产出未签名包。
- CI 把 keystore 解码到 `$RUNNER_TEMP/release.keystore`，通过 `ANDROID_KEYSTORE_PATH` 等
  环境变量注入；`app/build.gradle.kts` 的读取顺序是环境变量 > `android/keystore.properties`
  > 无签名（未签名包）。
- 构建后执行 `apksigner verify --print-certs`，验证失败不会进入 Release。
- 本地签名：`cp android/keystore.properties.example android/keystore.properties`，填
  `storeFile` / `storePassword` / `keyAlias` / `keyPassword`，再
  `cd android && ./gradlew :app:assembleRelease`。`storeFile` 相对 `android/` 解析。
- 同一包名的后续更新必须使用同一份 keystore，丢失后无法覆盖安装；keystore 和密码不能提交
  仓库（`.gitignore` 已忽略 `*.jks` / `*.keystore` / `keystore.properties`）。

## iOS

- 必须跑在 `xcode-27` 镜像（iOS 27 SDK）。不要退回旧镜像：旧 SDK 构建的包在 iOS 26+ 会走
  兼容外观。
- IPA 不签名（`CODE_SIGNING_ALLOWED=NO`）；使用者自行用 AltStore / Sideloadly / Xcode 重签。
  重签时描述文件的 App ID 必须等于 `PRODUCT_BUNDLE_IDENTIFIER`，否则 `.fileImporter` 拿不到
  可读 URL。

## 前置条件与发布前检查

- 仓库 Settings → Actions → General → Workflow permissions 需要 **Read and write
  permissions**，否则 `gh release create` 会因 `GITHUB_TOKEN` 无写权限失败。
- 发布前在 `android/` 跑 `./gradlew :app:testDebugUnitTest :app:lintDebug`。
- 确认目标版本号没有被已有标签占用，四个 Android secrets 已配置。
- 发布后核对 Release 里两个附件都存在且 APK 已签名。
