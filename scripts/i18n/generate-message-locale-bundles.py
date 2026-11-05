#!/usr/bin/env python3
"""
Generate messages_zh_TW / messages_ar_SA / messages_ja_JP / messages_ru_RU from
messages_zh_CN and messages_en_US in each module resources directory.

- zh_TW: OpenCC s2t on property values (comments/keys unchanged).
- ar_SA, ja_JP, ru_RU: copy of en_US values (same keys as English bundle; avoids
  silent fallback to default Chinese when locale is ar-SA / ja-JP / ru-RU).

Re-run after adding keys to zh_CN and en_US.
"""
from __future__ import annotations

import re
from pathlib import Path

try:
    import opencc
except ImportError as exc:  # pragma: no cover
    raise SystemExit("pip install opencc-python-reimplemented") from exc

REPO = Path(__file__).resolve().parents[2]
MODULES = [
    "ecshopx-distribution",
    "ecshopx-goods",
    "ecshopx-orders",
    "ecshopx-payment",
    "ecshopx-theme",
]
BASENAMES = ["messages", "theme-messages"]

OPENCC = opencc.OpenCC("s2t")
KV = re.compile(r"^(\s*[^#=\s][^=]*?)=(.*)$")


def convert_values_s2t(text: str) -> str:
    out_lines: list[str] = []
    for line in text.splitlines(keepends=True):
        if line.endswith("\n"):
            body, nl = line[:-1], "\n"
        else:
            body, nl = line, ""
        if not body.strip() or body.lstrip().startswith("#"):
            out_lines.append(line)
            continue
        m = KV.match(body)
        if not m:
            out_lines.append(line)
            continue
        key, val = m.group(1), m.group(2)
        out_lines.append(f"{key}={OPENCC.convert(val)}{nl}")
    return "".join(out_lines)


def write_if_source(resources: Path, basename: str, src_suffix: str, dst_name: str, transform) -> bool:
    src = resources / f"{basename}_{src_suffix}.properties"
    if not src.is_file():
        return False
    dst = resources / dst_name
    content = src.read_text(encoding="utf-8")
    header = f"# Generated from {src.name} by scripts/i18n/generate-message-locale-bundles.py\n"
    if dst_name == "messages_zh_TW.properties" or dst_name == "theme-messages_zh_TW.properties":
        body = transform(content)
    else:
        body = content
    dst.write_text(header + body, encoding="utf-8")
    return True


def main() -> None:
    for module in MODULES:
        resources = REPO / module / "src" / "main" / "resources"
        if not resources.is_dir():
            continue
        for basename in BASENAMES:
            if not (resources / f"{basename}_zh_CN.properties").is_file() and not (
                resources / f"{basename}_en_US.properties"
            ).is_file():
                continue
            write_if_source(
                resources,
                basename,
                "zh_CN",
                f"{basename}_zh_TW.properties",
                convert_values_s2t,
            )
            for dst in (
                f"{basename}_ar_SA.properties",
                f"{basename}_ja_JP.properties",
                f"{basename}_ru_RU.properties",
            ):
                path = resources / dst
                if path.is_file():
                    head = path.read_text(encoding="utf-8")[:80]
                    if "Localized from" in head:
                        continue
                write_if_source(resources, basename, "en_US", dst, lambda x: x)


if __name__ == "__main__":
    main()
