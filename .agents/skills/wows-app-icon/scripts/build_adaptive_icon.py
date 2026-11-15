#!/usr/bin/env python3
"""Build the Android launcher foreground from ``android/icon.svg``.

The background is the solid ``#0599FC`` color; everything else in the SVG
(ship and waves) becomes the foreground layer. Both layers are vectors, so the
launcher icon stays crisp at every density.

    python .agents/skills/wows-app-icon/scripts/build_adaptive_icon.py
"""

from __future__ import annotations

import re
from pathlib import Path
from xml.sax.saxutils import escape

# .agents/skills/wows-app-icon/scripts/build_adaptive_icon.py
ROOT = Path(__file__).resolve().parents[4]
SOURCE = ROOT / "android" / "icon.svg"
RES = ROOT / "android" / "app" / "src" / "main" / "res"

ELEMENT_RE = re.compile(r"<(path|rect|circle)\b([^>]*)/>", re.S)
ATTRIBUTE_RE = re.compile(r'([\w-]+)="([^"]*)"')


def attributes(raw: str) -> dict[str, str]:
    return dict(ATTRIBUTE_RE.findall(raw))


def color(value: str) -> str:
    value = value.strip()
    if not value.startswith("#"):
        return value
    digits = value[1:]
    if len(digits) == 3:
        digits = "".join(ch * 2 for ch in digits)
    if len(digits) == 6:
        digits = "FF" + digits
    return "#" + digits.upper()


def number(value: str) -> str:
    return str(float(value)).rstrip("0").rstrip(".")


def to_path(element: str, attrs: dict[str, str]) -> str:
    if element == "path":
        return " ".join(attrs["d"].split())
    if element == "rect":
        x = number(attrs.get("x", "0"))
        y = number(attrs.get("y", "0"))
        width = number(attrs["width"])
        height = number(attrs["height"])
        return f"M{x},{y}h{width}v{height}h-{width}z"
    if element == "circle":
        cx = float(attrs["cx"])
        cy = float(attrs["cy"])
        radius = float(attrs["r"])
        r = number(str(radius))
        left = number(str(cx - radius))
        diameter = number(str(radius * 2))
        return (
            f"M{left},{number(str(cy))}"
            f"a{r},{r} 0 1,0 {diameter},0"
            f"a{r},{r} 0 1,0 -{diameter},0z"
        )
    raise ValueError(element)


def android_path(element: str, attrs: dict[str, str]) -> str:
    path_data = escape(to_path(element, attrs), {'"': "&quot;"})
    return (
        f'    <path\n'
        f'        android:pathData="{path_data}"\n'
        f'        android:fillColor="{color(attrs.get("fill", "#000000"))}" />'
    )


def main() -> None:
    svg = SOURCE.read_text(encoding="utf-8")
    elements = [
        (match.group(1), attributes(match.group(2)))
        for match in ELEMENT_RE.finditer(svg)
    ]

    background = next(
        (
            (element, attrs)
            for element, attrs in elements
            if element == "rect"
            and attrs.get("width") == "1024"
            and attrs.get("height") == "1024"
        ),
        None,
    )
    if background is None:
        raise SystemExit("android/icon.svg: background rect not found")

    foreground_elements = [
        (element, attrs)
        for element, attrs in elements
        if (element, attrs) != background
    ]

    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        '    android:width="108dp"',
        '    android:height="108dp"',
        '    android:viewportWidth="1024"',
        '    android:viewportHeight="1024">',
    ]
    lines.extend(
        android_path(element, attrs)
        for element, attrs in foreground_elements
    )
    lines.append("</vector>")

    drawable = RES / "drawable"
    drawable.mkdir(parents=True, exist_ok=True)
    (drawable / "ic_launcher_foreground.xml").write_text(
        "\n".join(lines) + "\n",
        encoding="utf-8",
    )
    print(f"wrote {len(foreground_elements)} foreground elements")


if __name__ == "__main__":
    main()
