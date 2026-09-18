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

package cn.shopex.ecshopx.orders.service.front.wxapp;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class WxappCartParamMergeSupport {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private WxappCartParamMergeSupport() {}

	public static LinkedHashMap<String, Object> applyAuthAndCartDefaults(
			Map<String, Object> merged, Map<String, Object> sessionAuth) {
		LinkedHashMap<String, Object> p = new LinkedHashMap<>(merged);
		long companyId = longVal(sessionAuth.get("company_id"));
		long sessionUserId = longVal(sessionAuth.get("user_id"));
		p.put("company_id", companyId);
		p.put("user_id", sessionUserId);

		long promoterUserId = longVal(p.get("promoter_user_id"));
		p.put("promoter_user_id", promoterUserId);
		if (promoterUserId != 0L && promoterUserId == sessionUserId) {
			p.put("user_id", p.get("buy_user_id"));
		}

		String wxa = stringVal(sessionAuth.get("wxapp_appid"));
		p.put("wxa_appid", wxa);
		p.put("wxapp_appid", wxa);

		Object shopIdRaw = p.get("shop_id");
		if (!hasShopId(shopIdRaw)) {
			Object dist = p.get("distributor_id");
			if (dist != null && StringUtils.hasText(dist.toString()) && !"0".equals(dist.toString().trim())) {
				p.put("shop_id", dist);
			} else {
				p.put("shop_id", 0L);
			}
		}

		String activityType = stringVal(p.get("activity_type"));
		if (!StringUtils.hasText(activityType)) {
			p.put("activity_type", "normal");
		}

		if ("package".equals(stringVal(p.get("activity_type")))) {
			Object itemsIdRaw = p.get("items_id");
			if (itemsIdRaw == null || isEmptyItemsId(itemsIdRaw)) {
				throw new ResourceException("请选择组合商品");
			}
			if (itemsIdRaw instanceof String s) {
				if (!StringUtils.hasText(s)) {
					throw new ResourceException("请选择组合商品");
				}
				try {
					Object parsed = OBJECT_MAPPER.readValue(s, Object.class);
					if (parsed instanceof List<?> list) {
						List<Object> copy = new ArrayList<>(list);
						p.put("items_id", copy);
					} else {
						throw new BadRequestException("参数非法");
					}
				} catch (BadRequestException e) {
					throw e;
				} catch (Exception e) {
					throw new BadRequestException("参数非法");
				}
			}
		}

		p.put("isAccumulate", normalizeIsAccumulate(p.get("isAccumulate")));

		normalizeIntLikeParam(p, "iscrossborder", 0);
		normalizeIntLikeParam(p, "isShopScreen", 0);
		return p;
	}

	private static boolean hasShopId(Object raw) {
		if (raw == null) {
			return false;
		}
		String s = raw.toString().trim();
		return StringUtils.hasText(s) && !"0".equals(s);
	}

	private static boolean isEmptyItemsId(Object raw) {
		if (raw instanceof List<?> list) {
			return list.isEmpty();
		}
		if (raw instanceof String s) {
			return !StringUtils.hasText(s);
		}
		return true;
	}

	private static Object normalizeIsAccumulate(Object raw) {
		if (raw instanceof Boolean b) {
			return b;
		}
		if (raw instanceof String s && "false".equals(s)) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
	}

	private static void normalizeIntLikeParam(LinkedHashMap<String, Object> p, String key, int def) {
		if (!p.containsKey(key) || p.get(key) == null) {
			p.put(key, def);
			return;
		}
		Object v = p.get(key);
		if (v instanceof Number n) {
			p.put(key, n.intValue());
			return;
		}
		String s = v.toString().trim();
		if (!StringUtils.hasText(s)) {
			p.put(key, def);
			return;
		}
		try {
			p.put(key, Integer.parseInt(s));
		} catch (NumberFormatException e) {
			throw new BadRequestException("参数非法");
		}
	}

	private static String stringVal(Object v) {
		return v == null ? "" : v.toString().trim();
	}

	private static long longVal(Object v) {
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
}
