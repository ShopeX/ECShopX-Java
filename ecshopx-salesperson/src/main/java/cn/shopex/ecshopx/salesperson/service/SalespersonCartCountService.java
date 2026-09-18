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
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SalespersonCartCountService {

	private final SalespersonCartRepository salespersonCartRepository;

	public SalespersonCartCountService(SalespersonCartRepository salespersonCartRepository) {
		this.salespersonCartRepository = salespersonCartRepository;
	}

	public Map<String, Object> getCartItemCount(Map<String, Object> authMap) {
		// 以 Redis 会话中的导购 ID 为准；鉴权层会用库表写回 salesperson_id，不能单靠合并后的值判断
		long sessionSpId = longFromAuth(authMap.get("session_salesperson_id"));
		if (sessionSpId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long spId = longFromAuth(authMap.get("salesperson_id"));
		if (spId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		long companyId = longFromAuth(authMap.get("company_id"));
		if (companyId <= 0) {
			throw new ResourceException("您的账号有误");
		}
		return salespersonCartRepository.countCart(companyId, spId);
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
}
