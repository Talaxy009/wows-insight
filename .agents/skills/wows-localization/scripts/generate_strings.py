#!/usr/bin/env python3
"""Generate Android string resources from the iOS string catalog.

The iOS project is the single source of truth for the UI copy.  Run this
script from the repository root after changing
``ios/WoWsInsight/Localizable.xcstrings``::

    python .agents/skills/wows-localization/scripts/generate_strings.py
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from xml.sax.saxutils import escape

# .agents/skills/wows-localization/scripts/generate_strings.py
ROOT = Path(__file__).resolve().parents[4]
IOS_CATALOG = ROOT / "ios" / "WoWsInsight" / "Localizable.xcstrings"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

LOCALES = {
    "values": "en",
    "values-zh-rCN": "zh-Hans",
    "values-zh-rTW": "zh-Hant",
    "values-ja": "ja",
    "values-ko": "ko",
}

# Android-only copy that has no iOS counterpart.  Keep this list small and
# semantic; platform affordances (clear search, drag to reorder, ...) need
# accessible labels of their own.
EXTRA = {
    "common_back": {
        "en": "Back",
        "zh-Hans": "返回",
        "zh-Hant": "返回",
        "ja": "戻る",
        "ko": "뒤로",
    },
    "common_close": {
        "en": "Close",
        "zh-Hans": "关闭",
        "zh-Hant": "關閉",
        "ja": "閉じる",
        "ko": "닫기",
    },
    "player_charts_trend_a11y": {
        "en": "%1$s from %2$s to %3$s, latest %4$s.",
        "zh-Hans": "%2$s 至 %3$s 的%1$s，最新为 %4$s。",
        "zh-Hant": "%2$s 至 %3$s 的%1$s，最新為 %4$s。",
        "ja": "%2$s から %3$s までの%1$s。最新は %4$s。",
        "ko": "%2$s부터 %3$s까지의 %1$s. 최신 값은 %4$s입니다.",
    },
    "player_charts_no_data": {
        "en": "No data for this breakdown.",
        "zh-Hans": "该分类下没有数据。",
        "zh-Hant": "該分類下沒有資料。",
        "ja": "この内訳のデータはありません。",
        "ko": "이 분류의 데이터가 없습니다.",
    },
    "search_clear_query": {
        "en": "Clear search",
        "zh-Hans": "清除搜索",
        "zh-Hant": "清除搜尋",
        "ja": "検索をクリア",
        "ko": "검색 지우기",
    },
    "search_export_done": {
        "en": "Favorites exported",
        "zh-Hans": "收藏已导出",
        "zh-Hant": "收藏已匯出",
        "ja": "お気に入りを書き出しました",
        "ko": "즐겨찾기를 내보냈습니다",
    },
    "search_export_failed": {
        "en": "Could not export favorites",
        "zh-Hans": "无法导出收藏",
        "zh-Hant": "無法匯出收藏",
        "ja": "お気に入りを書き出せませんでした",
        "ko": "즐겨찾기를 내보내지 못했습니다",
    },
    "favorites_save_failed": {
        "en": "Could not save favorites",
        "zh-Hans": "无法保存收藏",
        "zh-Hant": "無法儲存收藏",
        "ja": "お気に入りを保存できませんでした",
        "ko": "즐겨찾기를 저장하지 못했습니다",
    },
    "search_favorites_move_down": {
        "en": "Move down",
        "zh-Hans": "下移",
        "zh-Hant": "下移",
        "ja": "下へ移動",
        "ko": "아래로 이동",
    },
    "search_favorites_move_up": {
        "en": "Move up",
        "zh-Hans": "上移",
        "zh-Hant": "上移",
        "ja": "上へ移動",
        "ko": "위로 이동",
    },
    "search_favorites_reorder": {
        "en": "Reorder favorites",
        "zh-Hans": "调整收藏顺序",
        "zh-Hant": "調整收藏順序",
        "ja": "お気に入りを並べ替え",
        "ko": "즐겨찾기 순서 변경",
    },
    "search_no_results_message": {
        "en": "No player matches “%1$s”.",
        "zh-Hans": "没有找到与“%1$s”匹配的玩家。",
        "zh-Hant": "沒有找到與「%1$s」相符的玩家。",
        "ja": "「%1$s」に一致するプレイヤーは見つかりませんでした。",
        "ko": "“%1$s”와 일치하는 플레이어가 없습니다.",
    },
    "search_no_results_title": {
        "en": "No results",
        "zh-Hans": "没有结果",
        "zh-Hant": "沒有結果",
        "ja": "結果なし",
        "ko": "결과 없음",
    },
    "search_results_header": {
        "en": "Search results",
        "zh-Hans": "搜索结果",
        "zh-Hant": "搜尋結果",
        "ja": "検索結果",
        "ko": "검색 결과",
    },
    "search_server_selector": {
        "en": "Select server",
        "zh-Hans": "选择服务器",
        "zh-Hant": "選擇伺服器",
        "ja": "サーバーを選択",
        "ko": "서버 선택",
    },
    "ship_not_found_message": {
        "en": "This ship is no longer in the player's list.",
        "zh-Hans": "该战舰已不在玩家的战舰列表中。",
        "zh-Hant": "該戰艦已不在玩家的戰艦列表中。",
        "ja": "この艦艇はプレイヤーの艦艇リストにありません。",
        "ko": "이 함선은 플레이어의 함선 목록에 없습니다.",
    },
    "ship_not_found_title": {
        "en": "Ship not found",
        "zh-Hans": "找不到战舰",
        "zh-Hant": "找不到戰艦",
        "ja": "艦艇が見つかりません",
        "ko": "함선을 찾을 수 없습니다",
    },
    "player_ship_image": {
        "en": "Ship image",
        "zh-Hans": "战舰图片",
        "zh-Hant": "戰艦圖片",
        "ja": "艦艇画像",
        "ko": "함선 이미지",
    },
}


def android_name(key: str) -> str:
    """Translate the iOS semantic key to a legal Android resource name."""
    name = key.replace(".", "_")
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "_", name)
    name = re.sub(r"[^A-Za-z0-9_]", "_", name).lower()
    return name


def android_format(value: str) -> str:
    """Convert Foundation format specifiers to Android's java.util.Formatter."""
    value = re.sub(r"%(\d+)\$@", r"%\1$s", value)
    value = re.sub(r"%(\d+)\$ld", r"%\1$d", value)
    value = value.replace("%@", "%s")
    value = value.replace("%ld", "%d")
    return value


