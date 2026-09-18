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
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class MembersWhitelistCreateDataService {

	private static final Pattern CN_MOBILE = Pattern.compile("^1[3456789][0-9]{9}$");

	private final MembersWhitelistMapper membersWhitelistMapper;

	public MembersWhitelistCreateDataService(MembersWhitelistMapper membersWhitelistMapper) {
		this.membersWhitelistMapper = membersWhitelistMapper;
	}

	public Map<String, Object> createData(long companyId, String mobileRaw, String nameRaw) {
		if (mobileRaw == null || mobileRaw.isEmpty()) {
			throw new BadRequestException("手机号必填");
		}
		if (nameRaw == null || nameRaw.isEmpty()) {
			throw new BadRequestException("姓名必填");
		}
		String mobile = mobileRaw.trim();
		if (!CN_MOBILE.matcher(mobile).matches()) {
			throw new BadRequestException("请填写正确的手机号");
		}
		String name = nameRaw;

		String mobileStoredForQuery = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile);
		LambdaQueryWrapper<MembersWhitelist> w = new LambdaQueryWrapper<MembersWhitelist>()
				.eq(MembersWhitelist::getCompanyId, companyId)
				.eq(MembersWhitelist::getMobile, mobileStoredForQuery)
				.last("LIMIT 1");
		MembersWhitelist existing = membersWhitelistMapper.selectOne(w);
		if (existing != null) {
			throw new ResourceException("该手机号已被使用");
		}

		long nowSec = Instant.now().getEpochSecond();
		MembersWhitelist row = new MembersWhitelist();
		row.setCompanyId(companyId);
		row.setMobile(LegacyFixedMobileEncrypt.fixedEncryptMobile(mobile));
		row.setName(LegacyFixedMobileEncrypt.fixedEncryptMobile(name));
		row.setCreated(nowSec);
		row.setUpdated(nowSec);

		try {
			membersWhitelistMapper.insert(row);
		} catch (DataIntegrityViolationException e) {
			throw new ResourceException("该手机号已被使用");
		}

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
