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

package cn.shopex.ecshopx.goods.service.pointsmall.export;

import cn.shopex.ecshopx.pointsmall.domain.PointsmallItems;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

public final class PointsmallItemsExportQuerySupport {

	private PointsmallItemsExportQuerySupport() {
	}

	public static void applyParams(LambdaQueryWrapper<PointsmallItems> w, Map<String, Object> params) {
		for (Map.Entry<String, Object> e : params.entrySet()) {
			String key = e.getKey();
			Object val = e.getValue();
			if (val == null) {
				continue;
			}
			if ("isGetSkuList".equals(key)) {
				continue;
			}
			int pipe = key.indexOf('|');
			if (pipe > 0) {
				String col = key.substring(0, pipe);
				String op = key.substring(pipe + 1);
				applyPipeFilter(w, col, op, val);
				continue;
			}
			switch (key) {
				case "company_id" -> w.eq(PointsmallItems::getCompanyId, toLong(val));
				case "templates_id" -> w.eq(PointsmallItems::getTemplatesId, toIntObj(val));
				case "regions_id" -> w.eq(PointsmallItems::getRegionsId, val.toString());
				case "nospec" -> w.eq(PointsmallItems::getNospec, val.toString());
				case "audit_status" -> w.eq(PointsmallItems::getAuditStatus, val.toString());
				case "approve_status" -> {
					if (val instanceof Collection<?> c) {
						List<String> strs = c.stream().map(Object::toString).collect(Collectors.toList());
						if (!strs.isEmpty()) {
							w.in(PointsmallItems::getApproveStatus, strs);
						}
					} else {
						w.eq(PointsmallItems::getApproveStatus, val.toString());
					}
				}
				case "item_type" -> w.eq(PointsmallItems::getItemType, val.toString());
				case "special_type" -> w.eq(PointsmallItems::getSpecialType, val.toString());
				case "brand_id" -> w.eq(PointsmallItems::getBrandId, toIntObj(val));
				case "item_bn" -> w.eq(PointsmallItems::getItemBn, val.toString());
				case "item_id" -> w.in(PointsmallItems::getItemId, toLongCollection(val));
				case "default_item_id" -> w.in(PointsmallItems::getDefaultItemId, toLongCollection(val));
				case "item_category" -> w.in(PointsmallItems::getItemCategory, toStringCollection(val));
				case "is_default" -> w.eq(PointsmallItems::getIsDefault, toBooleanStrict(val));
				default -> {
					// ignore unknown keys
				}
			}
		}
	}

	private static void applyPipeFilter(LambdaQueryWrapper<PointsmallItems> w, String col, String op, Object val) {
		switch (col) {
			case "item_name" -> {
				if ("contains".equals(op) || "like".equals(op)) {
					String s = val.toString();
					if (StringUtils.hasText(s)) {
						String pat = "%" + escapeSqlLike(s.trim()) + "%";
						w.like(PointsmallItems::getItemName, pat);
					}
				}
			}
			case "store" -> {
				int n = toInt(val);
				if ("gt".equals(op)) {
					w.gt(PointsmallItems::getStore, n);
				} else if ("lt".equals(op)) {
					w.lt(PointsmallItems::getStore, n);
				}
			}
			case "point" -> {
				int n = toInt(val);
				if ("gt".equals(op)) {
					w.gt(PointsmallItems::getPoint, n);
				} else if ("lt".equals(op)) {
					w.lt(PointsmallItems::getPoint, n);
				} else if ("gte".equals(op)) {
					w.ge(PointsmallItems::getPoint, n);
				} else if ("lte".equals(op)) {
					w.le(PointsmallItems::getPoint, n);
				}
			}
			case "item_id" -> {
				if ("neq".equals(op)) {
					w.ne(PointsmallItems::getItemId, toLong(val));
				}
			}
			default -> {
			}
		}
	}

	private static Collection<Long> toLongCollection(Object val) {
		if (val instanceof Collection<?> c) {
			return c.stream().map(PointsmallItemsExportQuerySupport::toLong).toList();
		}
		return List.of(toLong(val));
	}

	private static Collection<String> toStringCollection(Object val) {
		if (val instanceof Collection<?> c) {
			return c.stream().map(Object::toString).toList();
		}
		return List.of(val.toString());
	}

	private static long toLong(Object v) {
		if (v instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(v.toString().trim());
	}

	private static int toInt(Object v) {
		if (v instanceof Number n) {
			return n.intValue();
		}
		return (int) Double.parseDouble(v.toString().trim());
	}

	private static Integer toIntObj(Object v) {
		return toInt(v);
	}

	private static String escapeSqlLike(String raw) {
		if (raw == null || raw.isEmpty()) {
			return "";
		}
		return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static boolean toBooleanStrict(Object v) {
		if (v instanceof Boolean b) {
			return b;
		}
		return Boolean.parseBoolean(v.toString());
	}
}