def escape_value(value: str) -> str:
    value = android_format(value)
    value = escape(value)
    # Android treats an unescaped apostrophe as a resource syntax error.
    return value.replace("'", "\\'")


def main() -> None:
    catalog = json.loads(IOS_CATALOG.read_text(encoding="utf-8"))
    entries: dict[str, dict[str, str]] = {}

    for key, item in catalog["strings"].items():
        localizations = item.get("localizations", {})
        translations = {}
        for directory, ios_locale in LOCALES.items():
            unit = localizations.get(ios_locale, {}).get("stringUnit", {})
            translations[ios_locale] = unit.get("value", key)
        entries[android_name(key)] = translations

    for key, translations in EXTRA.items():
        if key in entries:
            raise SystemExit(f"duplicate string resource: {key}")
        entries[key] = translations

    for directory, ios_locale in LOCALES.items():
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- Generated by .agents/skills/wows-localization/scripts/generate_strings.py. Do not edit by hand. -->",
            "<resources>",
        ]
        for key in sorted(entries):
            value = entries[key].get(ios_locale)
            if value is None:
                continue
            lines.append(f'    <string name="{key}">{escape_value(value)}</string>')
        lines.append("</resources>")
        target = RES / directory / "strings.xml"
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"wrote {target.relative_to(ROOT)} ({len(entries)} strings)")


if __name__ == "__main__":
    main()
