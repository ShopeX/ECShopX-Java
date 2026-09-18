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

package cn.shopex.ecshopx.goods.repository;

import cn.shopex.ecshopx.goods.domain.ItemsCommission;
import cn.shopex.ecshopx.goods.mapper.ItemsCommissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class ItemsCommissionQueryRepository {

	private static final ObjectMapper OM = new ObjectMapper();

	private final ItemsCommissionMapper mapper;

	public ItemsCommissionQueryRepository(ItemsCommissionMapper mapper) {
		this.mapper = mapper;
	}

	public Optional<ItemsCommission> findByCompanyRelIdAndType(long companyId, long relId, String type) {
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId)
				.eq(ItemsCommission::getRelId, relId)
				.eq(ItemsCommission::getType, type)
				.last("LIMIT 1");
		return Optional.ofNullable(mapper.selectOne(w));
	}

	public List<ItemsCommission> listByCompanyRelIdsAndType(long companyId, Collection<Long> relIds, String type) {
		if (relIds == null || relIds.isEmpty()) {
			return List.of();
		}
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId)
				.eq(ItemsCommission::getType, type)
				.in(ItemsCommission::getRelId, relIds);
		return mapper.selectList(w);
	}

	/**
	 * item_id → commission_ratio 展示值（commission_conf 原样字符串，与现网比例配置一致）。
	 */
	public Map<Long, String> mapCommissionRatioByItemIds(long companyId, Collection<Long> itemIds) {
		Map<Long, String> out = new LinkedHashMap<>();
		if (itemIds == null || itemIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId).eq(ItemsCommission::getType, "item").in(ItemsCommission::getRelId, itemIds);
		List<ItemsCommission> rows = mapper.selectList(w);
		for (ItemsCommission r : rows) {
			if (r.getRelId() != null) {
				out.put(r.getRelId(), r.getCommissionConf() != null ? r.getCommissionConf() : "");
			}
		}
		return out;
	}

	/**
	 * 按 SPU（type=goods）查 {@code commission_conf.commission}，缺失时默认 0。
	 */
	public Map<Long, Number> mapCommissionRatioByGoodsIds(long companyId, Collection<Long> goodsIds) {
		Map<Long, Number> out = new LinkedHashMap<>();
		if (goodsIds == null || goodsIds.isEmpty()) {
			return out;
		}
		LambdaQueryWrapper<ItemsCommission> w = new LambdaQueryWrapper<>();
		w.eq(ItemsCommission::getCompanyId, companyId).eq(ItemsCommission::getType, "goods").in(ItemsCommission::getRelId, goodsIds);
		List<ItemsCommission> rows = mapper.selectList(w);
		for (ItemsCommission r : rows) {
			if (r.getRelId() == null) {
				continue;
			}
			out.put(r.getRelId(), parseCommissionNumber(r.getCommissionConf()));
		}
		return out;
	}

	private static Number parseCommissionNumber(String commissionConfJson) {
		if (!StringUtils.hasText(commissionConfJson)) {
			return 0;
		}
		try {
			JsonNode n = OM.readTree(commissionConfJson);
			if (n != null && n.has("commission")) {
				JsonNode c = n.get("commission");
				if (c.isNumber()) {
					return c.numberValue();
				}
				String s = c.asText("");
				if (!s.isEmpty()) {
					try {
						return new BigDecimal(s.trim());
					} catch (NumberFormatException ignored) {
						return 0;
					}
				}
			}
		} catch (Exception ignored) {
		}
		return 0;
	}
}
