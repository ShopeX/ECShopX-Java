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

package cn.shopex.ecshopx.goods.service.wxapp;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WxappGoodsItemsKeywordSpecialCharChecker {

	public boolean containsSpecialChar(String keywords) {
		if (!StringUtils.hasText(keywords)) {
			return false;
		}
		byte[] utf8 = keywords.getBytes(StandardCharsets.UTF_8);
		int i = 0;
		while (i < utf8.length) {
			int b = utf8[i] & 0xff;
			int charLen;
			if (b < 0x80) {
				charLen = 1;
			} else if ((b & 0xe0) == 0xc0) {
				charLen = 2;
			} else if ((b & 0xf0) == 0xe0) {
				charLen = 3;
			} else if ((b & 0xf8) == 0xf0) {
				charLen = 4;
			} else {
				charLen = 1;
			}
			if (charLen >= 4) {
				return true;
			}
			i += charLen;
		}
		return false;
	}
}
