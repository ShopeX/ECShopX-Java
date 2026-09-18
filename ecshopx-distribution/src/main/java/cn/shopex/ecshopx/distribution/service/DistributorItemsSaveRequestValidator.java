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

package cn.shopex.ecshopx.distribution.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DistributorItemsSaveRequestValidator {

	private DistributorItemsSaveRequestValidator() {}

	public static void validate(Map<String, Object> merged) {
		if (merged == null) {
			throw new ResourceException("参数 distributor_ids 不能为空");
		}
		if (!merged.containsKey("distributor_ids") || merged.get("distributor_ids") == null) {
			throw new ResourceException("参数 distributor_ids 不能为空");
		}
		if (!merged.containsKey("item_ids") || merged.get("item_ids") == null) {
			throw new ResourceException("参数 item_ids 不能为空");
		}
		Object did = merged.get("distributor_ids");
		if (!isValidDistributorIdsShape(did)) {
			throw new ResourceException("参数 distributor_ids 格式无效，需为数组或 _all");
		}
		Object iid = merged.get("item_ids");
		if (!isValidItemIdsShape(iid)) {
			throw new ResourceException("参数 item_ids 格式无效，需为数组或 _all");
		}
	}

	static boolean isValidDistributorIdsShape(Object raw) {
		if (raw instanceof String s) {
			return "_all".equals(s);
		}
		return raw instanceof List<?> && !((List<?>) raw).isEmpty();
	}

	static boolean isValidItemIdsShape(Object raw) {
		if (raw instanceof String s) {
			return "_all".equals(s);
		}
		return raw instanceof List<?> && !((List<?>) raw).isEmpty();
	}

	/**
	 * Parses {@code distributor_ids} after {@link #validate}; does not accept missing keys.
	 */
	public static java.util.List<Long> parseDistributorIdList(Map<String, Object> merged) {
		Object raw = merged.get("distributor_ids");
		if (raw instanceof String s && "_all".equals(s)) {
			return null;
		}
		if (raw instanceof List<?> list) {
			java.util.ArrayList<Long> out = new java.util.ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				out.add(toLongDistributorId(o));
			}
			if (out.isEmpty()) {
				throw new ResourceException("参数 distributor_ids 不能为空");
			}
			return out;
		}
		throw new ResourceException("参数 distributor_ids 格式无效，需为数组或 _all");
	}

	public static List<Long> parseDefaultItemIdList(Map<String, Object> merged) {
		Object raw = merged.get("item_ids");
		if (raw instanceof String s && "_all".equals(s)) {
			return null;
		}
		if (raw instanceof List<?> list) {
			java.util.ArrayList<Long> out = new java.util.ArrayList<>();
			for (Object o : list) {
				if (o == null) {
					continue;
				}
				out.add(toLongItemId(o));
			}
			if (out.isEmpty()) {
				throw new ResourceException("参数 item_ids 不能为空");
			}
			return out;
		}
		throw new ResourceException("参数 item_ids 格式无效，需为数组或 _all");
	}

	public static int itemIdsCountForQueueThreshold(Map<String, Object> merged) {
		Object raw = merged.get("item_ids");
		if (raw instanceof String s && "_all".equals(s)) {
			return 1;
		}
		if (raw instanceof List<?> list) {
			return list.size();
		}
		return 99;
	}

	public static boolean parseIsCanSale(Map<String, Object> merged) {
		if (!merged.containsKey("is_can_sale")) {
			return false;
		}
		Object v = merged.get("is_can_sale");
		if (v instanceof Boolean b) {
			return b;
		}
		if (v != null && "true".equalsIgnoreCase(v.toString().trim())) {
			return true;
		}
		return false;
	}

	private static long toLongDistributorId(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("店铺 ID 无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("店铺 ID 无效");
		}
	}

	private static long toLongItemId(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		String s = o.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("item_ids 元素无效");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("item_ids 元素无效");
		}
	}
}
