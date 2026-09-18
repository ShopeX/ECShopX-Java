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

package cn.shopex.ecshopx.kaquan.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberCardV2UpdatePort;
import cn.shopex.ecshopx.kaquan.openapi.thirdapi.v2.OpenapiThirdApiV2MemberCardUpdateService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberCardV2UpdatePortImpl implements OpenapiMemberCardV2UpdatePort {

	private final OpenapiThirdApiV2MemberCardUpdateService updateService;

	public OpenapiMemberCardV2UpdatePortImpl(
			OpenapiThirdApiV2MemberCardUpdateService updateService) {
		this.updateService = updateService;
	}

	@Override
	public void updateMemberCard(long companyId, Map<String, Object> params) {
		updateService.executeOpenapiUpdate(companyId, params);
	}
}
