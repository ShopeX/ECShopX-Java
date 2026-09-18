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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.distribution.service.DistributionConfigRedisReadService;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsCategoryProfit;
import cn.shopex.ecshopx.goods.domain.ItemsProfit;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsProfitSaveService {

	private static final int STATUS_PROFIT_DEFAULT = 0;
	private static final int STATUS_PROFIT_SCALE = 1;
	private static final int STATUS_PROFIT_FEE = 2;
	private static final int PROFIT_ITEM_PROFIT_SCALE = 2;
	private static final int PROFIT_ITEM_PROFIT_FEE = 3;

	private static final String KEY_PROFIT = "profit_conf_profit";
	private static final String KEY_POPULARIZE = "profit_conf_popularize_profit";

	private final ObjectMapper objectMapper;
	private final ItemsProfitRepository itemsProfitRepository;
	private final ItemsRepository itemsRepository;
	private final ItemsCategoryProfitRepository itemsCategoryProfitRepository;
	private final DistributionConfigRedisReadService distributionConfigRedisReadService;

	public ItemsProfitSaveService(ObjectMapper objectMapper, ItemsProfitRepository itemsProfitRepository,
			ItemsRepository itemsRepository, ItemsCategoryProfitRepository itemsCategoryProfitRepository,
			DistributionConfigRedisReadService distributionConfigRedisReadService) {
		this.objectMapper = objectMapper;
		this.itemsProfitRepository = itemsProfitRepository;
		this.itemsRepository = itemsRepository;
		this.itemsCategoryProfitRepository = itemsCategoryProfitRepository;
		this.distributionConfigRedisReadService = distributionConfigRedisReadService;
	}

	@Transactional(rollbackFor = Exception.class)
	public void save(long companyId, Map<String, Object> body) {
		List<JsonNode> entries = parseProfitConfEntries(body);
		List<Long> itemIds = new ArrayList<>(entries.size());
		for (JsonNode val : entries) {
			itemIds.add(requireItemId(val));
		}
		itemsProfitRepository.deleteByCompanyAndItemIds(companyId, itemIds);

		for (JsonNode val : entries) {
			long itemId = requireItemId(val);
			int profitType = intVal(val.get("profit_type"));
			if (profitType != STATUS_PROFIT_DEFAULT && profitType != STATUS_PROFIT_SCALE && profitType != STATUS_PROFIT_FEE) {
				throw new BadRequestException("分润类型错误");
			}
			if (profitType != STATUS_PROFIT_DEFAULT) {
				saveCustomProfit(companyId, itemId, profitType, val);
			} else {
				applyDefaultProfitBranch(companyId, itemId);
			}
		}
	}

	private List<JsonNode> parseProfitConfEntries(Map<String, Object> body) {
		Object raw = body.get("profit_conf");
		if (!(raw instanceof String str)) {
			throw new BadRequestException("profit_conf 必须为 JSON 字符串");
		}
		String trimmed = str.trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("导购分润配置错误");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(trimmed);
		} catch (JsonProcessingException e) {
			throw new ResourceException("导购分润配置错误");
		}
		List<JsonNode> entries = new ArrayList<>();
		if (root.isArray()) {
			for (JsonNode el : root) {
				entries.add(el);
			}
		} else if (root.isObject()) {
			for (Iterator<JsonNode> it = root.elements(); it.hasNext(); ) {
				entries.add(it.next());
			}
		} else {
			throw new ResourceException("导购分润配置错误");
		}
		if (entries.isEmpty()) {
			throw new ResourceException("导购分润配置错误");
		}
		for (JsonNode node : entries) {
			if (node == null || !node.isObject()) {
				throw new ResourceException("导购分润配置错误");
			}
		}
		return entries;
	}

	private void saveCustomProfit(long companyId, long itemId, int profitType, JsonNode val) {
		BigDecimal profitBd = requireBigDecimalField(val, KEY_PROFIT);
		BigDecimal popularizeBd = requireBigDecimalField(val, KEY_POPULARIZE);
		Map<String, BigDecimal> persistMap = Map.of("profit", profitBd, "popularize_profit", popularizeBd);
		String profitConfJson;
		try {
			profitConfJson = objectMapper.writeValueAsString(persistMap);
		} catch (JsonProcessingException e) {
			throw new ResourceException("导购分润配置错误");
		}
		ItemsProfit row = new ItemsProfit();
		row.setItemId(itemId);
		row.setCompanyId(companyId);
		row.setProfitType(String.valueOf(profitType));
		row.setProfitConf(profitConfJson);
		int rows = itemsProfitRepository.insertRow(row);
		if (rows != 1) {
			throw new ResourceException("保存商品导购分润配置失败");
		}

		int itemsTableProfitType = profitType == STATUS_PROFIT_SCALE ? PROFIT_ITEM_PROFIT_SCALE : PROFIT_ITEM_PROFIT_FEE;
		int profitFee;
		if (profitType == STATUS_PROFIT_SCALE) {
			int price = requirePrice(val);
			BigDecimal scale = popularizeBd.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
			profitFee = scale.multiply(BigDecimal.valueOf(price)).setScale(0, RoundingMode.DOWN).intValue();
		} else {
			profitFee = popularizeBd.setScale(0, RoundingMode.DOWN).intValue();
		}
		itemsRepository.updateProfitFeeByItemId(itemId, itemsTableProfitType, profitFee);
	}

	private void applyDefaultProfitBranch(long companyId, long itemId) {
		Items itemInfo = itemsRepository.findByItemId(itemId);
		Optional<Long> categoryIdOpt = resolveCategoryId(itemInfo);
		if (categoryIdOpt.isPresent()) {
			Optional<ItemsCategoryProfit> cat = itemsCategoryProfitRepository.findByCategoryIdOnly(categoryIdOpt.get());
			if (cat.isPresent()) {
				Optional<BigDecimal> popOpt = tryPopularizeFromCategoryConf(cat.get().getProfitConf());
				if (popOpt.isPresent()) {
					BigDecimal scale = popOpt.get().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
					itemsRepository.updateProfitByItemId(itemId, 1, scale.toPlainString());
					return;
				}
			}
		}
		BigDecimal popularizePercent = distributionConfigRedisReadService.getPopularizeSellerPercent(companyId);
		BigDecimal scale = popularizePercent.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
		itemsRepository.updateProfitByItemId(itemId, 0, scale.toPlainString());
	}

	private static Optional<Long> resolveCategoryId(Items itemInfo) {
		if (itemInfo == null) {
			return Optional.empty();
		}
		String cat = itemInfo.getItemCategory();
		if (cat == null) {
			return Optional.empty();
		}
		String t = cat.trim();
		if (t.isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(Long.parseLong(t));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	private Optional<BigDecimal> tryPopularizeFromCategoryConf(String profitConfJson) {
		if (profitConfJson == null || profitConfJson.isBlank()) {
			return Optional.empty();
		}
		try {
			JsonNode root = objectMapper.readTree(profitConfJson);
			JsonNode p = root.get("popularize_profit");
			if (p == null || p.isNull()) {
				return Optional.empty();
			}
			return Optional.of(parseBigDecimalFlexible(p));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	private static long requireItemId(JsonNode obj) {
		JsonNode n = obj.get("item_id");
		if (n == null || n.isNull()) {
			throw new ResourceException("导购分润配置错误");
		}
		try {
			if (n.isNumber()) {
				return n.longValue();
			}
			if (n.isTextual()) {
				String t = n.asText().trim();
				if (t.isEmpty()) {
					throw new ResourceException("导购分润配置错误");
				}
				return Long.parseLong(t);
			}
		} catch (NumberFormatException e) {
			throw new ResourceException("导购分润配置错误");
		}
		throw new ResourceException("导购分润配置错误");
	}

	private static int intVal(JsonNode n) {
		if (n == null || n.isNull()) {
			return 0;
		}
		if (n.isInt() || n.isLong()) {
			return n.intValue();
		}
		if (n.isNumber()) {
			return n.intValue();
		}
		if (n.isTextual()) {
			String t = n.asText().trim();
			if (t.isEmpty()) {
				return 0;
			}
			try {
				return (int) Double.parseDouble(t);
			} catch (NumberFormatException e) {
				return Integer.MIN_VALUE;
			}
		}
		return Integer.MIN_VALUE;
	}

	private static int requirePrice(JsonNode obj) {
		JsonNode n = obj.get("price");
		if (n == null || n.isNull()) {
			throw new ResourceException("导购分润配置错误");
		}
		try {
			if (n.isInt() || n.isLong()) {
				return n.intValue();
			}
			if (n.isNumber()) {
				return n.intValue();
			}
			if (n.isTextual()) {
				String t = n.asText().trim();
				if (t.isEmpty()) {
					throw new ResourceException("导购分润配置错误");
				}
				return Integer.parseInt(t);
			}
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ResourceException("导购分润配置错误");
		}
		throw new ResourceException("导购分润配置错误");
	}

	private static BigDecimal requireBigDecimalField(JsonNode obj, String key) {
		JsonNode n = obj.get(key);
		if (n == null || n.isNull()) {
			throw new ResourceException("导购分润配置错误");
		}
		try {
			return parseBigDecimalFlexible(n);
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ResourceException("导购分润配置错误");
		}
	}

	private static BigDecimal parseBigDecimalFlexible(JsonNode n) {
		if (n.isNumber()) {
			return n.decimalValue();
		}
		if (n.isTextual()) {
			String t = n.asText().trim();
			if (t.isEmpty()) {
				throw new NumberFormatException("empty");
			}
			return new BigDecimal(t);
		}
		throw new NumberFormatException("unsupported node");
	}

}
