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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class GoodsItemsCreateParamValidator {

	private GoodsItemsCreateParamValidator() {
	}

	public static void validateForUpdate(Map<String, Object> p, long pathItemId) {
		if (pathItemId < 1L) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
		long bodyItemId = toLongOrZero(p.get("item_id"));
		if (bodyItemId != 0L && bodyItemId != pathItemId) {
			throw new BadRequestException("请确认您所编辑的商品是否存在");
		}
		if (!StringUtils.hasText(str(p.get("item_name")))) {
			throw new BadRequestException("商品名称必填");
		}
		String consume = str(p.get("consume_type"));
		if (StringUtils.hasText(consume) && !java.util.Set.of("every", "all", "notconsume").contains(consume)) {
			throw new BadRequestException("核销类型参数不正确");
		}
		if (p.get("pics") == null || !StringUtils.hasText(str(p.get("pics")))) {
			throw new BadRequestException("请上传商品图片");
		}
		if (p.get("sort") == null) {
			throw new BadRequestException("排序值必须为整数");
		}
		try {
			Integer.parseInt(str(p.get("sort")));
		} catch (NumberFormatException e) {
			throw new BadRequestException("排序值必须为整数");
		}
		long sup = toLongOrZero(p.get("supplier_id"));
		boolean isSupplierGoods = sup > 0;
		String itemType = str(p.get("item_type"));
		if ("normal".equals(itemType)) {
			if (p.get("templates_id") == null) {
				throw new BadRequestException("运费模板必填");
			}
			if (p.get("brand_id") == null) {
				throw new BadRequestException("请选择品牌");
			}
			if (isSupplierGoods) {
				if (!StringUtils.hasText(str(p.get("audit_status")))) {
					throw new BadRequestException("审核状态不能为空");
				}
			} else {
				if (p.get("item_category") == null && p.get("item_main_cat_id") == null) {
					throw new BadRequestException("请选择销售分类");
				}
			}
		}
	}

	private static long toLongOrZero(Object o) {
		if (o == null) {
			return 0L;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (s.isEmpty() || "0".equals(s)) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	public static void validate(Map<String, Object> p) {
		if (!StringUtils.hasText(str(p.get("item_name")))) {
			throw new BadRequestException("商品名称必填");
		}
		String consume = str(p.get("consume_type"));
		if (StringUtils.hasText(consume) && !java.util.Set.of("every", "all", "notconsume").contains(consume)) {
			throw new BadRequestException("核销类型参数不正确");
		}
		if (p.get("pics") == null || !StringUtils.hasText(str(p.get("pics")))) {
			throw new BadRequestException("请上传商品图片");
		}
		if (p.get("sort") == null) {
			throw new BadRequestException("排序值必须为整数");
		}
		try {
			Integer.parseInt(str(p.get("sort")));
		} catch (NumberFormatException e) {
			throw new BadRequestException("排序值必须为整数");
		}
		String itemType = str(p.get("item_type"));
		if ("normal".equals(itemType)) {
			if (p.get("templates_id") == null) {
				throw new BadRequestException("运费模板必填");
			}
			if (p.get("brand_id") == null) {
				throw new BadRequestException("请选择品牌");
			}
			String op = str(p.get("operator_type"));
			if ("supplier".equals(op)) {
				if (!StringUtils.hasText(str(p.get("audit_status")))) {
					throw new BadRequestException("审核状态不能为空");
				}
			} else {
				if (p.get("item_category") == null && p.get("item_main_cat_id") == null) {
					throw new BadRequestException("请选择销售分类");
				}
			}
		}
	}

	private static String str(Object o) {
		return o == null ? "" : o.toString().trim();
	}
}
