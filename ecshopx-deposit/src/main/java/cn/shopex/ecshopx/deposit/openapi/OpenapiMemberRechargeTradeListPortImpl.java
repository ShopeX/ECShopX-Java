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

package cn.shopex.ecshopx.deposit.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberRechargeTradeListPort;
import cn.shopex.ecshopx.deposit.openapi.thirdapi.v2.OpenapiThirdApiV2MemberRechargeTradeListService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiMemberRechargeTradeListPortImpl implements OpenapiMemberRechargeTradeListPort {

	private final OpenapiThirdApiV2MemberRechargeTradeListService tradeListService;

	public OpenapiMemberRechargeTradeListPortImpl(
			OpenapiThirdApiV2MemberRechargeTradeListService tradeListService) {
		this.tradeListService = tradeListService;
	}

	@Override
	public Map<String, Object> getRechargeTradeList(
			long companyId,
			int page,
			int pageSize,
			String mobile,
			String tradeId,
			String shopId,
			String dateBegin,
			String dateEnd) {
		return tradeListService.executeOpenapiGetRechargeTradeList(
				companyId, page, pageSize, mobile, tradeId, shopId, dateBegin, dateEnd);
	}
}
