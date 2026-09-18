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
import cn.shopex.ecshopx.kujiale.domain.KujialeDesignerWorksItemRel;
import cn.shopex.ecshopx.kujiale.mapper.KujialeDesignerWorksItemRelMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class KujialeDesignerWorksUnbindItemService {

	private final KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper;

	public KujialeDesignerWorksUnbindItemService(KujialeDesignerWorksItemRelMapper kujialeDesignerWorksItemRelMapper) {
		this.kujialeDesignerWorksItemRelMapper = kujialeDesignerWorksItemRelMapper;
	}

	public Map<String, Object> unbind(JsonNode body) {
		List<JsonNode> bindings = normalizeBindings(body);
		if (bindings == null || bindings.isEmpty()) {
		throw new ResourceException("参数格式错误，需要传入数组格式");
		}

		List<String> errors = new ArrayList<>();
		int successCount = 0;
		int failCount = 0;

		try {
			int n = 0;
			for (JsonNode binding : bindings) {
				n++;
				if (!bindingHasNonNullValue(binding, "design_id") || !bindingHasNonNullValue(binding, "item_id")) {
					failCount++;
					errors.add("第" + n + "条数据缺少 design_id 或 item_id");
					continue;
				}
				JsonNode itemNode = binding.get("item_id");
				Long itemId = parsePositiveLong(itemNode);
				if (itemId == null) {
					failCount++;
					errors.add("第" + n + "条数据的 item_id 格式错误");
					continue;
				}
				String designId = designIdToTrimmedString(binding.get("design_id"));
				if (isEmptyDesignId(designId)) {
					failCount++;
					errors.add("第" + n + "条数据的 design_id 不能为空");
					continue;
				}

				LambdaQueryWrapper<KujialeDesignerWorksItemRel> w = new LambdaQueryWrapper<>();
				w.eq(KujialeDesignerWorksItemRel::getDesignId, designId)
						.eq(KujialeDesignerWorksItemRel::getItemId, itemId)
						.last("LIMIT 1");
				KujialeDesignerWorksItemRel existingRel = kujialeDesignerWorksItemRelMapper.selectOne(w);
				if (existingRel == null) {
					failCount++;
					errors.add("第" + n + "条数据的绑定关系不存在");
					continue;
				}
				kujialeDesignerWorksItemRelMapper.deleteById(existingRel.getId());
				successCount++;
			}
		} catch (Exception e) {
			throw new ResourceException("解绑失败：" + e.getMessage());
		}

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("success", true);
		result.put("message", "批量解绑完成");
		result.put("total", bindings.size());
		result.put("success_count", successCount);
		result.put("fail_count", failCount);
		if (!errors.isEmpty()) {
			result.put("errors", errors);
		}
		return result;
	}

	/**
	 * 归一化绑定数据：若首元素为数组/对象则视为多行，否则包装为单行列表。
	 * JSON {@code []} → 含一个空对象的单行列表；非数组根节点 → {@code [$root]}.
	 */
	private static List<JsonNode> normalizeBindings(JsonNode body) {
		if (body == null || body.isNull() || body.isMissingNode()) {
			return null;
		}
		if (body.isArray()) {
			if (body.isEmpty()) {
				// 空数组包装为含一个空对象的单行列表，保持与历史行为一致
				return List.of(JsonNodeFactory.instance.objectNode());
			}
			JsonNode first = body.get(0);
			if (first != null && (first.isObject() || first.isArray())) {
				List<JsonNode> out = new ArrayList<>(body.size());
				body.forEach(out::add);
				return out;
			}
			return List.of(body);
		}
		if (body.isObject()) {
			return List.of(body);
		}
		// number, string, boolean, etc.
		return List.of(body);
	}

	private static boolean bindingHasNonNullValue(JsonNode binding, String key) {
		if (!binding.isObject()) {
			return false;
		}
		if (!binding.has(key)) {
			return false;
		}
		JsonNode v = binding.get(key);
		return v != null && !v.isNull() && !v.isMissingNode();
	}

	private static String designIdToTrimmedString(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return "";
		}
		if (node.isObject() || node.isArray()) {
			return "";
		}
		return node.asText().trim();
	}

	private static boolean isEmptyDesignId(String t) {
		return t.isEmpty() || "0".equals(t);
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
