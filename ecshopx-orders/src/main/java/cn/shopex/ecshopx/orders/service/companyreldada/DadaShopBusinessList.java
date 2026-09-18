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

package cn.shopex.ecshopx.orders.service.companyreldada;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 达达商户经营类目编码与中文名对照（静态字典）。 */
public final class DadaShopBusinessList {

	private static final Map<String, String> MAP = new LinkedHashMap<>();

	static {
		MAP.put("1", "食品小吃");
		MAP.put("2", "饮料");
		MAP.put("3", "鲜花绿植");
		MAP.put("8", "文印票务");
		MAP.put("9", "便利店");
		MAP.put("13", "水果生鲜");
		MAP.put("19", "同城电商");
		MAP.put("20", "医药");
		MAP.put("21", "蛋糕");
		MAP.put("24", "酒品");
		MAP.put("25", "小商品市场");
		MAP.put("26", "服装");
		MAP.put("27", "汽修零配");
		MAP.put("28", "数码家电");
		MAP.put("29", "小龙虾");
		MAP.put("50", "个人");
		MAP.put("51", "火锅");
		MAP.put("53", "个护美妆");
		MAP.put("55", "母婴");
		MAP.put("57", "家居家纺");
		MAP.put("59", "手机");
		MAP.put("61", "家装");
		MAP.put("5", "其他");
	}

	private DadaShopBusinessList() {}

	public static Map<String, String> asMap() {
		return Collections.unmodifiableMap(MAP);
	}
}
