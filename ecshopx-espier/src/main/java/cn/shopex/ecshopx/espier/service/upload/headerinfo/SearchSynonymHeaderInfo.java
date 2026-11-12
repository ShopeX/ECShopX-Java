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

package cn.shopex.ecshopx.espier.service.upload.headerinfo;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderColumnInfo;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SearchSynonymHeaderInfo {
	private SearchSynonymHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("同义词组标识", new UploadHeaderColumnInfo(64, "", true));
		for (int i = 1; i <= 20; i++) {
			map.put("词语" + i, new UploadHeaderColumnInfo(64, "空单元格忽略", false));
		}
		return Collections.unmodifiableMap(map);
	}
}
