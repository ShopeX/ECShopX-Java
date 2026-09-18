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

package cn.shopex.ecshopx.pointsmall.validation;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PointsmallItemsCreateParamValidator {

	private static final List<String> CONSUME_TYPES = List.of("every", "all", "notconsume");

	public void validate(Map<String, Object> merged) {
		validateCommonGoodsFields(merged);
		requireText(merged, "templates_id", "运费模板必填");
		requireText(merged, "brand_id", "请选择品牌");
		requireItemCategory(merged);
	}

	public void validateForUpdate(Map<String, Object> merged) {
		validatePathItemId(merged);
		validateCommonGoodsFields(merged);
	}

	private void validatePathItemId(Map<String, Object> merged) {
		Object v = merged.get("item_id");
		if (v == null) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
		long id;
		if (v instanceof Number n) {
			double d = n.doubleValue();
			if (Double.isNaN(d) || d != Math.floor(d)) {
				throw new BadRequestException("请确认您所编辑的商品是否存在");
			}
			id = n.longValue();
		} else {
			String s = v.toString().trim();
			if (!StringUtils.hasText(s)) {
				throw new BadRequestException("请确认您所编辑的商品是否存在");
			}
			try {
				if (s.contains(".") || s.contains("e") || s.contains("E")) {
					double d = Double.parseDouble(s);
					if (Double.isNaN(d) || d != Math.floor(d)) {
						throw new BadRequestException("请确认您所编辑的商品是否存在");
					}
					id = (long) d;
				} else {
					id = Long.parseLong(s);
				}
			} catch (NumberFormatException e) {
				throw new BadRequestException("请确认您所编辑的商品是否存在");
			}
		}
		if (id < 1L) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
	}

	private void validateCommonGoodsFields(Map<String, Object> merged) {
		Object ct = merged.get("consume_type");
		if (ct != null && StringUtils.hasText(ct.toString())) {
			String s = ct.toString().trim();
			if (!CONSUME_TYPES.contains(s)) {
				throw new BadRequestException("核销类型参数不正确");
			}
		}
		requireText(merged, "item_name", "商品名称必填");
		requireText(merged, "pics", "请上传商品图片");
		requireInteger(merged, "sort", "排序值必须为整数");

		String itemType = normalizeItemType(merged.get("item_type"));
		merged.put("item_type", itemType);
		if (!"normal".equals(itemType)) {
			throw new BadRequestException("商品类型无效");
		}
	}

	private static void requireItemCategory(Map<String, Object> merged) {
		Object v = merged.get("item_category");
		if (v == null) {
			throw new BadRequestException("请选择商品分类");
		}
		if (v instanceof List<?> list) {
			if (list.isEmpty()) {
				throw new BadRequestException("请选择商品分类");
			}
			return;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new BadRequestException("请选择商品分类");
		}
	}

	private static String normalizeItemType(Object raw) {
		if (raw == null || !StringUtils.hasText(raw.toString())) {
			return "normal";
		}
		return raw.toString().trim();
	}

	private static void requireText(Map<String, Object> m, String key, String msg) {
		Object v = m.get(key);
		if (v == null || !StringUtils.hasText(v.toString())) {
			throw new BadRequestException(msg);
		}
	}

	private static void requireInteger(Map<String, Object> m, String key, String msg) {
		Object v = m.get(key);
		if (v == null) {
			throw new BadRequestException(msg);
		}
		if (v instanceof Number) {
			return;
		}
		try {
			Long.parseLong(v.toString().trim());
		} catch (NumberFormatException e) {
			throw new BadRequestException(msg);
		}
	}
}
