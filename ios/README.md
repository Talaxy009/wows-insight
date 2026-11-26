# WoWs Insight for iOS

原生 SwiftUI 客户端，功能、接口与统计口径和 [Android 版本](../android/README.md) 一致，功能列表、评分说明和共同限制见[根目录 README](../README.md)。

- 系统要求：iOS 18.0+
- 开发环境：Xcode 16 或更新版本、Swift 6（严格并发）
- 无第三方依赖：只使用 Foundation + SwiftUI + Observation

## 交互

- 玩家页右上角星标收藏 / 取消收藏，搜索结果左滑也能直接收藏。
- 首页左滑取消收藏，长按行拖动调整顺序。
- 首页右上角「⋯」菜单：导出 / 导入收藏 JSON、关于页。
- 界面语言跟随系统，App 内没有切换开关。

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

## 安装发布包

[Releases](https://github.com/Talaxy009/wows-insight/releases) 里的 `WoWsInsight-<版本>.ipa` **未签名**，需要自己用 AltStore / Sideloadly / Xcode 重签。重签名时描述文件的 App ID 必须等于 `PRODUCT_BUNDLE_IDENTIFIER`，否则 App 能启动，但导入收藏时「选了文件没反应」。

## 平台限制

- 收藏不支持 iCloud 同步：免费的个人 Team 无法签名 iCloud 能力，跨设备迁移只能靠导出 / 导入 JSON。
