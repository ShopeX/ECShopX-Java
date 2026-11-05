#!/usr/bin/env bash
# Run inside: /Library/WebServer/Documents/work/git/xecshop/platform/ecshopx-java-main
# While merge of feature/migrate-php-align-lang-ja-ru into master is in progress.
set -euo pipefail
ROOT="$(pwd)"
if [[ ! -f .git/MERGE_HEAD ]]; then
  echo "ERROR: no merge in progress in $ROOT" >&2
  exit 1
fi

git rm -f ecshopx-bootstrap/src/main/resources/db/migration/V20260916120000__multi_lang_ja_ru_tables.sql || true

python3 <<'PY'
from pathlib import Path
root = Path('.')

# 1) LanguageCanonicalCodes
p = root / 'ecshopx-common/src/main/java/cn/shopex/ecshopx/common/companys/language/LanguageCanonicalCodes.java'
text = p.read_text()
old = '''<<<<<<< HEAD
					"ru", "ru-RU");

	private static final Map<String, String> DISPLAY_NAMES;

	static {
		DISPLAY_NAMES = new LinkedHashMap<>();
		DISPLAY_NAMES.put("zh-CN", "中文");
		DISPLAY_NAMES.put("en-CN", "English");
		DISPLAY_NAMES.put("ar-SA", "العربية");
		DISPLAY_NAMES.put("zh-TW", "中文繁体");
		DISPLAY_NAMES.put("ja-JP", "日本語");
		DISPLAY_NAMES.put("ru-RU", "Русский");
	}
=======
					"ja-jp", "ja-JP",
					"ru", "ru-RU",
					"ru-ru", "ru-RU");

	private static final Map<String, String> DISPLAY_NAMES =
			new LinkedHashMap<>(
					Map.of(
							"zh-CN", "中文",
							"en-CN", "English",
							"ar-SA", "العربية",
							"zh-TW", "中文繁体",
							"ja-JP", "日本語",
							"ru-RU", "Русский"));
>>>>>>> feature/migrate-php-align-lang-ja-ru'''
new = '''					"ja-jp", "ja-JP",
					"ru", "ru-RU",
					"ru-ru", "ru-RU");

	private static final Map<String, String> DISPLAY_NAMES =
			new LinkedHashMap<>(
					Map.of(
							"zh-CN", "中文",
							"en-CN", "English",
							"ar-SA", "العربية",
							"zh-TW", "中文繁体",
							"ja-JP", "日本語",
							"ru-RU", "Русский"));'''
if old not in text:
    raise SystemExit('LanguageCanonicalCodes conflict block not found — already resolved?')
p.write_text(text.replace(old, new))
print('OK LanguageCanonicalCodes')

# 2) Test
p = root / 'ecshopx-common/src/test/java/cn/shopex/ecshopx/common/companys/language/LanguageCanonicalCodesTest.java'
text = p.read_text()
old = '''<<<<<<< HEAD
		assertThat(LanguageCanonicalCodes.sortOf("zh-TW")).isEqualTo(4);
=======
		assertThat(LanguageCanonicalCodes.displayName("ru-RU")).isEqualTo("Русский");
		assertThat(LanguageCanonicalCodes.sortOf("zh-TW")).isEqualTo(4);
		assertThat(LanguageCanonicalCodes.sortOf("ja-JP")).isEqualTo(5);
>>>>>>> feature/migrate-php-align-lang-ja-ru'''
new = '''		assertThat(LanguageCanonicalCodes.displayName("ru-RU")).isEqualTo("Русский");
		assertThat(LanguageCanonicalCodes.sortOf("zh-TW")).isEqualTo(4);
		assertThat(LanguageCanonicalCodes.sortOf("ja-JP")).isEqualTo(5);'''
if old not in text:
    raise SystemExit('LanguageCanonicalCodesTest conflict block not found')
p.write_text(text.replace(old, new))
print('OK LanguageCanonicalCodesTest')

# 3) CommonLangModReadService
p = root / 'ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/CommonLangModReadService.java'
text = p.read_text()
old = '''<<<<<<< HEAD
		if (LANG_ZH_CN.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesCn(companyId, roleIds);
		}
		if (LANG_EN_CN.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesEn(companyId, roleIds);
		}
		if (LANG_AR_SA.equalsIgnoreCase(lang)) {
			return loadRolesRoleNamesAr(companyId, roleIds);
		}
		// zh-TW and future locales: route by shard suffix (zhTW, ...)
=======
>>>>>>> feature/migrate-php-align-lang-ja-ru
		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(lang));'''
new = '''		return loadRolesRoleNamesFromOutside(companyId, roleIds, normalizeLangTableSuffix(lang));'''
if old not in text:
    raise SystemExit('CommonLangModReadService conflict block not found')
p.write_text(text.replace(old, new))
print('OK CommonLangModReadService')

# 4) ItemLangShardJdbc — master camelCase
p = root / 'ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/support/ItemLangShardJdbc.java'
text = p.read_text()
old = '''<<<<<<< HEAD
		for (String suffix : List.of("zhCN", "enCN", "arSA", "zhTW", "jaJP", "ruRU")) {
=======
		for (String suffix : List.of("zhCN", "enCN", "arSA", "zhtw", "jajp", "ruru")) {
>>>>>>> feature/migrate-php-align-lang-ja-ru'''
new = '''		for (String suffix : List.of("zhCN", "enCN", "arSA", "zhTW", "jaJP", "ruRU")) {'''
if old not in text:
    raise SystemExit('ItemLangShardJdbc conflict block not found')
p.write_text(text.replace(old, new))
print('OK ItemLangShardJdbc')
PY

git add \
  ecshopx-common/src/main/java/cn/shopex/ecshopx/common/companys/language/LanguageCanonicalCodes.java \
  ecshopx-common/src/test/java/cn/shopex/ecshopx/common/companys/language/LanguageCanonicalCodesTest.java \
  ecshopx-companys/src/main/java/cn/shopex/ecshopx/companys/service/CommonLangModReadService.java \
  ecshopx-goods/src/main/java/cn/shopex/ecshopx/goods/support/ItemLangShardJdbc.java

if git diff --name-only --diff-filter=U | grep -q .; then
  echo "ERROR: unresolved files remain:" >&2
  git diff --name-only --diff-filter=U >&2
  exit 1
fi

git commit -m "$(cat <<'EOF'
[TBID:Merge]Merge branch 'feature/migrate-php-align-lang-ja-ru' into master

Drop V20260916 jajp/ruru migration (V00000000000003 already has jaJP/ruRU/zhTW).
Resolve conflicts: LanguageCanonicalCodes aliases; CommonLangModRead six-lang normalize; ItemLangShardJdbc camelCase suffixes.

EOF
)"

git status -sb
echo "DONE: merge committed on master (not pushed)"
