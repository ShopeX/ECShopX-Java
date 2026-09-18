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

package cn.shopex.ecshopx.point.integration.members;

import cn.shopex.ecshopx.members.integration.admin.AdminMemberGetInfoPointPayloadPort;
import cn.shopex.ecshopx.point.service.PointMemberInfoService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service("adminMemberGetInfoPointPayloadPortImpl")
public class AdminMemberGetInfoPointPayloadPortImpl implements AdminMemberGetInfoPointPayloadPort {

	private final PointMemberInfoService pointMemberInfoService;

	public AdminMemberGetInfoPointPayloadPortImpl(PointMemberInfoService pointMemberInfoService) {
		this.pointMemberInfoService = pointMemberInfoService;
	}

	@Override
	public Map<String, Object> readPoint(long companyId, long userId) {
		return pointMemberInfoService.info(companyId, userId);
	}
}
