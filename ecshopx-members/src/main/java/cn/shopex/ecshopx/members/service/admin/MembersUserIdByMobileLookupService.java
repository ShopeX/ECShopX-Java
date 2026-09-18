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

import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class MembersUserIdByMobileLookupService {

	private final MembersMapper membersMapper;

	public MembersUserIdByMobileLookupService(MembersMapper membersMapper) {
		this.membersMapper = membersMapper;
	}

	public Long findUserIdByCompanyAndPlainMobile(long companyId, String plainMobile) {
		if (!StringUtils.hasText(plainMobile)) {
			return null;
		}
		String enc = LegacyFixedMobileEncrypt.fixedEncryptMobile(plainMobile.trim());
		Members row = membersMapper.selectMemberRowForAdminByCompanyAndMobileEnc(companyId, enc);
		return row == null ? null : row.getUserId();
	}
}
