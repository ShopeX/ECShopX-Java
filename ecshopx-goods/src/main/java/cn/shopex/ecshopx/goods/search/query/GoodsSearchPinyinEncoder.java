/**
 * Copyright 2019-2026 ShopeX
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.shopex.ecshopx.goods.search.query;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.util.StringUtils;

/**
 * 索引侧拼音编码：全拼与首字母（无声调、小写、去标点），供拼音检索字段写入与查询归一化。
 */
public final class GoodsSearchPinyinEncoder {

	private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

	static {
		FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		FORMAT.setVCharType(HanyuPinyinVCharType.WITH_V);
	}

	private GoodsSearchPinyinEncoder() {}

	/** 全拼连续串，例如「山岳户外」→ {@code shanyuehuwai}。 */
	public static String fullPinyin(String text) {
		return encode(text, false);
	}

	/** 首字母连续串，例如「山岳户外」→ {@code syhw}。 */
	public static String firstLetters(String text) {
		return encode(text, true);
	}

	/** 查询串归一化：仅保留 a-z0-9 并小写，便于匹配 ngram 拼音字段。 */
	public static String normalizeQuery(String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return "";
		}
		StringBuilder sb = new StringBuilder(keywords.length());
		for (int i = 0; i < keywords.length(); ) {
			int cp = keywords.codePointAt(i);
			i += Character.charCount(cp);
			if (cp >= 'A' && cp <= 'Z') {
				sb.append((char) (cp + 32));
			} else if ((cp >= 'a' && cp <= 'z') || (cp >= '0' && cp <= '9')) {
				sb.append((char) cp);
			}
		}
		return sb.toString();
	}

	private static String encode(String text, boolean firstLetterOnly) {
		if (!StringUtils.hasText(text)) {
			return "";
		}
		StringBuilder sb = new StringBuilder(text.length() * 2);
		for (int i = 0; i < text.length(); ) {
			int cp = text.codePointAt(i);
			i += Character.charCount(cp);
			if (isHan(cp)) {
				String syllable = syllableOf((char) cp);
				if (syllable.isEmpty()) {
					continue;
				}
				if (firstLetterOnly) {
					sb.append(syllable.charAt(0));
				} else {
					sb.append(syllable);
				}
			} else if (cp >= 'A' && cp <= 'Z') {
				sb.append((char) (cp + 32));
			} else if ((cp >= 'a' && cp <= 'z') || (cp >= '0' && cp <= '9')) {
				sb.append((char) cp);
			}
		}
		return sb.toString();
	}

	private static boolean isHan(int cp) {
		Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
		return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
				|| block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
				|| block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A;
	}

	private static String syllableOf(char ch) {
		try {
			String[] arr = PinyinHelper.toHanyuPinyinStringArray(ch, FORMAT);
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				return arr[0];
			}
		} catch (BadHanyuPinyinOutputFormatCombination ignored) {
			// fall through
		}
		return "";
	}
}
