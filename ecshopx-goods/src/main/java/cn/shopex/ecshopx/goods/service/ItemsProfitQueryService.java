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

package cn.shopex.ecshopx.goods.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsMedicine;
import cn.shopex.ecshopx.goods.domain.ItemsProfit;
import cn.shopex.ecshopx.goods.repository.ItemsMedicineRepository;
import cn.shopex.ecshopx.goods.repository.ItemsProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.distributor.DistributorItemsListSkuSpecApplier;
import cn.shopex.ecshopx.goods.service.items.GoodsItemsListFacadeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ItemsProfitQueryService {

	private static final List<String> FLAT_MEDICINE_KEYS = List.of(
			"medicine_type",
			"common_name",
			"dosage",
			"spec",
			"packing_spec",
			"manufacturer",
			"approval_number",
			"unit",
			"medicine_is_prescription",
			"special_common_name",
			"special_spec",
			"medicine_audit_status",
			"medicine_audit_reason",
			"medicine_item_type",
			"use_tip",
			"symptom",
			"max_num");

	private final ItemsRepository itemsRepository;
	private final GoodsItemsListFacadeService goodsItemsListFacadeService;
	private final DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier;
	private final ItemsProfitRepository itemsProfitRepository;
	private final ItemsMedicineRepository itemsMedicineRepository;
	private final ObjectMapper objectMapper;

	public ItemsProfitQueryService(
			ItemsRepository itemsRepository,
			GoodsItemsListFacadeService goodsItemsListFacadeService,
			DistributorItemsListSkuSpecApplier distributorItemsListSkuSpecApplier,
			ItemsProfitRepository itemsProfitRepository,
			ItemsMedicineRepository itemsMedicineRepository,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.goodsItemsListFacadeService = goodsItemsListFacadeService;
		this.distributorItemsListSkuSpecApplier = distributorItemsListSkuSpecApplier;
		this.itemsProfitRepository = itemsProfitRepository;
		this.itemsMedicineRepository = itemsMedicineRepository;
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> getItemsProfit(long companyId, long itemId, String acceptLanguageHeader) {
		Items head = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (head == null) {
			throw new ResourceException("商品获取失败");
		}
		boolean multiSpec = isMultiSpecNospec(head.getNospec());
		Map<String, Object> itemList =
				goodsItemsListFacadeService.buildAdminPlatformSkuListForMemberPrice(companyId, itemId, multiSpec, acceptLanguageHeader);
		List<Map<String, Object>> rows = (List<Map<String, Object>>) itemList.get("list");
		if (rows == null) {
			rows = new ArrayList<>();
			itemList.put("list", rows);
		}
		distributorItemsListSkuSpecApplier.apply(companyId, rows);

		Set<Long> medQueryIds = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			long id = parseItemId(row.get("item_id"));
			if (id > 0) {
				medQueryIds.add(id);
			}
		}
		Map<Long, ItemsMedicine> medMap = itemsMedicineRepository.mapByItemIds(companyId, medQueryIds);
		for (Map<String, Object> row : rows) {
			if (!Integer.valueOf(1).equals(toIntBoxed(row.get("is_medicine")))) {
				removeFlatMedicineKeys(row);
				row.remove("medicine_data");
				continue;
			}
			long skuId = parseItemId(row.get("item_id"));
			ItemsMedicine m = skuId > 0 ? medMap.get(skuId) : null;
			if (m == null) {
				removeFlatMedicineKeys(row);
				row.remove("medicine_data");
				continue;
			}
			LinkedHashMap<String, Object> nested = new LinkedHashMap<>();
			nested.put("item_id", m.getItemId());
			nested.put("company_id", m.getCompanyId());
			nested.put("medicine_type", m.getMedicineType());
			nested.put("common_name", m.getCommonName());
			nested.put("dosage", m.getDosage());
			nested.put("spec", m.getSpec());
			nested.put("packing_spec", m.getPackingSpec());
			nested.put("manufacturer", m.getManufacturer());
			nested.put("approval_number", m.getApprovalNumber());
			nested.put("unit", m.getUnit());
			nested.put("is_prescription", m.getIsPrescription());
			nested.put("special_common_name", m.getSpecialCommonName());
			nested.put("special_spec", m.getSpecialSpec());
			nested.put("audit_status", m.getAuditStatus());
			nested.put("audit_reason", m.getAuditReason());
			nested.put("item_type", m.getItemType());
			nested.put("use_tip", m.getUseTip());
			nested.put("symptom", m.getSymptom());
			nested.put("max_num", m.getMaxNum());
			row.put("medicine_data", nested);
			removeFlatMedicineKeys(row);
		}

		List<Long> profitItemIds = new ArrayList<>();
		Set<Long> seen = new LinkedHashSet<>();
		for (Map<String, Object> row : rows) {
			long skuId = parseItemId(row.get("item_id"));
			if (skuId > 0 && seen.add(skuId)) {
				profitItemIds.add(skuId);
			}
		}
		Map<Long, ItemsProfit> profitByItem = itemsProfitRepository.mapByCompanyAndItemIds(companyId, profitItemIds);
		for (Map<String, Object> row : rows) {
			long skuId = parseItemId(row.get("item_id"));
			ItemsProfit ip = profitByItem.get(skuId);
			if (ip == null) {
				continue;
			}
			row.put("profit_type", parseProfitType(ip.getProfitType()));
			String conf = ip.getProfitConf();
			if (!StringUtils.hasText(conf)) {
				continue;
			}
			JsonNode root;
			try {
				root = objectMapper.readTree(conf.trim());
			} catch (JsonProcessingException e) {
				throw new ResourceException("分润配置数据损坏");
			}
			if (!root.isObject()) {
				throw new ResourceException("分润配置数据损坏");
			}
			putProfitConfNumber(row, root, "profit", "profit_conf_profit");
			putProfitConfNumber(row, root, "popularize_profit", "profit_conf_popularize_profit");
		}
		return itemList;
	}

	private static void removeFlatMedicineKeys(Map<String, Object> row) {
		for (String k : FLAT_MEDICINE_KEYS) {
			row.remove(k);
		}
	}

	private void putProfitConfNumber(Map<String, Object> row, JsonNode root, String jsonKey, String outKey) {
		JsonNode n = root.get(jsonKey);
		if (n == null || n.isNull()) {
			return;
		}
		BigDecimal bd = jsonNodeToBigDecimal(n);
		if (bd == null) {
			throw new ResourceException("分润配置数据损坏");
		}
		row.put(outKey, bd);
	}

	private static BigDecimal jsonNodeToBigDecimal(JsonNode n) {
		if (n.isNumber()) {
			return n.decimalValue();
		}
		if (n.isTextual()) {
			String t = n.asText();
			if (!StringUtils.hasText(t)) {
				return null;
			}
			try {
				return new BigDecimal(t.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	private static int parseProfitType(String profitType) {
		if (profitType == null || !StringUtils.hasText(profitType)) {
			throw new ResourceException("分润类型数据损坏");
		}
		try {
			return Integer.parseInt(profitType.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("分润类型数据损坏");
		}
	}

	private static long parseItemId(Object iid) {
		if (iid instanceof Number n) {
			return n.longValue();
		}
		if (iid != null && StringUtils.hasText(iid.toString())) {
			try {
				return Long.parseLong(iid.toString().trim());
			} catch (NumberFormatException ignored) {
				return 0L;
			}
		}
		return 0L;
	}

	private static Integer toIntBoxed(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.intValue();
		}
		try {
			return Integer.parseInt(o.toString().trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * 与仓储层 nospec 语义一致：{@code false} / {@code "false"} / {@code 0} / {@code "0"} 表示多规格。
	 */
	private static boolean isMultiSpecNospec(Object nospec) {
		if (nospec == null) {
			return false;
		}
		if (nospec instanceof Boolean b) {
			return !b;
		}
		if (nospec instanceof Number n) {
			return n.intValue() == 0;
		}
		String s = nospec.toString().trim();
		return "false".equalsIgnoreCase(s) || "0".equals(s);
	}
}
