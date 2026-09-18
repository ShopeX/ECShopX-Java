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

package cn.shopex.ecshopx.thirdparty.service.dmcrm;

import cn.shopex.ecshopx.common.members.port.DmCrmMemberPointOverlayPort;
import java.util.Collections;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service("dmCrmMemberPointOverlayPortImpl")
@ConditionalOnProperty(
		prefix = "ecshopx.thirdparty.dm-crm.point-detail-list",
		name = "http-enabled",
		havingValue = "false",
		matchIfMissing = true)
public class DmCrmMemberPointOverlayNoOp implements DmCrmMemberPointOverlayPort {

	@Override
	public Map<String, Object> fetchPointOverlay(long companyId, String mobilePlain) {
		return Collections.emptyMap();
	}
}
