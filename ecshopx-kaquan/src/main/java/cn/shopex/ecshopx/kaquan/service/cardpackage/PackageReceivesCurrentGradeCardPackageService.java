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

package cn.shopex.ecshopx.kaquan.service.cardpackage;

import cn.shopex.ecshopx.common.exception.ResourceException;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PackageReceivesCurrentGradeCardPackageService {

	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;
	private final MemberAccountService memberAccountService;
	private final CardPackageSetTriggerPackageService cardPackageSetTriggerPackageService;

	public PackageReceivesCurrentGradeCardPackageService(VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService,
			MemberAccountService memberAccountService,
			CardPackageSetTriggerPackageService cardPackageSetTriggerPackageService) {
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
		this.memberAccountService = memberAccountService;
		this.cardPackageSetTriggerPackageService = cardPackageSetTriggerPackageService;
	}

	public String currentGardCardPackage(long companyId, long userId) {
		Map<String, Object> vipGrade = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, false);
		long gradeId;
		String type;
		if (Boolean.TRUE.equals(vipGrade.get("is_vip"))) {
			gradeId = longFrom(vipGrade.get("vip_grade_id"));
			type = "vip_grade";
		} else {
			Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
			if (memberInfo == null || memberInfo.isEmpty()) {
				throw new ResourceException("会员信息异常");
			}
			gradeId = longFrom(memberInfo.get("grade_id"));
			if (gradeId <= 0L) {
				throw new ResourceException("会员信息异常");
			}
			type = "grade";
		}
		cardPackageSetTriggerPackageService.triggerPackage(companyId, userId, gradeId, type, false);
		return type;
	}

	private static long longFrom(Object raw) {
		if (raw == null) {
			return 0L;
		}
		if (raw instanceof Number n) {
			return n.longValue();
		}
		try {
			return Long.parseLong(raw.toString().trim());
		} catch (NumberFormatException e) {
			return 0L;
		}
	}
}
