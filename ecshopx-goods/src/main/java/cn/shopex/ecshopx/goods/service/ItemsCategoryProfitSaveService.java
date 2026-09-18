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
import cn.shopex.ecshopx.goods.domain.ItemsCategoryProfit;
import cn.shopex.ecshopx.goods.repository.ItemsCategoryProfitRepository;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemsCategoryProfitSaveService {

	private static final String KEY_PROFIT = "profit_conf_profit";
	private static final String KEY_POPULARIZE = "profit_conf_popularize_profit";

	private final ObjectMapper objectMapper;
	private final ItemsCategoryProfitRepository itemsCategoryProfitRepository;
	private final ItemsRepository itemsRepository;

	public ItemsCategoryProfitSaveService(ObjectMapper objectMapper,
			ItemsCategoryProfitRepository itemsCategoryProfitRepository,
			ItemsRepository itemsRepository) {
		this.objectMapper = objectMapper;
		this.itemsCategoryProfitRepository = itemsCategoryProfitRepository;
		this.itemsRepository = itemsRepository;
	}

	@Transactional(rollbackFor = Exception.class)
	public void save(long companyId, Map<String, Object> body) {
		Object profitConfRaw = body.get("profit_conf");
		if (!(profitConfRaw instanceof String raw)) {
			throw new BadRequestException("profit_conf 必须为 JSON 字符串");
		}
		String trimmed = raw.trim();
		if (trimmed.isEmpty()) {
			throw new ResourceException("导购分润配置错误");
		}
		JsonNode root;
		try {
			root = objectMapper.readTree(trimmed);
		} catch (JsonProcessingException e) {
			throw new ResourceException("导购分润配置错误");
		}
		if (!root.isObject()) {
			throw new ResourceException("导购分润配置错误");
		}
		BigDecimal profitBd = requireBigDecimal(root, KEY_PROFIT);
		BigDecimal popularizeBd = requireBigDecimal(root, KEY_POPULARIZE);

		long categoryId = readCategoryId(body);
		if (categoryId <= 0) {
			throw new ResourceException("主类目id不存在");
		}

		itemsCategoryProfitRepository.deleteByCompanyAndCategory(companyId, categoryId);

		Map<String, BigDecimal> persistMap = Map.of("profit", profitBd, "popularize_profit", popularizeBd);
		String profitConfJson;
		try {
			profitConfJson = objectMapper.writeValueAsString(persistMap);
		} catch (JsonProcessingException e) {
			throw new ResourceException("导购分润配置错误");
		}

		ItemsCategoryProfit row = new ItemsCategoryProfit();
		row.setCategoryId(categoryId);
		row.setCompanyId(companyId);
		row.setProfitType("1");
		row.setProfitConf(profitConfJson);
		int insertRows = itemsCategoryProfitRepository.insertRow(row);

		BigDecimal scale = popularizeBd.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
		String scalePlainString = scale.toPlainString();
		itemsRepository.updateProfitByCompanyAndItemCategory(companyId, categoryId, 1, scalePlainString);

		if (insertRows != 1) {
			throw new ResourceException("保存商品分类导购分润配置失败");
		}
	}

	private static long readCategoryId(Map<String, Object> body) {
		Object v = body.get("category_id");
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static BigDecimal requireBigDecimal(JsonNode root, String key) {
		JsonNode n = root.get(key);
		if (n == null || n.isNull()) {
			throw new ResourceException("导购分润配置错误");
		}
		try {
			if (n.isNumber()) {
				return n.decimalValue();
			}
			if (n.isTextual()) {
				String t = n.asText().trim();
				if (t.isEmpty()) {
					throw new ResourceException("导购分润配置错误");
				}
				return new BigDecimal(t);
			}
		} catch (ResourceException e) {
			throw e;
		} catch (NumberFormatException | ArithmeticException e) {
			throw new ResourceException("导购分润配置错误");
		}
		throw new ResourceException("导购分润配置错误");
	}
}
