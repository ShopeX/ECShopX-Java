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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

public final class DistributorItemsDeleteRequestValidator {

	private DistributorItemsDeleteRequestValidator() {}

	public record ParseResult(long distributorId, List<Long> goodsIds) {}

	public static ParseResult validateAndParse(Map<String, Object> merged) {
		if (merged == null) {
			throw new ResourceException("请选择需要删除的店铺");
		}
		Object dRaw = merged.get("distributor_id");
		if (dRaw == null) {
			throw new ResourceException("请选择需要删除的店铺");
		}
		if (dRaw instanceof String s && !StringUtils.hasText(s.trim())) {
			throw new ResourceException("请选择需要删除的店铺");
		}
		long distributorId = parseDistributorId(dRaw);

		Object itemRaw = merged.get("item_ids");
		if (itemRaw == null) {
			throw new ResourceException("请选择需要删除的商品");
		}
		List<Long> goodsIds = parseGoodsIds(itemRaw);
		return new ParseResult(distributorId, goodsIds);
	}

	private static long parseDistributorId(Object raw) {
		if (raw instanceof Number n) {
			return n.longValue();
		}
		String s = raw.toString().trim();
		if (!StringUtils.hasText(s)) {
			throw new ResourceException("请选择需要删除的店铺");
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			throw new ResourceException("参数 distributor_id 格式无效");
		}
	}

	/**
	 * 宽松解析 {@code goods_id}：非数字 token 不会产生有效 ID，删除影响 0 行且返回 {@code status:true}。
	 * 仍要求非空标量或非空列表（见 {@link #validateAndParse}）。
	 */
	private static List<Long> parseGoodsIds(Object raw) {
		if (raw instanceof String s) {
			String t = s.trim();
			if (!StringUtils.hasText(t)) {
				throw new ResourceException("请选择需要删除的商品");
			}
			Long one = tryParseLong(t);
			return one != null ? Collections.singletonList(one) : Collections.emptyList();
		}
		if (raw instanceof Number n) {
			return Collections.singletonList(n.longValue());
		}
		if (raw instanceof List<?> list) {
			if (list.isEmpty()) {
				throw new ResourceException("请选择需要删除的商品");
			}
			List<Long> out = new ArrayList<>(list.size());
			for (Object o : list) {
				Long v = tryParseLongElement(o);
				if (v == null) {
					return Collections.emptyList();
				}
				out.add(v);
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new ResourceException("请选择需要删除的商品");
			}
			List<Long> out = new ArrayList<>(arr.length);
			for (Object o : arr) {
				Long v = tryParseLongElement(o);
				if (v == null) {
					return Collections.emptyList();
				}
				out.add(v);
			}
			return out;
		}
		throw new ResourceException("参数 item_ids 格式无效");
	}

	private static Long tryParseLongElement(Object o) {
		if (o == null) {
			return null;
		}
		if (o instanceof Number n) {
			return n.longValue();
		}
		return tryParseLong(o.toString().trim());
	}

	private static Long tryParseLong(String t) {
		if (!StringUtils.hasText(t)) {
			return null;
		}
		try {
			return Long.parseLong(t);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
