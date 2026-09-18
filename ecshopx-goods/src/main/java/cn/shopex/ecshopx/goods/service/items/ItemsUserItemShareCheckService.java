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

package cn.shopex.ecshopx.goods.service.items;

import cn.shopex.ecshopx.companys.domain.setting.ItemShareSettingView;
import cn.shopex.ecshopx.companys.service.setting.ItemShareSettingRedisService;
import cn.shopex.ecshopx.kaquan.service.vipgrade.VipGradeUserVipGradeGetService;
import cn.shopex.ecshopx.members.service.account.MemberAccountService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ItemsUserItemShareCheckService {

	private final ItemShareSettingRedisService itemShareSettingRedisService;
	private final MemberAccountService memberAccountService;
	private final VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService;

	public ItemsUserItemShareCheckService(
			ItemShareSettingRedisService itemShareSettingRedisService,
			MemberAccountService memberAccountService,
			VipGradeUserVipGradeGetService vipGradeUserVipGradeGetService) {
		this.itemShareSettingRedisService = itemShareSettingRedisService;
		this.memberAccountService = memberAccountService;
		this.vipGradeUserVipGradeGetService = vipGradeUserVipGradeGetService;
	}

	public Map<String, Object> checkUserItemShare(long companyId, long userId) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("status", false);

		ItemShareSettingView setting = itemShareSettingRedisService.getEffective(companyId);
		if (!setting.isOpen()) {
			result.put("status", true);
			return result;
		}

		List<String> validGrade = setting.getValidGrade() != null ? setting.getValidGrade() : List.of();

		Map<String, Object> memberInfo = memberAccountService.getMemberInfo(userId, companyId);
		Object gradeIdObj = memberInfo != null ? memberInfo.get("grade_id") : null;
		if (gradeIdObj != null && validGradeContains(validGrade, gradeIdObj)) {
			result.put("status", true);
			return result;
		}

		Map<String, Object> vipMap = vipGradeUserVipGradeGetService.userVipGradeGet(companyId, userId, true);
		for (String gradeType : vipMap.keySet()) {
			if (gradeType != null && validGradeContains(validGrade, gradeType)) {
				result.put("status", true);
				break;
			}
		}

		if (!Boolean.TRUE.equals(result.get("status"))) {
			result.put("msg", setting.getMsg());
			result.put("page", setting.getPage());
		}

		return result;
	}

	private static boolean validGradeContains(List<String> validGrade, Object candidate) {
		if (candidate == null) {
			return false;
		}
		String c = String.valueOf(candidate).trim();
		for (String g : validGrade) {
			if (c.equals(String.valueOf(g))) {
				return true;
			}
		}
		return false;
	}
}
