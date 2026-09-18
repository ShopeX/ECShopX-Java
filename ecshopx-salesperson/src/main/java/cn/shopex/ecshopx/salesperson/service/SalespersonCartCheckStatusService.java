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

package cn.shopex.ecshopx.salesperson.service;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.salesperson.repository.SalespersonCartRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartCheckStatusService {

	private final SalespersonCartRepository salespersonCartRepository;

	public SalespersonCartCheckStatusService(SalespersonCartRepository salespersonCartRepository) {
		this.salespersonCartRepository = salespersonCartRepository;
	}

	public void updateCartCheckStatus(Map<String, Object> merged, Map<String, Object> authMap) {
		long spId = longFromAuth(authMap.get("salesperson_id"));
		if (spId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long companyId = longFromAuth(authMap.get("company_id"));
		if (companyId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		Object rawCart = merged.get("cart_id");
		List<Long> cartIds = parseCartIdsOrThrow(rawCart);
		int bit = parseCheckedSelectionBit(merged.get("is_checked"));
		salespersonCartRepository.updateIsCheckedByCompanyAndCartIds(companyId, cartIds, bit == 1);
	}

	private static long longFromAuth(Object v) {
		if (v == null) {
			return 0L;
		}
		if (v instanceof Number n) {
			return n.longValue();
		}
		String s = v.toString().trim();
		if (s.isEmpty()) {
			return 0L;
		}
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static List<Long> parseCartIdsOrThrow(Object raw) {
		if (raw == null) {
			throw new ResourceException("购物车参数错误");
		}
		if (raw instanceof String s) {
			s = s.trim();
			if (s.isEmpty() || "0".equals(s)) {
				throw new ResourceException("购物车参数错误");
			}
			long id;
			try {
				id = Long.parseLong(s);
			} catch (NumberFormatException e) {
				throw new ResourceException("购物车参数错误");
			}
			if (id <= 0) {
				throw new ResourceException("购物车参数错误");
			}
			return List.of(id);
		}
		if (raw instanceof Number n) {
			long id = n.longValue();
			if (id == 0) {
				throw new ResourceException("购物车参数错误");
			}
			return List.of(id);
		}
		if (raw instanceof Collection<?> c) {
			if (c.isEmpty()) {
				throw new ResourceException("购物车参数错误");
			}
			List<Long> out = new ArrayList<>(c.size());
			for (Object elem : c) {
				long id = parseCartIdElementToLong(elem);
				if (id <= 0) {
					throw new ResourceException("购物车参数错误");
				}
				out.add(id);
			}
			return out;
		}
		if (raw instanceof Object[] arr) {
			if (arr.length == 0) {
				throw new ResourceException("购物车参数错误");
			}
			List<Long> out = new ArrayList<>(arr.length);
			for (Object elem : arr) {
				long id = parseCartIdElementToLong(elem);
				if (id <= 0) {
					throw new ResourceException("购物车参数错误");
				}
				out.add(id);
			}
			return out;
		}
		throw new ResourceException("购物车参数错误");
	}

	private static long parseCartIdElementToLong(Object elem) {
		if (elem == null) {
			return 0L;
		}
		if (elem instanceof Number n) {
			return n.longValue();
		}
		if (elem instanceof String s) {
			s = s.trim();
			if (s.isEmpty() || "0".equals(s)) {
				return 0L;
			}
			try {
				return Long.parseLong(s);
			} catch (NumberFormatException e) {
				return 0L;
			}
		}
		try {
			return Long.parseLong(elem.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}

	private static int parseCheckedSelectionBit(Object raw) {
		if (raw == null) {
			return 0;
		}
		if (raw instanceof Boolean b) {
			return b ? 1 : 0;
		}
		if (raw instanceof Number n) {
			return n.intValue() == 0 ? 0 : 1;
		}
		if (raw instanceof String s) {
			s = s.trim();
			if (s.isEmpty()) {
				return 0;
			}
			if ("false".equals(s)) {
				return 0;
			}
			return 1;
		}
		String s = raw.toString().trim();
		if (s.isEmpty()) {
			return 0;
		}
		if ("false".equals(s)) {
			return 0;
		}
		return 1;
	}
}
