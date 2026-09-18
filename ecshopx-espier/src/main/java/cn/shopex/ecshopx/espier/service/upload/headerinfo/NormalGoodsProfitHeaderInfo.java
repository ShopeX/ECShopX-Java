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

public final class NormalGoodsProfitHeaderInfo {
	private NormalGoodsProfitHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("商品编码", new UploadHeaderColumnInfo(32, "", true));
		map.put("分润类型", new UploadHeaderColumnInfo(255, "分润类型:0,1或2, 0默认分润 1固定比例分润 2固定金额分润", true));
		map.put("拉新分润", new UploadHeaderColumnInfo(255, "1:按照比例分润 1-100, 2:按照固定金额分润(元)，最多两位小数", true));
		map.put("推广分润", new UploadHeaderColumnInfo(255, "1:按照比例分润 1-100, 2:按照固定金额分润(元)，最多两位小数", true));
		return Collections.unmodifiableMap(map);
	}
}
