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

package cn.shopex.ecshopx.members.service.h5;

import cn.shopex.ecshopx.members.domain.MembersProtocolLog;
import cn.shopex.ecshopx.members.mapper.MembersProtocolLogMapper;
import cn.shopex.ecshopx.members.service.h5.protocol.H5RegisterProtocolQueryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
public class MembersProtocolLogOnLoginService {

	private final H5RegisterProtocolQueryService protocolQueryService;

	private final MembersProtocolLogMapper membersProtocolLogMapper;

	public MembersProtocolLogOnLoginService(
			H5RegisterProtocolQueryService protocolQueryService,
			MembersProtocolLogMapper membersProtocolLogMapper) {
		this.protocolQueryService = protocolQueryService;
		this.membersProtocolLogMapper = membersProtocolLogMapper;
	}

	public void appendAcceptedProtocolsIfNeeded(H5GenericUser user, Map<String, Object> credentials) {
		long userId = user.getUserId();
		if (userId <= 0) {
			return;
		}
		Object cid = user.getAttributes().get("company_id");
		if (cid == null) {
			return;
		}
		long companyId = toLong(cid);
		List<Map<String, Object>> protocols = protocolQueryService.listRegisterAndPrivacyProtocols(companyId);
		long now = System.currentTimeMillis() / 1000L;
		for (Map<String, Object> protocol : protocols) {
			Object digestObj = protocol.get("digest");
			if (digestObj == null || !StringUtils.hasText(String.valueOf(digestObj))) {
				continue;
			}
			MembersProtocolLog row = new MembersProtocolLog();
			row.setCompanyId(companyId);
			row.setUserId(userId);
			row.setDigest(String.valueOf(digestObj));
			row.setCreated(now);
			row.setUpdated(now);
			membersProtocolLogMapper.insert(row);
		}
	}

	private static long toLong(Object o) {
		if (o instanceof Number n) {
			return n.longValue();
		}
		return Long.parseLong(String.valueOf(o));
	}
}
