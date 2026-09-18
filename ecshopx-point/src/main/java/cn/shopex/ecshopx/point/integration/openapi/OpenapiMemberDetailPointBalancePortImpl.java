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

package cn.shopex.ecshopx.point.integration.openapi;

import cn.shopex.ecshopx.common.openapi.OpenapiMemberDetailPointBalancePort;
import cn.shopex.ecshopx.point.service.PointMemberBalanceReadService;
import org.springframework.stereotype.Service;

@Service
public class OpenapiMemberDetailPointBalancePortImpl implements OpenapiMemberDetailPointBalancePort {

	private final PointMemberBalanceReadService pointMemberBalanceReadService;

	public OpenapiMemberDetailPointBalancePortImpl(PointMemberBalanceReadService pointMemberBalanceReadService) {
		this.pointMemberBalanceReadService = pointMemberBalanceReadService;
	}

	@Override
	public long getPointBalance(long companyId, long userId) {
		return pointMemberBalanceReadService.getPointBalance(companyId, userId);
	}
}
