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

package cn.shopex.ecshopx.goods.service.cart.wxapp;

import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import cn.shopex.ecshopx.pointsmall.mapper.PointsmallItemsMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PointsmallCartSkuLoadService {

	private final PointsmallItemsMapper pointsmallItemsMapper;
	private final ObjectMapper objectMapper;

	public PointsmallCartSkuLoadService(PointsmallItemsMapper pointsmallItemsMapper, ObjectMapper objectMapper) {
		this.pointsmallItemsMapper = pointsmallItemsMapper;
		this.objectMapper = objectMapper;
	}

	public PointsmallItems requireSellableSku(long companyId, long itemId) {
		PointsmallItems item = pointsmallItemsMapper.selectById(itemId);
		if (item == null || item.getCompanyId() == null || item.getCompanyId() != companyId) {
			return null;
		}
		String approve = item.getApproveStatus() != null ? item.getApproveStatus() : "";
		if (!"onsale".equals(approve) && !"offline_sale".equals(approve)) {
			return null;
		}
		return item;
	}

	public Map<String, Object> querySkuPack(long companyId, List<Long> itemIds) {
		Map<String, Object> empty = new LinkedHashMap<>();
		empty.put("total_count", 0);
		empty.put("list", List.of());
		if (itemIds == null || itemIds.isEmpty()) {
			return empty;
		}
		LambdaQueryWrapper<PointsmallItems> w = new LambdaQueryWrapper<>();
		w.eq(PointsmallItems::getCompanyId, companyId).in(PointsmallItems::getItemId, itemIds);
		List<PointsmallItems> entities = pointsmallItemsMapper.selectList(w);
		List<Map<String, Object>> list = new ArrayList<>();
		for (PointsmallItems e : entities) {
			list.add(entityToSkuRow(e));
		}
		Map<String, Object> out = new LinkedHashMap<>();
		out.put("total_count", list.size());
		out.put("list", list);
		return out;
	}

	public String firstPicUrl(PointsmallItems item) {
		Object decoded = decodePics(item.getPics());
		if (decoded instanceof List<?> list && !list.isEmpty()) {
			Object first = list.get(0);
			return first != null ? String.valueOf(first) : "";
		}
		if (decoded instanceof String s && StringUtils.hasText(s)) {
			return firstPicFromString(s);
		}
		return "";
	}

	private Map<String, Object> entityToSkuRow(PointsmallItems e) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("item_id", e.getItemId());
		m.put("company_id", e.getCompanyId());
		m.put("item_name", nz(e.getItemName()));
		m.put("pics", decodePics(e.getPics()));
		m.put("price", e.getPrice() != null ? e.getPrice() : 0);
		m.put("point", e.getPoint() != null ? e.getPoint() : 0);
		m.put("store", e.getStore() != null ? e.getStore() : 0);
		m.put("market_price", e.getMarketPrice() != null ? e.getMarketPrice() : 0);
		m.put("approve_status", nz(e.getApproveStatus()));
		m.put("brief", nz(e.getBrief()));
		m.put("goods_id", e.getGoodsId() != null ? e.getGoodsId() : e.getItemId());
		m.put("item_category", nz(e.getItemCategory()));
		m.put("item_bn", nz(e.getItemBn()));
		m.put("templates_id", e.getTemplatesId() != null ? e.getTemplatesId() : 0);
		m.put("weight", e.getWeight() != null ? e.getWeight() : 0.0);
		m.put("default_item_id", e.getDefaultItemId() != null ? e.getDefaultItemId() : e.getItemId());
		m.put("type", e.getType() != null ? e.getType() : 0);
		m.put("crossborder_tax_rate", nz(e.getCrossborderTaxRate()));
		m.put("taxstrategy_id", "0");
		m.put("taxation_num", "0");
		m.put("origincountry_id", e.getOrigincountryId() != null ? e.getOrigincountryId() : 0);
		m.put("is_medicine", 0);
		m.put("start_num", 0);
		m.put("item_type", nz(e.getItemType()));
		m.put("special_type", nz(e.getSpecialType()));
		m.put("item_spec_desc", "");
		return m;
	}

	private Object decodePics(String raw) {
		if (!StringUtils.hasText(raw)) {
			return "";
		}
		String s = raw.trim();
		if (s.length() >= 2 && (s.charAt(0) == '[' || s.charAt(0) == '{')) {
			try {
				JsonNode n = objectMapper.readTree(s);
				if (n.isArray() || n.isObject()) {
					return objectMapper.convertValue(n, Object.class);
				}
			} catch (Exception ignored) {
			}
		}
		return s;
	}

	private static String firstPicFromString(String pics) {
		int comma = pics.indexOf(',');
		return comma > 0 ? pics.substring(0, comma).trim() : pics.trim();
	}

	private static String nz(String v) {
		return v != null ? v : "";
	}
}
