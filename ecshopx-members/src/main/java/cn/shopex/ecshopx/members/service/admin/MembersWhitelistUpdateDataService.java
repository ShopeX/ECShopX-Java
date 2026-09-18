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

import cn.shopex.ecshopx.common.exception.BadRequestException;
import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.MembersWhitelist;
import cn.shopex.ecshopx.members.mapper.MembersWhitelistMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MembersWhitelistUpdateDataService {

	private final MembersWhitelistMapper membersWhitelistMapper;

	public MembersWhitelistUpdateDataService(MembersWhitelistMapper membersWhitelistMapper) {
		this.membersWhitelistMapper = membersWhitelistMapper;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> updateData(long companyId, String pathWhitelistId, String nameRaw) {
		if (pathWhitelistId == null || pathWhitelistId.isEmpty()) {
			throw new BadRequestException("缺少id");
		}

		String namePlain = nameRaw == null ? "" : nameRaw.trim();
		if (namePlain.isEmpty()) {
			throw new BadRequestException("缺少姓名");
		}

		long whitelistId;
		try {
			whitelistId = Long.parseLong(pathWhitelistId.trim());
		} catch (NumberFormatException e) {
			throw new ResourceException("未查询到更新数据");
		}

		LambdaQueryWrapper<MembersWhitelist> w = new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getWhitelistId, whitelistId)
				.last("LIMIT 1");
		MembersWhitelist row = membersWhitelistMapper.selectOne(w);
		if (row == null) {
			throw new ResourceException("未查询到更新数据");
		}

		row.setName(LegacyFixedMobileEncrypt.fixedEncryptMobile(namePlain));
		row.setUpdated(Instant.now().getEpochSecond());
		membersWhitelistMapper.updateById(row);

		return toAdminRow(row, companyId);
	}

	private LinkedHashMap<String, Object> toAdminRow(MembersWhitelist row, long companyId) {
		String plainMobile = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getMobile());
		String plainName = LegacyFixedMobileEncrypt.fixedDecryptLegacyPayload(row.getName());
		LinkedHashMap<String, Object> out = new LinkedHashMap<>();
		out.put("whitelist_id", row.getWhitelistId());
		out.put("company_id", companyId);
		out.put("mobile", LegacyFixedMobileEncrypt.fixedEncryptMobile(plainMobile));
		out.put("name", plainName);
		out.put("created", row.getCreated());
		out.put("updated", row.getUpdated());
		return out;
	}
}
