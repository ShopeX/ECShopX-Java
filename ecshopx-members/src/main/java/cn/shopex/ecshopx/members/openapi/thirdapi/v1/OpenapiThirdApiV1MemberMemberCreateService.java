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

package cn.shopex.ecshopx.members.openapi.thirdapi.v1;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.domain.Members;
import cn.shopex.ecshopx.members.domain.MembersInfo;
import cn.shopex.ecshopx.members.mapper.MembersInfoMapper;
import cn.shopex.ecshopx.members.mapper.MembersMapper;
import cn.shopex.ecshopx.members.security.LegacyFixedMobileEncrypt;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.members.service.admin.MemberUserCardCodeAllocateService;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenapiThirdApiV1MemberMemberCreateService {

	private static final String PASSWORD_POOL =
			"QWERTYUIOPASDFGHJKLZXCVBNM1234567890qwertyuiopasdfghjklzxcvbnm";

	private final MemberAccountService memberAccountService;
	private final MembersMapper membersMapper;
	private final MembersInfoMapper membersInfoMapper;
	private final MemberUserCardCodeAllocateService memberUserCardCodeAllocateService;

	public OpenapiThirdApiV1MemberMemberCreateService(
			MemberAccountService memberAccountService,
			MembersMapper membersMapper,
			MembersInfoMapper membersInfoMapper,
			MemberUserCardCodeAllocateService memberUserCardCodeAllocateService) {
		this.memberAccountService = memberAccountService;
		this.membersMapper = membersMapper;
		this.membersInfoMapper = membersInfoMapper;
		this.memberUserCardCodeAllocateService = memberUserCardCodeAllocateService;
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> executeOpenapiMemberCreate(long companyId, String mobileOriginal) {
		if (mobileOriginal == null || mobileOriginal.isEmpty()) {
			throw new ResourceException("手机号必填");
		}

		Members existing = memberAccountService.findMemberByCompanyAndMobile(companyId, mobileOriginal);
		if (existing != null) {
			throw new ResourceException("当前手机号已经是会员");
		}

		String mobilePlain = mobileOriginal.trim();
		String mobileStored = LegacyFixedMobileEncrypt.fixedEncryptMobile(mobilePlain);
		String userCardCode = memberUserCardCodeAllocateService.allocateCode();
		String plainPassword10 = randomPassword10Plain();
		Long gradeId = membersMapper.selectDefaultGradeId(String.valueOf(companyId));

		long now = System.currentTimeMillis() / 1000L;
		ZonedDateTime z = ZonedDateTime.now(ZoneId.systemDefault());

		try {
			Members m = new Members();
			m.setCompanyId(companyId);
			m.setMobile(mobileStored);
			m.setPassword(plainPassword10);
			m.setUserCardCode(userCardCode);
			if (gradeId != null) {
				m.setGradeId(gradeId);
			}
			m.setCreated(now);
			m.setUpdated(now);
			m.setCreatedYear(z.getYear());
			m.setCreatedMonth(z.getMonthValue());
			m.setCreatedDay(z.getDayOfMonth());
			m.setDisabled(false);

			membersMapper.insert(m);
			long userId = m.getUserId();

			MembersInfo info = new MembersInfo();
			info.setUserId(userId);
			info.setCompanyId(companyId);
			info.setSex(0);
			info.setCreated(now);
			info.setUpdated(now);
			membersInfoMapper.insert(info);

			return successData(mobileOriginal, userId);
		} catch (Exception e) {
			throw new ResourceException("保存数据错误");
		}
	}

	private static String randomPassword10Plain() {
		char[] pool = PASSWORD_POOL.toCharArray();
		ThreadLocalRandom r = ThreadLocalRandom.current();
		for (int i = pool.length - 1; i > 0; i--) {
			int j = r.nextInt(i + 1);
			char t = pool[i];
			pool[i] = pool[j];
			pool[j] = t;
		}
		String shuffled = new String(pool);
		int start = 5;
		int len = 10;
		if (shuffled.length() < start + len) {
			return shuffled;
		}
		return shuffled.substring(start, start + len);
	}

	private static Map<String, Object> successData(String mobileOriginal, long userId) {
		LinkedHashMap<String, Object> data = new LinkedHashMap<>();
		data.put("mobile", mobileOriginal);
		data.put("uid", userId);
		return data;
	}
}
