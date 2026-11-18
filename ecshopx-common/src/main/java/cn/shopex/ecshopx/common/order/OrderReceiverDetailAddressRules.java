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

package cn.shopex.ecshopx.common.order;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.common.util.TextEmojiSupport;
import java.util.Map;
import org.springframework.util.StringUtils;

/** 下单收货详细地址规则（物流类配送）。 */
public final class OrderReceiverDetailAddressRules {

	public static final String EMOJI_NOT_ALLOWED_MSG = "详细地址不允许有表情符号";

	private OrderReceiverDetailAddressRules() {}

	public static void assertDetailAddressNoEmojiWhenLogistics(Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
			return;
		}
		String receipt = stringVal(params.get("receipt_type"));
		if (!("logistics".equals(receipt) || "dada".equals(receipt) || "merchant".equals(receipt))) {
			return;
		}
		String address = stringVal(params.get("receiver_address"));
		if (!StringUtils.hasText(address)) {
			return;
		}
		if (TextEmojiSupport.containsEmoji(address)) {
			throw new ResourceException(EMOJI_NOT_ALLOWED_MSG);
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}
}