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

package cn.shopex.ecshopx.selfservice.service;

import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RegistrationActivityFrontUserGradeResolveService {

	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberAccountService memberAccountService;

	public RegistrationActivityFrontUserGradeResolveService(
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberAccountService memberAccountService) {
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberAccountService = memberAccountService;
	}

	public String resolveUserGradeToken(long userId, long companyId) {
		Map<String, Object> vip = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		Object vipType = vip.get("vip_type");
		if (Boolean.TRUE.equals(vip.get("valid"))
				&& Boolean.TRUE.equals(vip.get("is_vip"))
				&& vipType != null
				&& StringUtils.hasText(String.valueOf(vipType).trim())) {
			return String.valueOf(vipType).trim();
		}
		Map<String, Object> member = memberAccountService.getMemberInfo(userId, companyId);
		Object gradeId = member.get("grade_id");
		String token = gradeId == null ? "" : String.valueOf(gradeId).trim();
		if (!StringUtils.hasText(token) || "0".equals(token)) {
			return "";
		}
		return token;
	}
}
