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

package cn.shopex.ecshopx.distribution.service;

import net.sourceforge.pinyin4j.PinyinHelper;
import org.springframework.util.StringUtils;

public final class DistributorFirstLetterUtil {

	private DistributorFirstLetterUtil() {
	}

	public static String firstLetter(String name) {
		if (!StringUtils.hasText(name)) {
			return "Z#";
		}
		String s = name.trim();
		int cp = s.codePointAt(0);
		if (Character.isLetter(cp)) {
			String ch = new String(Character.toChars(cp));
			String u = ch.toUpperCase();
			if (u.length() == 1 && u.charAt(0) >= 'A' && u.charAt(0) <= 'Z') {
				return u;
			}
		}
		try {
			String[] arr = PinyinHelper.toHanyuPinyinStringArray((char) cp);
			if (arr != null && arr.length > 0 && StringUtils.hasText(arr[0])) {
				char c = Character.toUpperCase(arr[0].charAt(0));
				if (c >= 'A' && c <= 'Z') {
					return String.valueOf(c);
				}
			}
		} catch (Exception ignored) {
			// fall through
		}
		return "Z#";
	}
}
