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

package cn.shopex.ecshopx.thirdparty.service.shopexcrm;

import cn.shopex.ecshopx.common.exception.BadRequestException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UpdateMemberSuccessShopexCrmSyncExecutionService {

	private final ShopexCrmSyncSingleMemberPort shopexCrmSyncSingleMemberPort;
	private final String crmCrmSync;

	public UpdateMemberSuccessShopexCrmSyncExecutionService(
			ShopexCrmSyncSingleMemberPort shopexCrmSyncSingleMemberPort,
			@Value("${crm.crm-sync:}") String crmCrmSync) {
		this.shopexCrmSyncSingleMemberPort = shopexCrmSyncSingleMemberPort;
		this.crmCrmSync = crmCrmSync;
	}

	public void executeAfterUpdateMemberSuccess(Map<String, Object> payload) {
		if (crmCrmSync == null || crmCrmSync.isBlank()) {
			return;
		}
		Object rawCompany = payload.get("company_id");
		Object rawUser = payload.get("user_id");
		if (rawCompany == null || rawUser == null) {
			throw new BadRequestException("company_id and user_id are required");
		}
		long companyId = ((Number) rawCompany).longValue();
		long userId = ((Number) rawUser).longValue();
		shopexCrmSyncSingleMemberPort.syncUpdatedMember(companyId, userId);
	}
}
