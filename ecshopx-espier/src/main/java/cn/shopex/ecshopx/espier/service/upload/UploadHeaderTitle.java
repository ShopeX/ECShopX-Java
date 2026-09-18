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

package cn.shopex.ecshopx.espier.service.upload;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Column labels and required labels for an upload template (display label → row field key).
 */
public record UploadHeaderTitle(
		Map<String, String> all, Map<String, String> isNeed, Map<String, UploadHeaderColumnInfo> headerInfo) {
	public UploadHeaderTitle {
		all = copyOrdered(all);
		isNeed = copyOrdered(isNeed);
		headerInfo = headerInfo == null ? Map.of() : copyOrdered(headerInfo);
	}

	public UploadHeaderTitle(Map<String, String> all, Map<String, String> isNeed) {
		this(all, isNeed, Map.of());
	}

	private static <K, V> Map<K, V> copyOrdered(Map<K, V> source) {
		return Collections.unmodifiableMap(new LinkedHashMap<>(source));
	}
}
