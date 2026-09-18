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

public final class UpdateDistributionItemHeaderInfo {
	private UpdateDistributionItemHeaderInfo() {}

	public static Map<String, UploadHeaderColumnInfo> map() {
		Map<String, UploadHeaderColumnInfo> map = new LinkedHashMap<>();
		map.put("店铺ID", new UploadHeaderColumnInfo(32, "店铺ID和店铺号需选一项进行填写", false));
		map.put("店铺号", new UploadHeaderColumnInfo(32, "店铺ID和店铺号需选一项进行填写", false));
		map.put("商品SPU", new UploadHeaderColumnInfo(32, "商品SPU货号，32位", true));
		map.put("商品货号", new UploadHeaderColumnInfo(32, "商品SKU货号，32位", true));
		map.put("是否上架", new UploadHeaderColumnInfo(32, "是否上架: 0否, 1是", false));
		map.put("是否总部发货", new UploadHeaderColumnInfo(32, "是否总部发货: 0否, 1是\n（针对多规格商品，是否总部发货这列数据，只需填写第一行sku。SPU 内其余 SKU 行该字段留空，不需要重复填写。\n若SPU下的sku都填写，取该 SPU 内最后一行填写的 SKU 的值，覆盖生效到本 SPU 下全部 SKU，前面填写的值则无效）", false));
		map.put("商品库存", new UploadHeaderColumnInfo(32, "库存为0-999999的整数 （若总部发货, 商品库存将不会更新）", false));
		map.put("商品价格", new UploadHeaderColumnInfo(32, "价格需大于0元 （若总部发货, 商品库存将不会更新）", false));
		return Collections.unmodifiableMap(map);
	}
}
