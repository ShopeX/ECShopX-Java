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

package cn.shopex.ecshopx.orders.service.companyrelshansong;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 闪送商户经营类目编码与中文名对照（静态字典）。 */
public final class ShansongShopBusinessList {

	private static final Map<String, String> MAP = new LinkedHashMap<>();

	static {
		MAP.put("1", "文件");
		MAP.put("3", "数码");
		MAP.put("5", "蛋糕");
		MAP.put("6", "餐饮");
		MAP.put("7", "鲜花");
		MAP.put("9", "汽配");
		MAP.put("10", "其他");
		MAP.put("12", "母婴");
		MAP.put("13", "医药健康");
		MAP.put("15", "商超");
		MAP.put("16", "水果");
	}

	private ShansongShopBusinessList() {}

	public static Map<String, String> asMap() {
		return Collections.unmodifiableMap(MAP);
	}
}
