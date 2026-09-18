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

package cn.shopex.ecshopx.kaquan.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Legacy PHP serialize for {@code kaquan_user_discount.use_condition} (Doctrine array column).
 */
public final class UserDiscountUseConditionLegacyCodec {

	private UserDiscountUseConditionLegacyCodec() {}

	public static String serialize(Map<String, Object> useCondition) {
		Objects.requireNonNull(useCondition, "useCondition");
		if (useCondition.isEmpty()) {
			return "a:0:{}";
		}
		try {
			ByteArrayOutputStream buf = new ByteArrayOutputStream(useCondition.size() * 48 + 16);
			buf.write('a');
			buf.write(':');
			buf.write(Integer.toString(useCondition.size()).getBytes(StandardCharsets.US_ASCII));
			buf.write(':');
			buf.write('{');
			for (Map.Entry<String, Object> e : useCondition.entrySet()) {
				writeSerializedString(buf, e.getKey());
				writeValue(buf, e.getValue());
			}
			buf.write('}');
			return buf.toString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("use_condition serialize failed", e);
		}
	}

	private static void writeValue(ByteArrayOutputStream buf, Object v) throws IOException {
		if (v == null) {
			buf.write('N');
			buf.write(';');
			return;
		}
		if (v instanceof Number n) {
			writeSerializedInt(buf, n.longValue());
			return;
		}
		if (v instanceof Boolean b) {
			writeSerializedInt(buf, b ? 1L : 0L);
			return;
		}
		writeSerializedString(buf, v.toString());
	}

	private static void writeSerializedInt(ByteArrayOutputStream buf, long n) throws IOException {
		buf.write('i');
		buf.write(':');
		buf.write(Long.toString(n).getBytes(StandardCharsets.US_ASCII));
		buf.write(';');
	}

	private static void writeSerializedString(ByteArrayOutputStream buf, String s) throws IOException {
		byte[] utf8 = s.getBytes(StandardCharsets.UTF_8);
		buf.write('s');
		buf.write(':');
		buf.write(Integer.toString(utf8.length).getBytes(StandardCharsets.US_ASCII));
		buf.write(':');
		buf.write('"');
		for (byte b : utf8) {
			int u = b & 0xFF;
			if (u == '\\') {
				buf.write('\\');
				buf.write('\\');
			} else if (u == '"') {
				buf.write('\\');
				buf.write('"');
			} else if (u == 0) {
				buf.write('\\');
				buf.write('0');
			} else {
				buf.write(b);
			}
		}
		buf.write('"');
		buf.write(';');
	}

	public static Map<String, Object> buildFromCardInfo(Map<String, Object> cardInfo, int leastCost) {
		Map<String, Object> useCondition = new LinkedHashMap<>();
		useCondition.put("accept_category", cardInfo.get("accept_category"));
		useCondition.put("reject_category", cardInfo.get("reject_category"));
		useCondition.put("least_cost", leastCost);
		useCondition.put("object_use_for", cardInfo.get("object_use_for"));
		useCondition.put("can_use_with_other_discount", cardInfo.get("can_use_with_other_discount"));
		return useCondition;
	}
}
