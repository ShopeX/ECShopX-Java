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

package cn.shopex.ecshopx.aftersales.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiAftersalesDetailPort;
import cn.shopex.ecshopx.openapi.thirdapi.v2.orders.OpenapiThirdApiV2AftersalesDetailService;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenapiAftersalesDetailPortImpl implements OpenapiAftersalesDetailPort {

	private final OpenapiThirdApiV2AftersalesDetailService detailService;

	public OpenapiAftersalesDetailPortImpl(OpenapiThirdApiV2AftersalesDetailService detailService) {
		this.detailService = detailService;
	}

	@Override
	public Map<String, Object> getAftersalesDetail(long companyId, String aftersalesBnRaw) {
		return detailService.getAftersalesDetail(companyId, aftersalesBnRaw);
	}
}
