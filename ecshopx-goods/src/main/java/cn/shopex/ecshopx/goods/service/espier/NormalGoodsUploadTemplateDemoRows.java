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

package cn.shopex.ecshopx.goods.service.espier;

import cn.shopex.ecshopx.espier.service.upload.UploadHeaderTitle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对齐 PHP {@code NormalGoodsUploadService::getDemoData}。
 */
public final class NormalGoodsUploadTemplateDemoRows {

	private NormalGoodsUploadTemplateDemoRows() {}

	public static List<List<Object>> forHeader(UploadHeaderTitle title) {
		List<List<Object>> out = new ArrayList<>();
		for (Map<String, String> raw : PHP_DEMO_ROWS) {
			out.add(toOrderedRow(title, raw));
		}
		return out;
	}

	private static List<Object> toOrderedRow(UploadHeaderTitle title, Map<String, String> raw) {
		List<Object> cells = new ArrayList<>(title.all().size());
		for (String fieldKey : title.all().values()) {
			cells.add(raw.getOrDefault(fieldKey, ""));
		}
		return cells;
	}

	private static Map<String, String> row(Map<String, String> fields) {
		return fields;
	}

	private static final List<Map<String, String>> PHP_DEMO_ROWS =
			List.of(
					row(demoRow(
							"HP->上衣->HP",
							"上衣外套",
							"SPU61401E3179BB6",
							"SKU61401E3179BB6",
							"1",
							"0.01",
							"99",
							"",
							"不可销售",
							"")),
					row(specDemo(
							"KC26011700000029",
							"100",
							"尺码:XXL|颜色:黄色1")),
					row(specDemo(
							"KC26011700000028",
							"101",
							"尺码:XL|颜色:黄色1")),
					row(specDemo(
							"KC26011700000027",
							"102",
							"尺码:L|颜色:黄色1")),
					row(specDemo(
							"KC26011700000026",
							"104",
							"尺码:M|颜色:黄色1")),
					row(specDemo(
							"KC26011700000025",
							"104",
							"尺码:S1|颜色:黄色1")),
					row(specDemo(
							"KC26011700000024",
							"105",
							"尺码:S|颜色:黄色1")),
					Map.of(),
					row(instructionRow()));

	private static Map<String, String> demoRow(
			String mainCategory,
			String itemName,
			String goodsBn,
			String itemBn,
			String brief,
			String price,
			String store,
			String deliveryTime,
			String approveStatus,
			String itemSpec) {
		LinkedHashMap<String, String> m = specBase();
		m.put("item_main_category", mainCategory);
		m.put("item_name", itemName);
		m.put("goods_bn", goodsBn);
		m.put("item_bn", itemBn);
		m.put("brief", brief);
		m.put("price", price);
		m.put("store", store);
		m.put("delivery_time", deliveryTime);
		m.put("approve_status", approveStatus);
		m.put("item_spec", itemSpec);
		m.put("is_profit", "-1");
		return m;
	}

	private static Map<String, String> specDemo(String itemBn, String store, String itemSpec) {
		LinkedHashMap<String, String> m = specBase();
		m.put("item_bn", itemBn);
		m.put("store", store);
		m.put("item_spec", itemSpec);
		m.put("delivery_time", "0");
		m.put("is_profit", "0");
		m.put("approve_status", "前台可销售");
		return m;
	}

	private static LinkedHashMap<String, String> specBase() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("item_main_category", "运动户外->品类->软壳");
		m.put("item_name", "林间漫步轻便冲锋衣外套");
		m.put("goods_bn", "PC26011700000025");
		m.put("brief", "");
		m.put("price", "2300");
		m.put("market_price", "0");
		m.put("cost_price", "0");
		m.put("start_num", "");
		m.put("member_price", "");
		m.put("pics", "");
		m.put("intro", "");
		m.put("item_spec_pics", "");
		m.put("videos", "");
		m.put("goods_brand", "ECshopX");
		m.put("templates_id", "全国包邮");
		m.put(
				"item_category",
				"运动户外->系列->城市休闲|运动户外->系列->徒步&穿越|运动户外->系列->野外跑步|运动户外->系列->登山&攀岩|运动户外->系列->商务系列|运动户外->系列->单双板滑雪");
		m.put("weight", "0");
		m.put("barcode", "");
		m.put("item_unit", "");
		m.put("item_params", "");
		m.put("profit_type", "0");
		m.put("profit", "");
		m.put("popularize_profit", "");
		return m;
	}

	private static Map<String, String> instructionRow() {
		LinkedHashMap<String, String> m = new LinkedHashMap<>();
		m.put("item_main_category", "");
		m.put("item_name", "");
		m.put("item_bn", "");
		m.put(
				"brief",
				"2为一条单规格商品，3-8为一条多规格商品  此文件数据仅作为参考，使用该模板请删除本句，重新修改数据。");
		return m;
	}
}
