#!/usr/bin/env python3
"""Translate messages_en_US.properties → ja_JP / ar_SA / ru_RU (rate-limited)."""
from __future__ import annotations

import argparse
import time
from pathlib import Path

from deep_translator import GoogleTranslator

REPO = Path(__file__).resolve().parents[2]


def translate_properties(src: Path, dst: Path, target_lang: str, delay_s: float) -> None:
	translator = GoogleTranslator(source="en", target=target_lang)
	with dst.open("w", encoding="utf-8") as out:
		out.write(f"# Localized from {src.name} (en_US → {target_lang})\n")
		for line in src.read_text(encoding="utf-8").splitlines():
			if not line.strip() or line.lstrip().startswith("#"):
				out.write(line + "\n")
				continue
			if "=" not in line:
				out.write(line + "\n")
				continue
			key, val = line.split("=", 1)
			translated = val
			for attempt in range(8):
				try:
					translated = translator.translate(val)
					break
				except Exception:
					time.sleep(min(8.0, 1.5 * (attempt + 1)))
			out.write(f"{key}={translated}\n")
			out.flush()
			time.sleep(delay_s)


def main() -> None:
	parser = argparse.ArgumentParser()
	parser.add_argument("module_rel", help="e.g. ecshopx-distribution/src/main/resources")
	parser.add_argument("basename", default="messages", nargs="?", const="messages")
	parser.add_argument("--delay", type=float, default=0.5)
	parser.add_argument(
		"--langs",
		default="ja,ar,ru",
		help="comma-separated target langs: ja,ar,ru",
	)
	args = parser.parse_args()
	base = REPO / args.module_rel
	src = base / f"{args.basename}_en_US.properties"
	if not src.is_file():
		raise SystemExit(f"missing {src}")
	mapping = {"ja": "ja_JP", "ar": "ar_SA", "ru": "ru_RU"}
	for lang in [x.strip() for x in args.langs.split(",") if x.strip()]:
		suffix = mapping.get(lang, lang)
		dst = base / f"{args.basename}_{suffix}.properties"
		print(f"Translating {dst.relative_to(REPO)} ...", flush=True)
		translate_properties(src, dst, lang, args.delay)
		print(f"  wrote {dst}", flush=True)


if __name__ == "__main__":
	main()
