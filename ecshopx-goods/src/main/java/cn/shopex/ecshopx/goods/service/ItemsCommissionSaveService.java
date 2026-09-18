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
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.domain.ItemsCommission;
import cn.shopex.ecshopx.goods.mapper.ItemsCommissionMapper;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ItemsCommissionSaveService {

	private final ItemsCommissionMapper itemsCommissionMapper;
	private final ItemsRepository itemsRepository;
	private final ObjectMapper objectMapper;

	public ItemsCommissionSaveService(ItemsCommissionMapper itemsCommissionMapper,
			ItemsRepository itemsRepository,
			ObjectMapper objectMapper) {
		this.itemsCommissionMapper = itemsCommissionMapper;
		this.itemsRepository = itemsRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveSpuCommissionRatioFromPercentScaled(long companyId, long relIdGoods, BigDecimal scaledCommission) {
		upsertSpu(companyId, relIdGoods, "1", scaledCommission);
	}

	@Transactional(rollbackFor = Exception.class)
	public void saveItemsCommission(long companyId, Map<String, Object> input) {
		long itemId = parseItemId(input.get("item_id"));
		long goodsId = parseGoodsId(input.get("goods_id"));
		String commissionType = input.get("commission_type").toString().trim();
		Object commissionScalar = input.get("commission");
		List<Map<String, Object>> skuList = resolveSkuList(input);

		checkParams(companyId, itemId, commissionScalar, skuList);

		upsertSpu(companyId, goodsId, commissionType, commissionScalar);

		if (skuList.isEmpty()) {
			return;
		}
		for (Map<String, Object> skuRow : skuList) {
			Object comField = skuRow.get("commission");
			long skuItemId = parseSkuItemIdStrict(skuRow);
			if (isSkuDeleteCommission(comField)) {
				deleteSkuIfExists(companyId, skuItemId);
				continue;
			}
			upsertSku(companyId, skuItemId, commissionType, comField);
		}
	}

	private void checkParams(long companyId, long itemId, Object commissionScalar,
			List<Map<String, Object>> skuList) {
		Items row = itemsRepository.getByItemIdAndCompany(itemId, companyId);
		if (row == null) {
			throw new ResourceException("商品获取失败");
		}
		validateSpuCommissionNonNegative(commissionScalar);
		for (Map<String, Object> skuRow : skuList) {
			Object c = skuRow.get("commission");
			if (isSkuCommissionEmptyForNegativeCheck(c)) {
				continue;
			}
			validateSkuCommissionNonNegative(c);
		}
	}

	private static void validateSpuCommissionNonNegative(Object raw) {
		if (raw == null) {
			throw new ResourceException("SPU结算佣金为大于等于0的数字");
		}
		String s = raw.toString().trim();
		BigDecimal bd;
		try {
			bd = new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("SPU结算佣金为大于等于0的数字");
		}
		if (bd.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResourceException("SPU结算佣金为大于等于0的数字");
		}
	}

	private static void validateSkuCommissionNonNegative(Object raw) {
		String s = raw.toString().trim();
		BigDecimal bd;
		try {
			bd = new BigDecimal(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("SKU结算佣金为大于等于0的数字");
		}
		if (bd.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResourceException("SKU结算佣金为大于等于0的数字");
		}
	}

	/** Whether to skip SKU non-negative validation (unset or zero-equivalent values). */
	private static boolean isSkuCommissionEmptyForNegativeCheck(Object o) {
		if (o == null) {
			return true;
		}
		if (o instanceof String str) {
			String t = str.trim();
			if (t.isEmpty()) {
				return true;
			}
			return "0".equals(t);
		}
		if (o instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		if (o instanceof Boolean b) {
			return !b;
		}
		if (o instanceof Map<?, ?> m) {
			return m.isEmpty();
		}
		if (o instanceof List<?> l) {
			return l.isEmpty();
		}
		return false;
	}

	private static boolean isSkuDeleteCommission(Object commission) {
		if (commission == null) {
			return true;
		}
		if (commission instanceof String s) {
			return s.trim().isEmpty();
		}
		if (commission instanceof Number n) {
			return n.doubleValue() == 0.0;
		}
		return false;
	}

	private void upsertSpu(long companyId, long goodsId, String commissionType, Object commissionScalar) {
		String commissionConfJson = writeCommissionConf(commissionScalar);
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId)
				.eq(ItemsCommission::getRelId, goodsId)
				.eq(ItemsCommission::getType, "goods")
				.last("LIMIT 1");
		ItemsCommission existing = itemsCommissionMapper.selectOne(w);
		if (existing != null) {
			existing.setCommissionType(commissionType);
			existing.setCommissionConf(commissionConfJson);
			int rows = itemsCommissionMapper.updateById(existing);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}
		ItemsCommission ins = new ItemsCommission();
		ins.setCompanyId(companyId);
		ins.setRelId(goodsId);
		ins.setType("goods");
		ins.setCommissionType(commissionType);
		ins.setCommissionConf(commissionConfJson);
		itemsCommissionMapper.insert(ins);
	}

	private void upsertSku(long companyId, long skuItemId, String commissionType, Object commissionScalar) {
		String commissionConfJson = writeCommissionConf(commissionScalar);
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId)
				.eq(ItemsCommission::getRelId, skuItemId)
				.eq(ItemsCommission::getType, "item")
				.last("LIMIT 1");
		ItemsCommission existing = itemsCommissionMapper.selectOne(w);
		if (existing != null) {
			existing.setCommissionType(commissionType);
			existing.setCommissionConf(commissionConfJson);
			int rows = itemsCommissionMapper.updateById(existing);
			if (rows == 0) {
				throw new ResourceException("未查询到更新数据");
			}
			return;
		}
		ItemsCommission ins = new ItemsCommission();
		ins.setCompanyId(companyId);
		ins.setRelId(skuItemId);
		ins.setType("item");
		ins.setCommissionType(commissionType);
		ins.setCommissionConf(commissionConfJson);
		itemsCommissionMapper.insert(ins);
	}

	private void deleteSkuIfExists(long companyId, long skuItemId) {
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId)
				.eq(ItemsCommission::getRelId, skuItemId)
				.eq(ItemsCommission::getType, "item")
				.last("LIMIT 1");
		ItemsCommission row = itemsCommissionMapper.selectOne(w);
		if (row != null) {
			itemsCommissionMapper.deleteById(row.getId());
		}
	}

	private String writeCommissionConf(Object commissionScalar) {
		try {
			return objectMapper.writeValueAsString(Map.of("commission", commissionScalar));
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	private List<Map<String, Object>> resolveSkuList(Map<String, Object> input) {
		if (!input.containsKey("sku_commission")) {
			return Collections.emptyList();
		}
		Object sc = input.get("sku_commission");
		if (sc == null) {
			return Collections.emptyList();
		}
		if (sc instanceof String s) {
			if (!StringUtils.hasText(s.trim())) {
				return Collections.emptyList();
			}
			JsonNode node;
			try {
				node = objectMapper.readTree(s);
			} catch (Exception e) {
				throw new BadRequestException("SKU佣金格式错误");
			}
			if (!node.isArray()) {
				throw new BadRequestException("SKU佣金格式错误");
			}
			return jsonArrayToSkuMaps(node);
		}
		if (sc instanceof List<?> list) {
			List<Map<String, Object>> out = new ArrayList<>();
			for (Object el : list) {
				if (!(el instanceof Map<?, ?> m)) {
					throw new BadRequestException("SKU佣金格式错误");
				}
				Map<String, Object> row = objectMapper.convertValue(m, new TypeReference<Map<String, Object>>() {});
				parseSkuItemIdStrict(row);
				out.add(row);
			}
			return out;
		}
		throw new BadRequestException("SKU佣金格式错误");
	}

	private List<Map<String, Object>> jsonArrayToSkuMaps(JsonNode arr) {
		List<Map<String, Object>> out = new ArrayList<>();
		for (JsonNode el : arr) {
			if (!el.isObject()) {
				throw new BadRequestException("SKU佣金格式错误");
			}
			Map<String, Object> row = objectMapper.convertValue(el, new TypeReference<Map<String, Object>>() {});
			parseSkuItemIdStrict(row);
			out.add(row);
		}
		return out;
	}

	private static long parseSkuItemIdStrict(Map<String, Object> row) {
		if (!row.containsKey("item_id") || row.get("item_id") == null) {
			throw new BadRequestException("SKU缺少item_id");
		}
		try {
			return Long.parseLong(row.get("item_id").toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("SKU缺少item_id");
		}
	}

	private static long parseItemId(Object v) {
		if (v == null) {
			throw new BadRequestException("商品不存在");
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("商品不存在");
		}
	}

	private static long parseGoodsId(Object v) {
		if (v == null) {
			throw new BadRequestException("产品不存在");
		}
		try {
			return Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException("产品不存在");
		}
	}
}
