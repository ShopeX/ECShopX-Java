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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.goods.service.ItemsProfitSaveService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class NormalGoodsProfitImportRowService {

	private static final int STATUS_PROFIT_DEFAULT = 0;
	private static final int STATUS_PROFIT_SCALE = 1;
	private static final int STATUS_PROFIT_FEE = 2;

	private final ItemsRepository itemsRepository;
	private final ItemsProfitSaveService itemsProfitSaveService;
	private final ObjectMapper objectMapper;

	public NormalGoodsProfitImportRowService(
			ItemsRepository itemsRepository,
			ItemsProfitSaveService itemsProfitSaveService,
			ObjectMapper objectMapper) {
		this.itemsRepository = itemsRepository;
		this.itemsProfitSaveService = itemsProfitSaveService;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void applyRow(long companyId, Map<String, Object> row) {
		Map<String, String> r = trimRow(row);
		String itemBn = r.get("item_bn");
		String profitTypeRaw = r.get("profit_type");
		String profitCell = r.get("profit");
		String popularizeCell = r.get("popularize_profit");
		if (!StringUtils.hasText(itemBn)) {
			throw new BadRequestException("缺少必填字段: item_bn");
		}
		if (!StringUtils.hasText(profitTypeRaw)) {
			throw new BadRequestException("缺少必填字段: profit_type");
		}
		if (!StringUtils.hasText(profitCell)) {
			throw new BadRequestException("缺少必填字段: profit");
		}
		if (!StringUtils.hasText(popularizeCell)) {
			throw new BadRequestException("缺少必填字段: popularize_profit");
		}
		int profitType = parseProfitTypeCell(profitTypeRaw);
		Items item = itemsRepository.findByItemBnAndCompany(itemBn, companyId);
		if (item == null || item.getItemId() == null || item.getItemId() <= 0L) {
			throw new ResourceException("未查询到对应商品");
		}
		long itemId = item.getItemId();
		int priceFen = item.getPrice() != null ? item.getPrice() : 0;

		ObjectNode entry = objectMapper.createObjectNode();
		entry.put("item_id", itemId);
		entry.put("profit_type", profitType);
		if (profitType == STATUS_PROFIT_SCALE) {
			entry.put("profit_conf_profit", parseYuanToPlainDecimalString(profitCell));
			entry.put("profit_conf_popularize_profit", parseYuanToPlainDecimalString(popularizeCell));
			entry.put("price", priceFen);
		} else if (profitType == STATUS_PROFIT_FEE) {
			entry.put("profit_conf_profit", yuanToFenBigDecimalString(profitCell));
			entry.put("profit_conf_popularize_profit", yuanToFenBigDecimalString(popularizeCell));
		}

		ArrayNode arr = objectMapper.createArrayNode();
		arr.add(entry);
		String profitConfJson;
		try {
			profitConfJson = objectMapper.writeValueAsString(arr);
		} catch (Exception e) {
			throw new ResourceException("导购分润配置错误");
		}
		Map<String, Object> body = new HashMap<>();
		body.put("profit_conf", profitConfJson);
		itemsProfitSaveService.save(companyId, body);
	}

	private static int parseProfitTypeCell(String raw) {
		String t = raw.trim();
		try {
			int n = new BigDecimal(t).intValue();
			if (n == STATUS_PROFIT_DEFAULT || n == STATUS_PROFIT_SCALE || n == STATUS_PROFIT_FEE) {
				return n;
			}
		} catch (NumberFormatException ignored) {
		}
		throw new BadRequestException("分润类型错误");
	}

	private static String parseYuanToPlainDecimalString(String raw) {
		try {
			return new BigDecimal(raw.trim()).stripTrailingZeros().toPlainString();
		} catch (NumberFormatException e) {
			throw new BadRequestException("分润金额格式错误");
		}
	}

	private static String yuanToFenBigDecimalString(String raw) {
		try {
			BigDecimal fen = new BigDecimal(raw.trim()).multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
			return fen.toPlainString();
		} catch (NumberFormatException e) {
			throw new BadRequestException("分润金额格式错误");
		}
	}

	private static Map<String, String> trimRow(Map<String, Object> row) {
		Map<String, String> out = new LinkedHashMap<>();
		for (Map.Entry<String, Object> e : row.entrySet()) {
			Object v = e.getValue();
			out.put(e.getKey(), v == null ? "" : String.valueOf(v).trim());
		}
		return out;
	}
}
