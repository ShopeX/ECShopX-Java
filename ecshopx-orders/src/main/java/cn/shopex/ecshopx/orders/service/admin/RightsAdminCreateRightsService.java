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

package cn.shopex.ecshopx.orders.service.admin;

import cn.shopex.ecshopx.common.goods.port.AdminManualRightsAddByItemPort;
import cn.shopex.ecshopx.members.service.admin.MembersUserIdByMobileLookupService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RightsAdminCreateRightsService {

	private final MembersUserIdByMobileLookupService membersUserIdByMobileLookupService;
	private final AdminManualRightsAddByItemPort adminManualRightsAddByItemPort;

	public RightsAdminCreateRightsService(
			MembersUserIdByMobileLookupService membersUserIdByMobileLookupService,
			AdminManualRightsAddByItemPort adminManualRightsAddByItemPort) {
		this.membersUserIdByMobileLookupService = membersUserIdByMobileLookupService;
		this.adminManualRightsAddByItemPort = adminManualRightsAddByItemPort;
	}

	public void createRights(long companyId, List<Long> itemIds, String mobilePlain) {
		Long userId = membersUserIdByMobileLookupService.findUserIdByCompanyAndPlainMobile(companyId, mobilePlain);
		if (userId == null) {
			throw new IllegalStateException("expected member for mobile");
		}
		for (Long itemId : itemIds) {
			adminManualRightsAddByItemPort.addRightsByItemId(itemId, userId, companyId, mobilePlain, "管理员手动添加");
		}
	}
}
