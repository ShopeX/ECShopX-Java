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

package cn.shopex.ecshopx.orders.service.espier;

/** Strips Excel/export noise from order id cells before numeric parse. */
final class NormalOrdersUploadImportOrderIdSanitizer {

	private NormalOrdersUploadImportOrderIdSanitizer() {}

	static String sanitize(String s) {
		if (s == null || s.isEmpty()) {
			return "";
		}
		String t = s.replace("\uFEFF", "");
		if (t.startsWith("\u00EF\u00BB\u00BF")) {
			t = t.substring(3);
		}
		int start = 0;
		int end = t.length();
		while (start < end && isOrderIdTrimChar(t.charAt(start))) {
			start++;
		}
		while (end > start && isOrderIdTrimChar(t.charAt(end - 1))) {
			end--;
		}
		return t.substring(start, end);
	}

	private static boolean isOrderIdTrimChar(char c) {
		return c == '"' || c == '\'' || c == ' ' || c == '\t' || c == '\r' || c == '\n';
	}
}
