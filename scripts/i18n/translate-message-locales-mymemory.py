#!/usr/bin/env python3
"""Translate messages_en_US.properties → ja_JP / ar_SA / ru_RU via MyMemory (Google fallback blocked)."""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import time
import urllib.parse
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]

PH_RE = re.compile(r"\{(\d+)\}")
QUOTA_SNIPPET = "MYMEMORY WARNING"
SKIP_VALUES = frozenset(
	{
		"uuuu-MM-dd",
		":00",
	}
)


def restore_mangled_placeholders(text: str) -> str:
	return re.sub(r"__\s*PH\s*(\d+)\s*__", r"{\1}", text)


def mymemory_translate(text: str, target: str, delay_s: float) -> str:
	if not text.strip() or text in SKIP_VALUES:
		return text
	pair = urllib.parse.quote(f"en|{target}")
	q = urllib.parse.quote(text)
	url = f"https://api.mymemory.translated.net/get?q={q}&langpair={pair}"
	for attempt in range(6):
		try:
			proc = subprocess.run(
				["curl", "-sS", "--max-time", "45", url],
				capture_output=True,
				text=True,
				check=False,
			)
			if proc.returncode != 0:
				raise RuntimeError(proc.stderr or f"curl exit {proc.returncode}")
			data = json.loads(proc.stdout)
			out = data.get("responseData", {}).get("translatedText", text)
			if data.get("quotaFinished") or QUOTA_SNIPPET in out:
				raise RuntimeError("MyMemory daily quota exhausted")
			if out and out != text:
				return restore_mangled_placeholders(out)
			time.sleep(min(5.0, 1.0 * (attempt + 1)))
		except Exception:
			time.sleep(min(10.0, 1.5 * (attempt + 1)))
	time.sleep(delay_s)
	return text


def translate_file(src: Path, dst: Path, target: str, delay_s: float) -> None:
	lines = src.read_text(encoding="utf-8").splitlines()
	with dst.open("w", encoding="utf-8") as out:
		out.write(f"# Localized from {src.name} (en_US → {target})\n")
		for line in lines:
			if not line.strip() or line.lstrip().startswith("#"):
				out.write(line + "\n")
				continue
			if "=" not in line:
				out.write(line + "\n")
				continue
			key, val = line.split("=", 1)
			translated = mymemory_translate(val, target, delay_s)
			out.write(f"{key}={translated}\n")
			out.flush()
			time.sleep(delay_s)


def main() -> None:
	parser = argparse.ArgumentParser()
	parser.add_argument("module_rel", help="e.g. ecshopx-distribution/src/main/resources")
	parser.add_argument("basename", nargs="?", default="messages")
	parser.add_argument("--delay", type=float, default=0.35)
	parser.add_argument("--langs", default="ja,ar,ru")
	args = parser.parse_args()
	base = REPO / args.module_rel
	src = base / f"{args.basename}_en_US.properties"
	if not src.is_file():
		raise SystemExit(f"missing {src}")
	mapping = {"ja": "ja_JP", "ar": "ar_SA", "ru": "ru_RU"}
	for lang in [x.strip() for x in args.langs.split(",") if x.strip()]:
		suffix = mapping.get(lang, lang)
		dst = base / f"{args.basename}_{suffix}.properties"
		print(f"Translating → {dst.relative_to(REPO)} ...", flush=True)
		translate_file(src, dst, lang, args.delay)
		print(f"  wrote {dst}", flush=True)


if __name__ == "__main__":
	main()
