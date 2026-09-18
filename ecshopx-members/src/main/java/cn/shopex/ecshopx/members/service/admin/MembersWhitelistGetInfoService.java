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

package cn.shopex.ecshopx.members.service.admin;

import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;

@Service
public class MembersWhitelistGetInfoService {

	private final MembersWhitelistMapper membersWhitelistMapper;

	public MembersWhitelistGetInfoService(MembersWhitelistMapper membersWhitelistMapper) {
		this.membersWhitelistMapper = membersWhitelistMapper;
	}

	public Object getInfo(long companyId, String idParam) {
		String s = idParam.trim();
		long whitelistId;
		try {
			whitelistId = Long.parseLong(s);
		} catch (NumberFormatException e) {
			return Collections.emptyList();
		}
		LambdaQueryWrapper<MembersWhitelist> w = new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getWhitelistId, whitelistId);
		MembersWhitelist row = membersWhitelistMapper.selectOne(w);
		if (row == null) {
			return Collections.emptyList();
		}
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		map.put("whitelist_id", row.getWhitelistId());
		map.put("company_id", row.getCompanyId());
		map.put("mobile", LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getMobile()));
		map.put("name", LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getName()));
		map.put("created", row.getCreated());
		map.put("updated", row.getUpdated());
		return map;
	}
}
