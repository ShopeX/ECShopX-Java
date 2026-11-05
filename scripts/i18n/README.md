# Message bundle i18n

## 六种语言文件

| 后缀 | 来源 |
|------|------|
| `messages_zh_CN.properties` | 人工维护（简体） |
| `messages_zh_TW.properties` | `generate-message-locale-bundles.py`（OpenCC 繁体） |
| `messages_en_US.properties` | 人工维护（`en-CN` 业务码也用它） |
| `messages_ja_JP.properties` | 日语文案（见下） |
| `messages_ar_SA.properties` | 阿拉伯语文案 |
| `messages_ru_RU.properties` | 俄语文案 |

## 脚本

```bash
# 从 zh_CN / en_US 生成 zh_TW，且仅在 ja/ar/ru 仍为「Generated from」占位时复制 en
python3 scripts/i18n/generate-message-locale-bundles.py

# 从 en_US 机器翻译整文件（需网络；Google 限流时请加大 --delay 或分时执行）
python3 scripts/i18n/translate-message-locales-from-en.py ecshopx-distribution/src/main/resources messages --delay 0.8 --langs ja

# MyMemory（curl，规避 macOS Python SSL）；免费额度约 5000 字/天，耗尽会写 MYMEMORY WARNING — 勿提交
python3 scripts/i18n/translate-message-locales-mymemory.py ecshopx-distribution/src/main/resources messages --delay 0.12 --langs ja
```

已译满的模块：`ecshopx-goods`、`ecshopx-orders`、`ecshopx-payment`、`ecshopx-theme`（含 `theme-messages_*`）、**`ecshopx-distribution`**（`messages_ja_JP` / `messages_ar_SA` / `messages_ru_RU`）。
