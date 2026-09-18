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

package cn.shopex.ecshopx.kujiale.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.goods.domain.Items;
import cn.shopex.ecshopx.goods.repository.ItemsRepository;
import cn.shopex.ecshopx.kujiale.api.admin.v1.request.KujialeDesignerWorksBindItemRequest;
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksItemRel;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KujialeDesignerWorksBindItemService {

	private final ItemsRepository itemsRepository;
	private final KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper;

	public KujialeDesignerWorksBindItemService(
			ItemsRepository itemsRepository, KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper) {
		this.itemsRepository = itemsRepository;
		this.kujialeDesignerWorksItemRelMapper = kujialeDesignerWorksItemRelMapper;
	}

	public Map<String, Object> bind(KujialeDesignerWorksBindItemRequest request) {
		Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
		JsonNode itemNode = request.getItemId();
		if (itemNode == null || itemNode.isNull() || itemNode.isMissingNode()) {
			fieldErrors.put("item_id", List.of("validation.required"));
		}
		JsonNode designNode = request.getDesignId();
		String designId = null;
		if (designNode == null || designNode.isNull() || designNode.isMissingNode()) {
			fieldErrors.put("design_id", List.of("validation.required"));
		} else if (!designNode.isTextual()) {
			fieldErrors.put("design_id", List.of("validation.string"));
		} else {
			designId = designNode.asText();
			if (designId.isEmpty()) {
				fieldErrors.put("design_id", List.of("validation.required"));
			}
		}
		if (!fieldErrors.isEmpty()) {
			throw new ResourceException("422 Unprocessable Content");
		}

		LinkedHashSet<Long> itemIds = normalizeItemIds(itemNode);
		if (itemIds.isEmpty()) {
			throw new ResourceException("item_id 参数无效");
		}

		int success = 0;
		int skipped = 0;
		List<String> errors = new ArrayList<>();
		List<Map<String, Object>> createdData = new ArrayList<>();
		long now = Instant.now().getEpochSecond();

		for (Long itemId : itemIds) {
			Items item = itemsRepository.findByItemId(itemId);
			if (item == null) {
				errors.add("商品不存在: " + itemId);
				continue;
			}
			if (existsRel(itemId, designId)) {
				skipped++;
				continue;
			}
			String goodsBn = item.getGoodsBn();
			KujialeDesignerWorksItemRel row = new KujialeDesignerWorksItemRel();
			row.setItemId(itemId);
			row.setDesignId(designId);
			row.setGoodsBn(goodsBn);
			row.setCreated((int) now);
			row.setUpdated((int) now);
			try {
				kujialeDesignerWorksItemRelMapper.insert(row);
				createdData.add(relToRowMap(row));
				success++;
			} catch (RuntimeException ex) {
				errors.add("商品 " + itemId + " 绑定失败：" + ex.getMessage());
			}
		}

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("success", errors.isEmpty());
		out.put("message", "绑定完成");
		out.put("success_count", success);
		out.put("skipped_count", skipped);
		out.put("data", createdData);
		out.put("errors", errors);
		return out;
	}

	private boolean existsRel(long itemId, String designId) {
		LambdaQueryWrapper<KujialeDesignerWorksItemRel> w = new LambdaQueryWrapper<>();
		w.eq(KujialeDesignerWorksItemRel::getItemId, itemId).eq(KujialeDesignerWorksItemRel::getDesignId, designId).last("LIMIT 1");
		return kujialeDesignerWorksItemRelMapper.selectOne(w) != null;
	}

	private static Map<String, Object> relToRowMap(KujialeDesignerWorksItemRel row) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", row.getId());
		m.put("item_id", row.getItemId());
		m.put("design_id", row.getDesignId());
		m.put("goods_bn", row.getGoodsBn());
		m.put("created", row.getCreated());
		m.put("updated", row.getUpdated());
		return m;
	}

	private static LinkedHashSet<Long> normalizeItemIds(JsonNode itemId) {
		LinkedHashSet<Long> out = new LinkedHashSet<>();
		if (itemId == null || itemId.isNull() || itemId.isMissingNode()) {
			return out;
		}
		if (itemId.isArray()) {
			for (JsonNode el : itemId) {
				Long v = parsePositiveLong(el);
				if (v != null) {
					out.add(v);
				}
			}
		} else {
			Long v = parsePositiveLong(itemId);
			if (v != null) {
				out.add(v);
			}
		}
		return out;
	}

	private static Long parsePositiveLong(JsonNode n) {
		if (n == null || n.isNull() || n.isMissingNode()) {
			return null;
		}
		if (n.isIntegralNumber()) {
			long v = n.longValue();
			return v > 0 ? v : null;
		}
		if (n.isFloatingPointNumber()) {
			double d = n.doubleValue();
			if (d <= 0 || d != Math.rint(d) || d > Long.MAX_VALUE) {
				return null;
			}
			return (long) d;
		}
		if (n.isTextual()) {
			String t = n.asText().trim();
			if (t.isEmpty()) {
				return null;
			}
			try {
				BigDecimal bd = new BigDecimal(t);
				if (bd.signum() <= 0) {
					return null;
				}
				long lv = bd.longValue();
				if (bd.compareTo(BigDecimal.valueOf(lv)) != 0) {
					return null;
				}
				return lv > 0 ? lv : null;
			} catch (NumberFormatException ex) {
				return null;
			}
		}
		return null;
	}
}
