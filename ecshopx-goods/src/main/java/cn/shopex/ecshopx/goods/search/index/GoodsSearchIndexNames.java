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

package cn.shopex.ecshopx.goods.search.index;

import cn.shopex.ecshopx.goods.search.SearchEsProperties;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 商品搜索索引物理名与 alias 命名。 */
public final class GoodsSearchIndexNames {

	public static final String DEFAULT_ALIAS = "goods_store_item";

	private static final DateTimeFormatter PHYSICAL_SUFFIX =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());

	private GoodsSearchIndexNames() {}

	/** 蓝绿重建用物理索引名，形如 {@code goods_store_item_yyyyMMddHHmmss}。 */
	public static String physicalIndex(long epochMillis) {
		return DEFAULT_ALIAS + "_" + PHYSICAL_SUFFIX.format(Instant.ofEpochMilli(epochMillis));
	}

	/** 读写 alias，取自配置或默认 {@link #DEFAULT_ALIAS}。 */
	public static String alias(SearchEsProperties properties) {
		if (properties == null || properties.getAliasName() == null || properties.getAliasName().isBlank()) {
			return DEFAULT_ALIAS;
		}
		return properties.getAliasName();
	}

	public static String alias() {
		return DEFAULT_ALIAS;
	}
}
