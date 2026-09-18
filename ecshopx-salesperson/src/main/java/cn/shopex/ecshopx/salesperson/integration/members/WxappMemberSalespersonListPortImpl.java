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

package cn.shopex.ecshopx.salesperson.integration.members;

import cn.shopex.ecshopx.common.members.port.WxappMemberSalespersonListPort;
import cn.shopex.ecshopx.salesperson.service.SalespersonListService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("wxappMemberSalespersonListPortImpl")
public class WxappMemberSalespersonListPortImpl implements WxappMemberSalespersonListPort {

	private final SalespersonListService salespersonListService;

	public WxappMemberSalespersonListPortImpl(SalespersonListService salespersonListService) {
		this.salespersonListService = salespersonListService;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, Object>> listForMember(long companyId, long userId, int limit) {
		int uid = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, userId));
		Map<String, Object> body = salespersonListService.listForSalemanShopList(companyId, uid, limit);
		Object list = body.get("list");
		if (!(list instanceof List<?> raw)) {
			return List.of();
		}
		return (List<Map<String, Object>>) (List<?>) raw;
	}
}
