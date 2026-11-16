---
name: wows-app-icon
description: Regenerate the WoWs Insight app icon assets from docs/icon.svg. Use when editing icon.svg, the Android adaptive icon (ic_launcher_foreground.xml, ic_launcher_background, mipmap-anydpi-v26) or the iOS AppIcon.appiconset PNG.
---

# 应用图标

两个平台的图标都来自同一个源文件 `docs/icon.svg`（放在仓库级 `docs/` 下，因为两端共用）。
改图标时先改源文件，再跑脚本同步产物，不要直接手改生成的 vector XML。

## 源文件

- `docs/icon.svg`：1024×1024 画布（外层还有两层 `svg` 嵌套，最内层 `viewBox` 是
  `0 0 1024 1024`）。
- 第一层 `<rect width="1024" height="1024" fill="#0599FC"/>` 是**背景**，其余元素是前景
  （船与海浪）。
- 背景色必须与 `android/app/src/main/res/values/colors.xml` 里的
  `ic_launcher_background` 和 `splash_background` 保持一致。

## Android

自适应图标由 `mipmap-anydpi-v26/ic_launcher.xml`（以及 `ic_launcher_round.xml`）引用：

```xml
<background android:drawable="@color/ic_launcher_background" />
<foreground android:drawable="@drawable/ic_launcher_foreground" />
```

`drawable/ic_launcher_foreground.xml` 是脚本生成的产物。改完 `icon.svg` 后在仓库根目录运行：

```sh
python .agents/skills/wows-app-icon/scripts/build_adaptive_icon.py
```

脚本把 SVG 里的 `path` / `rect` / `circle` 转成 `android:pathData`（1024 视口、108dp），
跳过作为背景的那个 `rect`，颜色统一转成 `#AARRGGBB` 形式。输出应当是
`wrote <n> foreground elements`。

## iOS

`ios/WoWsInsight/Assets.xcassets/AppIcon.appiconset/AppIcon.png` 是 1024×1024 单尺寸
`universal` 图标（`Contents.json` 里只有这一条），需要从同一份 `icon.svg` 导出的 PNG 替换。
脚本不生成 iOS 资源，替换后确认 PNG 是 1024×1024、无透明通道。

## 关于页用的方形图标副本

关于页显示的是完整方形图标（不是 adaptive icon），所以仓库里另存了两份 `AppIcon.png` 的副本：

- `ios/WoWsInsight/Assets.xcassets/AppIconPreview.imageset/AppIconPreview.png`（`Image("AppIconPreview")`）
- `android/app/src/main/res/drawable-nodpi/app_icon.png`（`R.drawable.app_icon`）

替换 `AppIcon.png` 后把同一份 PNG 覆盖到这两个路径（`cp` 即可，不需要改 `Contents.json`），
否则关于页会继续显示旧图标。

## 验证

- 生成后 `git diff --stat android/app/src/main/res` 只应出现预期文件；重复运行脚本不应产生
  额外改动。
- `./gradlew :app:assembleDebug` 能通过（生成的 XML 语法错误会让资源编译失败）。
- 需要看真实效果时装到设备上检查启动器里的图标（含 Android 12+ 的圆形 / 主题化蒙版）。
