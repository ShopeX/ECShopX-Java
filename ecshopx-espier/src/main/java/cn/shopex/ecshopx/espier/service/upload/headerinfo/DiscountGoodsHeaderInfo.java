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

public final class DiscountGoodsHeaderInfo {
	private DiscountGoodsHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("商品编码-精确到规格", new UploadHeaderColumnInfo(32, "商品编码-精确到规格", true));
		map.put("商品名称", new UploadHeaderColumnInfo(255, "商品名称", true));
		map.put("商品可兑换上限", new UploadHeaderColumnInfo(255, "商品可兑换上限为大于0的整数，请注意是整数，小数或浮点数将向上取整", true));
		return Collections.unmodifiableMap(map);
	}
}
