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

package cn.shopex.ecshopx.distribution.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiDistributorDetailPort;
import cn.shopex.ecshopx.distribution.openapi.thirdapi.v1.OpenapiThirdApiV1DistributorDetailService;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class OpenapiDistributorDetailPortImpl implements OpenapiDistributorDetailPort {

	private final OpenapiThirdApiV1DistributorDetailService detailService;

	public OpenapiDistributorDetailPortImpl(
			OpenapiThirdApiV1DistributorDetailService detailService) {
		this.detailService = detailService;
	}

	@Override
	public Map<String, Object> getDistributorDetail(long companyId, String shopCodeRaw) {
		return detailService.execute(companyId, shopCodeRaw);
	}
}
